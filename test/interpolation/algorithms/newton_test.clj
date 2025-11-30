(ns interpolation.algorithms.newton-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.algorithms.newton :as newton]
            [interpolation.algorithms.lagrange :as lagrange]
            [interpolation.test-helpers :refer [approx=]]))

(deftest divided-diffs-test
  (testing "divided differences for y = x"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 2.0]]
          diffs (newton/divided-diffs points)]
      (is (approx= 0.0 (nth diffs 0)))   ; f[x0] = 0
      (is (approx= 1.0 (nth diffs 1)))   ; f[x0,x1] = 1
      (is (approx= 0.0 (nth diffs 2))))) ; f[x0,x1,x2] = 0

  (testing "divided differences for y = x^2"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0]]
          diffs (newton/divided-diffs points)]
      (is (approx= 0.0 (nth diffs 0)))   ; f[x0] = 0
      (is (approx= 1.0 (nth diffs 1)))   ; f[x0,x1] = 1
      (is (approx= 1.0 (nth diffs 2)))))) ; f[x0,x1,x2] = 1

(deftest newton-poly-test
  (testing "interpolates given points exactly"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0]]
          f (newton/newton-poly points)]
      (is (approx= 0.0 (f 0.0)))
      (is (approx= 1.0 (f 1.0)))
      (is (approx= 4.0 (f 2.0)))))

  (testing "quadratic function y = x^2"
    (let [points [[0.0 0.0] [1.0 1.0] [2.0 4.0] [3.0 9.0]]
          f (newton/newton-poly points)]
      (is (approx= 0.25 (f 0.5)))
      (is (approx= 2.25 (f 1.5)))
      (is (approx= 6.25 (f 2.5)))))

  (testing "Newton equals Lagrange"
    (let [points [[0.0 1.0] [1.0 3.0] [2.0 7.0] [3.0 13.0]]
          newton-fn (newton/newton-poly points)
          lagrange-fn (lagrange/lagrange-poly points)]
      (doseq [x [0.0 0.5 1.0 1.5 2.0 2.5 3.0]]
        (is (approx= (newton-fn x) (lagrange-fn x)))))))

(deftest newton-properties-test
  (testing "interpolation passes through all given points"
    (let [points [[0.0 1.0] [1.0 3.0] [2.0 7.0] [3.0 15.0]]
          f (newton/newton-poly points)]
      (doseq [[x y] points]
        (is (approx= y (f x)))))))

