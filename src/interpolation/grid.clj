(ns interpolation.grid
  "X-grid generation for interpolation.")

(defn grid
  "Generate sequence of x values from start to end with given step.
   If inclusive? is true, includes end point; otherwise excludes it."
  ([x-start x-end step]
   (grid x-start x-end step true))
  ([x-start x-end step inclusive?]
   (let [eps (* 1e-9 (max 1.0 (Math/abs x-end)))
         check (if inclusive?
                 #(<= % (+ x-end eps))
                 #(< % (- x-end eps)))]
     (->> x-start
          (iterate #(+ % step))
          (take-while check)))))

