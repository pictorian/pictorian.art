(ns art.pictorian.charts.flow
  (:require
   [clojure.tools.logging :as log]
   [com.fulcrologic.statecharts.convenience :refer [assign-on choice on]]
   [com.fulcrologic.statecharts.data-model.operations :as ops]
   [com.fulcrologic.statecharts.elements :refer [assign invoke on-entry
                                                 parallel state transition]]
   [art.pictorian.chart-utils :refer [data-fn data-loc event-loc in]]
   [missionary.core :as m]))

(defn flow-state []
  (state {:id :s/flow}
         (state {:id :s/flow.stopped}
                (on-entry {} (assign {:location [:flow] :expr nil}))
                (assign-on :e/flow.start [:flow :service] (event-loc [:service]))
                (transition {:target :s/flow.running :cond (data-loc [:flow :service])}))
         (parallel {:id :s/flow.running}
                   (on :error.platform :s/flow.stopped)
                   (on :e/flow.stop :s/flow.stopped)
                   (on-entry {}
                             (assign {:location [:flow :auth-methods] :expr (fn [_ data] (get (-> data :settings :auth-methods) (-> data :flow :service)))})
                             (assign {:location [:flow :auth-method] :expr (data-fn #(if (= 1 (count %)) (first %) nil) [:flow :auth-methods])}))
                   (state {:id :s/flow.auth}
                          (choice {}
                                  (data-loc [:flow :auth-method]) :s/flow.auth.running
                                  (data-fn #(> (count %) 1) [:flow :auth-methods]) :s/flow.auth.selecting-method
                                  :else :s/flow.auth.success)
                          (state {:id :s/flow.auth.selecting-method}
                                 (on :e/flow.auth.method-selected :s/flow.auth.running
                                     (assign  {:location [:flow :auth-method] :expr (event-loc [:method])})))
                          (state {:id :s/flow.auth.success})
                          (state {:id :s/flow.auth.error})
                          (state {:id :s/flow.auth.running}
                                 (choice {}
                                         (data-fn = [:flow :auth-method] :nfc) :s/flow.auth.running.nfc
                                         (data-fn = [:flow :auth-method] :qr-code) :s/flow.auth.running.qr-code
                                         :else :s/flow.auth.selecting-method)
                                 (state {:id :s/flow.auth.running.nfc}
                                        (state {:id :s/flow.auth.running.nfc.scan-doc}
                                               (on-entry {} (assign {:location [:flow :scan-doc-timer] :expr (fn [_ data] (-> data :settings :nfc-scan-doc-timeout))}))
                                               (transition {:event :e/flow.auth.running.nfc.scan-doc.timeout
                                                            :target :s/flow.auth.error
                                                            :cond (fn [_ data] (<= (-> data :flow :scan-doc-timer) 0))})
                                               (transition {:target :s/flow.auth.running.fetch-client
                                                            :cond (data-loc [:flow :nfc-scan-data])})
                                               (on :error.platform :s/flow.auth.error)
                                               (invoke {:id :timer/nfc-scan-doc
                                                        :type       :timer
                                                        :params     {:interval 1000 :event :e/flow.auth.running.nfc.scan-doc.timeout}
                                                        :finalize   (fn [_ data]
                                                                      #_(info :scan-doc-timer (-> data :flow :scan-doc-timer))
                                                                      ;; Finalize gets to update the model before the event is delivered...
                                                                      [(ops/assign [:flow :scan-doc-timer] (dec (-> data :flow :scan-doc-timer)))])})
                                               (invoke {:id     :task/nfc-scan-doc
                                                        ;; :idlocation [:flow :tasks]
                                                        :type   :task
                                                        :params {:time 2}
                                                        :src    (fn [{:keys [time]}]
                                                                  (m/sp
                                                                   (log/debug "NFC scan task started")
                                                                   (m/? (m/sleep (* time 1000)))
                                                                   #_(throw (ex-info "NFC scan failed" {}))
                                                                   (log/debug "NFC scan task completed")
                                                                   {:scan-data {:user-id 1234}}))
                                                        :finalize (fn [_ data]
                                                                    (let [scan-data (-> data :_event :data :scan-data)]
                                                                      [(ops/assign [:flow :nfc-scan-data] scan-data)]))})))
                                 (state {:id :s/flow.auth.running.qr-code})
                                 (state {:id :s/flow.auth.running.fetch-client}
                                        (transition {:target :s/flow.auth.success
                                                     :cond (data-loc [:flow :auth-data])})
                                        (on :error.platform :s/flow.auth.error)
                                        (invoke {:id     :task/auth-fetch-client
                                                        ;; :idlocation [:flow :tasks]
                                                 :type   :task
                                                 :params {:time 2}
                                                 :src    (fn [{:keys [time]}]
                                                           (m/sp
                                                            (log/debug "Fetch client task started")
                                                            (m/? (m/sleep (* time 1000)))
                                                            #_(throw (ex-info "NFC scan failed" {}))
                                                            (log/debug "Fetch client task completed")
                                                            {:client-id 9999 :image "bbbbb"}))
                                                 :finalize (fn [_ data]
                                                             [(ops/assign [:flow :auth-data] (-> data :_event :data))])}))))
                   (state {:id :s/flow.svc}
                          (state {:id :s/flow.svc.stopped}
                                 (transition {:target :s/flow.svc.running :cond (in :s/flow.auth.success)}))
                          (state {:id :s/flow.svc.running}
                                 (choice {}
                                         (data-fn = [:flow :service]  :get-sim) :s/flow.svc.get-sim
                                         (data-fn = [:flow :service]  :order-sim) :s/flow.svc.order-sim
                                         (data-fn = [:flow :service]  :portal) :s/flow.svc.portal
                                         :else :s/flow.stopped)
                                 (state {:id :s/flow.svc.get-sim})
                                 (state {:id :s/flow.svc.order-sim})
                                 (state {:id :s/flow.svc.portal}))))))
