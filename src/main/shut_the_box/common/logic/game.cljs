(ns shut-the-box.common.logic.game
  (:require
    [shut-the-box.common.logic.tile :as tile]))

(def num-dice 2)

(defn new
  []
  {:state :waiting
   :players {}})

(defn add-player
  [game player-id]
  (when (and (= (:state game) :waiting)
             (not (contains? (:players game) player-id)))
    (assoc-in game
              [:players player-id]
              {:state :waiting
               :tiles (tile/reset)})))

(defn start-round
  [game]
  (when (and (= (:state game) :waiting)
             (not (zero? (count (:players game)))))
    (assoc game :state :playing)))
