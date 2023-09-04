(ns art.pictorian.charts.invocations
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [com.fulcrologic.statecharts :as sc]
   [com.fulcrologic.statecharts.elements :refer [invoke]]
   [com.fulcrologic.statecharts.environment :as env]
   [com.fulcrologic.statecharts.protocols :as sp])
  (:import
   [missionary Cancelled]))

(deftype Timer [active-invocations]
  sp/InvocationProcessor
  (supports-invocation-type? [_this typ] (= typ :timer))
  (start-invocation! [_this {::sc/keys [event-queue]
                             :as       env} {:keys [invokeid params]}]
    (let [source-session-id (env/session-id env)
          {:keys [interval event]
           :or   {interval 1000 event :interval-timer/timeout}} params
          send-id           (str source-session-id "." invokeid)
          notify!           (fn []
                              (log/debug "Timeout event" event)
                              (sp/send! event-queue env
                                        {:target            source-session-id
                                         :send-id           send-id
                                 ;; IMPORTANT: If you don't include the invokeid, then it won't register in finalize
                                         :invoke-id         invokeid
                                         :source-session-id send-id
                                         :event             event}))]
      (swap! active-invocations assoc send-id true)
      (async/go-loop []
        (async/<! (async/timeout interval))
        (if (get @active-invocations send-id)
          (do
            (notify!)
            (recur))
          (log/debug "Timer loop exited")))
      true))
  (stop-invocation! [_ env {:keys [invokeid] :as data}]
    (log/spy :info data)
    (log/debug "Invocation" invokeid "asked to stop")
    (let [source-session-id (env/session-id env)
          time-id           (str source-session-id "." invokeid)]
      (swap! active-invocations dissoc time-id)
      true))
  (forward-event! [_this _env _event] nil))

(defrecord TaskInvocationProcessor [active-tasks]
  sp/InvocationProcessor
  (supports-invocation-type? [_this typ] (= :task typ))
  (start-invocation! [_this {::sc/keys [event-queue]
                             :as       env} {:keys [invokeid src params]}]
    (log/trace "Start invoking task " invokeid src params)
    (let [source-session-id (env/session-id env)
          child-session-id  (str source-session-id "." invokeid)
          done-event-name   (keyword (str "done.invoke." invokeid))
          error-event-name   (keyword (str "error.platform." invokeid))
          event-base {:target            source-session-id
                      :send-id            child-session-id
                      :invoke-id invokeid
                      :source-session-id child-session-id}]
      (if-not (fn? src)
        (do
          (log/error "Invoked task src not supplied" child-session-id)
          (sp/send! event-queue env (merge event-base {:event             :error.platform
                                                       :data {:message "Could not invoke task. No function supplied."
                                                              :target  src}})))
        (let [t (src params)
              success (fn [result]
                        (swap! active-tasks dissoc child-session-id)
                        (sp/send! event-queue env (merge event-base {:event             done-event-name
                                                                     :data result})))
              error (fn [e]
                      (swap! active-tasks dissoc child-session-id)
                      (if (instance? Cancelled e)
                        (log/debug "Invoked task cancelled" child-session-id)
                        (do
                          (log/error "Invoked task error" e)
                          (sp/send! event-queue env (merge event-base {:event             :error.platform
                                                                       :data {:message "Invoked task error"
                                                                              :cause  e}})))))
              cancel (t success error)]
          (swap! active-tasks assoc child-session-id cancel)))
      true))
  (stop-invocation! [_ env {:keys [invokeid]}]
    #_(log/trace "Stop task" invokeid)
    (let [source-session-id (env/session-id env)
          child-session-id  (str source-session-id "." invokeid)
          cancel                 (get @active-tasks child-session-id)]
      (when cancel
        (log/trace "Cancel invoked task")
        (cancel))
      true))
  (forward-event! [_this _env _event]
    (log/warn "Missionary task event forwarding not supported")))

(defn new-timer-service
  "Create a new time service that can be invoked from a state chart."
  [] (->Timer (atom {})))

(defn new-task-processor
  "Create an invocation processor that can be used to run functions in futures."
  []
  (->TaskInvocationProcessor (atom {})))

#_(defn invoke-task [id event interval params finalize-expr]
    (invoke {:id id
             :type       :timer
             :params     params
             :finalize   finalize-expr}))
