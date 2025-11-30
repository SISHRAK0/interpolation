(ns interpolation.algorithms.newton
  "Newton interpolation algorithm with divided differences.")

(defn divided-diffs
  "Compute divided differences for Newton interpolation."
  [points]
  (let [n  (count points)
        xs (mapv first points)
        ys (mapv second points)]
    (loop [table [ys]
           k 1]
      (if (= k n)
        (mapv first table)
        (let [prev (peek table)
              next-row (mapv (fn [i]
                               (/ (- (nth prev (inc i)) (nth prev i))
                                  (- (nth xs (+ i k)) (nth xs i))))
                             (range (- n k)))]
          (recur (conj table next-row) (inc k)))))))

(defn newton-poly
  "Build Newton polynomial function from points."
  [points]
  (let [xs    (mapv first points)
        coefs (divided-diffs points)
        n     (count xs)]
    (fn [x]
      (loop [i 0, acc 0.0, term 1.0]
        (if (= i n)
          acc
          (recur (inc i)
                 (+ acc (* (nth coefs i) term))
                 (* term (- x (nth xs i)))))))))

