(ns heraldry.coat-of-arms.field.type.per-pale
  (:require [heraldry.coat-of-arms.field.options :as division-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per pale"
   :value :per-pale
   :parts ["dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]} (options/sanitize division (division-options/options division))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-top (assoc (:top points) :x (:x origin-point))
        real-bottom (assoc (:bottom points) :x (:x origin-point))
        bottom-right (:bottom-right points)
        effective-width (or (:width line) 1)
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        top (v/- real-top (v/v 0 required-extra-length))
        bottom (v/+ real-bottom (v/v 0 required-extra-length))
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         top
                                         bottom
                                         :render-options render-options
                                         :environment environment)

        parts [[["M" (v/+ top
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [(v/+ bottom
                                      line-one-end)
                                 (v/+ top
                                      line-one-start)])
                 "z"]
                [top-left
                 real-bottom]]

               [["M" (v/+ top
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:bottom :top]
                                [(v/+ bottom
                                      line-one-end)
                                 (v/+ top
                                      line-one-start)])
                 "z"]
                [real-top
                 bottom-right]]]
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     (line/render line [line-one-data] top outline? render-options)]))
