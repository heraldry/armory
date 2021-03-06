(ns heraldry.coat-of-arms.shared.chevron
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn arm-diagonals [chevron-angle origin-point anchor-point]
  (let [direction (-> (v/- anchor-point origin-point)
                      v/normal
                      (v/* 200)
                      (v/rotate (- chevron-angle)))
        direction (if (-> direction :y neg?)
                    (v/dot direction (v/v 1 -1))
                    direction)
        direction (if (-> direction :y Math/abs (< 5))
                    (v/+ direction (v/v 0 5))
                    direction)
        left (v/rotate direction chevron-angle)
        right (v/rotate (v/dot direction (v/v 1 -1)) chevron-angle)]
    [left right]))

(defn mirror-point [chevron-angle center point]
  (-> point
      (v/- center)
      (v/rotate (- chevron-angle))
      (v/dot (v/v 1 -1))
      (v/rotate chevron-angle)
      (v/+ center)))
