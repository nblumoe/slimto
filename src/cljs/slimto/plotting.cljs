(ns slimto.plotting
  (:require [slimto.utils.time :as time]))

(defn circle-component [x y color]
  [:circle {:cx x :cy y :r 2 :style {:fill color
                                       :stroke "#EEE"
                                       :stroke-width "0.5px"
                                       }}])

(defn plot-entry [entry color]
  (let [x (:x entry)
        y (:y entry)]
    (circle-component x y color)))

(defn timeseries-plot [title data key]
  (let [days      (map time/str->days (keys data))
        min-day   (apply min days)
        max-day   (apply max days)
        day-range (- max-day min-day)]
    [:div
     (.log js/console data)
     [:h5 title]
     [:svg#progress-plot {:preserveAspectRatio "xMidYMid"
                          :viewBox (clojure.string/join " " [min-day 0 (+  day-range 1) 100])}
      [:g {:transform (str "scale(1,-1), translate(0,-100)")}
       (map #(plot-entry {:x (time/str->days (first %)) :y (key (second %))} "red") data)]
      ]]))

(defn activity-plot [activities]
  (timeseries-plot "Aktivitäten" activities :activity))

(defn weight-plot [entries goals]
  (let [days      (map time/str->days (concat (keys entries) (keys goals)))
        min-day   (apply min days)
        max-day   (apply max days)
        day-range (- max-day min-day)]
    [:div
     [:h5 "Gewicht"]
     [:svg#progress-plot {:preserveAspectRatio "xMidYMid"
                          :viewBox (clojure.string/join " " [min-day 0 (+  day-range 1) 50])}
      [:g {:transform (str "scale(1,-1), translate(0,-100)")}
       (map #(plot-entry {:x (time/str->days (first %)) :y (:weight (second %))} "red")  entries)
       (map #(plot-entry {:x (time/str->days (first %)) :y (:weight (second %))} "blue") goals)]
      ]]))
