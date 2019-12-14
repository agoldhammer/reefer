(ns reefer.chartcomp
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reefer.subs :as subs]
            ["chart.js" :as chart]) )

;; TODO: second axis not working for now
(defn show-chart
  [element data]
  {:pre [(not (nil? element))]}
  (let [context (.getContext element "2d")
        ylabel1 (:ylabel1 data)
        ylabel2 (:ylabel2 data)
        selvar (:selvar data)
        chart-data {:type "line"
                    :data {:labels (:x data)
                           :datasets [{:data (:t data)
                                       :label ylabel1
                                       :yAxisID "l"
                                       :fill false
                                       :borderColor "blue"}
                                      {:data (get data selvar)
                                       :label ylabel2
                                       :yAxisID "r"
                                       :fill false
                                       :borderColor "red"}]}
                    :options {:responsive true
                              :title {:display true
                                      :text (:city-name data)}
                              :legend {:labels {:boxWidth 10}}
                              :scales
                              {:xAxes [{:type "category"
                                        :ticks {:autoskip true
                                                :maxTicksLimit 12}}]
                               :yAxes [{:id "l"
                                        :type "linear"
                                        :position "left"
                                        :scaleLabel {:display true
                                                     :fontColor "blue"
                                                     :labelString ylabel1
                                                     :beginAtZero false}}
                                       {:id "r"
                                        :type "linear"
                                        :position "right"
                                        :scaleLabel {:display true
                                                     :fontColor "red"
                                                     :labelString ylabel2
                                                     :beginAtZero false}}]}}}]
    (chart/Chart. context (clj->js chart-data))))

;; following this recipe
;; https://github.com/Day8/re-frame/blob/master/docs/Using-Stateful-JS-Components.md
(defn chart-inner []
  (let [canvas (r/atom nil)
        update (fn [this]
                 (let #_[data (:wxdata (r/props this))
                         city-name (:city-name (r/props this))]
                   [data (r/props this)]
                   (when @canvas
                     (show-chart @canvas data))))]
    (r/create-class
     {:display-name "chart-inner"
      :component-did-update update

      :component-did-mount
      (fn [this]
        (let [elt (r/dom-node this)]
          (reset! canvas elt)
          (update this)))

      :reagent-render
      (fn []
        [:canvas.chartcomp {:height "90%"
                            :padding "5px"}])})))

(defn chart-outer [city-id]
  (let [selvar @(rf/subscribe [::subs/selvar])
        ylabel1 @(rf/subscribe [::subs/varid-to-varname :t])
        ylabel2 @(rf/subscribe [::subs/varid-to-varname selvar])
        city-name @(rf/subscribe [::subs/city-name city-id])
        props (merge @(rf/subscribe
                       [::subs/get-datamap city-id])
                     {:city-name city-name
                      :selvar selvar
                      :ylabel1 ylabel1
                      :ylabel2 ylabel2})]
    (fn [_city-id]
      [chart-inner props])))
