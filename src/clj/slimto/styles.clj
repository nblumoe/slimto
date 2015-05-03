(ns slimto.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]))

(defstyles screen
  [:body
   {:font-size "180%"}
   [:.btn
    {:font-size "120%"}
    [:span.glyphicon
     {:margin-right "10px"}]]
   [:.input-group-addon
     {:font-size "110%"}]
   [:svg#progress-plot
    {:background-color "#EEE"
     :box-shadow "inset 0px 0px 10px 0px rgba(0,0,0,.75)"
     :max-height "200px"
     :height "auto"
     :width "100%"}]
   [:img.avatar
    {:max-height "200px"}]
   [:.slimtos
    {:color "green"
     :font-size "350%"}]])
