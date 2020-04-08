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
  (if (and (= (:state game) :waiting)
           (not (contains? (:players game) player-id)))
    [:ok (assoc-in game
                   [:players player-id]
                   {:state :waiting
                    :tiles (tile/reset)})]
    [:err]))

(defn start-round
  [game]
  (if (and (= (:state game) :waiting)
           (not (zero? (count (:players game)))))
    [:ok (assoc game :state :playing)]
    [:err]))
