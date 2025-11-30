(ns interpolation.grid-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.grid :as grid]
            [interpolation.test-helpers :refer [approx=]]))

(deftest grid-test
  (testing "basic grid generation"
    (is (= [0.0 1.0 2.0] (vec (grid/grid 0.0 2.0 1.0))))
    (is (= [0.0 0.5 1.0 1.5 2.0] (vec (grid/grid 0.0 2.0 0.5)))))

  (testing "exclusive mode"
    (is (= [0.0 1.0] (vec (grid/grid 0.0 2.0 1.0 false))))
    (is (= [0.0 0.5 1.0 1.5] (vec (grid/grid 0.0 2.0 0.5 false)))))

  (testing "single point"
    (is (= [1.0] (vec (grid/grid 1.0 1.0 0.5)))))

  (testing "fractional steps"
    (let [result (vec (grid/grid 0.0 1.0 0.3))]
      (is (= 4 (count result)))
      (is (approx= 0.0 (first result)))
      (is (approx= 0.9 (nth result 3) 1e-6)))))

