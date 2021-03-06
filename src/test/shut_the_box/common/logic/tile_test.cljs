(ns shut-the-box.common.logic.tile-test
  (:require
    [shut-the-box.common.logic.tile :as tile]
    [cljs.test :refer [deftest is testing]]))

(defn tile-bits
  [up-tiles]
  (mapv #(contains? up-tiles %)
        (range 1 (inc tile/num-tiles))))

(deftest tile-bits-test
  (let [tiles (tile-bits #{1 3 4 5 6 8 9 10})]
    (is (vector? tiles))
    (is (= tile/num-tiles (count tiles)))
    (is (= [true false true true true true false true true true] tiles))
    (is (= 46 (tile/score tiles)))))

(deftest reset-test
  (let [tiles (tile/reset)]
    (is (vector? tiles))
    (is (= tile/num-tiles (count tiles)))
    (is (= (tile-bits #{1 2 3 4 5 6 7 8 9 10}) tiles))
    (is (= 55 (tile/score tiles)))))

(deftest can-shut?-test
  (let [tiles (tile-bits #{1 3 4 5 6 8 9 10})]
    (is (tile/can-shut? tiles 1))
    (is (false? (tile/can-shut? tiles 2)))))

(deftest shut-test
  (let [tiles (tile-bits #{1 3 4 5 6 8 9 10})]
    ;; Can shut "up" tiles.
    (is (= (tile-bits #{3 4 5 6 8 9 10}) (tile/shut tiles 1)))
    (is (= (tile-bits #{1 3 4 5 6 8 9}) (tile/shut tiles 10)))
    ;; Can't shut "down" tiles.
    (is (nil? (tile/shut tiles 2)))
    (is (nil? (tile/shut tiles 7)))
    ;; Can't shut invalid tiles.
    (is (nil? (tile/shut tiles 0)))
    (is (nil? (tile/shut tiles 11)))
    ;; Can (try to) shut multiple tiles at once.
    (is (= (tile-bits #{3 4 5 6 8 9}) (tile/shut tiles [1 10])))
    (is (= (tile-bits #{3 5 8 9 10}) (tile/shut tiles [1 4 6])))
    (is (nil? (tile/shut tiles [1 2])))))

(deftest shut-the-box?-test
  (is (false? (tile/shut-the-box? (tile-bits #{1}))))
  (is (= 1 (tile/score (tile-bits #{1}))))
  (is (true? (tile/shut-the-box? (tile-bits #{}))))
  (is (= 0 (tile/score (tile-bits #{}))))
  (is (false? (-> (tile-bits #{2 7})
                  (tile/shut 2)
                  tile/shut-the-box?)))
  (is (true? (-> (tile-bits #{2 7})
                 (tile/shut 2)
                 (tile/shut 7)
                 tile/shut-the-box?))))

(deftest valid-combination?-test
  (let [tiles (tile-bits #{1 3 4 5 6 8 9 10})]
    ;; Combination is valid if the tiles match the sum and the tiles are
    ;; still up.
    (is (true? (tile/valid-combination? tiles 6 [1 5])))
    (is (true? (tile/valid-combination? tiles 6 [6])))
    ;; Combination is invalid if the sum does not match.
    (is (false? (tile/valid-combination? tiles 6 [1 6])))
    ;; Combination is invalid if one or more tiles are not up.
    (is (false? (tile/valid-combination? tiles 6 [2 4])))
    ;; Combination is invalid if the tiles are invalid.
    (is (false? (tile/valid-combination? tiles 6 [-2 8])))))

(deftest valid-combinations-test
  (let [tiles (tile-bits #{1 3 4 5 6 8 9 10})]
    (is (= #{[1 5] [6]} (set (tile/valid-combinations tiles 6))))
    (is (= #{[3]} (set (tile/valid-combinations tiles 3))))
    (is (= [] (tile/valid-combinations tiles 2)))))
