(ns art.pictorian.test-utils)

;; (defn system-state
;;   []
;;   #_(or @core/system state/system)
;;   (deref signals/!system))

;; (defn system-fixture
;;   []
;;   (fn [f]
;;     (when (nil? (system-state))
;;       (core/start-app {:opts {:profile :test}}))
;;     (f)))
