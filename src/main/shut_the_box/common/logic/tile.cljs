(ns shut-the-box.common.logic.tile
  (:require
    [clojure.math.combinatorics :as combo]))

(def num-tiles 10)

(defn reset
  []
  (vec (repeat num-tiles true)))

(defn score
  [tiles]
  (apply +
         (map-indexed
           (fn [idx up?]
             (if up? idx 0))
           tiles)))

(defn is-valid?
  [n]
  (<= 1 n num-tiles))

(defn can-shut?
  [tiles n]
  (and (is-valid? n)
       (nth tiles (dec n))))

(defn valid-combination?
  [tiles sum xs]
  (and (= sum (apply + xs))
       (every? #(can-shut? tiles %) xs)))

(defn valid-combinations
  [tiles sum]
  (let [tile-numbers (reduce
                       (fn [xs n]
                         (if (can-shut? tiles n)
                           (conj xs n)
                           xs))
                       []
                       (range 1 (inc num-tiles)))]
    (filter
      #(= sum (apply + %))
      (mapcat #(combo/combinations tile-numbers %) (range 1 6)))))

(defn shut
  [tiles x-or-xs]
  (if (coll? x-or-xs)
    (reduce shut tiles x-or-xs)
    (when (can-shut? tiles x-or-xs)
      (assoc tiles (dec x-or-xs) false))))

(defn shut-the-box?
  [tiles]
  (every? false? tiles))
