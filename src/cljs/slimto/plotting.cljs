(ns slimto.plotting
  (:require [slimto.utils.time :as time]))

(defn- plot-viewbox []
  (clojure.string/join " " [-0.05 -1.05 1.1 1.1]))

(defn- svg-circle [x y color]
  [:circle {:cx x :cy y :r "1.2%"
            :style {:fill color :stroke "#FFF" :opacity 1 :stroke-width ".3%"}}])

(defn- svg-square [x y color]
  [:rect {:x x :y y :width "2%" :height "2%"
            :style {:fill color :stroke "#FFF" :opacity 0.65 :stroke-width ".05%"}}])

(defn- plot-weight [entry color]
  (let [x (first entry)
        y (second entry)]
    (svg-circle x y color)))

(defn- plot-weights [weights color]
  (let [coords-list     (map #(clojure/string.join "," %) weights)
        polyline-coords (clojure/string.join " " coords-list)]
    [:g
     [:polyline {:points polyline-coords
                 :fill "none"
                 :stroke color
                 :stroke-width "1%"
                 :opacity "0.3"}]
     (map #(plot-weight % color) weights)
     ]))

(defn- plot-goal [entry color]
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
      [(plot-weights entries-data color)]
      (map #(plot-goal % color) goals-data))))

(defn user-colors [users]
  (let [color-palette ["green" "blue" "red"]]
    (zipmap users color-palette)))

(defn users-legend [users-data]
  (let [colors (user-colors (keys users-data))
        names (reduce-kv (fn [users key value]
                           (into users {(:name value)
                                        (key colors)})) [] users-data)]
    (map (fn [col] [:span.legend-names {:style {:color (str (last col))}}
                   (str (first col))]) names)))

(defn weight-plot [users-data]
  (let [names    (keys users-data)
        colors   (user-colors names)
        all-days (map time/str->days (reduce-kv (fn [days key value]
                                                       (into days
                                                         (concat
                                                           (keys (get-in value [:entries :weights]))
                                                           (keys (get-in value [:entries :goals]))))) [] users-data))
        days-scale   (unit-scale all-days)]
    [:div
     (progress-plot "Gewicht"
       (concat
         (map (fn [[key value]] (entries-goals-plot
                                 (into (sorted-map-by <)
                                   (get-in value [:entries :weights]))
                                 (get-in value [:entries :goals])
                                 (key colors)
                                 days-scale))
           users-data)))
     (users-legend users-data)]))

(defn activity-plot [data]
  (let [days             (map time/str->days (keys data))
        activities       (map :activity (vals data))
        days-scale       (unit-scale days)
        activities-scale (unit-scale activities)
        activities-data  (apply map list [(map days-scale days)
                                          (map activities-scale activities)])]
    (progress-plot "Aktivitäten"
      (map #(plot-weight % "green") activities-data))))
