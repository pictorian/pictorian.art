(ns art.pictorian.web.apps.admin.dashboard
  (:require
   [clojure.string :as str]
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [hyperfiddle.electric-ui4 :as ui]
   [hyperfiddle.history :as history]
   [art.pictorian.constants :as constants]
   [art.pictorian.web.apps.signals :as signals]))

#_(e/defn Controls []
    (<% :input.calendar
        {:type :datetime-local,
         :value "1974-12-18T19:05",
         :on-input (e/fn [e] (println e))}))

(e/defn PicRow
  [{:keys [id date status]}]
  (dom/tr
    (dom/td (dom/img (dom/props {:src nil
                                 :class "object-cover h-8"})))
    (dom/td (dom/text (str date)))
    (dom/td (dom/text (str status)))
    (dom/td (dom/div
              (dom/props {:class "button-group"})
              (history/link {:page :view-pic :pic id}
                            (dom/props {:class "button primary"})
                            (dom/text ""))
              (ui/button (e/fn [] (println "click"))
                         (dom/props {:class "button drop"})
                         (dom/text "Сбросить"))))))

(e/defn PicTable
  [limit offset statuses search]
  (dom/table
    (dom/props {:class "pic-table"})
    (dom/thead
      (dom/tr
        (dom/th (dom/text "Картина"))
        (dom/th (dom/text "Дата"))
        (dom/th (dom/text "Статус"))
        (dom/th (dom/text "Действия"))))
    #_(dom/tbody
        (e/for-by :card/id
                  [card (e/server
                          (e/offload
                           #(pics/find-user-pics
                             signals/node
                             {:statuses statuses :number search :limit limit :offset offset})))]
          (e/client (CardRow. card))))
    (dom/element :tfoot
      (dom/tr
        (dom/td
          (ui/button
            (e/fn [] (println "click") (history/swap-route! update ::limit
                                                            #(if % (+ % constants/default-page-size) (* 2 constants/default-page-size))))
            (dom/text "Загрузить еще")))))))

(def data {:alice   {:name "Alice B"}
           :bob     {:name "Bob C"}
           :charlie {:name "Charlie D"}
           :derek   {:name "Derek E"}})

(defn q [search] (into [] (keep (fn [[k {nm :name}]] (when (str/includes? nm search) k))) data))

(e/defn TagPicker []
  (e/server
    (let [!v (atom #{:alice :bob})]
      (ui/tag-picker (e/watch !v)
                     (e/fn [v] (e/client (prn [:V! v])) (swap! !v conj v))
                     (e/fn [v] (prn [:unV! v]) (swap! !v disj v))
                     (e/fn [search] (e/client (prn [:Options search])) (q search))
                     (e/fn [id] (e/client (prn [:OptionLabel id])) (-> data id :name))))))

(e/defn DashboardPage []
  (dom/h2 (dom/text "Картины"))
  #_(Controls.)
  (let [{:keys [::statuses ::search ::limit ::offset] :as _s} history/route]
    (ui/input search (e/fn V! [v] (history/swap-route! assoc ::search v)) ; todo (swap! router/!route assoc ::search v)
              (dom/props {:placeholder "Поиск" :type "search"}))
    #_(ui/tag-picker statuses
                     (e/fn V! [v] (history/swap-route! conj ::statuses v))
                     (e/fn V! [v] (history/swap-route! disj ::statuses v))
                     (e/fn [search] (e/client (prn [:Options search])) (q search))
                     (e/fn [id] (e/client (prn [:OptionLabel id])) (-> data id :name))
                     (dom/props {}))
    #_(TagPicker.)
    #_(let [statuses (or statuses #{})]
        (ui/tag-picker (e/watch statuses)
                       (e/fn [v] (history/swap-route! conj ::statuses v))
                       (e/fn [v] (history/swap-route! disj ::statuses v))
                       (e/fn [search] (e/client (prn [:Options search])) (q search))
                       (e/fn [id] (e/client (prn [:OptionLabel id])) (-> data id :name))))
    (dom/hr)
    (new PicTable (or limit constants/default-page-size) offset [:NOT_TAKEN] search)))
