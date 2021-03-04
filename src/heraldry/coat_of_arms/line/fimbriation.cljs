(ns heraldry.coat-of-arms.line.fimbriation
  (:require [clojure.walk :as walk]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render-tinctured-area [tincture path {:keys [svg-export?] :as render-options}]
  (let [mask-id (util/id "mask")]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d    (svg/make-path path)
               :fill "#ffffff"}]]]
     [:rect {:x      -500
             :y      -500
             :width  1100
             :height 1100
             :mask   (str "url(#" mask-id ")")
             :fill   (tincture/pick tincture render-options)
             :style  (when (not svg-export?)
                       {:pointer-events "none"})}]]))

(defn render [[start start-direction]
              [end end-direction]
              lines
              {:keys [:tincture-1 :tincture-2]}
              render-options]
  (let [{fimbriation-1             :fimbriation-1
         fimbriation-2             :fimbriation-2
         first-line-start          :line-start
         first-fimbriation-1-start :fimbriation-1-start
         first-fimbriation-2-start :fimbriation-2-start} (first lines)
        {last-line-end          :line-end
         last-fimbriation-1-end :fimbriation-1-end
         last-fimbriation-2-end :fimbriation-2-end}      (last lines)

        elements [:<> (when fimbriation-2
                        [render-tinctured-area
                         tincture-2
                         ["M" (v/+ start first-line-start)
                          (for [{:keys [line]} lines]
                            (svg/stitch line))
                          (if (= end-direction :none)
                            ["L" (v/+ end
                                      last-fimbriation-2-end)]
                            (infinity/path :counter-clockwise
                                           [end-direction end-direction]
                                           [(v/+ end
                                                 last-line-end)
                                            (v/+ end
                                                 last-fimbriation-2-end)]))
                          (for [{:keys [fimbriation-2]} (reverse lines)]
                            (svg/stitch fimbriation-2))
                          (if (= start-direction :none)
                            ["L" (v/+ start
                                      first-line-start)]
                            (infinity/path :counter-clockwise
                                           [start-direction start-direction]
                                           [(v/+ start
                                                 first-fimbriation-2-start)
                                            (v/+ start
                                                 first-line-start)]))
                          "z"]
                         render-options])

                  (when fimbriation-1
                    [render-tinctured-area
                     tincture-1
                     ["M" (v/+ start first-line-start)
                      (for [{:keys [line]} lines]
                        (svg/stitch line))
                      (if (= end-direction :none)
                        ["L" (v/+ end
                                  last-fimbriation-1-end)]
                        (infinity/path :counter-clockwise
                                       [end-direction end-direction]
                                       [(v/+ end
                                             last-line-end)
                                        (v/+ end
                                             last-fimbriation-1-end)]))
                      (for [{:keys [fimbriation-1]} (reverse lines)]
                        (svg/stitch fimbriation-1))
                      (if (= start-direction :none)
                        ["L" (v/+ start
                                  first-line-start)]
                        (infinity/path :counter-clockwise
                                       [start-direction start-direction]
                                       [(v/+ start
                                             first-fimbriation-1-start)
                                        (v/+ start
                                             first-line-start)]))
                      "z"]
                     render-options])]

        outlines [:<>
                  (when fimbriation-1
                    [:path {:d (svg/make-path
                                ["M" (v/+ end last-fimbriation-1-end)
                                 (for [{:keys [fimbriation-1]} (reverse lines)]
                                   (svg/stitch fimbriation-1))])}])

                  (when fimbriation-2
                    [:path {:d (svg/make-path
                                ["M" (v/+ end last-fimbriation-2-end)
                                 (for [{:keys [fimbriation-2]} (reverse lines)]
                                   (svg/stitch fimbriation-2))])}])]]
    [elements outlines]))

(defn dilate-and-fill-path [shape negate-shape thickness color {:keys [svg-export?]}]
  (let [mask-id (util/id "mask")]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d     shape
               :fill  "#ffffff"
               :style {:stroke-width      thickness
                       :stroke            "#ffffff"
                       :stroke-linejoin   "round"
                       :stroke-miterlimit 10}}]
       [:path {:d     negate-shape
               :fill  "#000000"
               :style {:stroke-width      thickness
                       :stroke            "#ffffff"
                       :stroke-linejoin   "round"
                       :stroke-miterlimit 10}}]]]

     [:rect {:x      -500
             :y      -500
             :width  1100
             :height 1100
             :mask   (str "url(#" mask-id ")")
             :fill   color
             :style  (when (not svg-export?)
                       {:pointer-events "none"})}]]))

(defn dilate-recursively [data stroke-width color]
  (walk/postwalk #(cond
                    (and (vector? %)
                         (-> % first (= :stroke-width)))    [(first %) stroke-width]
                    (and (vector? %)
                         (-> % first (= :style)))           [(first %) (-> %
                                                                           second
                                                                           (conj [:stroke-width stroke-width]))]
                    (and (vector? %)
                         (-> % first (= :stroke-linejoin))) [(first %) "round"]
                    (and (vector? %)
                         (-> % first #{:stroke :fill}))     [(first %) color]
                    :else                                   %)
                 data))

(defn dilate-and-fill [shape thickness color {:keys [svg-export?]} & {:keys [transform]}]
  (let [mask-id (util/id "mask")]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:g {:fill  "#ffffff"
            :style {:stroke-width      thickness
                    :stroke            "#ffffff"
                    :stroke-linejoin   "round"
                    :stroke-miterlimit 10}}
        (dilate-recursively shape thickness "#ffffff")]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [:g {:transform transform}
       [:rect {:x      -500
               :y      -500
               :width  1100
               :height 1100
               :fill   color
               :style  (when (not svg-export?)
                         {:pointer-events "none"})}]]]]))
