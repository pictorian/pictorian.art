(ns art.pictorian.web.apps.elements
  (:require
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]))

(e/defn PageWrapper [Title Content]
  (dom/div (dom/props {:class "page-wrapper"})
           (dom/div
             (dom/props {:class "page-header"})
             (dom/div
               (dom/props {:class "container-xl"})
               (new Title)))
           (dom/div (dom/props {:class "page-body"})
                    (dom/div (dom/props {:class "container-xl"})
                             (new Content)))))
