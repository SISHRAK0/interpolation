(ns interpolation.stream
  "Stream processing logic for interpolation."
  (:require [interpolation.parse :refer [parse-point validate-order!]]
            [interpolation.format :refer [print-points!]]))

(defn process-stream!
  "Process input stream with given algorithms.
   Algorithms is a vector of {:name, :state, :process-fn, :finalize-fn}."
  [algorithms lines verbose?]
  (loop [lines lines
         states (mapv :state algorithms)
         last-point nil]
    (if-let [line (first lines)]
      (if-let [point (parse-point line)]
        (do
          (validate-order! last-point point)
          (let [results (mapv (fn [algo state]
                                ((:process-fn algo) state point))
                              algorithms states)
                new-states (mapv first results)
                outputs (mapcat second results)]
            (print-points! (sort-by second outputs))
            (recur (rest lines) new-states point)))
        (recur (rest lines) states last-point))
      (do
        (when verbose?
          (binding [*out* *err*]
            (println "[main] EOF reached, finalizing all algorithms...")))
        (let [final-outputs (mapcat (fn [algo state]
                                      ((:finalize-fn algo) state))
                                    algorithms states)]
          (print-points! (sort-by second final-outputs)))))))

