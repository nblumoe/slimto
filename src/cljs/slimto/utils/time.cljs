(ns slimto.utils.time
  (:require [cljs-time.core   :as tcore]
            [cljs-time.format :as tformat]))

(def custom-formatter (tformat/formatter "yyyy-MM-dd"))

(defn ms->days [ms]
  (-> ms
    (/ 1000)
    (/ 3600)
    (/ 24)))

(defn str->days [s]
  (let [date (js/Date. (clj->js s))]
    (-> date .getTime ms->days)))

(defn today []
  (tformat/unparse custom-formatter (tcore/today-at 00 00)))
