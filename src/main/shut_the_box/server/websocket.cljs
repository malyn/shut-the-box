(ns shut-the-box.server.websocket
  (:require
    [cljs.core.async :refer [<! put!] :as async]
    [clojure.edn :as edn]
    [medley.core :refer [filter-vals]]
    [shut-the-box.common.logic.game :as game-logic]
    [shut-the-box.server.config :refer [env]]
    [shut-the-box.server.db :as db]
    [taoensso.timbre :as log]
    [twilio :as Twilio]))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Twilio client
;;

(defonce twilio-client
  (Twilio. (:twilio-account-sid @env) (:twilio-auth-token @env)))

(defn twilio-ice-servers!
  []
  (let [ch (async/chan)]
    (-> twilio-client
        .-tokens
        .create
        (.then (fn [token]
                 (put! ch (.stringify js/JSON (.-iceServers token))))))
    ch))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket Client List and ws-send! Function
;;

(defonce sockets
  (atom {}))

(defn ws-send!
  [socket-id msg]
  (some-> (get @sockets socket-id)
          (.send (prn-str msg))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game Storage/Broadcast
;;

(defn update-game!
  [game-id game]
  ;; Persist the game state.
  (db/put-game! @db/conn game-id game)

  ;; Loop over all of the players and send them the new game state.
  (doseq [player-id (-> game :players keys)]
    ;; TODO Should really do this send with a *player-id* list
    ;; and not assume that player-id is 1:1 with socket-id
    ;; (which won't be true once we have real auth and allow
    ;; reconnects).
    (try
      (ws-send! player-id [:game game-id game])
      (catch :default err
        (log/errorf "[p%d] ws-send! threw error: %s" player-id err)))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; High-Level Protocol
;;
;; TODO All of these need to be in a loop, with three possible exits:
;; logic and put succeeds; logic succeeds, put fails (conflict), logic
;; fails (already started), but we call this success (because the whole
;; point was to start the round); logic fails outright because the state
;; is invalid. (see the TODO.md for a discussion of all of this, because
;; this approach is quickly falling apart and we need to move to more of
;; the actor/clock model).

(defn welcome [socket-id player-id]
  (async/go
    (let [ice-servers (<! (twilio-ice-servers!))]
      (ws-send! socket-id [:welcome player-id ice-servers]))))

(defn new-game [socket-id player-id player-name]
  (async/go
    (let [game (-> (game-logic/new)
                   (game-logic/add-player player-id player-name))
          game-id (<! (db/create-game! @db/conn game))]
      (ws-send! socket-id [:game game-id game]))))

(defn join-game [socket-id game-id player-id player-name]
  (async/go
    ;; Get the game and add the player to the game.
    (if-let [game (some-> (<! (db/get-game @db/conn game-id))
                          (game-logic/add-player player-id player-name))]
      (update-game! game-id game)

      ;; Couldn't add the player; probably the game has already started
      ;; (or is gone).
      (ws-send! socket-id [:err]))))

(defn start-round [socket-id game-id player-id]
  (async/go
    ;; Get the game and start the round. This fails if the round has
    ;; already started, but for now we assume that only one person can
    ;; start the round (ideally the actor thing would make this smarter,
    ;; since we could reject the request entirely if the player is out
    ;; of sync).
    (if-let [game (some-> (<! (db/get-game @db/conn game-id))
                          (game-logic/start-round))]
      ;; Pick a random player and make them the active player. It
      ;; shouldn't be possible for this to fail... I feel like this is
      ;; where Rust would be better, because we could just `?` this and
      ;; let the whole function fail rather than adding error logic for
      ;; something that shouldn't really be possible.
      (let [starting-player-id (rand-nth (-> game :players keys))
            ;; TODO This is just to ensure that we can easily test
            ;; against the PC; remove this before checkin.
            ;; starting-player-id (first (-> game :players keys))
            game (game-logic/set-active-player game starting-player-id)]
        (update-game! game-id game))

      ;; Couldn't start the round; probably the game has already started
      ;; (or is gone). Unfortunately we have to return an error here,
      ;; even though returning the game state might be the most accurate
      ;; thing. This is where having the sync/actor logic would be
      ;; better, because then we could have rejected this earlier and
      ;; the client could re-sync.
      (ws-send! socket-id [:err]))))

(defn roll-dice [socket-id game-id player-id]
  (async/go
    (if-let [game (some-> (<! (db/get-game @db/conn game-id))
                          (game-logic/roll-dice player-id))]
      ;; Return the current state, regardless of if the player is done
      ;; or not. If the player is not done then they will come back with
      ;; a :shut-tiles command; if they *are* done then they will come
      ;; back with an :end-turn.
      (update-game! game-id game)

      ;; Couldn't roll the dice, but we don't actually know why (maybe
      ;; the player is not active, maybe they are not in the game,
      ;; etc.).
      (ws-send! socket-id [:err]))))

(defn end-turn [socket-id game-id player-id]
  (async/go
    (if-let [game (some-> (<! (db/get-game @db/conn game-id))
                          (game-logic/end-turn player-id))]
      ;; Player is done, let's see if everyone is done.
      (if-let [new-player-id (->> (:players game)
                                  (filter-vals #(= :waiting (:state %)))
                                  keys
                                  rand-nth)]
        ;; Selected another player; make this player the active player
        ;; and broadcast *that* game state.
        (update-game! game-id (game-logic/set-active-player game new-player-id))

        ;; Could not select another player; mark the game as done and
        ;; broadcast *that* game state. This is where the game lands
        ;; if someone shut's the box, BTW, since the game logic will
        ;; mark everyone as done as soon as anyone shuts the box.
        (update-game! game-id (assoc game :state :done)))

      ;; Couldn't roll the dice, but we don't actually know why (maybe
      ;; the player is not no-moves, maybe they are not in the game,
      ;; etc.).
      (ws-send! socket-id [:err]))))

(defn shut-tiles [socket-id game-id player-id tiles]
  (async/go
    (if-let [game (some-> (<! (db/get-game @db/conn game-id))
                          (game-logic/shut-tiles player-id tiles))]
      (update-game! game-id game)

      ;; Couldn't shut tiles, but we don't actually know why (maybe the
      ;; player is not active, maybe they are not in the game, maybe
      ;; their tile choices were bad, etc.).
      (ws-send! socket-id [:err]))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Low-Level WebSocket handler
;;

(defonce last-socket-id
  (atom 0))

(defn next-socket-id []
  (swap! last-socket-id inc))

(defn ws-connected
  [socket-id websocket]
  (swap! sockets assoc socket-id websocket)
  (welcome socket-id socket-id))

(defn ws-closed
  [socket-id websocket]
  ;; TODO Clean up the socket: remove the player from the channel, let
  ;; everyone else know about the disconnect. *Maybe.* Or maybe we just
  ;; remove the socket-id-to-socket mapping and then timeout the player
  ;; if it is their turn and they have taken longer than 30s to respond.
  ;; (*that* logic would work anytime, regardless of if the player is
  ;; connected or not)
  (swap! sockets dissoc socket-id websocket))

(defn ws-message
  [socket-id websocket message]
  (let [[op & args] (edn/read-string message)
        player-id socket-id]
    (log/debugf "[%d] Rcvd: op=%s, args=%s" socket-id op args)
    (case op
      :new-game (let [[player-name] args]
                  (new-game socket-id player-id player-name))
      :join-game (let [[game-id player-name] args]
                   (join-game socket-id game-id player-id player-name))
      :start-round (let [[game-id] args]
                     (start-round socket-id game-id player-id))
      :roll-dice (let [[game-id] args]
                   (roll-dice socket-id game-id player-id))
      :shut-tiles (let [[game-id tiles] args]
                    (shut-tiles socket-id game-id player-id tiles))
      :end-turn (let [[game-id] args]
                  (end-turn socket-id game-id player-id))
      :signal (let [[peer-id json-data] args]
                (ws-send! peer-id [:signal player-id json-data]))
      (do (log/warnf "[%d] Unknown command %s" socket-id op)
          (.send websocket (prn-str [:err]))))))

(defn handler
  "Raw WebSocket handler provided to Macchiato."
  [{:keys [remote-addr websocket] :as req}]
  (let [socket-id (next-socket-id)]
    (log/infof "[%d] WebSocket opened from %s" socket-id remote-addr)
    (ws-connected socket-id websocket)
    (.on websocket "message"
         (fn [message]
           (try
             (ws-message socket-id websocket message)
             (catch :default err
               (log/errorf "[%d] ws-message threw error: %s" socket-id err)
               (.terminate websocket)))))
    (.on websocket "close"
         (fn []
           (log/infof "[%d] WebSocket closed by %s" socket-id remote-addr)
           (ws-closed socket-id websocket)))))
