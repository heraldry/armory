(ns heraldry.coat-of-arms.ordinary.type.chief
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chief"
   :value        :chief}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                     (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                              geometry
        points                                      (:points environment)
        top                                         (:top points)
        top-left                                    (:top-left points)
        left                                        (:left points)
        right                                       (:right points)
        height                                      (:height environment)
        band-height                                 (-> size
                                                        ((util/percent-of height)))
        row                                         (+ (:y top) band-height)
        row-left                                    (v/v (:x left) row)
        row-right                                   (v/v (:x right) row)
        line                                        (-> line
                                                        (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                        (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}    (line/create line
                                                                 (:x (v/- right left))
                                                                 :reversed? true
                                                                 :angle 180
                                                                 :render-options render-options)
        parts                                       [[["M" (v/+ row-right
                                                                line-reversed-start)
                                                       (svg/stitch line-reversed)
                                                       (infinity/path :clockwise
                                                                      [:left :right]
                                                                      [(v/+ row-left
                                                                            line-reversed-start)
                                                                       (v/+ row-right
                                                                            line-reversed-start)])
                                                       "z"]
                                                      [top-left (v/+ row-right
                                                                     line-reversed-start)]]]
        field                                       (if (counterchange/counterchangable? field parent)
                                                      (counterchange/counterchange-field field parent)
                                                      field)
        [fimbriation-elements fimbriation-outlines] (fimbriation/render
                                                     [row-right :right]
                                                     [row-left :left]
                                                     [line-reversed-data]
                                                     (:fimbriation line)
                                                     render-options)]
    [:<>
     fimbriation-elements
     [division-shared/make-division
      :ordinary-chief [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ row-right
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines])
      environment ordinary context]]))