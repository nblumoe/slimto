(ns slimto.utils.time)

(defn ms->days [ms]
  (-> ms
    (/ 1000)
    (/ 3600)
    (/ 24)))

(defn str->days [s]
  (let [date (js/Date. s)]
    (-> date
      .getTime
      ms->days)))

(defn now []
  (.toDateString (js/Date.)))

(defn date [year month day]
  (.toDateString (js/Date. year month day)))
