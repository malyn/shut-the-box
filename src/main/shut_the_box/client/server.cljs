(ns shut-the-box.client.server
  (:require
    [cljs.core.async :as async]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch]]
    [shut-the-box.client.game-client :as game-client]
    [taoensso.timbre :as log]))

(defn game-client-message
  [[msg & data]]
  (case msg
    :welcome
    (let [[player-id ice-servers] data]
      (dispatch [:shut-the-box.client.events/set-player-id player-id])
      (dispatch [:shut-the-box.client.events/set-ice-servers ice-servers]))

    :game
    (let [[game-id game] data]
      (dispatch [:shut-the-box.client.events/update-game game-id game]))

    :signal
    (let [[peer-id json-data] data]
      #_(log/info "RTC signaling from" peer-id ":" json-data)
      (dispatch [:shut-the-box.client.events/signal peer-id json-data]))

    :err
    (log/error "Error:" data)

    (println "Ignored:" msg)))

(defn game-client-loop
  [conn]
  (async/go-loop []
    (when-let [msg (<! (game-client/recv-ch conn))]
      (game-client-message msg)
      (recur))))

(defstate ^{:on-reload :noop} conn
  :start (let [conn (game-client/connect!
                      (str "wss://" (-> js/document .-location .-host)))]
           (println "Connected to game server.")
           (game-client-loop conn)
           conn))

(defn new-game!
  [player-name]
  (game-client/send! @conn [:new-game player-name]))

(defn join-game!
  [game-id player-name]
  (game-client/send! @conn [:join-game game-id player-name]))

(defn start-round!
  [game-id]
  (game-client/send! @conn [:start-round game-id]))

(defn roll-dice!
  [game-id]
  (game-client/send! @conn [:roll-dice game-id]))

(defn shut-tiles!
  [game-id tiles]
  (game-client/send! @conn [:shut-tiles game-id tiles]))

(defn end-turn!
  [game-id]
  (game-client/send! @conn [:end-turn game-id]))

(defn signal-peer!
  [peer-id json-data]
  (game-client/send! @conn [:signal peer-id json-data]))
