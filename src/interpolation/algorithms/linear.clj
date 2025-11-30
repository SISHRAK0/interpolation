(ns interpolation.algorithms.linear
  "Linear interpolation algorithm."
  (:require [interpolation.grid :refer [grid]]))

(defn linear-fn
  "Returns linear interpolation function for segment [p1, p2]."
  [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1)
        m  (if (zero? dx) 0.0 (/ (- y2 y1) dx))]
    (fn [x]
      (+ y1 (* m (- x x1))))))

(defn make-state
  "Create initial state for linear interpolation."
  [step _window-size verbose?]
  {:step step
   :verbose? verbose?
   :prev-point nil
   :prev-prev-point nil
   :last-x nil})

(defn process
  "Process one point with linear interpolation.
   Returns [new-state, output-seq].
   Outputs interpolated points for segment [prev-point, point), not including point itself."
  [state point]
  (let [{:keys [step prev-point last-x verbose?]} state
        [px _] point]
    (when verbose?
      (binding [*out* *err*]
        (println (String/format java.util.Locale/US "[linear] received point: (%.4g, %.4g)"
                                (into-array Object [(first point) (second point)])))))
    (if prev-point
      (let [[x1 _] prev-point
            f (linear-fn prev-point point)
            start-x (if last-x (+ last-x step) x1)
            xs (grid start-x px step false)
            output (vec (for [x xs] [:linear x (f x)]))
            new-last-x (if (seq output) (second (last output)) last-x)]
        [{:step step :verbose? verbose? :prev-point point :prev-prev-point prev-point :last-x new-last-x} output])
      [{:step step :verbose? verbose? :prev-point point :prev-prev-point nil :last-x nil} []])))

(defn finalize
  "Finalize linear interpolation at EOF.
   Returns output-seq for remaining points up to and including last point."
  [state]
  (let [{:keys [step prev-point last-x prev-prev-point verbose?]} state]
    (when verbose?
      (binding [*out* *err*]
        (println "[linear] finalizing...")))
    (if (and prev-point prev-prev-point)
      (let [f (linear-fn prev-prev-point prev-point)
            [px py] prev-point
            start-x (if last-x (+ last-x step) (first prev-prev-point))
            xs (grid start-x px step true)
            output (vec (for [x xs] [:linear x (f x)]))]
        (if (or (empty? output) (< (second (last output)) (- px 1e-9)))
          (conj output [:linear px py])
          output))
      (if prev-point
        [[:linear (first prev-point) (second prev-point)]]
        []))))

