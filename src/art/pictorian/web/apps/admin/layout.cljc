(ns art.pictorian.web.apps.admin.layout
  (:require
   #?(:clj [art.pictorian.web.pages.layout :refer [render]])
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [hyperfiddle.electric-svg :as svg]
   [hyperfiddle.rcf :refer [tap]]
   [art.pictorian.web.apps.signals :as signals]))

(e/defn Header []
  (e/client
    (tap :admin-header)
    (dom/div
      (->  dom/node
           (.-outerHTML)
           (set!
            (:body (e/server (render e/*http-request* "header.html"))))))))

(e/defn Subheader []
  (dom/header
    (dom/props {:class "navbar-expand-md"})
    (dom/div
      (dom/props {:class "collapse navbar-collapse", :id "navbar-menu"})
      (dom/div
        (dom/props {:class "navbar"})
        (dom/div
          (dom/props {:class "container-xl"})
          (dom/ul
            (dom/props {:class "navbar-nav"})
            (dom/li
              (dom/props {:class "nav-item"})
              (dom/a
                (dom/props {:class "nav-link", :href "/" :target "_blank"})
                (dom/span
                  (dom/props {:class "nav-link-icon d-md-none d-lg-inline-block"})

                  (svg/svg
                   (dom/props {;; :xmlns "http://www.w3.org/2000/svg"
                               :class "icon",
                               :width "24",
                               :height "24",
                               :viewbox "0 0 24 24",
                               :stroke-width "2",
                               :stroke "currentColor",
                               :fill "none",
                               :stroke-linecap "round",
                               :stroke-linejoin "round"})
                   (svg/path (dom/props {:stroke "none",
                                         :d "M0 0h24v24H0z",
                                         :fill "none"}))
                   (svg/path (dom/props {:d "M5 12l-2 0l9 -9l9 9l-2 0"}))
                   (svg/path (dom/props
                              {:d
                               "M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7"}))
                   (svg/path
                    (dom/props
                     {:d "M9 21v-6a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v6"}))))
                (dom/span
                  (dom/props {:class "nav-link-title"})
                  (dom/text
                   "
                                            Сайт
                                        "))))
            #_(dom/li
                (dom/props {:class "nav-item"})
                (dom/a
                  (dom/props {:class "nav-link", :href "/kiosk", :target "_blank"})
                  (dom/span
                    (dom/props {:class "nav-link-icon d-md-none d-lg-inline-block"})
                    (svg/svg (dom/props {;; :xmlns "http://www.w3.org/2000/svg"
                                         :class "icon",
                                         :width "24",
                                         :height "24",
                                         :viewbox "0 0 24 24",
                                         :stroke-width "2",
                                         :stroke "currentColor",
                                         :fill "none",
                                         :stroke-linecap "round",
                                         :stroke-linejoin "round"})
                             (svg/path (dom/props {:stroke "none",
                                                   :d "M0 0h24v24H0z",
                                                   :fill "none"}))
                             (svg/path
                              (dom/props
                               {:d "M12 3l8 4.5l0 9l-8 4.5l-8 -4.5l0 -9l8 -4.5"}))
                             (svg/path (dom/props {:d "M12 12l8 -4.5"}))
                             (svg/path (dom/props {:d "M12 12l0 9"}))
                             (svg/path (dom/props {:d "M12 12l-8 -4.5"}))
                             (svg/path (dom/props {:d "M16 5.25l-8 4.5"}))))
                  (dom/span
                    (dom/props {:class "nav-link-title"})
                    (dom/text
                     "
                                            Киоск
                                        "))))
            (dom/li
              (dom/props {:class "nav-item"})
              (dom/a
                (dom/props {:class "nav-link", :href "/admin?page=settings"})
                (dom/span
                  (dom/props {:class "nav-link-icon d-md-none d-lg-inline-block"})
                  (svg/svg
                   (dom/props {;; :xmlns "http://www.w3.org/2000/svg"
                               :class "icon icon-tabler icon-tabler-key",
                               :width "24",
                               :height "24",
                               :viewbox "0 0 24 24",
                               :stroke-width "2",
                               :stroke "currentColor",
                               :fill "none",
                               :stroke-linecap "round",
                               :stroke-linejoin "round"})
                   (svg/path (dom/props {:stroke "none",
                                         :d "M0 0h24v24H0z",
                                         :fill "none"}))
                   (svg/path
                    (dom/props
                     {:d
                      "M16.555 3.843l3.602 3.602a2.877 2.877 0 0 1 0 4.069l-2.643 2.643a2.877 2.877 0 0 1 -4.069 0l-.301 -.301l-6.558 6.558a2 2 0 0 1 -1.239 .578l-.175 .008h-1.172a1 1 0 0 1 -.993 -.883l-.007 -.117v-1.172a2 2 0 0 1 .467 -1.284l.119 -.13l.414 -.414h2v-2h2v-2l2.144 -2.144l-.301 -.301a2.877 2.877 0 0 1 0 -4.069l2.643 -2.643a2.877 2.877 0 0 1 4.069 0z"}))
                   (svg/path (dom/props {:d "M15 9h.01"}))))
                (dom/span
                  (dom/props {:class "nav-link-title"})
                  (dom/text
                   "
                                            Настройки
                                        ")))))
          (dom/div
            (dom/props
             {:class
              "my-2 my-md-0 flex-grow-1 flex-md-grow-0 order-first order-md-last"})
            (dom/form
              (dom/props {:action "./",
                          :method "get",
                          :autocomplete "off",
                          :novalidate ""})
              (dom/div
                (dom/props {:class "input-icon"})
                (dom/span
                  (dom/props {:class "input-icon-addon"})

                  (svg/svg (dom/props {;; :xmlns "http://www.w3.org/2000/svg"
                                       :class "icon",
                                       :width "24",
                                       :height "24",
                                       :viewbox "0 0 24 24",
                                       :stroke-width "2",
                                       :stroke "currentColor",
                                       :fill "none",
                                       :stroke-linecap "round",
                                       :stroke-linejoin "round"})
                           (svg/path (dom/props {:stroke "none",
                                                 :d "M0 0h24v24H0z",
                                                 :fill "none"}))
                           (svg/path
                            (dom/props
                             {:d "M10 10m-7 0a7 7 0 1 0 14 0a7 7 0 1 0 -14 0"}))
                           (svg/path (dom/props {:d "M21 21l-6 -6"}))))
                (dom/input (dom/props {:type "text",
                                       :value "",
                                       :class "form-control",
                                       :placeholder "Поиск…",
                                       :aria-label "Search in website"}))))))))))

(e/defn AdminLayout [Content]
  (dom/div (dom/props {:class "page"})
           (dom/div (dom/props {:class "sticky-top"})
                    (new Header)
                    (new Subheader))
           (dom/div (dom/props {:class "page-wrapper"})
                    (dom/div (dom/props {:class "page-header"}))
                    (dom/div (dom/props {:class "page-body"})
                             (dom/div (dom/props {:class "container-xl"})
                                      (if (e/server signals/user)
                                        (new Content)
                                        (dom/div (dom/props {:class "text-center"})
                                                 (dom/h2 (dom/text "Вы не вошли в систему"))
                                                 (dom/div (dom/a (dom/props {:href "/auth/login"
                                                                             :class "btn btn-primary"}) (dom/text "Войдите")))
                                                 (dom/div (dom/text "или"))
                                                 (dom/div (dom/a (dom/props {:href "/auth/register"
                                                                             :class "btn"}) (dom/text "Зарегистрируйтесь"))))))))))
