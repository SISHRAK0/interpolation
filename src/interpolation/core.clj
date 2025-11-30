(ns interpolation.core
  "Main entry point and CLI handling."
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [interpolation.stream :refer [process-stream!]]
            [interpolation.algorithms.linear :as linear]
            [interpolation.algorithms.window :as window]))

;; -----------------------------------------------------------------------------
;; CLI options
;; -----------------------------------------------------------------------------

(def cli-options
  [["-l" "--linear" "Use linear interpolation" :default false]
   ["-L" "--lagrange" "Use Lagrange interpolation" :default false]
   ["-N" "--newton" "Use Newton interpolation" :default false]
   ["-n" "--points N" "Number of points for interpolation window (default: 4)"
    :parse-fn parse-long
    :default 4
    :validate [#(>= % 2) "Must be at least 2"]]
   ["-s" "--step STEP" "Sampling step for result points"
    :parse-fn #(Double/parseDouble %)
    :validate [pos? "Must be positive number"]]
   ["-v" "--verbose" "Enable verbose output for debugging" :default false]
   ["-h" "--help" "Show help"]])

(defn usage [summary]
  (->> ["Stream interpolation - Lab 3"
        ""
        "Usage: clj -M -m interpolation.core [options]"
        ""
        "Options:"
        summary
        ""
        "Examples:"
        "  echo -e '0 0\\n1 1\\n2 4' | clj -M -m interpolation.core --linear --step 0.5"
        "  clj -M -m interpolation.core --newton -n 4 --step 0.5 < data.txt"
        "  clj -M -m interpolation.core --linear --lagrange --step 1 < data.txt"
        ""
        "Input format: lines with \"x y\", \"x;y\" or \"x\\ty\", sorted by x ascending."
        "Output format: <algorithm>: <x> <y>"]
       (str/join \newline)))

;; -----------------------------------------------------------------------------
;; Main
;; -----------------------------------------------------------------------------

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        {:keys [linear lagrange newton points step help verbose]} options]
    (cond
      help
      (do (println (usage summary)) (System/exit 0))

      (seq errors)
      (do (binding [*out* *err*]
            (doseq [e errors] (println e))
            (println)
            (println (usage summary)))
          (System/exit 1))

      (and (not linear) (not lagrange) (not newton))
      (do (binding [*out* *err*]
            (println "Error: Specify at least one algorithm: --linear, --lagrange, or --newton")
            (println)
            (println (usage summary)))
          (System/exit 1))

      (nil? step)
      (do (binding [*out* *err*]
            (println "Error: Sampling step must be specified with --step")
            (println)
            (println (usage summary)))
          (System/exit 1)))

    (when verbose
      (binding [*out* *err*]
        (println (String/format java.util.Locale/US "[config] algorithms: linear=%s lagrange=%s newton=%s"
                                (into-array Object [linear lagrange newton])))
        (println (String/format java.util.Locale/US "[config] step=%.6g, window-size=%d"
                                (into-array Object [step points])))))

    (let [algorithms (cond-> []
                       linear (conj {:name :linear
                                     :state (linear/make-state step points verbose)
                                     :process-fn linear/process
                                     :finalize-fn linear/finalize})
                       lagrange (conj {:name :lagrange
                                       :state (window/make-lagrange-state step points verbose)
                                       :process-fn window/process
                                       :finalize-fn window/finalize})
                       newton (conj {:name :newton
                                     :state (window/make-newton-state step points verbose)
                                     :process-fn window/process
                                     :finalize-fn window/finalize}))
          lines (line-seq (java.io.BufferedReader. *in*))]
      (process-stream! algorithms lines verbose))))
