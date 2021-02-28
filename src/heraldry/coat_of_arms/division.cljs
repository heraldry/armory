(ns heraldry.coat-of-arms.division
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(def overlap-stroke-width 0.1)
(def outline-stroke-width 0.5)

(def outline-style
  {:stroke          "#000"
   :stroke-width    outline-stroke-width
   :fill            "none"
   :stroke-linecap  "round"
   :stroke-linejoin "round"})

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees  "45°"
                 :top-left-origin     "Top-left to origin"
                 :top-right-origin    "Top-right to origin"
                 :bottom-left-origin  "Bottom-left to origin"
                 :bottom-right-origin "Bottom-right to origin"}]
    (->> type
         (get {:per-bend                    [:forty-five-degrees
                                             :top-left-origin]
               :bendy                       [:forty-five-degrees
                                             :top-left-origin]
               :per-bend-sinister           [:forty-five-degrees
                                             :top-right-origin]
               :bendy-sinister              [:forty-five-degrees
                                             :top-right-origin]
               :per-chevron                 [:forty-five-degrees
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :per-saltire                 [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :gyronny                     [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :tierced-per-pairle          [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin]
               :tierced-per-pairle-reversed [:forty-five-degrees
                                             :bottom-left-origin
                                             :bottom-right-origin]})
         (map (fn [key]
                [(get options key) key])))))

(def default-options
  {:line          line/default-options
   :origin        position/default-options
   :diagonal-mode {:type    :choice
                   :default :top-left-origin}
   :layout        {:num-fields-x    {:type     :range
                                     :min      2
                                     :max      20
                                     :default  6
                                     :integer? true}
                   :num-fields-y    {:type     :range
                                     :min      2
                                     :max      20
                                     :default  6
                                     :integer? true}
                   :num-base-fields {:type     :range
                                     :min      2
                                     :max      8
                                     :default  2
                                     :integer? true}
                   :offset-x        {:type    :range
                                     :min     -1
                                     :max     1
                                     :default 0}
                   :offset-y        {:type    :range
                                     :min     -1
                                     :max     1
                                     :default 0}
                   :stretch-x       {:type    :range
                                     :min     0.5
                                     :max     2
                                     :default 1}
                   :stretch-y       {:type    :range
                                     :min     0.5
                                     :max     2
                                     :default 1}
                   :rotation        {:type    :range
                                     :min     -90
                                     :max     90
                                     :default 0}}})

(defn pick-options [paths & values]
  (let [values  (first values)
        options (loop [options       {}
                       [path & rest] paths]
                  (let [next-options (-> options
                                         (assoc-in path (get-in default-options path)))]
                    (if (nil? rest)
                      next-options
                      (recur next-options rest))))]
    (loop [options              options
           [[key value] & rest] values]
      (let [next-options (if key
                           (assoc-in options key value)
                           options)]
        (if (nil? rest)
          next-options
          (recur next-options rest))))))

(defn options [division]
  (when division
    (->
     (case (:type division)
       :per-pale                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]]
                                                  {[:origin :point :choices] position/point-choices-x})
       :per-fess                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]]
                                                  {[:origin :point :choices] position/point-choices-y})
       :per-bend                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend)
                                                   [:origin :point :choices] position/point-choices-y})
       :per-bend-sinister           (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend-sinister)
                                                   [:diagonal-mode :default] :top-right-origin
                                                   [:origin :point :choices] position/point-choices-y})
       :per-chevron                 (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-chevron)
                                                   [:diagonal-mode :default] :forty-five-degrees
                                                   [:origin :point :choices] position/point-choices-y
                                                   [:line :offset :min]      0})
       :per-saltire                 (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-saltire)
                                                   [:line :offset :min]      0})
       :quartered                   (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]]
                                                  {[:line :offset :min] 0})
       :quarterly                   (pick-options [[:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]]
                                                  {[:layout :num-fields-x :default] 3
                                                   [:layout :num-fields-y :default] 4})
       :gyronny                     (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :gyronny)
                                                   [:line :offset :min]      0})
       :paly                        (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]])
       :barry                       (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]])
       :chequy                      (pick-options [[:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]]
                                                  {[:layout :num-fields-y :default] nil})
       :lozengy                     (pick-options [[:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:layout :rotation]]
                                                  {[:layout :num-fields-y :default] nil
                                                   [:layout :stretch-y :max]        3})
       :bendy                       (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :bendy)
                                                   [:origin :point :choices] position/point-choices-y})
       :bendy-sinister              (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :bendy)
                                                   [:diagonal-mode :default] :top-right-origin
                                                   [:origin :point :choices] position/point-choices-y})
       :tierced-per-pale            (pick-options [[:line]
                                                   [:layout :stretch-x]
                                                   [:origin :point]
                                                   [:origin :offset-x]]
                                                  {[:origin :point :choices] position/point-choices-x})
       :tierced-per-fess            (pick-options [[:line]
                                                   [:layout :stretch-y]
                                                   [:origin :point]
                                                   [:origin :offset-y]]
                                                  {[:origin :point :choices] position/point-choices-y})
       :tierced-per-pairle          (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle)
                                                   [:line :offset :min]      0})
       :tierced-per-pairle-reversed (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle-reversed)
                                                   [:diagonal-mode :default] :forty-five-debrees
                                                   [:line :offset :min]      0})
       {})
     (update-in [:line] (fn [line]
                          (when line
                            (options/merge (line/options (get-in division [:line]))
                                           line)))))))

(defn mandatory-part-count [{:keys [type] :as division}]
  (let [{:keys [num-base-fields]} (options/sanitize division (options division))]
    (if (get #{:paly :barry} type)
      num-base-fields
      (case type
        nil                          0
        :tierced-per-pale            3
        :tierced-per-fess            3
        :tierced-per-pairle          3
        :tierced-per-pairle-reversed 3
        2))))

(defn counterchangable? [division]
  ;; TODO: potentially also should look at the parts, maybe demand no
  ;; ordinaries and charges as well, but for now this check suffices
  (and (-> division mandatory-part-count (= 2))
       (-> division :fields (get 0) :division :type not)
       (-> division :fields (get 1) :division :type not)))

(defn default-fields [{:keys [type] :as division}]
  (let [{:keys [layout]}                                    (options/sanitize division (options division))
        {:keys [num-fields-x num-fields-y num-base-fields]} layout
        defaults                                            [default/field
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :azure))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :sable))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :gules))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :or))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :vert))]]
    (into (subvec defaults 0 2)
          (cond
            (= :per-saltire type)                  [{:ref 1} {:ref 0}]
            (= :quartered type)                    [{:ref 1} {:ref 0}]
            (= :quarterly type)                    (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (->> (for [j (range num-fields-y)
                                                                        i (range num-fields-x)]
                                                                    {:ref (mod (+ i j) num-base-fields)})
                                                                  (drop num-base-fields))))
            (= :gyronny type)                      [{:ref 1} {:ref 0} {:ref 0} {:ref 1} {:ref 1} {:ref 0}]
            (= :paly type)                         (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-x num-base-fields)))))
            (= :barry type)                        (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
            (= :chequy type)                       (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2)))))
            (#{:bendy
               :bendy-sinister} type)              (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
            (#{:tierced-per-pale
               :tierced-per-fess
               :tierced-per-pairle
               :tierced-per-pairle-reversed} type) [(nth defaults 2)]))))

(defn get-field [fields index]
  (let [part (get fields index)
        ref  (:ref part)]
    (if ref
      (get fields ref)
      part)))

(defn division-context-key [key]
  (keyword (str "division-" (name key))))

