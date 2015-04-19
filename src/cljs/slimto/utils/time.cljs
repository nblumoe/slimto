(ns slimto.utils.time)

(defn ms->days [ms]
  (-> ms
    (/ 1000)
    (/ 3600)
    (/ 24)))

(defn now []
  (ms->days (.now js/Date)))

(defn date [year month day]
  (ms->days (.getTime (js/Date. year month day))))
