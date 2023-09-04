(ns art.pictorian.db
  (:require
   [clojure.set :refer [rename-keys]]
   [buddy.hashers :as hashers]
   [clojure.java.io :as io]
   [hyperfiddle.rcf :refer [% tap tests]]
   [integrant.core :as ig]
   [kit.ig-utils :as ig-utils]
   [missionary.core :as m]
   [xtdb.api :as xt])
  (:import
   (java.util UUID)))

(defn transact-sync [node data]
  (xt/await-tx node (xt/submit-tx node data)))

(defn id [] (UUID/randomUUID))
(defn xt->id [x] (rename-keys x {:xt/id :id}))
(defn id->xt [x] (rename-keys x {:id :xt/id}))

(defn list-by-ids [node ids]
  (->> ids
       (map #(xt->id (xt/entity (xt/db node) %)))))

(defn get-by-id [node id]
  (first (list-by-ids node [id])))

(defn save-multi!
  "Creates a transaction for a list of entities.
  Each transaction must have an :id.
  Ignores nil items."
  [node entitiy-list & {:keys [valid-time]}]
  (->> entitiy-list
       (filterv some?)
       (mapv
        #(if valid-time
           (vector ::xt/put (id->xt %) valid-time)
           (vector ::xt/put (id->xt %))))
       (xt/submit-tx node))
  #_(xt/sync node)
  entitiy-list)

(defn save! [node entity & {:keys [valid-time]}]
  (first (save-multi! node [entity] :valid-time valid-time)))

(defn create! [node entity]
  (first (save-multi! node [(assoc entity :id (id))])))

(defn update! [node previous-entity new-entity & {:keys [valid-time]}]
  (->> [[::xt/cas
         (id->xt previous-entity)
         (id->xt new-entity)
         valid-time]]
       ;#(when valid-time (conj % valid-time))
       (xt/submit-tx node))
  #_(xt/sync node)
  new-entity)

(defn set-key! [node k v]
  (xt/submit-tx node [[::xt/put {:xt/id k :value v}]]))

(defn delete-by-ids! [node ids]
  (->> ids
       (mapv #(vector ::xt/delete %))
       (xt/submit-tx node)))

(defn delete-by-id! [node id]
  (delete-by-ids! node [id]))

(defn delete-by-attributes! [node attrs]
  (->> (xt/q (xt/db node)
             {:find  '[id]
              :where (mapv (fn [[attr value]]
                             ['id attr value]) attrs)})
       (map first)
       (mapv #(vector ::xt/delete %))
       (xt/submit-tx node)))

(defn list-ids-by-attributes [node attrs]
  (->> (xt/q (xt/db node)
             {:find  '[id]
              :where (mapv (fn [[attr value]]
                             ['id attr value])
                           (id->xt attrs))
              ;; :limit 10
              ;; :offset 10
              })
       (map first)))

(defn list-ids-by-attribute [node attr value]
  (list-ids-by-attributes node {attr value}))

(defn get-id-by-attributes [node attrs]
  (first (list-ids-by-attributes node attrs)))

(defn get-id-by-attribute [node attr value]
  (first (list-ids-by-attribute node attr value)))

(defn list-by-attributes [node attrs]
  (->> (list-ids-by-attributes node attrs)
       (map #(get-by-id node %))))

(defn list-by-attribute [node attr value]
  (list-by-attributes (xt/db node) {attr value}))

(defn get-by-attributes [node attrs]
  (first (list-by-attributes node attrs)))

(defn get-by-attribute [node attr value]
  (first (list-by-attribute (xt/db node) attr value)))

(defn exists-by-id? [node id]
  (if id
    (-> (xt/db node)
        (xt/q {:find  '[?id]
               :where [['?id :xt/id id]]})
        first
        nil?
        not)
    false))

(defn list-by-ids-with-timestamps [node ids]
  (let [db (xt/db node)]
    (->> ids
         (map #(let [h (xt/entity-history db % :desc)]
                 (assoc (xt->id (xt/entity db %))
                        :created (-> h last ::xt/valid-time)
                        :updated (-> h first ::xt/valid-time)))))))

(defn get-by-id-with-timestamps [node id]
  (first (list-by-ids-with-timestamps (xt/db node) [id])))

(defn list-by-ids-with-history [node ids]
  (let [db (xt/db node)]
    (->> ids
         (map #(let [h (xt/entity-history db % :desc {:with-docs? true})]
                 (assoc (xt->id (xt/entity db %))
                        :history h))))))

(defn get-by-id-with-history [node id]
  (first (list-by-ids-with-history (xt/db node) [id])))

(defn get-key [node k]
  (let [q (xt/q (xt/db node) {:find  '[value]
                              :where '[[id :value value]]
                              :args  [{'id k}]})]
    (ffirst q)))

(defn evict-by-ids [node ids]
  (xt/submit-tx node
                (for [id ids]
                  [::xt/evict id])))

(defmethod ig/init-key :db/node
  [_ {:keys [dir]}]
  (letfn [(dirf [file] (io/file dir file))]
    (letfn [(kv-store [dir]
              {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store ;; 'avisi.xtdb.xodus/->kv-store
                          :db-dir (io/file dir)
                          :sync? true}})]
      (xt/start-node
       {:xtdb/tx-log (kv-store (dirf "tx"))
        :xtdb/document-store (kv-store (dirf "doc"))
        :xtdb/index-store (kv-store (dirf "idx"))}))))

(defmethod ig/halt-key! :db/node
  [_ xtdb-node]
  (.close xtdb-node))

;; On suspend, nothing is done
(defmethod ig/suspend-key! :db/node [_ _])

;; On resume, we call a function that checks if the new options match the old options
;; and if so, does nothing, otherwise re-initializes db
(defmethod ig/resume-key :db/node
  [key opts old-opts old-impl]
  (ig-utils/resume-handler key opts old-opts old-impl))

(defmethod ig/init-key :db/q
  [_ {:keys [node]}]
  (partial xt/q (xt/db node)))

(defmethod ig/init-key :db/tx
  [_ {:keys [node]}]
  (partial xt/submit-tx node))

(defmethod ig/init-key :db/tx>
  [_ {:keys [node]}]
  (->> (m/observe (fn [f]
                    (let [listener (xt/listen node {::xt/event-type ::xt/indexed-tx :with-tx-ops? true} f)]
                      #(.close listener))))
       (m/reductions {} (xt/latest-completed-tx node)) ; initial value is the latest known tx, possibly nil
       (m/relieve {})
       (m/latest (fn [{:keys [:xt/tx-time] :as ?tx}]
                   (if tx-time (xt/db node {::xt/tx-time tx-time})
                       ?tx
                       #_(xt/db node))))))

#_(defmethod ig/init-key :db.xtdb/submit-tx-fn
    [_ {:keys [node]}]
    (partial xt/submit-tx node))

#_(defmethod ig/init-key :db.xtdb/query-fn
    [_ {:keys [node]}]
    (partial xt/q node))

#_(defn latest-db>
    "return flow of latest XTDB tx, but only works for XTDB in-process mode. see
  https://clojurians.slack.com/archives/CG3AM2F7V/p1677432108277939?thread_ts=1677430221.688989&cid=CG3AM2F7V"
    [!xtdb]
    (->> (m/observe (fn [!]
                      (let [listener (xt/listen !xtdb {::xt/event-type ::xt/indexed-tx :with-tx-ops? true} !)]
                        #(.close listener))))
         (m/reductions {} (xt/latest-completed-tx !xtdb)) ; initial value is the latest known tx, possibly nil
         (m/relieve {})
         (m/latest (fn [{:keys [:xt/tx-time] :as ?tx}]
                     (if tx-time (xt/db !xtdb {::xt/tx-time tx-time})
                         (xt/db !xtdb))))))
