(ns slimto.plotting
  (:require [slimto.utils.time :as time]))

(defn- plot-viewbox []
  (clojure.string/join " " [-0.2 -1.2 1.4 1.4]))

(defn- svg-circle [x y color]
  [:circle {:cx x :cy y :r "2%"
            :style {:fill color :stroke "#FFF" :opacity 0.65 :stroke-width ".05%"}}])

(defn- svg-square [x y color]
  [:rect {:x x :y y :width "3%" :height "3%"
            :style {:fill color :stroke "#FFF" :opacity 0.65 :stroke-width ".05%"}}])

(defn- plot-circle [entry color]
  (let [x (first entry)
        y (second entry)]
    (svg-circle x y color)))

(defn- plot-square [entry color]
  (let [x (first entry)
        y (second entry)]
    (svg-square x y color)))

(defn- unit-scale [values]
  (let [min (apply min values)
        max (apply max values)]
    (fn [value]
      (/ (- value min) (- max min)))))

(defn- progress-plot [title data-series]
  [:div
   [:h4 title]
   [:svg.progress-plot {:viewBox (plot-viewbox)}
    [:g {:transform (str "scale(1,-1)")}
     data-series]]])

(defn entries-goals-plot [entries goals color days-scale]
  (let [entries-days    (map time/str->days (keys entries))
        goals-days      (map time/str->days (keys goals))
        entries-weights (map :weight (vals entries))
        goals-weights   (map :goal (vals goals))
        all-weights     (concat entries-weights goals-weights)
        weights-scale   (unit-scale all-weights)
        entries-data    (apply map list [(map days-scale entries-days)
                                         (map weights-scale entries-weights)])
        goals-data      (apply map list [(map days-scale goals-days)
                                         (map weights-scale goals-weights)])]
    (concat
      (map #(plot-circle % color) entries-data)
      (map #(plot-square % color) goals-data))))

(defn weight-plot [users-data]
  (let [color-palette ["green" "blue" "red" ]
        user-colors   (zipmap (keys users-data) color-palette)
        all-days      (map time/str->days (reduce-kv (fn [days key value]
                                                       (into days
                                                         (concat
                                                           (keys (get-in value [:entries :weights]))
                                                           (keys (get-in value [:entries :goals]))))) [] users-data))
        days-scale   (unit-scale all-days)]
    (progress-plot "Gewicht"
      (concat
        (map (fn [[key value]] (entries-goals-plot
                                (get-in value [:entries :weights])
                                (get-in value [:entries :goals])
                                (key user-colors)
                                days-scale))
          users-data)))))

(defn activity-plot [data]
  (let [days             (map time/str->days (keys data))
        activities       (map :activity (vals data))
        days-scale       (unit-scale days)
        activities-scale (unit-scale activities)
        activities-data  (apply map list [(map days-scale days)
                                          (map activities-scale activities)])]
    (progress-plot "Aktivitäten"
      (map #(plot-circle % "green") activities-data))))
