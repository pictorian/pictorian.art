(ns art.pictorian.chart
  (:require
   [com.fulcrologic.statecharts :as sc]
   [com.fulcrologic.statecharts.event-queue.core-async-event-loop :as loop]
   [com.fulcrologic.statecharts.events :as evts]
   [com.fulcrologic.statecharts.protocols :as sp]
   [com.fulcrologic.statecharts.simple :as simple]
   [integrant.core :as ig]
   [com.fulcrologic.statecharts.invocation.future :as future-invocations]
   [art.pictorian.charts.invocations :as invocations]
   [kit.ig-utils :as ig-utils]))

(defn send!
  ([env event] (send! env event {}))
  ([env event data] (send! env event data nil))
  ([env event data delay] (simple/send! env {:target :main :event event :data data :delay delay})))

;; wmem
(defmethod ig/init-key :chart/wmem
  [_ {:keys []}]
;; Override the working memory store so we can watch our working memory change
  (let [a (atom {})]
    #_(add-watch a :printer (fn [_ _ _ n] (pprint n)))
    a))

(defmethod ig/halt-key! :chart/wmem
  [_ _wmem])

(defmethod ig/suspend-key! :chart/wmem [_ _])

(defmethod ig/resume-key :chart/wmem
  [key opts old-opts old-impl]
  (ig-utils/resume-handler key opts old-opts old-impl))

(defn register-main-chart [env main-chart]
  (simple/register! env `main-chart main-chart)
  #_(simple/register! env `child-chart child-chart))

;; env
(defmethod ig/init-key :chart/env
  [_ {:keys [wmem main-chart]}]
  #_(pprint @wmem)
  ;; Create an env that has all the components needed, but override the working memory store
  (let [session-id :main
        env (simple/simple-env
             {::sc/invocation-processors [(invocations/new-task-processor)
                                          (invocations/new-timer-service)
                                          (future-invocations/new-future-processor)]
              ::sc/working-memory-store
              (reify sp/WorkingMemoryStore
                (get-working-memory [_ _ _] @wmem)
                (save-working-memory! [_ _ _ m] (reset! wmem m)))})]
    ;; Register charts under a well-known names
    (register-main-chart env main-chart)
    (simple/start! env `main-chart session-id)
    env))

(defmethod ig/suspend-key! :chart/env [_ _])

(defmethod ig/resume-key :chart/env
  [key opts old-opts old-impl]
  (ig-utils/resume-handler key opts old-opts old-impl))

(defmethod ig/halt-key! :chart/env
  [_ env]
  (simple/send! env {:target :main
                     :event  evts/cancel-event}))

;; loop
(defmethod ig/init-key :chart/loop
  [_ {:keys [env interval main-chart]}]
  (register-main-chart env main-chart)
  (let [;; Run an event loop that polls the queue every 100ms
        running? (loop/run-event-loop! env interval)]
    running?))

(defmethod ig/halt-key! :chart/loop
  [_ running?]
  ;; Stop event loop
  (reset! running? false))
