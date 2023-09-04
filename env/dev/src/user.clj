(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [clj-async-profiler.core :as prof]
   [clojure.spec.alpha :as s]
   [clojure.tools.namespace.repl :as tns-repl]
   [expound.alpha :as expound]
   [hyperfiddle.rcf :as rcf]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [integrant.repl.state :as ig-state]
   [art.pictorian.config]
   [art.pictorian.core]
   [art.pictorian.web.apps.signals :refer [reset-system!]]
   [lambdaisland.classpath.watch-deps :as watch-deps]
   [lambdaisland.classpath :as licp]
   [shadow.cljs.devtools.api]
   [shadow.cljs.devtools.server]
   [portal.api :as p]))

;; uncomment to enable hot loading for deps
(when-not @watch-deps/watcher (watch-deps/start! {:aliases [:dev :test]}))
#_(licp/update-classpath! '{:aliases [:dev :test]})
(alter-var-root #'s/*explain-out* (constantly expound/printer))

;; morse inspector
#_(defonce mrs (morse/launch-in-proc))
#_(add-tap #'morse/inspect)

#_(add-tap (bound-fn* clojure.pprint/pprint))
(add-tap #'p/submit) ; Add portal as a tap> target

(defn dev-prep!
  []
  (ig-repl/set-prep! (fn []
                       (-> (art.pictorian.config/system-config {:profile :dev})
                           (ig/prep)))))

#_(defn test-prep!
    []
    (ig-repl/set-prep! (fn []
                         (-> (config/system-config {:profile :test})
                             (ig/prep)))))

;; Can change this to test-prep! if want to run tests as the test profile in your repl
;; You can run tests in the dev profile, too, but there are some differences between
;; the two profiles.
(dev-prep!)

(tns-repl/set-refresh-dirs "src" "env")
#_(ns-repl/disable-reload! art.pictorian.core)

#_(def refresh repl/refresh)

#_(def rcf-enable! (delay @(requiring-resolve 'hyperfiddle.rcf/enable!)))

#_(defn rcf-shadow-hook
    {:shadow.build/stages #{:compile-prepare :compile-finish}}
    [build-state & _args]
    build-state)

#_(defn install-rcf-shadow-hook
    []
    (alter-var-root #'rcf-shadow-hook
                    (constantly (fn [build-state & _args]
                                ;; NOTE this won’t prevent RCF tests to run
                                ;; during :require-macros phase
                                  (case (:shadow.build/stage build-state)
                                    :compile-prepare (@rcf-enable! false)
                                    :compile-finish (@rcf-enable!))
                                  build-state))))

(defn start-shadow
  []
  (shadow.cljs.devtools.server/start!)
  (shadow.cljs.devtools.api/watch :dev {:verbose false}))

(defn stop-shadow [] (shadow.cljs.devtools.server/stop!))

(defn alter-system [f]
  (let [result (f)]
    (reset-system! ig-state/system)
    result))

(defn start []
  #_(@rcf-enable! false)
  (rcf/enable! false)
  (start-shadow)
  ;; enable RCF after Datomic is loaded – to resolve circular dependency
  #_(install-rcf-shadow-hook)
  #_(@rcf-enable!)
  (rcf/enable!)
  (alter-system ig-repl/go))

(defn stop []
  (stop-shadow)
  (alter-system ig-repl/halt))

(defn reset []
  (alter-system ig-repl/reset))

(defn go []
  (alter-system ig-repl/go))

(defn reset-all []
  #_(alter-system ig-repl/reset-all)
  (ig-repl/halt)
  (tns-repl/refresh :after `go)
  #_(shadow.cljs.devtools.api/watch-compile! :dev))

(comment
  #_(kit/sync-modules)
  #_(kit/list-installed-modules)
  #_(kit/list-modules)
  #_(e/run (println (e/watch (-> state/system :db/tx>))))
  (start)
  (reset)
  (reset-all)
  (stop)
  (shadow.cljs.devtools.server/start!)
  (prof/profile (shadow.cljs.devtools.api/watch :dev))
  (prof/serve-ui 8080))
