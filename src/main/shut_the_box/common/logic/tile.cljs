(ns shut-the-box.common.logic.tile)

(def num-tiles 10)

(defn reset
  []
  (vec (repeat num-tiles true)))

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

(defn shut
  [tiles x-or-xs]
  (if (coll? x-or-xs)
    (reduce shut tiles x-or-xs)
    (when (can-shut? tiles x-or-xs)
      (assoc tiles (dec x-or-xs) false))))

(defn shut-the-box?
  [tiles]
  (every? false? tiles))
