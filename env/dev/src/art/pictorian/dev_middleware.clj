(ns art.pictorian.dev-middleware)

(defn wrap-dev [handler _opts]
  (-> handler))
