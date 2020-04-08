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
