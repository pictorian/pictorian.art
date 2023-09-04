(ns art.pictorian.web.middleware.electric
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hyperfiddle.electric-jetty-adapter :as adapter]
   [hyperfiddle.rcf :refer [tap]]
   [ring.adapter.jetty9 :as ring]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.params :refer [wrap-params]]))

(def ^:const VERSION (not-empty (System/getProperty "HYPERFIDDLE_ELECTRIC_SERVER_VERSION"))) ; see Dockerfile

(defn wrap-reject-stale-client
  "Intercept websocket UPGRADE request and check if client and server versions matches.
  An electric client is allowed to connect if its version matches the server's version, or if the server doesn't have a version set (dev mode).
  Otherwise, the client connection is rejected gracefully."
  [next-handler]
  (fn [ring-req]
    (if (ring/ws-upgrade-request? ring-req)
      (let [client-version (get-in ring-req [:query-params "HYPERFIDDLE_ELECTRIC_CLIENT_VERSION"])]
        (cond
          (nil? VERSION)             (next-handler ring-req)
          (= client-version VERSION) (next-handler ring-req)
          :else (adapter/reject-websocket-handler 1008 "stale client") ; https://www.rfc-editor.org/rfc/rfc6455#section-7.4.1
          ))
      (next-handler ring-req))))

(defn wrap-electric-websocket [next-handler]
  (fn [ring-request]
    (if (ring/ws-upgrade-request? ring-request)
      (let [;; authenticated-request    (auth/basic-authentication-request ring-request authenticate) ; optional
            electric-message-handler (partial adapter/electric-ws-message-handler ring-request)] ; takes the ring request as first arg - makes it available to electric program
        (ring/ws-upgrade-response (adapter/electric-ws-adapter electric-message-handler)))
      (next-handler ring-request))))

(def manifest-path "public/js/manifest.edn")

(defn get-modules []
  (when-let [manifest (io/resource manifest-path)]
    (let [manifest-folder (when-let [folder-name (second (rseq (str/split manifest-path #"\/")))]
                            (str "/" folder-name "/"))]
      (->> (slurp manifest)
           (edn/read-string)
           (reduce (fn [r module] (assoc r (keyword "hyperfiddle.client.module" (name (:name module))) (str manifest-folder (:output-name module)))) {})))))

#_(defn electric-script []
    (when-some [{:keys [hyperfiddle.client.module/main]} (get-modules)]
      main))

(defn electric-scripts []
  (tap :electric-scripts)
  (->> (get-modules)
       (map (fn [m] {(-> (key m) name keyword) (val m)}))
       (into {})))

(defn wrap-electric-scripts [next-handler]
  (fn [ring-request]
    (next-handler (assoc ring-request :electric-scripts (electric-scripts)))))

(defn electric-websocket-middleware [next-handler]
  (-> (wrap-electric-websocket next-handler) ; 4. connect electric client
      (wrap-cookies) ; 3. makes cookies available to Electric app
      (wrap-reject-stale-client) ; 2. reject stale electric client
      (wrap-params) ; 1. parse query params
      #_(wrap-electric-scripts)))
