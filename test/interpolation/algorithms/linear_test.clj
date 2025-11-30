(ns interpolation.algorithms.linear-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.algorithms.linear :as linear]
            [interpolation.test-helpers :refer [approx=]]))

(deftest linear-fn-test
  (testing "simple linear function y = x"
    (let [f (linear/linear-fn [0.0 0.0] [1.0 1.0])]
      (is (approx= 0.0 (f 0.0)))
      (is (approx= 0.5 (f 0.5)))
      (is (approx= 1.0 (f 1.0)))))

  (testing "linear function y = 2x + 1"
    (let [f (linear/linear-fn [0.0 1.0] [1.0 3.0])]
      (is (approx= 1.0 (f 0.0)))
      (is (approx= 2.0 (f 0.5)))
      (is (approx= 3.0 (f 1.0)))))

  (testing "horizontal line"
    (let [f (linear/linear-fn [0.0 5.0] [10.0 5.0])]
      (is (approx= 5.0 (f 0.0)))
      (is (approx= 5.0 (f 5.0)))
      (is (approx= 5.0 (f 10.0)))))

  (testing "same x values (vertical handling)"
    (let [f (linear/linear-fn [1.0 2.0] [1.0 5.0])]
      (is (approx= 2.0 (f 1.0))))))

(deftest linear-streaming-test
  (testing "process first point - no output"
    (let [state (linear/make-state 0.5 4 false)
          [new-state output] (linear/process state [0.0 0.0])]
      (is (empty? output))
      (is (= [0.0 0.0] (:prev-point new-state)))))

  (testing "process second point - outputs first segment"
    (let [state (linear/make-state 0.5 4 false)
          [state1 _] (linear/process state [0.0 0.0])
          [_state2 output] (linear/process state1 [1.0 1.0])]
      (is (= 2 (count output)))
      (is (= :linear (first (first output))))
      (is (approx= 0.0 (second (first output))))
      (is (approx= 0.5 (second (second output))))))

  (testing "finalize outputs remaining points"
    (let [state (linear/make-state 1.0 4 false)
          [s1 _] (linear/process state [0.0 0.0])
          [s2 _] (linear/process s1 [2.0 4.0])
          final-output (linear/finalize s2)]
      (is (seq final-output))
      (is (= :linear (first (last final-output))))
      (is (approx= 2.0 (second (last final-output)))))))

(deftest linear-edge-cases-test
  (testing "single point linear finalize"
    (let [state (linear/make-state 0.5 4 false)
          [s1 _] (linear/process state [1.0 2.0])
          final (linear/finalize s1)]
      (is (= 1 (count final)))
      (is (= [:linear 1.0 2.0] (first final)))))

  (testing "no points linear finalize"
    (let [state (linear/make-state 0.5 4 false)
          final (linear/finalize state)]
      (is (empty? final))))

  (testing "negative values"
    (let [f (linear/linear-fn [-2.0 -4.0] [2.0 4.0])]
      (is (approx= -4.0 (f -2.0)))
      (is (approx= 0.0 (f 0.0)))
      (is (approx= 4.0 (f 2.0))))))

(deftest linear-properties-test
  (testing "linear interpolation at endpoints"
    (doseq [p1 [[0.0 0.0] [1.0 5.0] [-1.0 2.0]]
            p2 [[2.0 4.0] [3.0 -1.0] [10.0 10.0]]]
      (let [f (linear/linear-fn p1 p2)]
        (is (approx= (second p1) (f (first p1))))
        (is (approx= (second p2) (f (first p2))))))))