(defn make-division [type fields parts mask-overlaps outline parent-environment parent
                     {:keys [render-field db-path transform svg-export?] :as context}]
  (let [mask-ids     (->> (range (count fields))
                          (map (fn [idx] [(util/id (str (name type) "-" idx))
                                          (util/id (str (name type) "-" idx))])))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box & extra]]
                                         (let [field (get-field fields idx)]
                                           (field-environment/create
                                            (svg/make-path shape-path)
                                            {:parent               parent
                                             :context              [type idx]
                                             :bounding-box         (svg/bounding-box bounding-box)
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (:counterchanged? field))
                                                                     parent-environment)
                                             :mask                 (first extra)}))))
                          vec)]
    [:<>
     [:defs
      (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
        (let [env               (get environments idx)
              environment-shape (:shape env)
              overlap-paths     (get mask-overlaps idx)]
          ^{:key idx}
          [:<>
           [(if svg-export?
              :mask
              :clipPath) {:id clip-path-id}
            [:path {:d    environment-shape
                    :fill "#fff"}]
            (cond
              (= overlap-paths :all) [:path {:d            environment-shape
                                             :fill         "none"
                                             :stroke-width overlap-stroke-width
                                             :stroke       "#fff"}]
              overlap-paths          (for [[idx shape] (map-indexed vector overlap-paths)]
                                       ^{:key idx}
                                       [:path {:d            shape
                                               :fill         "none"
                                               :stroke-width overlap-stroke-width
                                               :stroke       "#fff"}]))]
           (when-let [mask-shape (-> env :meta :mask)]
             [:mask {:id mask-id}
              [:path {:d    environment-shape
                      :fill "#fff"}]
              [:path {:d    mask-shape
                      :fill "#000"}]])]))]

     (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
       (let [env (get environments idx)]
         ^{:key idx}
         [:g {(if svg-export?
                :mask
                :clip-path) (str "url(#" clip-path-id ")")}
          [:g {:transform transform
               :mask      (when (-> env :meta :mask)
                            (str "url(#" mask-id ")"))}
           [render-field
            (get-field fields idx)
            (get environments idx)
            (-> context
                (assoc :db-path (if (-> type
                                        name
                                        (s/split #"-" 2)
                                        first
                                        (->> (get #{"charge" "ordinary"}))) ;; FIXME: bit of a hack
                                  (conj db-path :field)
                                  (conj db-path :fields idx))))]]]))
     outline]))

(defn paly-parts [{:keys [num-fields-x
                          offset-x
                          stretch-x]} top-left bottom-right line hints render-options]
  (let [offset-x                 (or offset-x 0)
        stretch-x                (or stretch-x 1)
        width                    (- (:x bottom-right)
                                    (:x top-left))
        pallet-width             (-> width
                                     (/ num-fields-x)
                                     (* stretch-x))
        required-width           (* pallet-width
                                    num-fields-x)
        middle                   (-> width
                                     (/ 2)
                                     (+ (:x top-left)))
        x0                       (-> middle
                                     (- (/ required-width 2))
                                     (+ (* offset-x
                                           pallet-width)))
        y1                       (:y top-left)
        y2                       (:y bottom-right)
        height                   (- y2 y1)
        {line-down :line}        (line/create line
                                              height
                                              :flipped? true
                                              :angle 90
                                              :render-options render-options)
        {line-up        :line
         line-up-length :length} (line/create line
                                              height
                                              :angle -90
                                              :reversed? true
                                              :render-options render-options)
        line-up-origin           (v/extend (v/v 0 y1) (v/v 0 y2) line-up-length)
        parts                    (->> (range num-fields-x)
                                      (map (fn [i]
                                             (let [x1         (+ x0 (* i pallet-width))
                                                   x2         (+ x1 pallet-width)
                                                   last-part? (-> i inc (= num-fields-x))]
                                               [(cond
                                                  (zero? i) ["M" [x2 y1]
                                                             (line/stitch line-down)
                                                             (infinity/path :clockwise
                                                                            [:bottom :top]
                                                                            [(v/v x2 y2) (v/v x2 y1)])
                                                             "z"]
                                                  (even? i) (concat
                                                             ["M" [x1 (:y line-up-origin)]
                                                              (line/stitch line-up)]
                                                             (cond
                                                               last-part? [(infinity/path :clockwise
                                                                                          [:top :bottom]
                                                                                          [(v/v x1 y1) (v/v x1 y2)])
                                                                           "z"]
                                                               :else      [(infinity/path :clockwise
                                                                                          [:top :top]
                                                                                          [(v/v x1 y1) (v/v x2 y1)])
                                                                           "L" [x2 y1]
                                                                           (line/stitch line-down)
                                                                           (infinity/path :clockwise
                                                                                          [:bottom :bottom]
                                                                                          [(v/v x2 y2) (v/v x1 y2)])
                                                                           "z"]))
                                                  :else     (concat
                                                             ["M" [x1 y1]
                                                              (line/stitch line-down)]
                                                             (cond
                                                               last-part? [(infinity/path :counter-clockwise
                                                                                          [:bottom :top]
                                                                                          [(v/v x1 y2) (v/v x1 y1)])
                                                                           "z"]
                                                               :else      [(infinity/path :counter-clockwise
                                                                                          [:bottom :bottom]
                                                                                          [(v/v x1 y2) (v/v x2 y2)])
                                                                           "L" [x2 (:y line-up-origin)]
                                                                           (line/stitch line-up)
                                                                           (infinity/path :clockwise
                                                                                          [:top :top]
                                                                                          [(v/v x2 y1) (v/v x1 y1)])
                                                                           "z"])))
                                                [(v/v x1 y1) (v/v x2 y2)]])))
                                      vec)
        edges                    (->> num-fields-x
                                      dec
                                      range
                                      (map (fn [i]
                                             (let [x1 (+ x0 (* i pallet-width))
                                                   x2 (+ x1 pallet-width)]
                                               (if (even? i)
                                                 (svg/make-path ["M" [x2 y1]
                                                                 (line/stitch line-down)])
                                                 (svg/make-path ["M" [x2 (:y line-up-origin)]
                                                                 (line/stitch line-up)])))))
                                      vec)
        overlap                  (-> edges
                                     (->> (map vector))
                                     vec
                                     (conj nil))
        outlines                 (when (or (:outline? render-options)
                                           (:outline? hints))
                                   [:g outline-style
                                    (for [i (range (dec num-fields-x))]
                                      ^{:key i}
                                      [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defn barry-parts [{:keys [num-fields-y
                           offset-y
                           stretch-y]} top-left bottom-right line hints render-options]
  (let [offset-y                   (or offset-y 0)
        stretch-y                  (or stretch-y 1)
        height                     (- (:y bottom-right)
                                      (:y top-left))
        bar-height                 (-> height
                                       (/ num-fields-y)
                                       (* stretch-y))
        required-height            (* bar-height
                                      num-fields-y)
        middle                     (-> height
                                       (/ 2)
                                       (+ (:y top-left)))
        y0                         (-> middle
                                       (- (/ required-height 2))
                                       (+ (* offset-y
                                             bar-height)))
        x1                         (:x top-left)
        x2                         (:x bottom-right)
        width                      (- x2 x1)
        {line-right :line}         (line/create line
                                                width
                                                :render-options render-options)
        {line-left        :line
         line-left-length :length} (line/create line
                                                width
                                                :angle 180
                                                :flipped? true
                                                :reversed? true
                                                :render-options render-options)
        line-left-origin           (v/extend (v/v x1 0) (v/v x2 0) line-left-length)
        parts                      (->> (range num-fields-y)
                                        (map (fn [i]
                                               (let [y1         (+ y0 (* i bar-height))
                                                     y2         (+ y1 bar-height)
                                                     last-part? (-> i inc (= num-fields-y))]
                                                 [(cond
                                                    (zero? i) ["M" [x1 y2]
                                                               (line/stitch line-right)
                                                               (infinity/path :counter-clockwise
                                                                              [:right :left]
                                                                              [(v/v x2 y2) (v/v x1 y2)])
                                                               "z"]
                                                    (even? i) (concat ["M" [(:x line-left-origin) y1]
                                                                       (line/stitch line-left)]
                                                                      (cond
                                                                        last-part? [(infinity/path :counter-clockwise
                                                                                                   [:left :right]
                                                                                                   [(v/v x1 y1) (v/v x2 y1)])
                                                                                    "z"]
                                                                        :else      [(infinity/path :counter-clockwise
                                                                                                   [:left :left]
                                                                                                   [(v/v x1 y1) (v/v x1 y2)])
                                                                                    "L" [x1 y2]
                                                                                    (line/stitch line-right)
                                                                                    (infinity/path :counter-clockwise
                                                                                                   [:right :right]
                                                                                                   [(v/v x2 y2) (v/v x2 y1)])]))
                                                    :else     (concat ["M" [x1 y1]
                                                                       (line/stitch line-right)]
                                                                      (cond
                                                                        last-part? [(infinity/path :clockwise
                                                                                                   [:right :left]
                                                                                                   [(v/v x2 y1) (v/v x1 y1)])
                                                                                    "z"]
                                                                        :else      [(infinity/path :clockwise
                                                                                                   [:right :right]
                                                                                                   [(v/v x2 y1) (v/v x2 y2)])
                                                                                    "L" [(:x line-left-origin) y2]
                                                                                    (line/stitch line-left)
                                                                                    (infinity/path :clockwise
                                                                                                   [:left :left]
                                                                                                   [(v/v x1 y2) (v/v x1 y1)])
                                                                                    "z"])))
                                                  [(v/v x1 y1) (v/v x2 y2)]])))
                                        vec)
        edges                      (->> num-fields-y
                                        dec
                                        range
                                        (map (fn [i]
                                               (let [y1 (+ y0 (* i bar-height))
                                                     y2 (+ y1 bar-height)]
                                                 (if (even? i)
                                                   (svg/make-path ["M" [x1 y2]
                                                                   (line/stitch line-right)])
                                                   (svg/make-path ["M" [(:x line-left-origin) y2]
                                                                   (line/stitch line-left)])))))
                                        vec)
        overlap                    (-> edges
                                       (->> (map vector))
                                       vec
                                       (conj nil))
        outlines                   (when (or (:outline? render-options)
                                             (:outline? hints))
                                     [:g outline-style
                                      (for [i (range (dec num-fields-y))]
                                        ^{:key i}
                                        [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defn quarterly-parts [{:keys [num-fields-x
                               offset-x
                               stretch-x
                               num-fields-y
                               offset-y
                               stretch-y]} top-left bottom-right hints render-options]
  (let [offset-x        (or offset-x 0)
        stretch-x       (or stretch-x 1)
        width           (- (:x bottom-right)
                           (:x top-left))
        part-width      (-> width
                            (/ num-fields-x)
                            (* stretch-x))
        required-width  (* part-width
                           num-fields-x)
        middle-x        (-> width
                            (/ 2)
                            (+ (:x top-left)))
        x0              (-> middle-x
                            (- (/ required-width 2))
                            (+ (* offset-x
                                  part-width)))
        offset-y        (or offset-y 0)
        stretch-y       (or stretch-y 1)
        height          (- (:y bottom-right)
                           (:y top-left))
        part-height     (-> height
                            (/ num-fields-y)
                            (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        middle-y        (-> height
                            (/ 2)
                            (+ (:y top-left)))
        y0              (-> middle-y
                            (- (/ required-height 2))
                            (+ (* offset-y
                                  part-height)))
        parts           (->> (for [j (range num-fields-y)
                                   i (range num-fields-x)]
                               (let [x1       (+ x0 (* i part-width))
                                     x2       (+ x1 part-width)
                                     y1       (+ y0 (* j part-height))
                                     y2       (+ y1 part-height)
                                     first-x? (zero? i)
                                     first-y? (zero? j)
                                     last-x?  (-> num-fields-x dec (= i))
                                     last-y?  (-> num-fields-y dec (= j))]
                                 (cond
                                   (and first-x?
                                        first-y?) [["M" [x1 y2]
                                                    "L" [x2 y2]
                                                    "L" [x2 y1]
                                                    (infinity/path :counter-clockwise
                                                                   [:top :left]
                                                                   [(v/v x2 y1) (v/v x1 y2)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   (and last-x?
                                        first-y?) [["M" [x1 y1]
                                                    "L" [x1 y2]
                                                    "L" [x2 y2]
                                                    (infinity/path :counter-clockwise
                                                                   [:right :top]
                                                                   [(v/v x2 y2) (v/v x1 y1)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   (and first-x?
                                        last-y?)  [["M" [x1 y1]
                                                    "L" [x2 y1]
                                                    "L" [x2 y2]
                                                    (infinity/path :clockwise
                                                                   [:bottom :left]
                                                                   [(v/v x2 y2) (v/v x1 y1)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   (and last-x?
                                        last-y?)  [["M" [x1 y2]
                                                    "L" [x1 y1]
                                                    "L" [x2 y1]
                                                    (infinity/path :clockwise
                                                                   [:right :bottom]
                                                                   [(v/v x2 y1) (v/v x1 y2)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   first-x?       [["M" [x1 y1]
                                                    "L" [x2 y1]
                                                    "L" [x2 y2]
                                                    "L" [x1 y2]
                                                    (infinity/path :counter-clockwise
                                                                   [:left :left]
                                                                   [(v/v x1 y2) (v/v x1 y1)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   first-y?       [["M" [x1 y1]
                                                    "L" [x1 y2]
                                                    "L" [x2 y2]
                                                    "L" [x2 y1]
                                                    (infinity/path :counter-clockwise
                                                                   [:top :top]
                                                                   [(v/v x2 y1) (v/v x1 y1)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   last-x?        [["M" [x2 y2]
                                                    "L" [x1 y2]
                                                    "L" [x1 y1]
                                                    "L" [x2 y1]
                                                    (infinity/path :clockwise
                                                                   [:right :right]
                                                                   [(v/v x2 y1) (v/v x2 y2)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   last-y?        [["M" [x1 y2]
                                                    "L" [x1 y1]
                                                    "L" [x2 y1]
                                                    "L" [x2 y2]
                                                    (infinity/path :clockwise
                                                                   [:bottom :bottom]
                                                                   [(v/v x2 y2) (v/v x1 y2)])
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]]
                                   :else          [["M" [x1 y1]
                                                    "L" [x2 y1]
                                                    "L" [x2 y2]
                                                    "L" [x1 y2]
                                                    "z"]
                                                   [(v/v x1 y1) (v/v x2 y2)]])))
                             vec)
        overlap         (->> (for [j (range num-fields-y)
                                   i (range num-fields-x)]
                               (let [x1 (+ x0 (* i part-width))
                                     x2 (+ x1 part-width)
                                     y1 (+ y0 (* j part-height))
                                     y2 (+ y1 part-height)]
                                 [(svg/make-path ["M" [x2 y1]
                                                  "L" [x2 y2]
                                                  "L" [x1 y2]])]))
                             vec)
        outline-extra   50
        outlines        (when (or (:outline? render-options)
                                  (:outline? hints))
                          [:g outline-style
                           (for [i (range 1 num-fields-x)]
                             (let [x1 (+ x0 (* i part-width))]
                               ^{:key [:x i]}
                               [:path {:d (svg/make-path ["M" [x1 (- y0 outline-extra)]
                                                          "L" [x1 (+ y0 required-height outline-extra)]])}]))
                           (for [j (range 1 num-fields-y)]
                             (let [y1 (+ y0 (* j part-height))]
                               ^{:key [:y j]}
                               [:path {:d (svg/make-path ["M" [(- x0 outline-extra) y1]
                                                          "L" [(+ x0 required-width outline-extra) y1]])}]))])]
    [parts overlap outlines]))

(defn per-pale
  {:display-name "Per pale"
   :parts        ["dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]}          (options/sanitize division (options division))
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        top                            (assoc (:top points) :x (:x origin-point))
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        bottom-right                   (:bottom-right points)
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle 90
                                                    :render-options render-options)
        parts                          [[["M" (v/+ top
                                                   line-one-start)
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:bottom :top]
                                                         [(v/+ bottom
                                                               line-one-start)
                                                          (v/+ top
                                                               line-one-start)])
                                          "z"]
                                         [top-left
                                          (v/+ bottom
                                               line-one-start)]]

                                        [["M" (v/+ top
                                                   line-one-start)
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:bottom :top]
                                                         [(v/+ bottom
                                                               line-one-start)
                                                          (v/+ top
                                                               line-one-start)])
                                          "z"]
                                         [(v/+ top
                                               line-one-start)
                                          bottom-right]]]
        [fimbriation-elements
         fimbriation-outlines] (line/render-fimbriation
                                [top :top]
                                [bottom :bottom]
                                [line-one-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [make-division
      (division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ top
                               line-one-start)
                      (line/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(defn per-fess
  {:display-name "Per fess"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]}          (options/sanitize division (options division))
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        bottom-right                   (:bottom-right points)
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data} (line/create line
                                                    (:x (v/- right left))
                                                    :render-options render-options)
        parts                          [[["M" (v/+ left
                                                   line-one-start)
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/+ right
                                                               line-one-start)
                                                          (v/+ left
                                                               line-one-start)])
                                          "z"]
                                         [top-left
                                          (v/+ right
                                               line-one-start)]]

                                        [["M" (v/+ left
                                                   line-one-start)
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :left]
                                                         [(v/+ right
                                                               line-one-start)
                                                          (v/+ left
                                                               line-one-start)])
                                          "z"]
                                         [(v/+ left
                                               line-one-start)
                                          bottom-right]]]
        [fimbriation-elements
         fimbriation-outlines] (line/render-fimbriation
                                [left :left]
                                [right :right]
                                [line-one-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [make-division
      (division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ left
                               line-one-start)
                      (line/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(defn angle-to-point [p1 p2]
  (let [d         (v/- p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (-> angle-rad
        (/ Math/PI)
        (* 180))))

(defn direction [diagonal-mode points & [origin]]
  (let [top-left      (:top-left points)
        top-right     (:top-right points)
        bottom-left   (:bottom-left points)
        bottom-right  (:bottom-right points)
        origin        (or origin (:fess points))
        origin-height (-> origin
                          (v/- top-left)
                          :y)
        dir           (case diagonal-mode
                        :top-left-origin     (v/- origin top-left)
                        :top-right-origin    (v/- origin top-right)
                        :bottom-left-origin  (v/- origin bottom-left)
                        :bottom-right-origin (v/- origin bottom-right)
                        (v/v origin-height origin-height))]
    (v/v (-> dir :x Math/abs)
         (-> dir :y Math/abs))))

(defn per-bend
  {:display-name "Per bend"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (options division))
        points                              (:points environment)
        origin-point                        (position/calculate origin environment :fess)
        top-right                           (:top-right points)
        bottom-left                         (:bottom-left points)
        left                                (:left points)
        right                               (:right points)
        direction                           (direction diagonal-mode points origin-point)
        diagonal-start                      (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end                        (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle                               (angle-to-point diagonal-start diagonal-end)
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}      (line/create line
                                                         (v/abs (v/- diagonal-end diagonal-start))
                                                         :angle angle
                                                         :render-options render-options)
        parts                               [[["M" (v/+ diagonal-start
                                                        line-one-start)
                                               (line/stitch line-one)
                                               (infinity/path :counter-clockwise
                                                              [:right :top]
                                                              [(v/+ diagonal-end
                                                                    line-one-start)
                                                               (v/+ diagonal-start
                                                                    line-one-start)])
                                               "z"]
                                              [(v/+ diagonal-start
                                                    line-one-start)
                                               top-right
                                               (v/+ diagonal-end
                                                    line-one-start)]]
                                             [["M" (v/+ diagonal-start
                                                        line-one-start)
                                               (line/stitch line-one)
                                               (infinity/path :clockwise
                                                              [:right :top]
                                                              [(v/+ diagonal-end
                                                                    line-one-start)
                                                               (v/+ diagonal-start
                                                                    line-one-start)])
                                               "z"]
                                              [(v/+ diagonal-start
                                                    line-one-start)
                                               (v/+ diagonal-end
                                                    line-one-start)
                                               bottom-left]]]
        [fimbriation-elements
         fimbriation-outlines]              (line/render-fimbriation
                                             [diagonal-start :top]
                                             [diagonal-end :right]
                                             [line-one-data]
                                             (:fimbriation line)
                                             render-options)]
    [:<>
     [make-division
      (division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ diagonal-start
                               line-one-start)
                      (line/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(defn per-bend-sinister
  {:display-name "Per bend sinister"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (options division))
        points                              (:points environment)
        origin-point                        (position/calculate origin environment :fess)
        top-left                            (:top-left points)
        bottom-right                        (:bottom-right points)
        left                                (:left points)
        right                               (:right points)
        direction                           (direction diagonal-mode points origin-point)
        diagonal-start                      (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x left))
        diagonal-end                        (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x right))
        angle                               (angle-to-point diagonal-start diagonal-end)
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}      (line/create line
                                                         (v/abs (v/- diagonal-end diagonal-start))
                                                         :angle angle
                                                         :render-options render-options)
        parts                               [[["M" (v/+ diagonal-start
                                                        line-one-start)
                                               (line/stitch line-one)
                                               (infinity/path :counter-clockwise
                                                              [:top :left]
                                                              [(v/+ diagonal-end
                                                                    line-one-start)
                                                               (v/+ diagonal-start
                                                                    line-one-start)])
                                               "z"]
                                              [(v/+ diagonal-start
                                                    line-one-start)
                                               top-left
                                               (v/+ diagonal-end
                                                    line-one-start)]]

                                             [["M" (v/+ diagonal-start
                                                        line-one-start)
                                               (line/stitch line-one)
                                               (infinity/path :clockwise
                                                              [:top :left]
                                                              [(v/+ diagonal-end
                                                                    line-one-start)
                                                               (v/+ diagonal-start
                                                                    line-one-start)])
                                               "z"]
                                              [(v/+ diagonal-start
                                                    line-one-start)
                                               bottom-right
                                               (v/+ diagonal-end
                                                    line-one-start)]]]
        [fimbriation-elements
         fimbriation-outlines] (line/render-fimbriation
                                [diagonal-start :left]
                                [diagonal-end :right]
                                [line-one-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [make-division
      (division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ diagonal-start
                               line-one-start)
                      (line/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(defn per-chevron
  {:display-name "Per chevron"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (options division))
        points                              (:points environment)
        origin-point                        (position/calculate origin environment :fess)
        top-left                            (:top-left points)
        top-right                           (:top-right points)
        bottom-left                         (:bottom-left points)
        bottom-right                        (:bottom-right points)
        left                                (:left points)
        right                               (:right points)
        direction                           (direction diagonal-mode points origin-point)
        diagonal-bottom-left                (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right               (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                   (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                  (angle-to-point origin-point diagonal-bottom-right)
        joint-angle                         (- angle-bottom-left angle-bottom-right)
        {line-left       :line
         line-left-start :line-start
         :as             line-left-data}    (line/create line
                                                         (v/abs (v/- diagonal-bottom-left origin-point))
                                                         :angle (+ angle-bottom-left 180)
                                                         :joint-angle (- joint-angle)
                                                         :reversed? true
                                                         :render-options render-options)
        {line-right       :line
         line-right-start :line-start
         line-right-end   :line-end
         :as              line-right-data}  (line/create line
                                                         (v/abs (v/- diagonal-bottom-right origin-point))
                                                         :angle angle-bottom-right
                                                         :joint-angle (- joint-angle)
                                                         :render-options render-options)
        parts                               [[["M" (v/+ diagonal-bottom-left
                                                        line-left-start)
                                               (line/stitch line-left)
                                               (line/stitch line-right)
                                               (infinity/path :counter-clockwise
                                                              [:right :left]
                                                              [(v/+ diagonal-bottom-right
                                                                    line-right-end)
                                                               (v/+ diagonal-bottom-left
                                                                    line-left-start)])
                                               "z"]
                                              [top-left
                                               top-right
                                               (v/+ diagonal-bottom-left
                                                    line-left-start)
                                               (v/+ diagonal-bottom-right
                                                    line-left-start)]]

                                             [["M" (v/+ diagonal-bottom-left
                                                        line-left-start)
                                               (line/stitch line-left)
                                               (line/stitch line-right)
                                               (infinity/path :clockwise
                                                              [:right :left]
                                                              [(v/+ diagonal-bottom-right
                                                                    line-right-end)
                                                               (v/+ diagonal-bottom-left
                                                                    line-left-start)])
                                               "z"]
                                              [(v/+ diagonal-bottom-left
                                                    line-left-start)
                                               origin-point
                                               (v/+ diagonal-bottom-right
                                                    line-right-end)
                                               bottom-left
                                               bottom-right]]]
        [fimbriation-elements
         fimbriation-outlines] (line/render-fimbriation
                                [diagonal-bottom-left :left]
                                [diagonal-bottom-right :right]
                                [line-left-data
                                 line-right-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [make-division
      (division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ diagonal-bottom-left
                               line-left-start)
                      (line/stitch line-left)
                      "L" (v/+ origin-point
                               line-right-start)
                      (line/stitch line-right)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(defn per-saltire
  {:display-name "Per saltire"
   :parts        ["chief" "dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]}   (options/sanitize division (options division))
        points                                (:points environment)
        origin-point                          (position/calculate origin environment :fess)
        top                                   (:top points)
        bottom                                (:bottom points)
        left                                  (:left points)
        right                                 (:right points)
        direction                             (direction diagonal-mode points origin-point)
        diagonal-top-left                     (v/project-x origin-point (v/dot direction (v/v -1 -1)) (-> left :x (- 50)))
        diagonal-top-right                    (v/project-x origin-point (v/dot direction (v/v 1 -1)) (-> right :x (+ 50)))
        diagonal-bottom-left                  (v/project-x origin-point (v/dot direction (v/v -1 1)) (-> left :x (- 50)))
        diagonal-bottom-right                 (v/project-x origin-point (v/dot direction (v/v 1 1)) (-> right :x (+ 50)))
        angle-top-left                        (angle-to-point origin-point diagonal-top-left)
        angle-top-right                       (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                     (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                    (angle-to-point origin-point diagonal-bottom-right)
        {line-top-left       :line
         line-top-left-start :line-start}     (line/create line
                                                           (v/abs (v/- diagonal-top-left origin-point))
                                                           :angle (+ angle-top-left 180)
                                                           :reversed? true
                                                           :render-options render-options)
        {line-top-right       :line
         line-top-right-start :line-start}    (line/create line
                                                           (v/abs (v/- diagonal-top-right origin-point))
                                                           :angle angle-top-right
                                                           :flipped? true
                                                           :render-options render-options)
        {line-bottom-right       :line
         line-bottom-right-start :line-start} (line/create line
                                                           (v/abs (v/- diagonal-bottom-right origin-point))
                                                           :angle (+ angle-bottom-right 180)
                                                           :reversed? true
                                                           :render-options render-options)
        {line-bottom-left       :line
         line-bottom-left-start :line-start}  (line/create line
                                                           (v/abs (v/- diagonal-bottom-left origin-point))
                                                           :angle angle-bottom-left
                                                           :flipped? true
                                                           :render-options render-options)
        parts                                 [[["M" (v/+ diagonal-top-left
                                                          line-top-left-start)
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :counter-clockwise
                                                                [:right :left]
                                                                [(v/+ diagonal-top-right
                                                                      line-top-left-start)
                                                                 (v/+ diagonal-top-left
                                                                      line-top-left-start)])
                                                 "z"]
                                                [top
                                                 (v/+ diagonal-top-right
                                                      line-top-right-start)
                                                 origin-point
                                                 (v/+ diagonal-top-left
                                                      line-top-left-start)]]

                                               [["M" (v/+ diagonal-top-left
                                                          line-top-left-start)
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-bottom-left)
                                                 (infinity/path :clockwise
                                                                [:left :left]
                                                                [(v/+ diagonal-bottom-left
                                                                      line-bottom-left-start)
                                                                 (v/+ diagonal-top-left
                                                                      line-top-left-start)])
                                                 "z"]
                                                [left
                                                 (v/+ diagonal-bottom-left
                                                      line-bottom-left-start)
                                                 origin-point
                                                 (v/+ diagonal-top-left
                                                      line-top-left-start)]]

                                               [["M" (v/+ diagonal-bottom-right
                                                          line-bottom-right-start)
                                                 (line/stitch line-bottom-right)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :clockwise
                                                                [:right :right]
                                                                [(v/+ diagonal-top-right
                                                                      line-top-right-start)
                                                                 (v/+ diagonal-bottom-right
                                                                      line-bottom-right-start)])
                                                 "z"]
                                                [right
                                                 (v/+ diagonal-top-right
                                                      line-top-right-start)
                                                 origin-point
                                                 (v/+ diagonal-bottom-right
                                                      line-bottom-right-start)]]

                                               [["M" (v/+ diagonal-bottom-right
                                                          line-bottom-right-start)
                                                 (line/stitch line-bottom-right)
                                                 "L" origin-point
                                                 (line/stitch line-bottom-left)
                                                 (infinity/path :counter-clockwise
                                                                [:left :right]
                                                                [(v/+ diagonal-bottom-left
                                                                      line-bottom-left-start)
                                                                 (v/+ diagonal-bottom-right
                                                                      line-bottom-right-start)])
                                                 "z"]
                                                [bottom
                                                 (v/+ diagonal-bottom-left
                                                      line-bottom-left-start)
                                                 origin-point
                                                 (v/+ diagonal-bottom-right
                                                      line-bottom-right-start)]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ origin-point
                  line-bottom-left-start)
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" (v/+ diagonal-bottom-right
                  line-bottom-right-start)
         (line/stitch line-bottom-right)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-top-left
                              line-top-left-start)
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ origin-point
                              line-top-right-start)
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-bottom-right
                              line-bottom-right-start)
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ origin-point
                              line-bottom-left-start)
                     (line/stitch line-bottom-left)])}]])
     environment division context]))

(defn quartered
  {:display-name "Quarterly 2x2"
   :parts        ["I" "II" "III" "IV"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]}           (options/sanitize division (options division))
        points                          (:points environment)
        origin-point                    (position/calculate origin environment :fess)
        top                             (assoc (:top points) :x (:x origin-point))
        top-left                        (:top-left points)
        top-right                       (:top-right points)
        bottom                          (assoc (:bottom points) :x (:x origin-point))
        bottom-left                     (:bottom-left points)
        bottom-right                    (:bottom-right points)
        left                            (assoc (:left points) :y (:y origin-point))
        right                           (assoc (:right points) :y (:y origin-point))
        {line-top       :line
         line-top-start :line-start}    (line/create line
                                                     (v/abs (v/- top origin-point))
                                                     :angle 90
                                                     :reversed? true
                                                     :render-options render-options)
        {line-right       :line
         line-right-start :line-start}  (line/create line
                                                     (v/abs (v/- right origin-point))
                                                     :flipped? true
                                                     :render-options render-options)
        {line-bottom       :line
         line-bottom-start :line-start} (line/create line
                                                     (v/abs (v/- bottom origin-point))
                                                     :angle -90
                                                     :reversed? true
                                                     :render-options render-options)
        {line-left       :line
         line-left-start :line-start}   (line/create line
                                                     (v/abs (v/- left origin-point))
                                                     :angle -180
                                                     :flipped? true
                                                     :render-options render-options)
        parts                           [[["M" (v/+ top
                                                    line-top-start)
                                           (line/stitch line-top)
                                           "L" origin-point
                                           (line/stitch line-left)
                                           (infinity/path :clockwise
                                                          [:left :top]
                                                          [(v/+ left
                                                                line-left-start)
                                                           (v/+ top
                                                                line-top-start)])
                                           "z"]
                                          [top-left origin-point]]

                                         [["M" (v/+ top
                                                    line-top-start)
                                           (line/stitch line-top)
                                           "L" origin-point
                                           (line/stitch line-right)
                                           (infinity/path :counter-clockwise
                                                          [:right :top]
                                                          [(v/+ right
                                                                line-right-start)
                                                           (v/+ top
                                                                line-top-start)])
                                           "z"]
                                          [origin-point top-right]]

                                         [["M" (v/+ bottom
                                                    line-bottom-start)
                                           (line/stitch line-bottom)
                                           "L" origin-point
                                           (line/stitch line-left)
                                           (infinity/path :counter-clockwise
                                                          [:left :bottom]
                                                          [(v/+ left
                                                                line-left-start)
                                                           (v/+ bottom
                                                                line-bottom-start)])
                                           "z"]
                                          [origin-point bottom-left]]

                                         [["M" (v/+ bottom
                                                    line-bottom-start)
                                           (line/stitch line-bottom)
                                           "L" origin-point
                                           (line/stitch line-right)
                                           (infinity/path :clockwise
                                                          [:right :bottom]
                                                          [(v/+ right
                                                                line-right-start)
                                                           (v/+ bottom
                                                                line-bottom-start)])
                                           "z"]
                                          [origin-point bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" (v/+ bottom
                  line-bottom-start)
         (line/stitch line-bottom)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ top
                              line-top-start)
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom
                              line-bottom-start)
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-left)])}]])
     environment division context]))

(defn quarterly
  {:display-name "Quarterly NxM"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [layout]}         (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        bottom-right             (:bottom-right points)
        [parts overlap outlines] (quarterly-parts layout top-left bottom-right hints render-options)]
    [make-division
     (division-context-key type) fields parts
     overlap
     outlines
     environment division context]))

(defn gyronny
  {:display-name "Gyronny"
   :parts        ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (options division))
        points                              (:points environment)
        origin-point                        (position/calculate origin environment :fess)
        top                                 (assoc (:top points) :x (:x origin-point))
        bottom                              (assoc (:bottom points) :x (:x origin-point))
        left                                (assoc (:left points) :y (:y origin-point))
        right                               (assoc (:right points) :y (:y origin-point))
        direction                           (direction diagonal-mode points origin-point)
        diagonal-top-left                   (v/project-x origin-point (v/dot direction (v/v -1 -1)) (-> left :x (- 50)))
        diagonal-top-right                  (v/project-x origin-point (v/dot direction (v/v 1 -1)) (-> right :x (+ 50)))
        diagonal-bottom-left                (v/project-x origin-point (v/dot direction (v/v -1 1)) (-> left :x (- 50)))
        diagonal-bottom-right               (v/project-x origin-point (v/dot direction (v/v 1 1)) (-> right :x (+ 50)))
        angle-top-left                      (angle-to-point origin-point diagonal-top-left)
        angle-top-right                     (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                   (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                  (angle-to-point origin-point diagonal-bottom-right)
        {line-top       :line
         line-top-start :line-start}        (line/create line
                                                         (v/abs (v/- top origin-point))
                                                         :angle 90
                                                         :reversed? true
                                                         :render-options render-options)
        {line-right       :line
         line-right-start :line-start}      (line/create line
                                                         (v/abs (v/- right origin-point))
                                                         :reversed? true
                                                         :angle 180
                                                         :render-options render-options)
        {line-bottom       :line
         line-bottom-start :line-start}     (line/create line
                                                         (v/abs (v/- bottom origin-point))
                                                         :angle -90
                                                         :reversed? true
                                                         :render-options render-options)
        {line-left       :line
         line-left-start :line-start}       (line/create line
                                                         (v/abs (v/- left origin-point))
                                                         :reversed? true
                                                         :render-options render-options)
        {line-top-left :line}               (line/create line
                                                         (v/abs (v/- diagonal-top-left origin-point))
                                                         :flipped? true
                                                         :angle angle-top-left
                                                         :render-options render-options)
        {line-top-right :line}              (line/create line
                                                         (v/abs (v/- diagonal-top-right origin-point))
                                                         :flipped? true
                                                         :angle angle-top-right
                                                         :render-options render-options)
        {line-bottom-right :line}           (line/create line
                                                         (v/abs (v/- diagonal-bottom-right origin-point))
                                                         :flipped? true
                                                         :angle angle-bottom-right
                                                         :render-options render-options)
        {line-bottom-left :line}            (line/create line
                                                         (v/abs (v/- diagonal-bottom-left origin-point))
                                                         :flipped? true
                                                         :angle angle-bottom-left
                                                         :render-options render-options)
        parts                               [[["M" (v/+ top
                                                        line-top-start)
                                               (line/stitch line-top)
                                               "L" origin-point
                                               (line/stitch line-top-left)
                                               (infinity/path :clockwise
                                                              [:left :top]
                                                              [diagonal-top-left
                                                               (v/+ top
                                                                    line-top-start)])
                                               "z"]
                                              [diagonal-top-left
                                               origin-point
                                               (v/+ top
                                                    line-top-start)]]

                                             [["M" (v/+ top
                                                        line-top-start)
                                               (line/stitch line-top)
                                               "L" origin-point
                                               (line/stitch line-top-right)
                                               (infinity/path :counter-clockwise
                                                              [:right :top]
                                                              [diagonal-top-right
                                                               (v/+ top
                                                                    line-top-start)])
                                               "z"]
                                              [(v/+ top
                                                    line-top-start)
                                               origin-point
                                               diagonal-top-right]]

                                             [["M" (v/+ left
                                                        line-left-start)
                                               (line/stitch line-left)
                                               "L" origin-point
                                               (line/stitch line-top-left)
                                               (infinity/path :counter-clockwise
                                                              [:left :left]
                                                              [diagonal-top-left
                                                               (v/+ left
                                                                    line-left-start)])
                                               "z"]
                                              [(v/+ left
                                                    line-left-start)
                                               origin-point
                                               diagonal-top-left]]

                                             [["M" (v/+ right
                                                        line-right-start)
                                               (line/stitch line-right)
                                               "L" origin-point
                                               (line/stitch line-top-right)
                                               (infinity/path :clockwise
                                                              [:right :right]
                                                              [diagonal-top-right
                                                               (v/+ right
                                                                    line-right-start)])
                                               "z"]
                                              [diagonal-top-right
                                               origin-point
                                               (v/+ right
                                                    line-right-start)]]

                                             [["M" (v/+ left
                                                        line-left-start)
                                               (line/stitch line-left)
                                               "L" origin-point
                                               (line/stitch line-bottom-left)
                                               (infinity/path :clockwise
                                                              [:left :left]
                                                              [diagonal-bottom-left
                                                               (v/+ left
                                                                    line-left-start)])
                                               "z"]
                                              [diagonal-bottom-left
                                               origin-point
                                               (v/+ left
                                                    line-left-start)]]

                                             [["M" (v/+ right
                                                        line-right-start)
                                               (line/stitch line-right)
                                               "L" origin-point
                                               (line/stitch line-bottom-right)
                                               (infinity/path :counter-clockwise
                                                              [:right :right]
                                                              [diagonal-bottom-right
                                                               (v/+ right
                                                                    line-right-start)])
                                               "z"]
                                              [(v/+ right
                                                    line-right-start)
                                               origin-point
                                               diagonal-bottom-right]]

                                             [["M" (v/+ bottom
                                                        line-bottom-start)
                                               (line/stitch line-bottom)
                                               "L" origin-point
                                               (line/stitch line-bottom-left)
                                               (infinity/path :counter-clockwise
                                                              [:left :bottom]
                                                              [diagonal-bottom-left
                                                               (v/+ bottom
                                                                    line-bottom-start)])
                                               "z"]
                                              [(v/+ bottom
                                                    line-bottom-start)
                                               origin-point
                                               diagonal-bottom-left]]

                                             [["M" (v/+ bottom
                                                        line-bottom-start)
                                               (line/stitch line-bottom)
                                               "L" origin-point
                                               (line/stitch line-bottom-right)
                                               (infinity/path :clockwise
                                                              [:right :bottom]
                                                              [diagonal-bottom-right
                                                               (v/+ bottom
                                                                    line-bottom-start)])
                                               "z"]
                                              [diagonal-bottom-right
                                               origin-point
                                               (v/+ bottom
                                                    line-bottom-start)]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-top-right)])]
      [(svg/make-path
        ["M" (v/+ left
                  line-left-start)
         (line/stitch line-left)])]
      [(svg/make-path
        ["M" (v/+ right
                  line-right-start)
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-right)])]
      [(svg/make-path
        ["M" (v/+ bottom
                  line-bottom-start)
         (line/stitch line-bottom)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ top
                              line-top-start)
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ right
                              line-right-start)
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom
                              line-bottom-start)
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ left
                              line-left-start)
                     (line/stitch line-left)])}]])
     environment division context]))

(defn paly
  {:display-name "Paly"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        bottom-right             (:bottom-right points)
        [parts overlap outlines] (paly-parts layout top-left bottom-right line hints render-options)]
    [make-division
     (division-context-key type) fields parts
     overlap
     outlines
     environment division context]))

(defn barry
  {:display-name "Barry"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        bottom-right             (:bottom-right points)
        [parts overlap outlines] (barry-parts layout top-left bottom-right line hints render-options)]
    [make-division
     (division-context-key type) fields parts
     overlap
     outlines
     environment division context]))

(defn chequy
  {:display-name "Chequy"
   :parts        []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout]}        (options/sanitize division (options division))
        points                  (:points environment)
        top-left                (:top-left points)
        bottom-right            (:bottom-right points)
        {:keys [num-base-fields
                num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y]}     layout
        offset-x                (or offset-x 0)
        stretch-x               (or stretch-x 1)
        width                   (- (:x bottom-right)
                                   (:x top-left))
        unstretched-part-width  (-> width
                                    (/ num-fields-x))
        part-width              (-> unstretched-part-width
                                    (* stretch-x))
        offset-y                (or offset-y 0)
        stretch-y               (or stretch-y 1)
        height                  (- (:y bottom-right)
                                   (:y top-left))
        unstretched-part-height (if num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height             (-> unstretched-part-height
                                    (* stretch-y))
        middle-x                (/ width 2)
        origin-x                (+ (:x top-left)
                                   middle-x)
        pattern-id              (util/id "chequy")]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id            (str pattern-id "-outline")
                   :width         part-width
                   :height        part-height
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline-style
          [:path {:d (str "M 0,0 h " part-width)}]
          [:path {:d (str "M 0,0 v " part-height)}]
          [:path {:d (str "M 0," part-height " h " part-width)}]
          [:path {:d (str "M " part-width ",0 v " part-height)}]]])
      (for [idx (range num-base-fields)]
        ^{:key idx}
        [:pattern {:id            (str pattern-id "-" idx)
                   :width         (* part-width num-base-fields)
                   :height        (* part-height num-base-fields)
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x      0
                 :y      0
                 :width  (* part-width num-base-fields)
                 :height (* part-height num-base-fields)
                 :fill   "#000000"}]
         (for [j (range num-base-fields)
               i (range num-base-fields)]
           (when (-> i (+ j) (mod num-base-fields) (= idx))
             ^{:key [i j]}
             [:rect {:x      (* i part-width)
                     :y      (* j part-height)
                     :width  part-width
                     :height part-height
                     :fill   "#ffffff"}]))])]
     (for [idx (range num-base-fields)]
       (let [mask-id  (util/id "mask")
             tincture (-> fields
                          (get idx)
                          :content
                          :tincture)]
         ^{:key idx}
         [:<>
          [:mask {:id mask-id}
           [:rect {:x      -500
                   :y      -500
                   :width  1100
                   :height 1100
                   :fill   (str "url(#" pattern-id "-" idx ")")}]]
          [:rect {:x      -500
                  :y      -500
                  :width  1100
                  :height 1100
                  :mask   (str "url(#" mask-id ")")
                  :fill   (tincture/pick tincture render-options)}]]))
     (when (or (:outline? render-options)
               (:outline? hints))
       [:rect {:x      -500
               :y      -500
               :width  1100
               :height 1100
               :fill   (str "url(#" pattern-id "-outline)")}])]))

(defn lozengy
  {:display-name "Lozengy"
   :parts        []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout]}        (options/sanitize division (options division))
        points                  (:points environment)
        top-left                (:top-left points)
        bottom-right            (:bottom-right points)
        {:keys [num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y
                rotation]}      layout
        offset-x                (or offset-x 0)
        stretch-x               (or stretch-x 1)
        width                   (- (:x bottom-right)
                                   (:x top-left))
        unstretched-part-width  (-> width
                                    (/ num-fields-x))
        part-width              (-> unstretched-part-width
                                    (* stretch-x))
        offset-y                (or offset-y 0)
        stretch-y               (or stretch-y 1)
        height                  (- (:y bottom-right)
                                   (:y top-left))
        unstretched-part-height (if num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height             (-> unstretched-part-height
                                    (* stretch-y))
        middle-x                (/ width 2)
        pattern-id              (util/id "lozengy")
        lozenge-shape           (svg/make-path ["M" [(/ part-width 2) 0]
                                                "L" [part-width (/ part-height 2)]
                                                "L" [(/ part-width 2) part-height]
                                                "L" [0 (/ part-height 2)]
                                                "z"])]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id            (str pattern-id "-outline")
                   :width         part-width
                   :height        part-height
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* middle-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline-style
          [:path {:d lozenge-shape}]]])
      [:pattern {:id            (str pattern-id "-0")
                 :width         part-width
                 :height        part-height
                 :x             (+ (* part-width offset-x)
                                   (:x top-left)
                                   (- middle-x
                                      (* middle-x stretch-x)))
                 :y             (+ (* part-height offset-y)
                                   (:y top-left))
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x      0
               :y      0
               :width  part-width
               :height part-height
               :fill   "#000000"}]
       [:path {:d    lozenge-shape
               :fill "#ffffff"}]]
      [:pattern {:id            (str pattern-id "-1")
                 :width         part-width
                 :height        part-height
                 :x             (+ (* part-width offset-x)
                                   (:x top-left)
                                   (- middle-x
                                      (* middle-x stretch-x)))
                 :y             (+ (* part-height offset-y)
                                   (:y top-left))
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x      0
               :y      0
               :width  part-width
               :height part-height
               :fill   "#ffffff"}]
       [:path {:d    lozenge-shape
               :fill "#000000"}]]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      (for [idx (range 2)]
        (let [mask-id  (util/id "mask")
              tincture (-> fields
                           (get idx)
                           :content
                           :tincture)]
          ^{:key idx}
          [:<>
           [:mask {:id mask-id}
            [:rect {:x      -500
                    :y      -500
                    :width  1100
                    :height 1100
                    :fill   (str "url(#" pattern-id "-" idx ")")}]]
           [:g {:mask (str "url(#" mask-id ")")}
            [:rect {:x         -500
                    :y         -500
                    :width     1100
                    :height    1100
                    :transform (str "rotate(" rotation ")")
                    :fill      (tincture/pick tincture render-options)}]]]))
      (when (or (:outline? render-options)
                (:outline? hints))
        [:rect {:x      -500
                :y      -500
                :width  1100
                :height 1100
                :fill   (str "url(#" pattern-id "-outline)")}])]]))

(defn bendy
  {:display-name "Bendy"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin diagonal-mode]} (options/sanitize division (options division))
        points                                     (:points environment)
        top-left                                   (:top-left points)
        top-right                                  (:top-right points)
        origin-point                               (position/calculate origin environment :fess)
        direction                                  (direction diagonal-mode points origin-point)
        direction-orthogonal                       (v/v (-> direction :y) (-> direction :x -))
        angle                                      (angle-to-point (v/v 0 0) direction)
        required-half-width                        (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        required-half-height                       (v/distance-point-to-line top-right origin-point (v/+ origin-point direction))
        [parts overlap outlines]                   (barry-parts layout
                                                                (v/v (- required-half-width) (- required-half-height))
                                                                (v/v required-half-width required-half-height)
                                                                line hints render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")")}
     [make-division
      (division-context-key type) fields parts
      overlap
      outlines
      environment division context]]))

(defn bendy-sinister
  {:display-name "Bendy sinister"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin diagonal-mode]} (options/sanitize division (options division))
        points                                     (:points environment)
        top-left                                   (:top-left points)
        top-right                                  (:top-right points)
        origin-point                               (position/calculate origin environment :fess)
        direction                                  (direction diagonal-mode points origin-point)
        direction-orthogonal                       (v/v (-> direction :y) (-> direction :x -))
        angle                                      (angle-to-point (v/v 0 0) (v/dot direction (v/v 1 -1)))
        required-half-width                        (v/distance-point-to-line top-right origin-point (v/+ origin-point direction))
        required-half-height                       (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        [parts overlap outlines]                   (barry-parts layout
                                                                (v/v (- required-half-width) (- required-half-height))
                                                                (v/v required-half-width required-half-height)
                                                                line hints render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")")}
     [make-division
      (division-context-key type) fields parts
      overlap
      outlines
      environment division context]]))

(defn tierced-per-pale
  {:display-name "Tierced per pale"
   :parts        ["dexter" "fess" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin]}      (options/sanitize division (options division))
        {:keys [stretch-x]}               layout
        points                            (:points environment)
        origin-point                      (position/calculate origin environment :fess)
        top                               (assoc (:top points) :x (:x origin-point))
        top-left                          (:top-left points)
        bottom                            (assoc (:bottom points) :x (:x origin-point))
        bottom-right                      (:bottom-right points)
        width                             (:width environment)
        middle-half-width                 (-> width
                                              (/ 6)
                                              (* stretch-x))
        col1                              (- (:x origin-point) middle-half-width)
        col2                              (+ (:x origin-point) middle-half-width)
        first-top                         (v/v col1 (:y top))
        first-bottom                      (v/v col1 (:y bottom))
        second-top                        (v/v col2 (:y top))
        second-bottom                     (v/v col2 (:y bottom))
        {line-one       :line
         line-one-start :line-start}      (line/create line
                                                       (:y (v/- bottom top))
                                                       :angle 90
                                                       :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start} (line/create line
                                                       (:y (v/- bottom top))
                                                       :angle -90
                                                       :reversed? true
                                                       :flipped? true
                                                       :render-options render-options)
        parts                             [[["M" (v/+ first-top
                                                      line-one-start)
                                             (line/stitch line-one)
                                             (infinity/path :clockwise
                                                            [:bottom :top]
                                                            [(v/+ first-bottom
                                                                  line-one-start)
                                                             (v/+ first-top
                                                                  line-one-start)])
                                             "z"]
                                            [top-left
                                             (v/+ first-bottom
                                                  line-one-start)]]

                                           [["M" (v/+ second-bottom
                                                      line-reversed-start)
                                             (line/stitch line-reversed)
                                             (infinity/path :counter-clockwise
                                                            [:top :top]
                                                            [(v/+ second-top
                                                                  line-reversed-start)
                                                             (v/+ first-top
                                                                  line-one-start)])
                                             (line/stitch line-one)
                                             (infinity/path :counter-clockwise
                                                            [:bottom :bottom]
                                                            [(v/+ first-top
                                                                  line-one-start)
                                                             (v/+ second-bottom
                                                                  line-reversed-start)
                                                             first-bottom second-bottom])
                                             "z"]
                                            [(v/+ first-top
                                                  line-one-start)
                                             (v/+ second-bottom
                                                  line-reversed-start)]]

                                           [["M" (v/+ second-bottom
                                                      line-reversed-start)
                                             (line/stitch line-reversed)
                                             (infinity/path :clockwise
                                                            [:top :bottom]
                                                            [(v/+ second-top
                                                                  line-reversed-start)
                                                             (v/+ second-bottom
                                                                  line-reversed-start)])
                                             "z"]
                                            [(v/+ second-top
                                                  line-reversed-start)
                                             bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ second-bottom
                  line-reversed-start)
         (line/stitch line-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-top
                              line-one-start)
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-bottom
                              line-reversed-start)
                     (line/stitch line-reversed)])}]])
     environment division context]))

