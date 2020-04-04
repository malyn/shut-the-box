(ns shut-the-box.client.events
  (:require
    [clojure.spec.alpha :as s]
    [re-frame.core :refer [reg-event-db reg-event-fx] :as re-frame]
    [taoensso.timbre :as log]
    [shut-the-box.client.agora-rtc :as agora]
    [shut-the-box.client.db :as db]
    [shut-the-box.client.rtc :as rtc]))


;; -- Interceptors ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (re-frame/after (partial check-and-throw ::db/app-db))
    []))


;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  validate-spec
  (fn [_ _]
    db/default-db))

(reg-event-db
  ::join-channel
  (fn [db [_ {:keys [channel uid on-success on-failure]}]]
    (agora/join-channel!
      @rtc/client
      channel
      uid
      (fn [channel uid]
        (re-frame/dispatch [on-success channel uid]))
      (fn [channel err]
        (re-frame/dispatch [on-failure channel err])))
    (assoc db
           :state :joining
           :channel channel
           :peers #{})))

(reg-event-db
  ::join-succeeded
  (fn [db [_ channel uid]]
    (log/info "Join succeeded for channel" channel "; uid=" uid)
    (assoc db
           :state :joined
           :channel channel
           :uid uid
           :peers #{})))

(reg-event-db
  ::join-failed
  (fn [db [_ channel err]]
    (log/error "Join failed for channel" channel "; err=" err)
    (assoc db
           :state :unjoin
           :peers #{})))

(reg-event-db
  ::peer-joined
  (fn [{:keys [peers] :as db} [_ uid]]
    (assoc db
           :peers (conj peers uid))))

(reg-event-db
  ::peer-left
  (fn [{:keys [peers] :as db} [_ uid]]
    (assoc db
           :peers (disj peers uid))))
