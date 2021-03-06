(ns heraldry.coat-of-arms.field.type.tierced-per-pairle
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per pairle"
   :value :heraldry.field.type/tierced-per-pairle
   :parts ["chief" "dexter" "sinister"]}
  [{:keys [type fields] :as field} environment {:keys [render-options] :as context}]
  (let [{:keys [line opposite-line extra-line
                origin anchor outline?]} (options/sanitize field (field-options/options field))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        chevron-angle 270
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     chevron-angle)
        bottom (assoc (:bottom points) :x (:x origin-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point chevron-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point chevron-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-right relative-left] (chevron/arm-diagonals chevron-angle origin-point anchor-point)
        diagonal-top-left (v/+ origin-point relative-left)
        diagonal-top-right (v/+ origin-point relative-right)
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-top-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-top-right environment)
        end-left (-> intersection-left
                     (v/- origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-top-left :line
         line-top-left-start :line-start} (line/create line
                                                       origin-point diagonal-top-left
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end end
                                                       :render-options render-options
                                                       :environment environment)
        {line-top-right :line
         line-top-right-start :line-start} (line/create opposite-line
                                                        origin-point diagonal-top-right
                                                        :flipped? true
                                                        :mirrored? true
                                                        :real-start 0
                                                        :real-end end
                                                        :render-options render-options
                                                        :environment environment)
        {line-bottom :line
         line-bottom-start :line-start} (line/create extra-line
                                                     origin-point bottom
                                                     :flipped? true
                                                     :mirrored? true
                                                     :render-options render-options
                                                     :environment environment)
        {line-bottom-reversed :line
         line-bottom-reversed-start :line-start} (line/create extra-line
                                                              origin-point bottom
                                                              :reversed? true
                                                              :render-options render-options
                                                              :environment environment)
        parts [[["M" (v/+ diagonal-top-left
                          line-top-left-start)
                 (svg/stitch line-top-left)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ diagonal-top-right
                                      line-top-right-start)
                                 (v/+ diagonal-top-left
                                      line-top-left-start)])
                 "z"]
                [top-left
                 origin-point
                 top-right]]

               [["M" (v/+ bottom
                          line-bottom-reversed-start)
                 (svg/stitch line-bottom-reversed)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/+ diagonal-top-right
                                      line-top-right-start)
                                 (v/+ bottom
                                      line-bottom-reversed-start)])
                 "z"]
                [origin-point
                 bottom
                 top-right
                 bottom-right]]

               [["M" (v/+ diagonal-top-left
                          line-top-left-start)
                 (svg/stitch line-top-left)
                 "L" origin-point
                 (svg/stitch line-bottom)
                 (infinity/path :clockwise
                                [:bottom :left]
                                [(v/+ bottom
                                      line-bottom-start)
                                 (v/+ diagonal-top-left
                                      line-top-left-start)])
                 "z"]
                [origin-point
                 bottom-left
                 bottom
                 top-left]]]]
    [:<>
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ bottom
                   line-bottom-reversed-start)
          (svg/stitch line-bottom-reversed)])]
       nil]
      environment field context]
     (when (or (:outline? render-options)
               outline?)
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-top-left
                              line-top-left-start)
                     (svg/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom)])}]])]))
