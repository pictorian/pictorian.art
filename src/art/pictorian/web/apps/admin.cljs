(ns ^:dev/always art.pictorian.web.apps.admin
  (:require
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   #_[hyperfiddle.rcf]
   [art.pictorian.web.apps.admin.admin-app :as admin-app]))

(defonce reactor nil)

(defn ^:dev/after-load ^:export start []
  (set! reactor ((e/boot
                   (binding [dom/node (.querySelector js/document "#admin-app")]
                     (set! (.-innerHTML (.querySelector dom/node "#loading")) "")
                     (new admin-app/AdminApp)))
                 #(js/console.log "Reactor success:" %)
                 (fn [error]
                   (case (:hyperfiddle.electric/type (ex-data error))
                     :hyperfiddle.electric-client/stale-client (do (js/console.log "Server and client version mismatch. Refreshing page.")
                                                                   (.reload (.-location js/window)))
                     (js/console.error "Reactor failure:" error))))))

(defn ^:dev/before-load stop []
  (when reactor (reactor))            ; teardown
  (set! reactor nil))
