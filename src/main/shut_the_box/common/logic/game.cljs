(ns shut-the-box.common.logic.game
  (:require
    [medley.core :refer [find-first map-vals]]
    [shut-the-box.common.logic.tile :as tile]))

;; The game logic assumes the following: players take turns playing out
;; their part of the round (roll the dice, close tiles, repeat) until
;; they can make no further moves. Players *cannot* play out their turns
;; in parallel, in other words. Note that this is why most of the logic
;; is actually in the *tiles* namespace (because *game* is about
;; enforcing the game-level, turn-taking logic).

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

(defn valid-player?
  [game player-id]
  (contains? (:players game) player-id))

(defn active-player?
  [game player-id]
  (and (valid-player? game player-id)
       (-> game :players (get player-id) :state (= :rolling))))

(defn start-round
  [game]
  (when (and (= (:state game) :waiting)
             (not (zero? (count (:players game)))))
    (assoc game :state :playing)))

(defn set-active-player
  [game player-id]
  (when (and (= (:state game) :playing)
             (valid-player? game player-id))
    (-> game
        (update :players (fn [ps] (map-vals #(assoc % :state :waiting) ps)))
        (assoc-in [:players player-id :state] :rolling))))

(defn roll-dice
  [{:keys [state players] :as game} player-id]
  (when (and (= state :playing)
             (active-player? game player-id))
    (let [roll (apply + (repeatedly num-dice #(inc (rand-int 6))))]
      (assoc-in game [:players player-id :last-roll] roll))))

(defn shut-tiles
  [{:keys [state players] :as game} player-id tiles]
  (when (and (= state :playing)
             (active-player? game player-id))
    (let [player (get players player-id)]
      (when (tile/valid-combination? (:tiles player)
                                     (:last-roll player)
                                     tiles)
        (update-in game [:players player-id :tiles] tile/shut tiles)))))
