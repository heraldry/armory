(ns heraldry.coat-of-arms.geometry)

(def default-options
  {:size {:type :range
          :min 5
          :max 100
          :default 50
          :ui {:label "Size"
               :step 0.1}}
   :stretch {:type :range
             :min 0.33
             :max 3
             :default 1
             :ui {:label "Stretch"
                  :step 0.01}}
   :mirrored? {:type :boolean
               :default false
               :ui {:label "Mirrored"}}
   :reversed? {:type :boolean
               :default false
               :ui {:label "Reversed"}}
   :ui {:label "Geometry"
        :form-type :geometry}})
