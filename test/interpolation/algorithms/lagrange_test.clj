(ns interpolation.algorithms.lagrange-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.algorithms.lagrange :as lagrange]
            [interpolation.test-helpers :refer [approx=]]))

(deftest lagrange-basis-test
  (testing "basis functions on 3 points"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0]]]
      ;; L_0(0) = 1, L_0(1) = 0, L_0(2) = 0
      (is (approx= 1.0 (lagrange/lagrange-basis points 0 0.0)))
      (is (approx= 0.0 (lagrange/lagrange-basis points 0 1.0)))
      (is (approx= 0.0 (lagrange/lagrange-basis points 0 2.0)))
      ;; L_1(0) = 0, L_1(1) = 1, L_1(2) = 0
      (is (approx= 0.0 (lagrange/lagrange-basis points 1 0.0)))
      (is (approx= 1.0 (lagrange/lagrange-basis points 1 1.0)))
      (is (approx= 0.0 (lagrange/lagrange-basis points 1 2.0))))))

(deftest lagrange-poly-test
  (testing "interpolates given points exactly"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0]]
          f (lagrange/lagrange-poly points)]
      (is (approx= 0.0 (f 0.0)))
      (is (approx= 1.0 (f 1.0)))
      (is (approx= 4.0 (f 2.0)))))

  (testing "quadratic function y = x^2"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0]]
          f (lagrange/lagrange-poly points)]
      (is (approx= 0.25 (f 0.5)))
      (is (approx= 2.25 (f 1.5)))
      (is (approx= 6.25 (f 2.5)))))

  (testing "linear function with 2 points"
    (let [points [[0.0 0.0] [2.0 4.0]]
          f (lagrange/lagrange-poly points)]
      (is (approx= 0.0 (f 0.0)))
      (is (approx= 2.0 (f 1.0)))
      (is (approx= 4.0 (f 2.0))))))

(deftest lagrange-properties-test
  (testing "interpolation passes through all given points"
    (let [points [[0.0 1.0] [1.0 3.0] [2.0 7.0] [3.0 15.0]]
          f (lagrange/lagrange-poly points)]
      (doseq [[x y] points]
        (is (approx= y (f x)))))))

