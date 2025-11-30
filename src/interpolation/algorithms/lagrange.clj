(ns interpolation.algorithms.lagrange
  "Lagrange interpolation algorithm.")

(defn lagrange-basis
  "Compute Lagrange basis polynomial L_i(x) for given points and index i."
  [points i x]
  (let [n (count points)
        xi (first (nth points i))]
    (reduce (fn [acc j]
              (if (= i j)
                acc
                (let [xj (first (nth points j))]
                  (* acc (/ (- x xj) (- xi xj))))))
            1.0
            (range n))))

(defn lagrange-poly
  "Build Lagrange polynomial function from points."
  [points]
  (let [n (count points)]
    (fn [x]
      (reduce (fn [acc i]
                (let [yi (second (nth points i))
                      Li (lagrange-basis points i x)]
                  (+ acc (* yi Li))))
              0.0
              (range n)))))

