(ns art.pictorian.web.apps.admin.settings
  (:require
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]))

(e/defn SettingsPage []
  (dom/h2 (dom/text "Настройки")))
