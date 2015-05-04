(ns slimto.plotting
  (:require [slimto.utils.time :as time]))

(defn- svg-circle [x y color]
  [:circle {:cx x :cy y :r "3%"
            :style {:fill color :stroke "#FFF" :stroke-width ".75%"}}])

(defn- plot-circle [entry color]
  (let [x (first entry)
        y (second entry)]
    (svg-circle x y color)))

(defn- unit-scale [values]
  (let [min (apply min values)
        max (apply max values)]
    (fn [value]
      (/ (- value min) (- max min)))))

(defn- plot-viewbox []
  (clojure.string/join " " [-0.2 -1.2 1.4 1.4]))

(defn- progress-plot [title data-series]
  [:div
   [:h4 title]
   [:svg.progress-plot {:viewBox (plot-viewbox)}
    [:g {:transform (str "scale(1,-1)")}
     data-series]]])

(defn weight-plot [entries goals]
  (let [entries-days    (map time/str->days (keys entries))
        goals-days      (map time/str->days (keys goals))
        entries-weights (map :weight (vals entries))
        goals-weights   (map :goal (vals goals))
        all-days        (concat entries-days goals-days)
        all-weights     (concat entries-weights goals-weights)
        days-scale      (unit-scale all-days)
        weights-scale   (unit-scale all-weights)
        entries-data    (apply map list [(map days-scale entries-days)
                                         (map weights-scale entries-weights)])
        goals-data      (apply map list [(map days-scale goals-days)
                                         (map weights-scale goals-weights)])]
    (progress-plot "Gewicht"
      (concat
        (map #(plot-circle % "red") entries-data)
        (map #(plot-circle % "blue") goals-data)))))

(defn activity-plot [data]
  (let [days             (map time/str->days (keys data))
        activities       (map :activity (vals data))
        days-scale       (unit-scale days)
        activities-scale (unit-scale activities)
        activities-data  (apply map list [(map days-scale days)
                                          (map activities-scale activities)])]
    (progress-plot "Aktivitäten"
      (map #(plot-circle % "green") activities-data))))