(defn tierced-per-fess
  {:display-name "Tierced per fess"
   :parts        ["chief" "fess" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin]}      (options/sanitize division (options division))
        {:keys [stretch-y]}               layout
        points                            (:points environment)
        origin-point                      (position/calculate origin environment :fess)
        top-left                          (:top-left points)
        bottom-right                      (:bottom-right points)
        left                              (assoc (:left points) :y (:y origin-point))
        right                             (assoc (:right points) :y (:y origin-point))
        height                            (:height environment)
        middle-half-height                (-> height
                                              (/ 6)
                                              (* stretch-y))
        row1                              (- (:y origin-point) middle-half-height)
        row2                              (+ (:y origin-point) middle-half-height)
        first-left                        (v/v (:x left) row1)
        first-right                       (v/v (:x right) row1)
        second-left                       (v/v (:x left) row2)
        second-right                      (v/v (:x right) row2)
        {line-one       :line
         line-one-start :line-start}      (line/create line
                                                       (:x (v/- right left))
                                                       :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start} (line/create line
                                                       (:x (v/- right left))
                                                       :reversed? true
                                                       :flipped? true
                                                       :angle 180
                                                       :render-options render-options)
        parts                             [[["M" (v/+ first-left
                                                      line-one-start)
                                             (line/stitch line-one)
                                             (infinity/path :counter-clockwise
                                                            [:right :left]
                                                            [(v/+ first-right
                                                                  line-one-start)
                                                             (v/+ first-left
                                                                  line-one-start)])
                                             "z"]
                                            [top-left (v/+ first-right
                                                           line-one-start)]]

                                           [["M" (v/+ first-left
                                                      line-one-start)
                                             (line/stitch line-one)
                                             (infinity/path :clockwise
                                                            [:right :right]
                                                            [(v/+ first-left
                                                                  line-one-start)
                                                             (v/+ second-right
                                                                  line-reversed-start)])
                                             (line/stitch line-reversed)
                                             (infinity/path :clockwise
                                                            [:left :left]
                                                            [(v/+ second-left
                                                                  line-reversed-start)
                                                             (v/+ first-left
                                                                  line-one-start)])
                                             "z"]
                                            [(v/+ first-left
                                                  line-one-start)
                                             second-right]]

                                           [["M" (v/+ second-right
                                                      line-reversed-start)
                                             (line/stitch line-reversed)
                                             (infinity/path :counter-clockwise
                                                            [:left :right]
                                                            [(v/+ second-left
                                                                  line-reversed-start)
                                                             (v/+ second-right
                                                                  line-reversed-start)])
                                             "z"]
                                            [(v/+ second-left
                                                  line-reversed-start) bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ second-right
                  line-reversed-start)
         (line/stitch line-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-left
                              line-one-start)
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-right
                              line-reversed-start)
                     (line/stitch line-reversed)])}]])
     environment division context]))

