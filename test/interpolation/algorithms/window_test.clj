(ns interpolation.algorithms.window-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.algorithms.window :as window]
            [interpolation.test-helpers :refer [approx=]]))

(deftest newton-streaming-test
  (testing "no output until window is full"
    (let [state (window/make-newton-state 0.5 3 false)
          [s1 out1] (window/process state [0.0 0.0])
          [_s2 out2] (window/process s1 [1.0 1.0])]
      (is (empty? out1))
      (is (empty? out2))))

  (testing "output when window is full"
    (let [state (window/make-newton-state 0.5 3 false)
          [s1 _] (window/process state [0.0 0.0])
          [s2 _] (window/process s1 [1.0 1.0])
          [_s3 out3] (window/process s2 [2.0 4.0])]
      (is (seq out3))
      (is (every? #(= :newton (first %)) out3))))

  (testing "finalize outputs remaining points"
    (let [state (window/make-newton-state 1.0 3 false)
          [s1 _] (window/process state [0.0 0.0])
          [s2 _] (window/process s1 [1.0 1.0])
          [s3 _] (window/process s2 [2.0 4.0])
          final (window/finalize s3)]
      (is (seq final))
      (is (approx= 2.0 (second (last final)))))))

(deftest lagrange-streaming-test
  (testing "produces same results as Newton for same points"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0]]
          newton-state (window/make-newton-state 0.5 3 false)
          lagrange-state (window/make-lagrange-state 0.5 3 false)

          process-all (fn [init-state]
                        (let [[final-state outputs]
                              (reduce (fn [[state acc] point]
                                        (let [[new-state out] (window/process state point)]
                                          [new-state (concat acc out)]))
                                      [init-state []]
                                      points)
                              final-out (window/finalize final-state)]
                          (concat outputs final-out)))

          newton-results (process-all newton-state)
          lagrange-results (process-all lagrange-state)]
      ;; Both should produce outputs
      (is (seq newton-results))
      (is (seq lagrange-results))
      ;; Same number of outputs
      (is (= (count newton-results) (count lagrange-results)))
      ;; Same x and y values (different algo names)
      (is (every? true? (map (fn [n l]
                               (and (approx= (second n) (second l) 1e-6)
                                    (approx= (nth n 2) (nth l 2) 1e-6)))
                             newton-results lagrange-results))))))

(deftest window-edge-cases-test
  (testing "insufficient points for Newton"
    (let [state (window/make-newton-state 0.5 4 false)
          [s1 _] (window/process state [0.0 0.0])
          [s2 _] (window/process s1 [1.0 1.0])
          final (window/finalize s2)]
      (is (empty? final)))))

