(ns build
  "build electric.jar library artifact and demos"
  (:require [clojure.tools.build.api :as b]
            [clojure.java.shell :as sh]
            [shadow.cljs.devtools.api :as shadow-api] ; so as not to shell out to NPM for shadow
            [shadow.cljs.devtools.server :as shadow-server]))

(def lib 'art.pictorian/website)
(def version (b/git-process {:git-args "describe --tags --long --always --dirty"}))
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:prod]}))

(def class-dir "target/classes")
(defn default-jar-name [{:keys [version] :or {version version}}]
  (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean-cljs [_]
  (b/delete {:path "resources/public/js"}))

(defn clean-css [_]
  (b/delete {:path "resources/public/css"}))

(defn clean [_opts]
  (println "Cleaning cljs compiler output")
  (clean-cljs nil)

  ;; (println "Cleaning css compiler output")
  ;; (clean-css nil)

  (b/delete {:path "target"}))

(defn run-shell-command [command]
  (println "Running shell command: " command)
  (let [{:keys [exit out err]} (apply sh/sh command)]
    (when err (println err))
    (when out (println out))
    (when-not (zero? exit)  (println "Exit code" exit) (throw (ex-info "Shell command failed" {:exit-code exit})))))

(defn build-css [& {:keys [watch]
                    :or {watch false}}]
  (println "Building css" (when watch "with watch"))
  (let [command (->> ["./node_modules/.bin/sass" "-I" "node_modules" "src/art/pictorian/web/main.scss" "resources/public/css/main.css"
                      (when watch "--watch")]
                     (remove nil?))]
    (run-shell-command command)))

(defn build-client
  "Prod optimized ClojureScript client build. (Note: in dev, the client is built on startup)"
  [{:keys [optimize debug verbose version]
    :or {optimize true, debug false, verbose false, version version}}]
  (println "Building client. Version:" version)
  (shadow-server/start!)
  (shadow-api/release :prod {:debug debug,
                             :verbose verbose,
                             :config-merge [{:compiler-options {:optimizations (if optimize :advanced :simple)}
                                             :closure-defines {'hyperfiddle.electric-client/VERSION version}}]})
  (shadow-server/stop!))

;; (defn compile-java [_]
;;   (println "Compiling java")
;;   (b/javac {:src-dirs ["src/java"]
;;             :class-dir class-dir
;;             :basis basis
;;             ;; :javac-opts ["-source" "11" "-target" "11"]
;;             }))

(defn uberjar [{:keys [jar-name version optimize debug verbose]
                :or   {version version, optimize true, debug false, verbose false}}]

  (println "Cleaning up before build")
  (clean nil)

  #_(build-css)
  (build-client {:optimize optimize, :debug debug, :verbose verbose, :version version})

  (println "Bundling sources")
  (b/copy-dir {:src-dirs   ["src" "resources" "env/prod/src" "env/prod/resources"]
               :target-dir class-dir})

  (println "Compiling server. Version:" version)
  (b/compile-clj {:basis      basis
                  :class-dir  class-dir
                  ;; :src-dirs   ["src" "env/prod/src"]
                  :ns-compile '[art.pictorian.core]})

  (println "Building uberjar")
  (b/uber {:basis     basis
           :class-dir class-dir
           :uber-file (str (or jar-name (default-jar-name {:version version})))
           :main      'art.pictorian.core}))

(defn noop [_])                         ; run to preload mvn deps
