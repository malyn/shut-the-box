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
  [game player-id player-name]
  (when (and (= (:state game) :waiting)
             (not (contains? (:players game) player-id)))
    (assoc-in game
              [:players player-id]
              {:state :waiting
               :name player-name
               :tiles (tile/reset)})))

(defn valid-player?
  [game player-id]
  (contains? (:players game) player-id))

(defn start-round
  [game]
  (cond
    ;; Game has never been started.
    (and (= (:state game) :waiting)
         (not (zero? (count (:players game)))))
    (assoc game :state :playing)

    ;; All players are done and next round is being started.
    (and (= (:state game) :done)
         (not (zero? (count (:players game))))
         (every? #(= :done (:state %)) (-> game :players vals)))
    (-> game
        (assoc :state :playing)
        (update-in [:players] (fn [players]
                                (map-vals #(assoc %
                                                  :state :waiting
                                                  :tiles (tile/reset))
                                          players))))

    ;; Round is already started; reject the (re)start attempt.
    :else
    nil))

(defn set-active-player
  [game player-id]
  (when (and (= (:state game) :playing)
             (valid-player? game player-id))
    (assoc-in game [:players player-id :state] :rolling)))

(defn roll-dice
  [{:keys [state players] :as game} player-id]
  (when (and (= state :playing)
             (valid-player? game player-id)
             (-> game :players (get player-id) :state (= :rolling)))
    (let [roll (repeatedly num-dice #(inc (rand-int 6)))
          game (assoc-in game [:players player-id :last-roll] roll)]
      ;; Can the player satisfy this roll? If so, they are in the
      ;; thinking state, if not, they are no-moves.
      (if (seq (tile/valid-combinations
                 (-> game :players (get player-id) :tiles)
                 (apply + roll)))
        (assoc-in game [:players player-id :state] :thinking)
        (assoc-in game [:players player-id :state] :no-moves)))))

(defn shut-tiles
  [{:keys [state players] :as game} player-id tiles]
  (when (and (= state :playing)
             (valid-player? game player-id)
             (-> game :players (get player-id) :state (= :thinking)))
    (let [player (get players player-id)]
      (when (tile/valid-combination? (:tiles player)
                                     (apply + (:last-roll player))
                                     tiles)
        (let [game (update-in game [:players player-id :tiles] tile/shut tiles)]
          (if (tile/shut-the-box? (-> game :players (get player-id) :tiles))
            ;; Shut the box; the player is shut-box and the game is
            ;; done.
            (-> game
                (assoc :state :done)
                (assoc-in [:players player-id :state] :shut-box))
            ;; Box not shut; player is back to rolling.
            (assoc-in game [:players player-id :state] :rolling)))))))

(defn end-turn
  [{:keys [state players] :as game} player-id]
  (when (and (= state :playing)
             (valid-player? game player-id)
             (-> game :players (get player-id) :state (= :no-moves)))
    (assoc-in game [:players player-id :state] :done)))
