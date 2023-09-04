(ns art.pictorian.web.jetty
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [ring.adapter.jetty9 :refer [run-jetty]])
  (:import [org.eclipse.jetty.server.handler.gzip GzipHandler]))

(defn start [handler {:keys [port] :as opts}]
  (try
    (let [server (run-jetty handler (dissoc opts :handler))]
      (log/info "server started on port" port)
      server)
    (catch Throwable t
      (log/error t (str "server failed to start on port: " port)))))

(defn stop [server]
  (when server
    (.stop server)
    (log/info "HTTP server stopped")))

(defn- add-gzip-handler
  "Makes Jetty server compress responses. Optional but recommended."
  [server]
  (.setHandler server
               (doto (GzipHandler.)
                 #_(.setIncludedMimeTypes (into-array ["text/css" "text/plain" "text/javascript" "application/javascript" "application/json" "image/svg+xml"])) ; only compress these
                 (.setMinGzipSize 1024)
                 (.setHandler (.getHandler server)))))

(defmethod ig/prep-key :server/http
  [_ config]
  (merge {:port 3000
          :host "0.0.0.0"
          :join? false
          :configurator add-gzip-handler}
         config))

(defmethod ig/init-key :server/http
  [_ opts]
  (let [handler (atom (delay (:handler opts)))]
    {:handler handler
     :server  (start (fn [req] (@@handler req)) (dissoc opts :handler))}))

(defmethod ig/halt-key! :server/http
  [_ {:keys [server]}]
  (stop server))

(defmethod ig/suspend-key! :server/http
  [_ {:keys [handler]}]
  (reset! handler (promise)))

(defmethod ig/resume-key :server/http
  [k opts old-opts old-impl]
  (if (= (dissoc opts :handler) (dissoc old-opts :handler))
    (do (deliver @(:handler old-impl) (:handler opts))
        old-impl)
    (do (ig/halt-key! k old-impl)
        (ig/init-key k opts))))
