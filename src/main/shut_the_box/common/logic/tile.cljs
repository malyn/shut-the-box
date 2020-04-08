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

(defn shut
  [tiles n]
  (when (can-shut? tiles n)
    (assoc tiles (dec n) false)))

(defn shut-the-box?
  [tiles]
  (every? false? tiles))

(defn valid-combination?
  [tiles sum xs]
  (and (= sum (apply + xs))
       (every? #(can-shut? tiles %) xs)))
