(ns art.pictorian.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [art.pictorian.config :as config]
   [art.pictorian.db]
   [art.pictorian.env :refer [defaults]]
   [art.pictorian.chart]
   [art.pictorian.charts.main-chart]
   [art.pictorian.web.apps.signals :refer [!system reset-system!]]
   [art.pictorian.web.handler]
   [art.pictorian.web.jetty]
   [art.pictorian.web.routes.api]
   [art.pictorian.web.routes.auth]
   [art.pictorian.web.routes.pages]
   [art.pictorian.settings]
   [hyperfiddle.rcf :as rcf])
  (:gen-class))

;; Disable rcf
(rcf/enable! false)

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref !system) (ig/halt!))
  (shutdown-agents))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/prep)
       (ig/init)
       (reset-system!))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))
