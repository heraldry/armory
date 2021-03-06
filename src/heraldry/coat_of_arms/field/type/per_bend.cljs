(ns heraldry.coat-of-arms.field.type.per-bend
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per bend"
   :value :heraldry.field.type/per-bend
   :parts ["chief" "base"]}
  [{:keys [type fields] :as field} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor outline?]} (options/sanitize field (field-options/options field))
        points (:points environment)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        direction (v/- anchor-point origin-point)
        direction (v/normal (v/v (-> direction :x Math/abs)
                                 (-> direction :y Math/abs)))
        initial-diagonal-start (-> direction
                                   (v/* -1000)
                                   (v/+ origin-point))
        initial-diagonal-end (-> direction
                                 (v/* 1000)
                                 (v/+ origin-point))
        [real-diagonal-start
         real-diagonal-end] (v/environment-intersections
                             initial-diagonal-start
                             initial-diagonal-end
                             environment)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        extra-dir (-> direction
                      (v/* required-extra-length))
        diagonal-start (v/- real-diagonal-start extra-dir)
        diagonal-end (v/+ real-diagonal-end extra-dir)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         diagonal-start diagonal-end
                                         :render-options render-options
                                         :environment environment)
        parts [[["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [real-diagonal-start
                 top-right
                 real-diagonal-end]]

               [["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [real-diagonal-start
                 real-diagonal-end
                 bottom-left]]]
        outline? (or (:outline? render-options)
                     outline?)]
    [:<>
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all nil]
      environment field context]
     (line/render line [line-one-data] diagonal-start outline? render-options)]))
