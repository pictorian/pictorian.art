(ns art.pictorian.web.apps.admin.admin-app
  (:require
   #?(:clj [art.pictorian.users :as users])
   [art.pictorian.web.apps.admin.dashboard :as dashboard]
   [art.pictorian.web.apps.admin.layout :refer [AdminLayout]]
   [art.pictorian.web.apps.admin.settings :as settings]
   [art.pictorian.web.apps.router :as router]
   [art.pictorian.web.apps.signals :as signals]
   [clojure.string :as str]
   contrib.str
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [hyperfiddle.electric-svg :as svg]
   [hyperfiddle.history :as history]))

(e/defn DemoGoogHistory []
  (let [{:keys [page]} (router/parse-route router/path)]
    (case page
      "home" (do (dom/h1 (dom/text "Admin home")) (router/Link. {:page "subroute1"} (e/fn [] (dom/props {:class "btn btn-primary"}) (dom/text "aa"))))
      "subroute1" (do (dom/h1 (dom/text "A")) (router/Link. {:page "home"} (e/fn [] (dom/props {:class "btn btn-primary"}) (dom/text "home"))))
      (dom/h1 (dom/text "no such page" page)))))

(e/defn SuccessModal
  []
  (dom/div
    (dom/button (dom/props {:type "button",
                            :class "btn btn-primary",
                            :data-bs-toggle "modal",
                            :data-bs-target "#exampleModal"})
                (dom/text "
  Launch demo modal
"))
    (dom/div
      (dom/props {:class "modal", :id "exampleModal", :tabindex "-1"})
      (dom/div
        (dom/props {:class "modal-dialog modal-sm", :role "document"})
        (dom/div
          (dom/props {:class "modal-content"})
          (dom/button (dom/props {:type "button",
                                  :class "btn-close",
                                  :data-bs-dismiss "modal",
                                  :aria-label "Close"}))
          (dom/div (dom/props {:class "modal-status bg-success"}))
          (dom/div
            (dom/props {:class "modal-body text-center py-4"})
            (svg/svg
             (dom/props {:class "icon mb-2 text-green icon-lg",
                         :width "24",
                         :height "24",
                         :viewbox "0 0 24 24",
                         :stroke-width "2",
                         :stroke "currentColor",
                         :fill "none",
                         :stroke-linecap "round",
                         :stroke-linejoin "round"})
             (svg/path (dom/props
                        {:stroke "none", :d "M0 0h24v24H0z", :fill "none"}))
             (svg/circle (dom/props {:cx "12", :cy "12", :r "9"}))
             (svg/path (dom/props {:d "M9 12l2 2l4 -4"})))
            (dom/h3 (dom/text "Payment succedeed"))
            (dom/div
              (dom/props {:class "text-muted"})
              (dom/text
               "Your payment of $290 has been successfully submitted. Your invoice has been sent to support@tabler.io.")))
          (dom/div
            (dom/props {:class "modal-footer"})
            (dom/div
              (dom/props {:class "w-100"})
              (dom/div
                (dom/props {:class "row"})
                (dom/div
                  (dom/props {:class "col"})
                  (dom/a (dom/props {:href "#",
                                     :class "btn w-100",
                                     :data-bs-dismiss "modal"})
                         (dom/text
                          "
                Go to dashboard
              ")))
                (dom/div
                  (dom/props {:class "col"})
                  (dom/a
                    (dom/props {:href "#",
                                :class "btn btn-success w-100",
                                :data-bs-dismiss "modal"})
                    (dom/text
                     "
                View invoice
              ")))))))))))

(e/defn NotFoundPage []
  (e/client (dom/h1 (dom/text "Page not found2"))
            (history/Link {:page `dashboard/DashboardPage}
                          (dom/text "Dashboard"))))

(e/defn Pages [page]
  (e/server
    (case page
      `dashboard/DashboardPage dashboard/DashboardPage
      NotFoundPage)))

(e/defn AdminRouter []
  (let [{:keys [page]} (router/parse-route router/path)]
    (case page
      nil (new dashboard/DashboardPage)
      "settings" (new settings/SettingsPage)
      (dom/h1 (dom/text "Страница не существует!")))))

(defn get-query-string [path]
  (println "path" path)
  (let [res (-> path (str/split #"\?") (second))]
    (println "res" res)
    (or res "")))

#_(e/defn AdminHistoryRouter []
    (binding [history/encode #(str "?" (ednish/encode-uri %))
              history/decode #(or (ednish/decode-path (get-query-string %) hf/read-edn-str)
                                  [`dashboard/DashboardPage])]
      (history/router (new history/HTML5-History)
                      #_(set! (.-title js/document) (str (str/capitalize (name (first history/route))) " - Hyperfiddle"))
                      (dom/pre (dom/text (contrib.str/pprint-str history/route)))
                      (history/Link [`dashboard/DashboardPage]
                                    (dom/text "Dashboard"))
                      #_(let [[page & _args] history/route]
                          (e/server (new (Pages. page)))))))

(e/defn AdminApp []
  (e/server
    #_(tap> {:identity (-> e/*http-request* :session :identity)})
    (binding [signals/user (users/find-user signals/node (-> e/*http-request* :session :identity))]
      #_(tap> {:user signals/user})
      (e/client (new AdminLayout
                     (e/fn []
                       #_(new AdminHistoryRouter)
                       (new AdminRouter)))))))
