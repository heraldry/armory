(ns or.coad.field-content
  (:require [or.coad.division :as division]
            [or.coad.hatching :as hatching]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]))

(defn render [field environment options]
  (let [division   (:division field)
        ordinaries (:ordinaries field)
        tincture   (get-in field [:content :tincture])]
    [:<>
     (cond
       tincture (let [fill (case (:mode options)
                             :colours  (get tincture/tinctures tincture)
                             :hatching (hatching/get-for tincture))]
                  [:path {:d      (:shape environment)
                          :fill   fill
                          :stroke fill}])
       division [division/render division environment render options])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary environment render options])]))
