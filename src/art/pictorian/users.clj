(ns art.pictorian.users
  (:require
   [buddy.hashers :as hashers]
   [hyperfiddle.rcf :refer [% tap tests]]
   [art.pictorian.db :as db]
   [xtdb.api :as xt]))

(defn find-user
  "Find user by email"
  [node email]
  (ffirst (xt/q (xt/db node)
                '{:find [(pull ?user [*])]
                  :in [email]
                  :where [[?user :user/email email]]}
                email)))

(defn add-user
  "Add user to database"
  [node {:keys [email password]}]
  (when-not (find-user node email)
    (db/transact-sync node
                      [[::xt/put
                        {:xt/id (random-uuid) #_(keyword "user" email)
                         :user/email email
                         :user/password (hashers/derive password)}]])))

(tests "Create user if not exists"
       (let [data {:email "foo@bar.com"
                   :password "foopas"}]
         (with-open [node (xt/start-node {})]
           (future (tap (add-user node data))
                   (tap (add-user node data))
                   (tap (find-user node "foo@bar.com")))
           (map? %) := true
           % := nil
           (map? %) := true)))
