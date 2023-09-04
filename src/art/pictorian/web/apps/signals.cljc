(ns art.pictorian.web.apps.signals
  (:require
   [com.fulcrologic.statecharts :as sc]
   [com.fulcrologic.statecharts.data-model.working-memory-data-model :as wmdm]
   [hyperfiddle.electric :as e]))

#?(:clj (defonce !system (atom nil)))
#?(:clj (def reset-system! (partial reset! !system)))

#_(e/def node #?(:cljs nil :clj (:db/node core/system)))

(e/def system (e/server (e/watch !system)))
(e/def node (e/server (:db/node system)))
;; (e/def disp-conn (e/server (:dispenser/conn system)))
;; 
;; (e/def !fsm (e/server (:state/fsm system)))
;; (e/def fsm (e/server (e/watch !fsm)))
;; (e/def !wf (e/server (:workflow/wf system)))
;; (e/def wf (e/server (e/watch !wf)))

;; #?(:clj (defonce !auth-task-timer (atom nil)))
;; (e/def auth-task-timer (e/server (e/watch !auth-task-timer)))

(e/def !ses (e/server (:ses/ses system)))
(e/def ses (e/server (e/watch !ses)))

(e/def env (e/server (:chart/env system)))
(e/def wmem (e/server (e/watch (:chart/wmem system))))
(e/def state (e/server (::sc/configuration wmem)))
(e/def data (e/server (::wmdm/data-model wmem)))

(e/def user)
