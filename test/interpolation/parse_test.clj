(ns interpolation.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [interpolation.parse :as parse]))

(deftest parse-point-test
  (testing "space-separated values"
    (is (= [1.0 2.0] (parse/parse-point "1 2")))
    (is (= [0.0 0.0] (parse/parse-point "0 0")))
    (is (= [3.14 2.71] (parse/parse-point "3.14 2.71")))
    (is (= [-1.5 -2.5] (parse/parse-point "-1.5 -2.5"))))

  (testing "semicolon-separated values"
    (is (= [1.0 2.0] (parse/parse-point "1;2")))
    (is (= [3.5 4.5] (parse/parse-point "3.5;4.5"))))

  (testing "tab-separated values"
    (is (= [1.0 2.0] (parse/parse-point "1\t2")))
    (is (= [5.0 6.0] (parse/parse-point "5\t6"))))

  (testing "whitespace handling"
    (is (= [1.0 2.0] (parse/parse-point "  1 2  ")))
    (is (= [1.0 2.0] (parse/parse-point "1   2"))))

  (testing "empty and invalid input"
    (is (nil? (parse/parse-point "")))
    (is (nil? (parse/parse-point "   ")))
    (is (nil? (parse/parse-point "not a number")))))