(defn tierced-per-pairle
  {:display-name "Tierced per pairle"
   :parts        ["chief" "dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]}      (options/sanitize division (options division))
        points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        bottom-left                              (:bottom-left points)
        bottom-right                             (:bottom-right points)
        left                                     (assoc (:left points) :y (:y origin-point))
        right                                    (assoc (:right points) :y (:y origin-point))
        direction                                (direction diagonal-mode points origin-point)
        diagonal-top-left                        (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                       (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle-top-left                           (angle-to-point origin-point diagonal-top-left)
        angle-top-right                          (angle-to-point origin-point diagonal-top-right)
        {line-top-left       :line
         line-top-left-start :line-start}        (line/create line
                                                              (v/abs (v/- diagonal-top-left origin-point))
                                                              :angle (+ angle-top-left 180)
                                                              :reversed? true
                                                              :render-options render-options)
        {line-top-right       :line
         line-top-right-start :line-start}       (line/create line
                                                              (v/abs (v/- diagonal-top-right origin-point))
                                                              :angle angle-top-right
                                                              :flipped? true
                                                              :render-options render-options)
        {line-bottom       :line
         line-bottom-start :line-start}          (line/create line
                                                              (v/abs (v/- bottom origin-point))
                                                              :flipped? true
                                                              :angle 90
                                                              :render-options render-options)
        {line-bottom-reversed       :line
         line-bottom-reversed-start :line-start} (line/create line
                                                              (v/abs (v/- bottom origin-point))
                                                              :angle -90
                                                              :reversed? true
                                                              :render-options render-options)
        parts                                    [[["M" (v/+ diagonal-top-left
                                                             line-top-left-start)
                                                    (line/stitch line-top-left)
                                                    "L" origin-point
                                                    (line/stitch line-top-right)
                                                    (infinity/path :counter-clockwise
                                                                   [:right :left]
                                                                   [(v/+ diagonal-top-right
                                                                         line-top-right-start)
                                                                    (v/+ diagonal-top-left
                                                                         line-top-left-start)])
                                                    "z"]
                                                   [(v/+ diagonal-top-left
                                                         line-top-left-start)
                                                    origin-point
                                                    (v/+ diagonal-top-right
                                                         line-top-right-start)]]

                                                  [["M" (v/+ bottom
                                                             line-bottom-reversed-start)
                                                    (line/stitch line-bottom-reversed)
                                                    "L" origin-point
                                                    (line/stitch line-top-right)
                                                    (infinity/path :clockwise
                                                                   [:right :bottom]
                                                                   [(v/+ diagonal-top-right
                                                                         line-top-right-start)
                                                                    (v/+ bottom
                                                                         line-bottom-reversed-start)])
                                                    "z"]
                                                   [origin-point
                                                    (v/+ diagonal-top-right
                                                         line-top-right-start)
                                                    (v/+ bottom
                                                         line-bottom-reversed-start)
                                                    diagonal-top-right
                                                    bottom-right]]

                                                  [["M" (v/+ diagonal-top-left
                                                             line-top-left-start)
                                                    (line/stitch line-top-left)
                                                    "L" origin-point
                                                    (line/stitch line-bottom)
                                                    (infinity/path :clockwise
                                                                   [:bottom :left]
                                                                   [(v/+ bottom
                                                                         line-bottom-start)
                                                                    (v/+ diagonal-top-left
                                                                         line-top-left-start)])
                                                    "z"]
                                                   [origin-point
                                                    bottom-left
                                                    (v/+ bottom
                                                         line-bottom-start)
                                                    (v/+ diagonal-top-left
                                                         line-top-left-start)]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ bottom
                  line-bottom-reversed-start)
         (line/stitch line-bottom-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-top-left
                              line-top-left-start)
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom)])}]])
     environment division context]))

