(ns art.pictorian.charts.main-chart
  (:require
   [com.fulcrologic.statecharts.algorithms.v20150901-validation :as v]
   [com.fulcrologic.statecharts.chart :refer [statechart]]
   [com.fulcrologic.statecharts.elements :refer [data-model parallel]]
   [integrant.core :as ig]
   [art.pictorian.charts.flow :refer [flow-state]]
   [art.pictorian.settings :refer [get-settings]]))

(defn main-chart [node]
  (statechart {}
              (parallel {:id :system}
                        (data-model {:expr {:settings (get-settings node)}})
                        (flow-state))))

(defmethod ig/init-key :chart/main-chart
  [_ {:keys [node]}]
  (main-chart node))

(comment
  (v/problems main-chart))

(comment
  (require '[charts-dev.chart-io :refer [statechart->plantuml]])
  (spit "/tmp/main-chart.plantuml" (statechart->plantuml main-chart)))
