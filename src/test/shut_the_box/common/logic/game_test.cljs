(ns shut-the-box.common.logic.game-test
  (:require
    [shut-the-box.common.logic.game :as game]
    [shut-the-box.common.logic.tile :as tile]
    [shut-the-box.common.logic.tile-test :refer [tile-bits]]
    [cljs.test :refer [deftest is testing]]))

(def player1-id 27)
(def player2-id 54)

(deftest new-game-test
  (let [g (game/new)]
    (is (= :waiting (-> g :state)))
    (is (empty? (-> g :players)))))

(deftest add-player-test
  ;; Players can be added immediately after the game is created.
  (let [g (-> (game/new)
              (game/add-player player1-id))]
    (is (= 1 (-> g :players keys count)))
    (is (= :waiting (-> g :players (get player1-id) :state)))
    (is (= tile/num-tiles (-> g :players (get player1-id) :tiles count))))

  ;; Cannot add the same player twice.
  (let [g (-> (game/new)
              (game/add-player player1-id))]
    (is (nil? (game/add-player g player1-id))))

  ;; Players *cannot* be added after a round has started.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              game/start-round)]
    (is (nil? (game/add-player g player2-id)))))

(deftest start-round-test
  ;; The round can start as soon as one player has been added.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/start-round))]
    (is (= :playing (-> g :state))))

  ;; Consequently, at least one player must be in the game in order for
  ;; a round to start.
  (is (nil? (-> (game/new)
                ;; Notice that there is no add-player
                (game/start-round)))))

(deftest set-active-player-test
  ;; Any player can be made the active player at any time after the
  ;; round has been started.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id)
              (game/start-round))]
    (let [g (game/set-active-player g player1-id)]
      (is (= :rolling (-> g :players (get player1-id) :state)))
      (is (= :waiting (-> g :players (get player2-id) :state)))
      (let [g (game/set-active-player g player2-id)]
        (is (= :waiting (-> g :players (get player1-id) :state)))
        (is (= :rolling (-> g :players (get player2-id) :state))))))

  ;; Only added players can be marked active.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              game/start-round)]
    (is (nil? (game/set-active-player g player2-id)))))

(deftest roll-dice-test
  ;; The active player can roll the dice; inactive players *cannot* roll
  ;; the dice.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id)
              (game/start-round)
              (game/set-active-player player1-id)
              (game/roll-dice player1-id))]
    (is (<= 2 (-> g :players (get player1-id) :last-roll) 12))
    (is (nil? (game/roll-dice g player2-id))))

  ;; The round must have been started for the dice to be rolled.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id))]
    (is (nil? (game/roll-dice g player1-id)))))

(deftest shut-tiles-test
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id)
              (game/start-round)
              (game/set-active-player player1-id)
              (assoc-in [:players player1-id :last-roll] 8)
              (assoc-in [:players player1-id :tiles]
                        (tile-bits #{2 3 4 5 6 8 9 10})))]
    ;; Tiles that match the sum can be shut.
    (let [g (game/shut-tiles g player1-id [2 6])]
      (is (= (tile-bits #{3 4 5 8 9 10})
             (-> g :players (get player1-id) :tiles))))
    (let [g (game/shut-tiles g player1-id [8])]
      (is (= (tile-bits #{2 3 4 5 6 9 10})
             (-> g :players (get player1-id) :tiles))))
    ;; Only tiles not already shut can be shut.
    (is (nil? (game/shut-tiles g player1-id [1 2 4])))
    ;; Inactive players *cannot* shut tiles.
    (is (nil? (game/shut-tiles g player2-id [8])))))
