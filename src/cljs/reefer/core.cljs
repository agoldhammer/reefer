(ns reefer.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reefer.events :as events]
   [reefer.views :as views]
   [reefer.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (r/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [::events/initialize-db])
  #_(rf/dispatch [::events/get-city-forecast "Venice,IT"])
  (rf/dispatch-sync [::events/get-forecasts
                     (events/cities-list :capitals)])
  #_(events/setup)
  (dev-setup)
  (mount-root))
