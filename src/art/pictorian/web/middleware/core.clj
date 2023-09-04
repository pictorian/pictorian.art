(ns art.pictorian.web.middleware.core
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [clojure.tools.logging :as log]
   [art.pictorian.env :as env]
   [art.pictorian.web.pages.layout :as layout]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.session.cookie :as cookie]
   [ring.util.http-response :as http-response]))

(defn wrap-csrf []
  (let [error-page (layout/error-page
                    {:status 403
                     :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (layout/error-page {:status 500
                            :title "Something very bad has happened!"
                            :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-xtdb [node]
  (fn [handler]
    (fn [req]
      (handler (assoc req :node node)))))

(defn on-error [request response]
  (http-response/found "/auth/login"))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

#_(defn wrap-base [handler]
    (-> ((:middleware defaults) handler)
        wrap-auth
        (wrap-defaults
         (-> site-defaults
             (assoc-in [:security :anti-forgery] false)
             (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
        wrap-internal-error))

(defn wrap-base
  [{:keys [_metrics site-defaults-config cookie-secret node] :as opts}]
  (fn [handler]
    (cond-> ((:middleware env/defaults) handler opts)
      true wrap-auth
      true (defaults/wrap-defaults
            (-> site-defaults-config
                (assoc-in [:security :anti-forgery] false)
                #_(assoc-in  [:session :store] (ttl-memory-store (* 60 30)))
                (assoc-in  [:session :store] (cookie/cookie-store {:key (.getBytes ^String cookie-secret)}))))
      true wrap-internal-error)))
