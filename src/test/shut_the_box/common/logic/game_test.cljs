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
        ;; FIXME We need to rename set-active-player to
        ;; set-player-state, then it makes it clear that two players can
        ;; be in the same state (which is weird, but let's ignore that
        ;; for now and fix it when we separate the "player" logic from
        ;; the "game" logic).
        (is (= :rolling (-> g :players (get player1-id) :state)))
        (is (= :rolling (-> g :players (get player2-id) :state))))))

  ;; Only added players can be marked active.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              game/start-round)]
    (is (nil? (game/set-active-player g player2-id)))))

(deftest roll-dice-test
  ;; The active player can roll the dice.
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id)
              (game/start-round)
              (game/set-active-player player1-id)
              (game/roll-dice player1-id))]
    ;; The player has a valid roll and is now :thinking.
    (is (= 2 (-> g :players (get player1-id) :last-roll count)))
    (is (<= 1 (-> g :players (get player1-id) :last-roll (nth 0)) 6))
    (is (<= 1 (-> g :players (get player1-id) :last-roll (nth 1)) 6))
    (is (= :thinking (-> g :players (get player1-id) :state)))

    ;; The active player cannot re-roll until they have submitted their
    ;; action.
    (is (nil? (game/roll-dice g player1-id)))

    ;; Inactive players cannot roll the dice at all.
    (is (nil? (game/roll-dice g player2-id))))

  ;; A roll that has no possible actions needs to immediately transition
  ;; the player to :done. This effectively means that no one is active
  ;; (since no one is :waiting); set-active-player will need to be
  ;; called to select another player. We test this by rolling the dice
  ;; when the player only has the number 1 tile up (since you can't roll
  ;; a 1 with two dice).
  (let [g (-> (game/new)
              (game/add-player player1-id)
              (game/add-player player2-id)
              (game/start-round)
              (game/set-active-player player1-id)
              (assoc-in [:players player1-id :tiles] (tile-bits #{1}))
              (game/roll-dice player1-id))]
    ;; The player has a roll, they are done (because they have no
    ;; ability to satisfy the roll), but the game is *not* done
    ;; (roll-dice doesn't decide if the game is done, it just marks the
    ;; player as done), but also the second player is still not active
    ;; (that has to be done explicitly). Note that `shut-tiles` *can*
    ;; mark the game as done since shutting the box is a key part of the
    ;; game logic, whereas auto-sequencing players (and the game being
    ;; done if there are no waiting players) is a higher-level construct
    ;; that the session implements.
    ;; TODO I wonder if this *should* be part of the game logic? Maybe
    ;; we are conflating "game" and "player" logic in `game` and we
    ;; should instead have a `player` logic namespace that focuses on a
    ;; single player and deals with them, and then the `game` logic
    ;; namespace actually does implement things like sequencing?
    ;; `websocket` could then avoid doing that on its own, which I think
    ;; would be better.
    (is (= 2 (-> g :players (get player1-id) :last-roll count)))
    (is (= :done (-> g :players (get player1-id) :state)))
    (is (= :playing (-> g :state)))
    (is (= :waiting (-> g :players (get player2-id) :state))))

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
              (update-in [:players player1-id]
                         assoc
                         :state :thinking
                         :last-roll [2 6]
                         :tiles (tile-bits #{2 3 4 5 6 8 9 10})))]
    ;; Tiles that match the sum can be shut; the player's state goes
    ;; back to :rolling after they shut tiles.
    (let [g (game/shut-tiles g player1-id [2 6])]
      (is (= (tile-bits #{3 4 5 8 9 10})
             (-> g :players (get player1-id) :tiles)))
      (is (= :rolling (-> g :players (get player1-id) :state))))
    (let [g (game/shut-tiles g player1-id [8])]
      (is (= (tile-bits #{2 3 4 5 6 9 10})
             (-> g :players (get player1-id) :tiles)))
      (is (= :rolling (-> g :players (get player1-id) :state))))

    ;; Only tiles not already shut can be shut.
    (is (nil? (game/shut-tiles g player1-id [1 2 4])))

    ;; Both the player and the game transition to :done if the player
    ;; has shut the box.
    (let [g (-> g
                (update-in [:players player1-id]
                           assoc
                           :last-roll [4 4]
                           :tiles (tile-bits #{2 6}))
                (game/shut-tiles player1-id [2 6]))]
      (is (= (tile-bits #{}) (-> g :players (get player1-id) :tiles)))
      (is (= :done (-> g :players (get player1-id) :state)))
      (is (= :done (-> g :state))))

    ;; Inactive players *cannot* shut tiles.
    (is (nil? (game/shut-tiles g player2-id [8])))))
