(ns shut-the-box.client.server
  (:require
    [cljs.core.async :as async]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch]]
    [taoensso.timbre :as log]
    [shut-the-box.client.game-client :as game-client]
    #_[shut_the_box.client.config :refer [env]]))

(defn game-client-message
  [[msg & data]]
  (case msg
    :welcome
    (let [[player-id] data]
      (dispatch [:shut-the-box.client.events/set-player-id player-id]))

    :game
    (let [[game-id game] data]
      (dispatch [:shut-the-box.client.events/update-game game-id game]))

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
  :start (let [conn (game-client/connect!)]
           (println "Connected to game server.")
           (game-client-loop conn)
           conn))

(defn new-game!
  []
  (game-client/send! @conn [:new-game]))
