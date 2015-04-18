(ns slimto.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]))

(defstyles screen
  [:.btn
   [:span.glyphicon
    {:margin-right "10px"}]]
  [:.slimtos
   {:color "green"
    :font-size "350%"}])
