(ns shut-the-box.client.events
  (:require
    [clojure.spec.alpha :as s]
    [re-frame.core :refer [reg-event-db reg-event-fx] :as re-frame]
    [shut-the-box.client.db :as db]
    [shut-the-box.client.server :as server]
    [simple-peer :as Peer]
    [taoensso.timbre :as log]))


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
  ::set-ice-servers
  (fn [db [_ ice-servers]]
    (assoc db :ice-servers ice-servers)))

(defn add-peer!
  [initiator? peer-id ice-servers]
  (doto (Peer. #js {:initiator initiator?
                    :config #js {:iceServers (.parse js/JSON ice-servers)}})
        (.on "signal"
             (fn [data]
               (let [json (.stringify js/JSON data)]
                 #_(log/info "Signaling data for peer" peer-id ":" json)
                 (server/signal-peer! peer-id json))))
        (.on "connect"
             (fn []
               (log/info "Connected to peer" peer-id)
               (re-frame/dispatch [::peer-connected peer-id])))
        (.on "data"
             (fn [data]
               (log/info "Message from peer" peer-id ":" data)))
        (.on "stream"
             (fn [stream]
               (log/info "Got stream from peer" peer-id)
               (let [video (.querySelector js/document (str "#video-" peer-id))]
                 (set! (.-srcObject video) stream)
                 (.play video))
               (re-frame/dispatch [::add-remote-stream peer-id stream])))
        (.on "error"
             (fn [err]
               (log/error "RTC error:" err)))))

(reg-event-fx
  ::signal
  (fn [{:keys [db]} [_ peer-id json-data]]
    (let [peer (-> db :peers (get peer-id))]
      (.signal peer (.parse js/JSON json-data)))

    ;; No effects.
    nil))

(reg-event-fx
  ::peer-connected
  (fn [{:keys [db]} [_ peer-id]]
    ;; Have *we* started streaming video? If so, this peer would have
    ;; missed the initial stream, so give it to them now.
    (when-let [local-stream (get db :local-stream)]
      (log/info "Sending local stream to *new* peer" peer-id)
      (.addStream (-> db :peers (get peer-id)) local-stream))

    ;; No effects.
    nil))

(reg-event-db
  ::add-local-stream
  (fn [{:keys [player-id peers] :as db} [_ stream]]
    ;; Make this *our* avatar (but don't play audio, since that would
    ;; just produce a local echo).
    (let [video (.querySelector js/document (str "#video-" player-id))]
      (set! (.-muted video) true)
      (set! (.-srcObject video) stream)
      (.play video))

    ;; Send this stream to our existing peers.
    (doseq [[peer-id peer] peers]
      (log/info "Sending local stream to peer" peer-id)
      (.addStream peer stream))

    ;; Persist the local stream so that we can send it to any new
    ;; players that join.
    (-> db
        (assoc :local-stream stream)
        (update :video-players conj player-id))))

(reg-event-fx
  ::enable-video
  (fn [_ [_]]
    (-> js/navigator
        .-mediaDevices
        (.getUserMedia #js {:video #js {:width 96
                                        :height 96
                                        :facingMode "user"}
                            :audio true})
        (.then (fn [stream]
                 (re-frame/dispatch [::add-local-stream stream])))
        (.catch (fn []
                  (log/error "Unable to initialize camera."))))
    ;; No additional effects.
    nil))

(reg-event-db
  ::add-remote-stream
  (fn [db [_ peer-id]]
    (update db :video-players conj peer-id)))

(reg-event-db
  ::update-game
  (fn [{:keys [player-id ice-servers] :as db} [_ game-id game]]
    ;; Update the database.
    (let [db (assoc db
                    :state :joined
                    :game-id game-id
                    :game game)
          new-peers (clojure.set/difference
                      (-> db :game :players (dissoc player-id) keys set)
                      (-> db :peers keys set))]
      ;; Do we have a new peer? If so, figure out if we are the
      ;; initiator the receiver (the initiator is whichever one of us
      ;; has the smaller player-id) and construct the Peer object;
      ;; events on the Peer object will then drive the signaling process
      ;; through the server.
      (log/info "New Peers:" new-peers)
      (if (seq new-peers)
        ;; New peers; create and store Peer objects for each one.
        (update db :peers merge
                (into (hash-map)
                      (map (fn [peer-id]
                             [peer-id
                              (add-peer! (< player-id peer-id) peer-id ice-servers)])
                           new-peers)))

        ;; No new peers; return (already-updated-earlier) db as-is.
        db))))

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

(reg-event-fx
  ::end-turn
  (fn [{:keys [db]} [_]]
    ;; TODO This should probably be a `reg-fx` event? ::game-io/new-game?
    (server/end-turn! (:game-id db))))
