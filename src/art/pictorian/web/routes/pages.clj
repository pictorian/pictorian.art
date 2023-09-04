(ns art.pictorian.web.routes.pages
  (:require
   [integrant.core :as ig]
   [art.pictorian.web.middleware.core :as middleware]
   [art.pictorian.web.middleware.electric :as electric]
   [art.pictorian.web.middleware.exception :as exception]
   [art.pictorian.web.pages.layout :as layout]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn home [request]
  (layout/render request "home.html"))

(defn admin [request]
  (layout/render request "admin.html"))

;; Routes
(defn page-routes [_opts]
  [["/" {:get home
         :middleware [;; electric/electric-websocket-middleware
                      #_middleware/wrap-restricted]}]
   ["/admin" {:get admin
              :middleware [electric/electric-websocket-middleware
                           #_middleware/wrap-restricted]}]])

(defn route-data [opts]
  (merge
   opts
   {:middleware
    [;; Default middleware for pages
     (middleware/wrap-csrf)
     ;; query-params & form-params
     parameters/parameters-middleware
     ;; encoding response body
     muuntaja/format-response-middleware
     ;; exception handling
     exception/wrap-exception]}))

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  [base-path (route-data opts) (page-routes opts)])
