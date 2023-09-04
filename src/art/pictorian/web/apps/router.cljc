(ns art.pictorian.web.apps.router
  #?(:cljs
     (:import goog.history.Html5History
              goog.history.Html5History.TokenTransformer
              goog.history.EventType))
  (:require [contrib.cljs-target :refer [do-browser]]
            #?(:cljs goog.events)
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.rcf :refer [tests tap % with]]
            [missionary.core :as m]
            [lambdaisland.uri :as uri]
            [clojure.string :as str]))

#?(:cljs
   (do-browser

    (defn new-goog-history
      ([] (new-goog-history (fn [path-prefix location]
                              (str (.-pathname location)
                                   (.-search location)
                                   (.-hash location)))))
      ([retrieve-fn]
       (let [transformer (TokenTransformer.)]
         (set! (.. transformer -retrieveToken) retrieve-fn)
         (set! (.. transformer -createUrl) (fn [token path-prefix location] (str path-prefix token)))
         (doto (Html5History. js/window transformer)
           (.setUseFragment false)
           (.setPathPrefix "")
           (.setEnabled true)))))

    (defn path> [^js h]
      (m/observe (fn mount [!]
                   (let [k (goog.events/listen h EventType.NAVIGATE (fn [^js e] (! (.-token e))))]
                     (! (.getToken h))
                     (fn unmount []
                       (goog.events/unlistenByKey k))))))

     ;; Usage from Missionary

    #_(tests "goog-history-router"
             (.isSupported Html5History js/window) := true
             (def h (new-goog-history))
             (def >x (m/cp (tap (m/?< (m/relieve {} (path> h))))))
             (def it (>x #(tap ::notify) #(tap ::terminate)))
             % := ::notify
             @it
             % := "/"
             (.setToken h "/a")
             % := ::notify
             @it
             % := "/a"
             (.setToken h "/b")
             % := ::notify
             (.setToken h "/")
             @it
             % := "/"
             (it))))

;; Usage from Electric

(e/def !history (e/client (new-goog-history)))
(e/def path (e/client (new (m/relieve {} (path> !history)))))

;; hack Cannot infer target type in expression, todo Electric compiler not propagating hint
#?(:cljs (defn -historySetToken [^js !history href] (.setToken !history href)))

(e/defn Link [route-params F]
  (e/client
    (let [href (str "?" (uri/map->query-string route-params))]
      (tap href)
      (dom/a {:href href}
             (dom/on "click" (e/fn [e]
                               (-historySetToken !history href)
                               (.preventDefault e)))
             (F.)))))

(defn parse-route [path]
  (let [query-params-map (-> path (str/split #"\?") (second) uri/query-string->map)]
    (tap query-params-map)
    query-params-map))
