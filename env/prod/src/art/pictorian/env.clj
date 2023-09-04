(ns art.pictorian.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[scs starting]=-"))
   :start      (fn []
                 (log/info "\n-=[scs started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[scs has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
