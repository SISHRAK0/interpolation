(ns interpolation.format-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.format :as fmt]))

(deftest fmt-num-test
  (testing "integer values"
    (is (= "0" (fmt/fmt-num 0.0)))
    (is (= "1" (fmt/fmt-num 1.0)))
    (is (= "42" (fmt/fmt-num 42.0)))
    (is (= "-5" (fmt/fmt-num -5.0))))

  (testing "decimal values without trailing zeros"
    (is (= "0.5" (fmt/fmt-num 0.5)))
    (is (= "3.14" (fmt/fmt-num 3.14)))
    (is (= "1.25" (fmt/fmt-num 1.25)))))

(deftest fmt-point-test
  (testing "formats point correctly"
    (is (= "linear: 0 0" (fmt/fmt-point :linear 0.0 0.0)))
    (is (= "newton: 1.5 2.25" (fmt/fmt-point :newton 1.5 2.25)))
    (is (= "lagrange: 3 9" (fmt/fmt-point :lagrange 3.0 9.0)))))

