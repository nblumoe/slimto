(ns slimto.plotting
  (:require [slimto.utils.time :as time]))

(defn circle-component [x y color]
  [:circle {:cx x :cy y :r 2 :style {:fill color}}])

(defn plot-entry [entry color]
  (let [x (:date entry)
        y (:weight entry)]
    (circle-component x y color)))

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
       (map #(plot-entry {:date (time/str->days (first %)) :weight (:weight (second %))} "red") entries)
       (map #(plot-entry {:date (time/str->days (first %)) :weight (:weight (second %))} "blue") goals)]
      ]]))
