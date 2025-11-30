(ns interpolation.format
  "Output formatting utilities."
  (:require [clojure.string :as str]))

(defn fmt-num
  "Format number: remove trailing zeros after decimal point, always use dot."
  [x]
  (if (== x (Math/floor x))
    (str (long x))
    (let [s (String/format java.util.Locale/US "%.6f" (into-array Object [(double x)]))]
      (str/replace s #"(\.\d*?)0+$" "$1"))))

(defn fmt-point
  "Format a single interpolation result point."
  [algo x y]
  (format "%s: %s %s" (name algo) (fmt-num x) (fmt-num y)))

(defn print-points!
  "Print points and flush output."
  [points]
  (doseq [[algo x y] points]
    (println (fmt-point algo x y)))
  (flush))

