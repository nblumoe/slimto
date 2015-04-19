(ns slimto.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]))

(defstyles screen
  [:.btn
   [:span.glyphicon
    {:margin-right "10px"}]]
  [:svg#progress-plot
   {:max-height "200px"
    :height "auto"
    :width "100%"}]
  [:img.avatar
   {:max-height "200px"}]
  [:.slimtos
   {:color "green"
    :font-size "350%"}])
