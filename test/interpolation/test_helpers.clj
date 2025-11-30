(ns interpolation.test-helpers
  "Common test helper functions.")

(defn approx=
  "Check if two numbers are approximately equal within epsilon."
  ([a b] (approx= a b 1e-9))
  ([a b eps]
   (< (Math/abs (- (double a) (double b))) eps)))

(defn points-approx=
  "Check if two sequences of points are approximately equal."
  [expected actual]
  (and (= (count expected) (count actual))
       (every? (fn [[e a]]
                 (and (= (first e) (first a))
                      (approx= (second e) (second a) 1e-6)
                      (approx= (nth e 2) (nth a 2) 1e-6)))
               (map vector expected actual))))

