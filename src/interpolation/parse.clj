(ns interpolation.parse
  "Input parsing and validation."
  (:require [clojure.string :as str]))

(defn parse-point
  "Parse a line into [x y] point. Returns nil if parsing fails."
  [line]
  (let [line (str/trim line)]
    (when (seq line)
      (try
        (let [[xs ys] (cond
                        (str/includes? line ";") (str/split line #";")
                        (str/includes? line "\t") (str/split line #"\t")
                        :else (str/split line #"\s+"))]
          (when (and xs ys)
            [(Double/parseDouble (str/trim xs))
             (Double/parseDouble (str/trim ys))]))
        (catch Exception _
          (binding [*out* *err*]
            (println (str "Warning: cannot parse line: \"" line "\"")))
          nil)))))

(defn validate-order!
  "Check that new point has x greater than previous point's x.
   Prints warning to stderr if not sorted."
  [prev-point new-point]
  (when (and prev-point new-point)
    (let [[prev-x _] prev-point
          [new-x _] new-point]
      (when (>= prev-x new-x)
        (binding [*out* *err*]
          (println (String/format java.util.Locale/US
                                  "Warning: input not sorted by x: %.6g >= %.6g"
                                  (into-array Object [prev-x new-x]))))))))

