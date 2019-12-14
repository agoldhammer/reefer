(ns reefer.views
  (:require
   #_[reagent.core :as r]
   [re-frame.core :as rf]
   [re-com.core :as re-com]
   [reefer.subs :as subs]
   [reefer.events :as events]
   [reefer.chartcomp :as chartcomp]))
  
  
(defn title []
  [re-com/title
   :label "Weather (times are UTC)"
   :level :level3])

(defn varsel
  "radio buttons to select second display variable"
  []
  (fn []
    [re-com/h-box
     :gap "20px"
     :padding "10px"
     :children (doall (for [v [:w :p :h]]
                        ^{:key v} 
                        [re-com/radio-button
                         :label @(rf/subscribe
                                  [::subs/varid-to-varname v])
                         :value v
                         :model (rf/subscribe [::subs/selvar])
                         :on-change #(rf/dispatch 
                                      [::events/change-selvar %])]))]))

(defn sbutton
  [sid stext]
  [:button {:on-click
            #(rf/dispatch 
              [::events/get-forecasts (events/cities-list sid)])}
   stext])

(defn  get-img-url-prefix []
  (let
   [proto (.. js/document -location -protocol)
    hostname (.. js/document -location -hostname)
    port (.. js/document -location -port)
    port2 (if (= port "") "" (str ":" port))
    wxurl (str proto "//" hostname  port2 "/vendor/images/")]
    wxurl))

(def icon-style {:style {:background-color "darksalmon" :width "2.4%"}})

(defn icon-src [index]
  {:src (str (get-img-url-prefix) index ".png")})

(defn icon-def [index]
  (merge icon-style (icon-src index)))

(defn temp-chart [city-id]
  (let [status @(rf/subscribe [::subs/get-status city-id])
        selvar @(rf/subscribe [::subs/selvar])
        ylabel1 @(rf/subscribe [::subs/varid-to-varname :t])
        ylabel2 @(rf/subscribe [::subs/varid-to-varname selvar])
        city-name @(rf/subscribe [::subs/city-name city-id])
        data-map @(rf/subscribe [::subs/get-datamap city-id])
        icons (:i data-map)]
    (condp = status
      :valid [:div 
              [chartcomp/chart-inner
               (merge data-map
                {:city-name city-name
                 :selvar selvar
                 :ylabel1 ylabel1
                 :ylabel2 ylabel2})]
              (into [:div {:style {:display "inline-block" :padding-left "50px"}}]
                    (map (fn [icon] [:img (icon-def icon)]) icons))]
      :error [:p "error fetching:" (str " city-id:" city-id)]
      :pending [:p "pending"]
      [:p "No data!"])))



(defn main-panel []
  [re-com/v-box
   :height "100%"
   :width "70%"
   :gap "5px"
   :padding "5px"
   :children [[:div {:style {:background-color "lightblue"
                             :padding "5px"
                             :height "20%"}}
               [title]
               [:div
                (sbutton :capitals "Capitals")
                (sbutton :france "France")
                (sbutton :italy "Italy")
                (sbutton :germany "Germany")
                (sbutton :uk "UK")
                (sbutton :us "US")]
               [varsel]]
              [:div {:style {:maxHeight "80vh"
                             :overflow "auto"}}
               (into [:div]
                     (mapv temp-chart @(rf/subscribe [::subs/city-ids])))]]])




;; http://api.openweathermap.org/data/2.5/weather?q=London&appid=4ebbe49da36f4bdd3aeb1b82e10d9e6c


