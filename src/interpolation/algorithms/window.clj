(ns interpolation.algorithms.window
  "Window-based streaming interpolation (for Newton and Lagrange)."
  (:require [interpolation.grid :refer [grid]]
            [interpolation.algorithms.newton :refer [newton-poly]]
            [interpolation.algorithms.lagrange :refer [lagrange-poly]]))

(defn make-state
  "Create initial state for window-based interpolation (Newton/Lagrange)."
  [algo-name step window-size verbose? poly-fn]
  {:algo algo-name
   :step step
   :window-size window-size
   :verbose? verbose?
   :poly-fn poly-fn
   :points []
   :last-x nil
   :first-window? true})

(defn process
  "Process one point with window-based interpolation.
   Returns [new-state, output-seq]."
  [state point]
  (let [{:keys [algo step window-size points last-x first-window? verbose? poly-fn]} state
        points' (conj points point)
        n (count points')]
    (when verbose?
      (binding [*out* *err*]
        (println (String/format java.util.Locale/US "[%s] received point: (%.4g, %.4g), buffer size: %d"
                                (into-array Object [(name algo) (first point) (second point) n])))))
    (if (< n window-size)
      [{:algo algo :step step :window-size window-size :verbose? verbose? :poly-fn poly-fn
        :points points' :last-x last-x :first-window? true} []]
      (let [win (vec (take-last window-size points'))
            xs-win (mapv first win)
            mid-idx (quot window-size 2)
            x-mid (nth xs-win mid-idx)
            start-x (if first-window? (first xs-win) (+ last-x step))
            xs (grid start-x x-mid step)
            f (poly-fn win)
            output (vec (for [x xs] [algo x (f x)]))
            new-last-x (if (seq output) (second (last output)) last-x)]
        (when verbose?
          (binding [*out* *err*]
            (println (String/format java.util.Locale/US "[%s] window: %s, outputting %d points"
                                    (into-array Object [(name algo) (pr-str (mapv first win)) (count output)])))))
        [{:algo algo :step step :window-size window-size :verbose? verbose? :poly-fn poly-fn
          :points points' :last-x new-last-x :first-window? false}
         output]))))

(defn finalize
  "Finalize window-based interpolation at EOF.
   Returns output-seq for remaining points."
  [state]
  (let [{:keys [algo step window-size points last-x verbose? poly-fn]} state]
    (when verbose?
      (binding [*out* *err*]
        (println (String/format java.util.Locale/US "[%s] finalizing with %d points..."
                                (into-array Object [(name algo) (count points)])))))
    (if (>= (count points) window-size)
      (let [win (vec (take-last window-size points))
            xs-win (mapv first win)
            x-max (last xs-win)
            start-x (if last-x (+ last-x step) (first xs-win))
            xs (grid start-x x-max step)
            f (poly-fn win)
            output (vec (for [x xs] [algo x (f x)]))]
        (if (or (empty? output) (< (second (last output)) (- x-max 1e-9)))
          (conj output [algo x-max (f x-max)])
          output))
      [])))

;; Factory functions
(defn make-newton-state [step window-size verbose?]
  (make-state :newton step window-size verbose? newton-poly))

(defn make-lagrange-state [step window-size verbose?]
  (make-state :lagrange step window-size verbose? lagrange-poly))

