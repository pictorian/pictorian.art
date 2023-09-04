(ns art.pictorian.utils
  (:require
   [buddy.core.codecs :as codecs]
   [buddy.core.hash :as hash]
   [clojure.string :as string]))

(defn keywordize [s]
  (when s
    (-> (string/lower-case s)
        (string/replace "_" "-")
        (string/replace "." "-")
        (keyword))))

(defn csv-data->map [csv-data]
  (map zipmap
       (->> (first csv-data)                                ;; First row is the header
            (map keyword)                                   ;; Drop if you want string keys instead
            repeat)
       (rest csv-data)))

(defn map-values->hex-hash [m]
  (-> (str (vals m))
      (hash/md5)
      (codecs/bytes->hex)))
