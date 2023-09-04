(ns art.pictorian.env
  (:require
   [clojure.tools.logging :as log]
   [art.pictorian.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[scs starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[scs started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[scs has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev
                :persist-data? true}})
