(ns art.pictorian.web.routes.auth
  (:require
   [buddy.hashers :as hashers]
   [integrant.core :as ig]
   [art.pictorian.users :as users]
   [art.pictorian.web.middleware.core :as middleware]
   [art.pictorian.web.middleware.exception :as exception]
   [art.pictorian.web.pages.layout :as layout]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.http-response :as http-response]))

(defn validate-register [params]
  nil
  #_(first (st/validate params register-schema)))

(defn validate-login [params]
  nil
  #_(first (st/validate params login-schema)))

#_(defn auth-page [request]
    (layout/render request "auth/auth.html"))

(defn register-page [{:keys [flash] :as request}]
  (layout/render request "auth/register.html" (select-keys flash [:errors :email])))

(defn login-page [{:keys [flash] :as request}]
  (layout/render request "auth/login.html" (select-keys flash [:errors :email])))

(defn register-handler [{:keys [node params]}]
  (if-let [errors (validate-register params)]
    (-> (http-response/found "/auth/register")
        (assoc :flash {:errors errors
                       :email (:email params)}))
    (if-not (users/add-user node params)
      (-> (http-response/found "/auth/register")
          (assoc :flash {:errors {:email "User with that email already exists"}
                         :email (:email params)}))
      (-> (http-response/found "/auth/login")
          (assoc :flash {:messages {:success "User is registered! You can log in now."}
                         :email (:email params)})))))

(defn password-valid? [user pass]
  (hashers/check pass (:user/password user)))

(defn login-handler [{:keys [params session node]}]
  (if-let [errors (validate-login params)]
    (-> (http-response/found "/auth/login")
        (assoc :flash {:errors errors
                       :email (:email params)}))
    (let [user (users/find-user node (:email params))]
      (cond
        (not user)
        (-> (http-response/found "/auth/login")
            (assoc :flash {:errors {:email "user with that email does not exist"}
                           :email (:email params)}))
        (and user
             (not (password-valid? user (:password params))))
        (-> (http-response/found "/auth/login")
            (assoc :flash {:errors {:password "The password is wrong"}
                           :email (:email params)}))
        (and user
             (password-valid? user (:password params)))
        (let [updated-session (assoc session :identity (:email params))]
          (-> (http-response/found "/")
              (assoc :session updated-session)))))))

(defn logout-handler [request]
  (-> (http-response/found "/")
      (assoc :session {})))

;; Routes
(defn auth-routes [_opts]
  [#_["/" {:get auth-page}]
   ["/register" {:get register-page
                 :post register-handler}]
   ["/login" {:get login-page
              :post login-handler}]
   ["/logout" {:get logout-handler}]])

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

(derive :reitit.routes/auth :reitit/routes)

(defmethod ig/init-key :reitit.routes/auth
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  [base-path (route-data opts) (auth-routes opts)])
