(ns art.pictorian.settings
  (:require
   [hyperfiddle.rcf :refer [% tap tests]]
   [integrant.core :as ig]
   [art.pictorian.db :as db]
   [xtdb.api :as xt]))

(def default-settings {:logo-image                        nil
                       :org                               "Демо-банк"
                       :office                            "Демо-офис"
                       :office-address                    ""
                       :term                              "TERM_001"
                       :color-primary                     "#3F51B5"
                       :color-secondary                   "#F44336"
                       :get-card-code-timeout-sec         180
                       :card-expiration-days              30
                       :card-take-timeout-sec             30
                       :card-evict-days                   90
                       :card-recycle-at-once-count        100
                       :kiosk-idle-timeout-sec            90
                       :report-format                     "xlsx"
                       :report-daily-enabled              false
                       :report-daily-time                 #inst"1970-01-01T19:00:00.000-00:00"
                       :report-card-loading-enabled       false
                       :report-card-loading-delay-minutes 5
                       :smtp-to                           ""
                       :smtp-host                         "localhost"
                       :smtp-port                         25
                       :smtp-user                         ""
                       :smtp-password                     ""
                       :smtp-security                     "none"
                       :smtp-from                         "noreply@localhost.localdomain"
                       :license-file                      ""
                       :service-access-code               ""
                       :cvm                               {:dispenserModel "MOCK"
                                                           :dispenserPrinterModel "Seaory S22M"}
                       :nfc-scan-doc-timeout 10
                       :auth-methods {:get-sim [:nfc]
                                      :order-sim [:nfc :qr-code]
                                      :portal nil}
                       :dispenser {:grpc-host "127.0.0.1"
                                   :grpc-port 9090
                                   :grpc-ssl? false
                                   :com-port 1}})

(defn save-setting
  ([node id value]
   (db/save! node {:id    id
                   :type  :setting
                   :value value})))

(defn save-settings [node m]
  (let [m (->> m
               (into [])
               (map (fn [[k v]]
                      {:id   k
                       :type  :setting
                       :value v})))]
    (db/save-multi! node m)
    #_(Thread/sleep 3000)
    #_(throw (ex-info "Error" {:message "Ошибка сохранения настроек"}))))

(defn create-setting-if-not-exists [node id value]
  (when-not (db/get-by-id node id)
    (save-setting node id value)))

(defn list-settings-by-ids [node ids]
  (->> ids
       (map (fn [field]
              (db/get-by-attributes node {:type :setting :id field})))
       (map (fn [m] {(:id m) (:value m)}))
       (into {})))

#_(defn get-settings [node & [setting-keys]]
    (let [saved-settings (list-settings-by-ids node (or (seq setting-keys) (keys default-settings)))
          merged-settings (-> default-settings (merge saved-settings))]
      (if (seq setting-keys)
        (select-keys merged-settings setting-keys)
        merged-settings)))

(defn get-settings [node & [setting-keys]]
  (list-settings-by-ids node (or (seq setting-keys) (keys default-settings))))

#_(defn get-setting [node key]
    (let [saved-settings (list-settings-by-ids node [key])]
      (-> default-settings
          (merge saved-settings)
          (get key))))

(defn get-setting [node key]
  (first (list-settings-by-ids node [key])))

(defmethod ig/init-key :settings/defaults
  [_ {:keys [node]}]
  (doseq [[k v] default-settings]
    (create-setting-if-not-exists node k v)))

(tests "Save setting"
       (with-open [node (xt/start-node {})]
         (save-setting node :foo :bar)
         (future (tap (get-setting node :foo)))
         % := :bar))

(tests "Create default settings"
       (with-open [node (xt/start-node {})]
         (doseq [[k v] default-settings]
           (create-setting-if-not-exists node k v))
         (future (tap (get-settings node [])))
         % := default-settings))
