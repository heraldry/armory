(ns heraldry.coat-of-arms.division.type.per-fess
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per fess"
   :value        :per-fess
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]}          (options/sanitize division (division-options/options division))
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        bottom-right                   (:bottom-right points)
        {line-one       :line
         line-one-start :line-start
         line-one-end   :line-end
         :as            line-one-data} (line/create line
                                                    (:x (v/- right left))
                                                    :render-options render-options)
        parts                          [[["M" (v/+ left
                                                   line-one-start)
                                          (svg/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/+ right
                                                               line-one-end)
                                                          (v/+ left
                                                               line-one-start)])
                                          "z"]
                                         [top-left
                                          right]]

                                        [["M" (v/+ left
                                                   line-one-start)
                                          (svg/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :left]
                                                         [(v/+ right
                                                               line-one-end)
                                                          (v/+ left
                                                               line-one-start)])
                                          "z"]
                                         [left
                                          bottom-right]]]
        [fimbriation-elements
         fimbriation-outlines] (fimbriation/render
                                [left :left]
                                [right :right]
                                [line-one-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ left
                               line-one-start)
                      (svg/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))