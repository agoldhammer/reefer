(ns reefer.events
  #_(:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [re-frame.core :as rf]
   [reefer.db :as db]
   [reefer.subs :as subs]
   [cljs-http.client :as http]
   [cljs.core.async :as async :refer [take!]]
   #_[cljs-ajax :as ajax]
   [day8.re-frame.tracing :refer-macros [fn-traced #_defn-traced]]
   ))

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(rf/reg-event-db
 ::good-http-result
 (fn [db [_ value]]
   (assoc db :wx value)))

(defn erase-forecasts
  [db]
  (dissoc db :forecasts))

(defn set-pending
  [db [city-id city-name]]
  (assoc-in (erase-forecasts db) [:forecasts :city-id city-id]
            {:city-name city-name :status :pending}))

(defn cities-list
  [list-id]
  (seq @(rf/subscribe [::subs/city-list list-id])))

(declare wx-loc2)
(rf/reg-fx
 ::ajax
 (fn 
   [cities-list]
   #_(prn "ajax" cities-list)
   (doall (map wx-loc2 cities-list))))

(rf/reg-event-db
 ::set-pending
 (fn [db [_ city-list]]
   (reduce #(set-pending %1 %2) db city-list)))

;; a loc is a vector [city-id city-name]
;; sets all city id statuses to :pending
;; usage (rf/dispatch [::get-forecasts (cities-list :capitals | :italy | :france)])
(rf/reg-event-fx
 ::get-forecasts
 (fn [_ [_ city-list]]
   {:dispatch [::set-pending city-list]
    ::ajax city-list}))

(rf/reg-event-db
 ::http-result
 (fn [db [_ [city-id city-name response]]]
   #_(prn "http" city-name (:success response))
   (let [m
         (if (:success response)
           {:status :valid
            :wxdata response
            :city-name city-name}
           {:status :error
            :code (:status response)})]
     (update-in db [:forecasts :city-id city-id] merge m))))

(defn wx-loc2
  [[city-id city-name]]
  (let [cname (clojure.string/replace city-name "," "%2C")
        proto (.. js/document -location -protocol)
        hostname (.. js/document -location -hostname)
        port (.. js/document -location -port)
        port2 (if (= port "") "" (str ":" port))
        wxurl (str proto "//" hostname  port2 "/wx/")]
    (take! (http/get (str wxurl cname) {:with-credentials? false})
           #(rf/dispatch [::http-result [city-id city-name %]]))))

(defn setup []
  (map wx-loc2 (cities-list ::subs/cities)))

(rf/reg-event-db
 ::change-selvar
 (fn [db [_ selvar]]
   (assoc db :selvar selvar)))

(comment
 ;; setup
  (map wx-loc2 (seq @(rf/subscribe [::subs/cities])))
  (map wx-loc2 (cities-list ::subs/cities))
  (map wx-loc2 @(rf/subscribe [::subs/countries :france])))
