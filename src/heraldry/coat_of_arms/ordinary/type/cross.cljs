(ns heraldry.coat-of-arms.ordinary.type.cross
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.ordinary.type.chevron :as chevron]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Cross"
   :value :heraldry.ordinary.type/cross}
  [{:keys [field] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry outline? cottising]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        bottom (assoc (:bottom points) :x (:x origin-point))
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of width)))
        col1 (- (:x origin-point) (/ band-width 2))
        col2 (+ col1 band-width)
        pale-top-left (v/v col1 (-> top :y (- 10)))
        pale-bottom-left (v/v col1 (-> bottom :y (+ 10)))
        pale-top-right (v/v col2 (-> top :y (- 10)))
        pale-bottom-right (v/v col2 (-> bottom :y (+ 10)))
        row1 (- (:y origin-point) (/ band-width 2))
        row2 (+ row1 band-width)
        fess-top-left (v/v (-> left :x (- 10)) row1)
        fess-top-right (v/v (-> right :x (+ 10)) row1)
        fess-bottom-left (v/v (-> left :x (- 10)) row2)
        fess-bottom-right (v/v (-> right :x (+ 10)) row2)
        corner-top-left (v/v col1 row1)
        corner-top-right (v/v col2 row1)
        corner-bottom-left (v/v col1 row2)
        corner-bottom-right (v/v col2 row2)
        intersection-pale-top-left (v/find-first-intersection-of-ray corner-top-left pale-top-left environment)
        intersection-pale-top-right (v/find-first-intersection-of-ray corner-top-right pale-top-right environment)
        intersection-pale-bottom-left (v/find-first-intersection-of-ray corner-bottom-left pale-bottom-left environment)
        intersection-pale-bottom-right (v/find-first-intersection-of-ray corner-bottom-right pale-bottom-right environment)
        intersection-fess-top-left (v/find-first-intersection-of-ray corner-top-left fess-top-left environment)
        intersection-fess-top-right (v/find-first-intersection-of-ray corner-top-right fess-top-right environment)
        intersection-fess-bottom-left (v/find-first-intersection-of-ray corner-bottom-left fess-bottom-left environment)
        intersection-fess-bottom-right (v/find-first-intersection-of-ray corner-bottom-right fess-bottom-right environment)
        end-pale-top-left (-> intersection-pale-top-left
                              (v/- corner-top-left)
                              v/abs)
        end-pale-top-right (-> intersection-pale-top-right
                               (v/- corner-top-right)
                               v/abs)
        end-pale-bottom-left (-> intersection-pale-bottom-left
                                 (v/- corner-bottom-left)
                                 v/abs)
        end-pale-bottom-right (-> intersection-pale-bottom-right
                                  (v/- corner-bottom-right)
                                  v/abs)
        end-fess-top-left (-> intersection-fess-top-left
                              (v/- corner-top-left)
                              v/abs)
        end-fess-top-right (-> intersection-fess-top-right
                               (v/- corner-top-right)
                               v/abs)
        end-fess-bottom-left (-> intersection-fess-bottom-left
                                 (v/- corner-bottom-left)
                                 v/abs)
        end-fess-bottom-right (-> intersection-fess-bottom-right
                                  (v/- corner-bottom-right)
                                  v/abs)
        end (max end-pale-top-left
                 end-pale-top-right
                 end-pale-bottom-left
                 end-pale-bottom-right
                 end-fess-top-left
                 end-fess-top-right
                 end-fess-bottom-left
                 end-fess-bottom-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-pale-top-left :line
         line-pale-top-left-start :line-start
         line-pale-top-left-min :line-min
         :as line-pale-top-left-data} (line/create line
                                                   corner-top-left pale-top-left
                                                   :real-start 0
                                                   :real-end end
                                                   :render-options render-options
                                                   :environment environment)
        {line-pale-top-right :line
         line-pale-top-right-start :line-start
         :as line-pale-top-right-data} (line/create line
                                                    corner-top-right pale-top-right
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end end
                                                    :render-options render-options
                                                    :environment environment)
        {line-fess-top-right :line
         line-fess-top-right-start :line-start
         :as line-fess-top-right-data} (line/create line
                                                    corner-top-right fess-top-right
                                                    :real-start 0
                                                    :real-end end
                                                    :render-options render-options
                                                    :environment environment)
        {line-fess-bottom-right :line
         line-fess-bottom-right-start :line-start
         :as line-fess-bottom-right-data} (line/create line
                                                       corner-bottom-right fess-bottom-right
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end end
                                                       :render-options render-options
                                                       :environment environment)
        {line-pale-bottom-right :line
         line-pale-bottom-right-start :line-start
         :as line-pale-bottom-right-data} (line/create line
                                                       corner-bottom-right pale-bottom-right
                                                       :real-start 0
                                                       :real-end end
                                                       :render-options render-options
                                                       :environment environment)
        {line-pale-bottom-left :line
         line-pale-bottom-left-start :line-start
         :as line-pale-bottom-left-data} (line/create line
                                                      corner-bottom-left pale-bottom-left
                                                      :reversed? true
                                                      :real-start 0
                                                      :real-end end
                                                      :render-options render-options
                                                      :environment environment)
        {line-fess-bottom-left :line
         line-fess-bottom-left-start :line-start
         :as line-fess-bottom-left-data} (line/create line
                                                      corner-bottom-left fess-bottom-left
                                                      :real-start 0
                                                      :real-end end
                                                      :render-options render-options
                                                      :environment environment)
        {line-fess-top-left :line
         line-fess-top-left-start :line-start
         :as line-fess-top-left-data} (line/create line
                                                   corner-top-left fess-top-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :render-options render-options
                                                   :environment environment)
        parts [[["M" (v/+ corner-top-left
                          line-pale-top-left-start)
                 (svg/stitch line-pale-top-left)
                 (infinity/path :clockwise
                                [:top :top]
                                [(v/+ pale-top-left
                                      line-pale-top-left-start)
                                 (v/+ pale-top-right
                                      line-pale-top-right-start)])
                 (svg/stitch line-pale-top-right)
                 "L" (v/+ corner-top-right
                          line-fess-top-right-start)
                 (svg/stitch line-fess-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/+ fess-top-right
                                      line-fess-top-right-start)
                                 (v/+ fess-bottom-right
                                      line-fess-bottom-right-start)])
                 (svg/stitch line-fess-bottom-right)
                 "L" (v/+ corner-bottom-right
                          line-pale-bottom-right-start)
                 (svg/stitch line-pale-bottom-right)
                 (infinity/path :clockwise
                                [:bottom :bottom]
                                [(v/+ pale-bottom-right
                                      line-pale-bottom-right-start)
                                 (v/+ pale-bottom-left
                                      line-pale-bottom-left-start)])
                 (svg/stitch line-pale-bottom-left)
                 "L" (v/+ corner-bottom-left
                          line-fess-bottom-left-start)
                 (svg/stitch line-fess-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/+ fess-bottom-left
                                      line-fess-bottom-left-start)
                                 (v/+ fess-top-left
                                      line-fess-top-left-start)])
                 (svg/stitch line-fess-top-left)
                 "z"]
                [top bottom left right]]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field ordinary parent)
                field)
        outline? (or (:outline? render-options)
                     outline?)
        {:keys [cottise-1
                cottise-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-fess-top-left-data
                        line-pale-top-left-data] fess-top-left outline? render-options)
     (line/render line [line-pale-top-right-data
                        line-fess-top-right-data] pale-top-right outline? render-options)
     (line/render line [line-fess-bottom-right-data
                        line-pale-bottom-right-data] fess-bottom-right outline? render-options)
     (line/render line [line-pale-bottom-left-data
                        line-fess-bottom-left-data] pale-bottom-left outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (:cottise-1 cottising)
             chevron-base {:type :heraldry.ordinary.type/chevron
                           :line (:line cottise-1)
                           :opposite-line (:opposite-line cottise-1)}
             chevron-options (ordinary-options/options chevron-base)
             {:keys [line
                     opposite-line]} (options/sanitize chevron-base chevron-options)
             half-joint-angle 45
             half-joint-angle-rad (-> half-joint-angle
                                      (/ 180)
                                      (* Math/PI)
                                      Math/sin)
             dist (-> (+ (:distance cottise-1-data))
                      (/ 100)
                      (* width)
                      (- line-pale-top-left-min)
                      (/ (if (zero? half-joint-angle)
                           0.00001
                           (Math/sin half-joint-angle-rad))))
             new-anchor {:point :angle
                         :angle half-joint-angle}]
         [:<>
          (for [[chevron-angle point] [[225 corner-top-left]
                                       [315 corner-top-right]
                                       [135 corner-bottom-left]
                                       [45 corner-bottom-right]]]
            (let [point-offset (-> (v/v dist 0)
                                   (v/rotate chevron-angle)
                                   (v/+ point))
                  fess-offset (v/- point-offset (get points :fess))
                  new-origin {:point :fess
                              :offset-x (-> fess-offset
                                            :x
                                            (/ width)
                                            (* 100))
                              :offset-y (-> fess-offset
                                            :y
                                            (/ height)
                                            (* 100)
                                            -)
                              :alignment :right}
                  new-direction-anchor {:point :angle
                                        :angle (- chevron-angle 90)}]
              ^{:key chevron-angle} [chevron/render (-> {:type :heraldry.ordinary.type/chevron
                                                         :outline? (-> ordinary :outline?)}
                                                        (assoc :cottising {:cottise-opposite-1 cottise-2})
                                                        ;; swap line/opposite-line because the cottise fess is upside down
                                                        (assoc :line opposite-line)
                                                        (assoc :opposite-line line)
                                                        (assoc :field (:field cottise-1))
                                                        (assoc-in [:geometry :size] (:thickness cottise-1-data))
                                                        (assoc :origin new-origin)
                                                        (assoc :direction-anchor new-direction-anchor)
                                                        (assoc :anchor new-anchor)) parent environment
                                     context]))]))]))
