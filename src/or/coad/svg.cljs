(ns or.coad.svg
  (:require [clojure.string :as s]
            [or.coad.vector :as v]))

(defn new-path [d]
  (let [p (js/document.createElementNS "http://www.w3.org/2000/svg" "path")]
    (.setAttribute p "d" d)
    p))

(defn points [^js/SVGPath path n]
  (let [length (.getTotalLength path)]
    (mapv (fn [i]
            (let [p (.getPointAtLength path (-> length (* i) (/ n)))]
              (v/v (.-x p) (.-y p)))) (range (inc n)))))

(defn min-max-x-y [[{x :x y :y} & rest]]
  (reduce (fn [[min-x max-x min-y max-y] {x :x y :y}]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn avg-x-y [[p & rest]]
  (let [[s n] (reduce (fn [[s n] p]
                        [(v/+ s p)
                         (inc n)])
                      [p 1]
                      rest)]
    (v// s n)))

(defn bounding-box-from-path [d]
  (let [path   (new-path d)
        points (points path 50)
        box    (min-max-x-y points)]
    box))

(defn bounding-box [points]
  (min-max-x-y points))

(defn center [d]
  (let [path   (new-path d)
        points (points path 50)
        center (avg-x-y points)]
    center))

(defn s [[x y]]
  (str x "," y))

(defn make-path [v]
  (cond
    (string? v)     v
    (and (map? v)
         (:x v)
         (:y v))    (str (:x v) "," (:y v))
    (sequential? v) (s/join " " (map make-path v))
    :else           (str v)))

(defn translate [[x y] [dx dy]]
  [(+ x dx)
   (+ y dy)])

(defn scale [[x y] f]
  [(* x f)
   (* y f)])