(defn tierced-per-pairle-reversed
  {:display-name "Tierced per pairle reversed"
   :parts        ["dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]}   (options/sanitize division (options division))
        points                                (:points environment)
        origin-point                          (position/calculate origin environment :fess)
        top                                   (assoc (:top points) :x (:x origin-point))
        top-left                              (:top-left points)
        top-right                             (:top-right points)
        bottom-left                           (:bottom-left points)
        bottom-right                          (:bottom-right points)
        left                                  (assoc (:left points) :y (:y origin-point))
        right                                 (assoc (:right points) :y (:y origin-point))
        direction                             (direction diagonal-mode points origin-point)
        diagonal-bottom-left                  (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right                 (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                     (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                    (angle-to-point origin-point diagonal-bottom-right)
        line                                  (-> line
                                                  (update :offset max 0))
        {line-bottom-right       :line
         line-bottom-right-start :line-start} (line/create line
                                                           (v/abs (v/- diagonal-bottom-right origin-point))
                                                           :angle (+ angle-bottom-right 180)
                                                           :reversed? true
                                                           :render-options render-options)
        {line-bottom-left       :line
         line-bottom-left-start :line-start}  (line/create line
                                                           (v/abs (v/- diagonal-bottom-left origin-point))
                                                           :angle angle-bottom-left
                                                           :flipped? true
                                                           :render-options render-options)
        {line-top       :line
         line-top-start :line-start}          (line/create line
                                                           (v/abs (v/- top origin-point))
                                                           :flipped? true
                                                           :angle -90
                                                           :render-options render-options)
        {line-top-reversed       :line
         line-top-reversed-start :line-start} (line/create line
                                                           (v/abs (v/- top origin-point))
                                                           :angle 90
                                                           :reversed? true
                                                           :render-options render-options)
        parts                                 [[["M" (v/+ top
                                                          line-top-reversed-start)
                                                 (line/stitch line-top-reversed)
                                                 "L" origin-point
                                                 (line/stitch line-bottom-left)
                                                 (infinity/path :clockwise
                                                                [:left :top]
                                                                [(v/+ diagonal-bottom-left
                                                                      line-bottom-left-start)
                                                                 (v/+ top
                                                                      line-top-reversed-start)])
                                                 "z"]
                                                [top-left
                                                 origin-point
                                                 (v/+ diagonal-bottom-left
                                                      line-bottom-left-start)
                                                 (v/+ top
                                                      line-top-reversed-start)]]

                                               [["M" (v/+ diagonal-bottom-right
                                                          line-bottom-right-start)
                                                 (line/stitch line-bottom-right)
                                                 "L" origin-point
                                                 (line/stitch line-top)
                                                 (infinity/path :clockwise
                                                                [:top :right]
                                                                [(v/+ top
                                                                      line-top-start)
                                                                 (v/+ diagonal-bottom-right
                                                                      line-bottom-right-start)])
                                                 "z"]
                                                [top-right
                                                 origin-point
                                                 (v/+ top
                                                      line-top-start)
                                                 (v/+ diagonal-bottom-right
                                                      line-bottom-right-start)]]

                                               [["M" (v/+ diagonal-bottom-right
                                                          line-bottom-right-start)
                                                 (line/stitch line-bottom-right)
                                                 "L" origin-point
                                                 (line/stitch line-bottom-left)
                                                 (infinity/path :counter-clockwise
                                                                [:left :right]
                                                                [(v/+ diagonal-bottom-left
                                                                      line-bottom-left-start)
                                                                 (v/+ diagonal-bottom-right
                                                                      line-bottom-right-start)])
                                                 "z"]
                                                [origin-point bottom-left bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ diagonal-bottom-right
                  line-bottom-right-start)
         (line/stitch line-bottom-right)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-bottom-right
                              line-bottom-right-start)
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]])
     environment division context]))

(def divisions
  [#'per-pale
   #'per-fess
   #'per-bend
   #'per-bend-sinister
   #'per-chevron
   #'per-saltire
   #'quartered
   #'quarterly
   #'gyronny
   #'tierced-per-pale
   #'tierced-per-fess
   #'tierced-per-pairle
   #'tierced-per-pairle-reversed
   #'paly
   #'barry
   #'bendy
   #'bendy-sinister
   #'chequy
   #'lozengy])

(def kinds-function-map
  (->> divisions
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> divisions
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(def division-map
  (util/choices->map choices))

;; https://gist.github.com/jimweirich/1388782
(def roman-reductions
  '((1000 "M")
    (900 "CM") (500 "D") (400 "CD") (100 "C")
    (90 "XC") (50 "L") (40 "XL") (10 "X")
    (9 "IX") (5 "V") (4 "IV") (1 "I")))

(defn to-roman [number]
  (let [counts
        (map first
             (drop 1
                   (reductions (fn [[_ r] v]
                                 (list (int (/ r v))
                                       (- r (* v (int (/ r v))))))
                               (list 0 number)
                               (map first roman-reductions))))
        glyphs (map second roman-reductions)]
    (apply str
           (flatten
            (map (fn [[c g]] (take c (repeat g)))
                 (map vector counts glyphs))))))

(defn part-name [type index]
  (let [function (get kinds-function-map type)]
    (-> function meta :parts (get index) (or (to-roman (inc index))))))

(defn render [{:keys [type] :as division} environment context]
  (let [function (get kinds-function-map type)]
    [function division environment context]))
