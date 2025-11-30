(ns interpolation.integration-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.algorithms.linear :as linear]
            [interpolation.algorithms.window :as window]
            [interpolation.test-helpers :refer [approx=]]))

(deftest integration-linear-test
  (testing "full linear interpolation pipeline"
    (let [state (linear/make-state 0.5 4 false)
          points [[0.0 0.0] [1.0 2.0] [2.0 4.0]]

          [final-state all-outputs]
          (reduce (fn [[state acc] point]
                    (let [[new-state output] (linear/process state point)]
                      [new-state (concat acc output)]))
                  [state []]
                  points)

          final-outputs (linear/finalize final-state)
          all-results (concat all-outputs final-outputs)]

      ;; Check that we have outputs
      (is (seq all-results))
      ;; Check that all are linear
      (is (every? #(= :linear (first %)) all-results))
      ;; Check first and last x values
      (is (approx= 0.0 (second (first all-results))))
      (is (approx= 2.0 (second (last all-results))))
      ;; Check y = 2x relationship at some points
      (doseq [[_ x y] all-results]
        (is (approx= (* 2 x) y 1e-6))))))

(deftest integration-quadratic-newton-test
  (testing "Newton correctly interpolates quadratic"
    (let [state (window/make-newton-state 0.5 4 false)
          ;; Points from y = x^2
          points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0] [4.0 16.0]]

          [final-state all-outputs]
          (reduce (fn [[state acc] point]
                    (let [[new-state output] (window/process state point)]
                      [new-state (concat acc output)]))
                  [state []]
                  points)

          final-outputs (window/finalize final-state)
          all-results (concat all-outputs final-outputs)]

      ;; Check that we have outputs
      (is (seq all-results))
      ;; Check y = x^2 relationship
      (doseq [[_ x y] all-results]
        (is (approx= (* x x) y 1e-6))))))

(deftest integration-quadratic-lagrange-test
  (testing "Lagrange correctly interpolates quadratic"
    (let [state (window/make-lagrange-state 0.5 4 false)
          points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0] [4.0 16.0]]

          [final-state all-outputs]
          (reduce (fn [[state acc] point]
                    (let [[new-state output] (window/process state point)]
                      [new-state (concat acc output)]))
                  [state []]
                  points)

          final-outputs (window/finalize final-state)
          all-results (concat all-outputs final-outputs)]

      (is (seq all-results))
      (doseq [[_ x y] all-results]
        (is (approx= (* x x) y 1e-6))))))

(deftest integration-multiple-algorithms-test
  (testing "linear and newton produce results"
    (let [linear-state (linear/make-state 1.0 4 false)
          newton-state (window/make-newton-state 1.0 4 false)
          points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0] [4.0 16.0]]

          process-linear (fn [state point] (linear/process state point))
          process-newton (fn [state point] (window/process state point))

          [final-linear linear-outputs]
          (reduce (fn [[state acc] point]
                    (let [[new-state output] (process-linear state point)]
                      [new-state (concat acc output)]))
                  [linear-state []]
                  points)

          [final-newton newton-outputs]
          (reduce (fn [[state acc] point]
                    (let [[new-state output] (process-newton state point)]
                      [new-state (concat acc output)]))
                  [newton-state []]
                  points)

          all-linear (concat linear-outputs (linear/finalize final-linear))
          all-newton (concat newton-outputs (window/finalize final-newton))]

      (is (seq all-linear))
      (is (seq all-newton))
      (is (every? #(= :linear (first %)) all-linear))
      (is (every? #(= :newton (first %)) all-newton)))))

