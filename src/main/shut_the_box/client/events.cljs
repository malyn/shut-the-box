(ns shut-the-box.client.events
  (:require
    [clojure.spec.alpha :as s]
    [re-frame.core :refer [reg-event-db reg-event-fx] :as re-frame]
    [taoensso.timbre :as log]
    [shut-the-box.client.agora-rtc :as agora]
    [shut-the-box.client.db :as db]
    [shut-the-box.client.rtc :as rtc]
    [shut-the-box.client.server :as server]))


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
  ::set-player-id
  (fn [db [_ player-id]]
    (assoc db :player-id player-id)))

(reg-event-db
  ::update-game
  (fn [db [_ game-id game]]
    (assoc db
           :state :joined
           :game-id game-id
           :game game)))

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
  ::new-game
  (fn [db [_]]
    (assoc db :creating-new-game? true)))

(reg-event-db
  ::update-player-name
  (fn [db [_ player-name]]
    (assoc db :player-name player-name)))

(reg-event-fx
  ::start-new-game
  (fn [{:keys [db]} [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    (server/new-game! (:player-name db))
    {:db (assoc db
                :creating-new-game? false
                :state :joining)}))

(reg-event-db
  ::cancel-new-game
  (fn [db [_]]
    (assoc db :creating-new-game? false)))

(reg-event-db
  ::join-game
  (fn [db [_]]
    (assoc db :joining-game? true)))

(reg-event-db
  ::update-game-id
  (fn [db [_ game-id]]
    (assoc db :game-id game-id)))

(reg-event-fx
  ::start-join-game
  (fn [{:keys [db]} [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    ;; TODO Should verify with a spec that game-id in the db is always a
    ;; number (since we had that wrong at first).
    (server/join-game! (:game-id db)
                       (:player-name db))
    {:db (assoc db
                :joining-game? false
                :state :joining)}))

(reg-event-db
  ::cancel-join-game
  (fn [db [_]]
    (assoc db :joining-game? false)))

(reg-event-fx
  ::start-round
  (fn [{:keys [db]} [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    (server/start-round! (:game-id db))))

(reg-event-fx
  ::roll-dice
  (fn [{:keys [db]} [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    (server/roll-dice! (:game-id db))))

(reg-event-db
  ::select-tile
  (fn [db [_ tile]]
    (update db :selected-tiles conj tile)))

(reg-event-db
  ::deselect-tile
  (fn [db [_ tile]]
    (update db :selected-tiles disj tile)))

(reg-event-db
  ::shut-tiles
  (fn [db [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    (server/shut-tiles! (:game-id db) (vec (:selected-tiles db)))
    (assoc db :selected-tiles #{})))

(reg-event-db
  ::undo-tiles
  (fn [db [_ tile]]
    (assoc db :selected-tiles #{})))

(reg-event-fx
  ::publish-video
  (fn [{:keys [db]} [_ {:keys [on-success on-failure]}]]
    (agora/create-stream!
      @rtc/client
      "video-me"
      (:uid db)
      true
      true
      (fn []
        (re-frame/dispatch [on-success]))
      (fn []
        (re-frame/dispatch [on-failure])))))

(reg-event-fx
  ::subscribe-stream
  (fn [_ [_ {:keys [uid stream on-failure]}]]
    (agora/subscribe-stream!
      @rtc/client
      stream
      (fn []
        (re-frame/dispatch [on-failure uid])))))

(reg-event-fx
  ::join-succeeded
  (fn [{:keys [db]} [_ channel uid]]
    (log/info "Join succeeded for channel" channel "; uid=" uid)
    {:db (assoc db
                :state :joined
                :channel channel
                :uid uid
                :peers #{})
     :dispatch [::publish-video {:on-success ::publish-succeeded
                                 :on-failure ::publish-failed}]}))

(reg-event-db
  ::publish-succeeded
  (fn [db [_]]
    db))

(reg-event-db
  ::publish-failed
  (fn [db [_]]
    db))

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

(reg-event-fx
  ::stream-added
  (fn [{:keys [db]} [_ uid stream]]
    (log/info "Stream added for uid" uid)
    (when (not= (:uid db) uid)
      {:dispatch [::subscribe-stream {:uid uid
                                      :stream stream
                                      :on-failure ::subscribe-failed}]})))

(reg-event-fx
  ::stream-subscribed
  (fn [{:keys [db]} [_ uid stream]]
    (log/info "Stream subscribed for uid" uid)
    (agora/play-stream!
      stream
      (str "video-" uid))
    ;; TODO Mark the player's stream as playing.
    {:db db}))
