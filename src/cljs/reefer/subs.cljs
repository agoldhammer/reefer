(ns reefer.subs
  (:require
   [re-frame.core :as rf]
   [goog.string :as gs]))

(defn xform-fc-item
  "transform a single forecast item, extracting key wx variables"
  [item]
  (let [main (:main item)
        wind (:speed (:wind item))
        time (:dt_txt item)
        icon (:icon (nth (:weather item) 0))
        {:keys [temp pressure humidity]} main]
    {:x time :t temp :p pressure :h humidity :w wind :i icon}))

(defn extract-day-hour
  "extract the day and hour from datetime of form yyyy-mm-dd hh:mm:ss"
  [datetime-string]
  (js->clj (gs/splitLimit (subs datetime-string 8 13) " " 2)))

;; get the city list for a country; list-ids :capitals, :italy, :france, ...
(rf/reg-sub
 ::city-list
 (fn [db [_ list-id]]
   (get db list-id :no-such-country)))

;; raw wxdata, not used in app, for testing purposes
(rf/reg-sub
 ::raw-wxdata
 (fn [db _]
   (:wx db)))

;; get the main subclasses of each city's wxdata
(defn xform-city-forecast
  [city-fc]
  (let [fcs (:list (:body city-fc))]
    (doall (map #(select-keys % [:main :weather :clouds :wind :dt_txt]) fcs))))

;; get forecast for each city-id
(rf/reg-sub
 ::city-forecast
 (fn [db [_ city-id]]
   (get-in db [:forecasts :city-id city-id :wxdata])))

;; sort the city ids numerically; TODO: might want to sort on city name
(rf/reg-sub
 ::city-ids
 (fn [db [_]]
   (sort (keys (get-in db [:forecasts :city-id])))))

;; get the city name from city forecast
(rf/reg-sub
 ::city-name
 (fn [_ [_ city-id]]
   (->> @(rf/subscribe [::city-forecast city-id])
        :body
        :city
        :name)))

;; call (subscribe [::subs/get-status id])
;; returns: :pending :valid :error
(rf/reg-sub
 ::get-status
 (fn [db [_ id]]
   (get-in db [:forecasts :city-id id :status])))

;; transform a city forecast usings xform-city-forecast to extract key vars
(defn xform-fc
  [id]
  (->> @(rf/subscribe [::city-forecast id])
       xform-city-forecast
       (map xform-fc-item)))
 
(defn munge-date-time
  "squash day and hour together for graph labels on x axis"
  [dt]
  (let [[day hour] (extract-day-hour dt)]
    (str day "-" hour "h")))

(defn reshape-forecast-for-var
  "transform from list of maps to map of list for var v
  if v is :x, munge date and hour together with munge-date-time
  usage: (reshape-forecast-for-var :t)"
  [id v]
  (let [data-map (xform-fc id)
        munger (if (= :x v) munge-date-time
                   identity)]
    {v (for [item data-map]
         (munger (get item v)))}))

(defn combine-all-wx-vars
  "for city-id, returns map:
{:x (dt1, dt2, ...), :t (t1, t2, ...), :p (p1, p2, ...)}"
  [city-id]
  (let [vs '(:x :t :p :h :w :i)
        vmaps (mapv #(reshape-forecast-for-var city-id %) vs)]
    (apply merge vmaps)))

;; combine all vars using above fn, merge in city name
(rf/reg-sub
 ::get-datamap
 (fn [_ [_ city-id]]
   (merge 
    (combine-all-wx-vars city-id)
    {:city-name @(rf/subscribe [::city-name city-id])})))

;; maps vars to human-readable names: :t Temp :p Press etc.
(rf/reg-sub
 ::varid-to-varname
 (fn [db [_ varid]]
   (get-in db [:varnames varid])))

;; returns id of selected radio button for mapping second graph var
(rf/reg-sub
 ::selvar
 (fn [db _]
   (:selvar db)))
