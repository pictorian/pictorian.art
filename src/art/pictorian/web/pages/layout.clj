(ns art.pictorian.web.pages.layout
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hyperfiddle.rcf :refer [tap]]
   [art.pictorian.users :as users]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.response]
   [selmer.parser :as parser]))

(def selmer-opts {:custom-resource-path (io/resource "html")})

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

(def electric-scripts
  (->> (get-modules)
       (map (fn [m] {(-> (key m) name) (val m)}))
       (into {})))

(defn init-selmer!
  [{:keys [env]}]

  (if (= env :prod)
    (parser/cache-on!)
    (parser/cache-off!))

  (parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))

  (parser/add-tag! :shared-js
                   (fn [_ _]
                     (let [shared-script (get electric-scripts "shared")]
                       (format "<script src=\"%s\"></script>" shared-script))))

  (parser/add-tag! :app
                   (fn [args _ content]
                     (let [app-id (first args)]
                       (when-let [script (get electric-scripts app-id)]
                         (format
                          "<div id=\"%s\">
<div id=\"loading\">%s</div>
</div>
<script src=\"%s\" defer=\"true\"></script>"
                          app-id (str (or (get-in content [:app :content]) "Loading...")) script))))
                   :endapp))

(defn render-template []
  {:body "<div>aaa</div>"})

(defn render
  [request template &  {:keys [electric] :as params}]
  (-> (parser/render-file template
                          (assoc params
                                 :page template
                                 :csrf-token *anti-forgery-token*
                                 ;; :electric (if electric (electric-script) nil)
                                 ;; :electric-scripts electric-scripts
                                 :user (users/find-user (:node request) (-> request :session :identity)))
                          selmer-opts)
      (ok)
      (content-type "text/html; charset=utf-8")))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)
   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details selmer-opts)})
