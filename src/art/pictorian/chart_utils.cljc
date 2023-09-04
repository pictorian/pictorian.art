(ns art.pictorian.chart-utils
  (:require
   [com.fulcrologic.statecharts :as sc]))

(defn data-loc [loc]
  (fn [_ data] (get-in data loc)))

(defn event-loc [loc]
  (fn [_ data] (get-in (-> data :_event :data) loc)))

(defn data-fn [f loc &  args]
  (fn [_ data] (apply f (get-in data loc) args)))

(defn in [state]
  (fn [env _]
    (boolean (some #{state} (-> env ::sc/vwmem deref ::sc/configuration)))))
