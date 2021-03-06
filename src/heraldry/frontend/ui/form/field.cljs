(ns heraldry.frontend.ui.form.field
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]
            [heraldry.frontend.ui.element.range :as range]))

(defn form [path _]
  [:<>
   (for [option [:inherit-environment?
                 :counterchanged?
                 :type
                 :tincture
                 :line
                 :opposite-line
                 :extra-line
                 :variant
                 :thickness
                 :origin
                 :direction-anchor
                 :anchor
                 :geometry
                 :layout
                 :outline?]]
     ^{:key option} [interface/form-element (conj path option)])])

(defn parent-field [path]
  (let [index (last path)
        parent @(rf/subscribe [:get-value (drop-last 2 path)])]
    (when (and (int? index)
               (-> parent :type (or :dummy) namespace (= "heraldry.field.type")))
      parent)))

(defn name-prefix-for-part [path]
  (when-let [parent (parent-field path)]
    (-> (field/part-name (:type parent) (last path))
        util/upper-case-first)))

(defn non-mandatory-part-of-parent? [path]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent (parent-field path)]
        (>= index (field/mandatory-part-count parent))))))

(defmethod interface/component-node-data :heraldry.component/field [path component-data _component-options]
  (let [ref? (-> component-data :type (= :heraldry.field.type/ref))]
    {:title (util/combine ": "
                          [(name-prefix-for-part path)
                           (if ref?
                             (str "like " (name-prefix-for-part (-> path
                                                                    drop-last
                                                                    vec
                                                                    (conj (:index component-data)))))
                             (field/title component-data))])
     :buttons (if ref?
                [{:icon "fas fa-sliders-h"
                  :title "Change"
                  :handler #(state/dispatch-on-event % [:override-field-part-reference path])}]
                (cond-> [{:icon "fas fa-plus"
                          :title "Add"
                          :menu [{:title "Ordinary"
                                  :handler #(state/dispatch-on-event % [:add-component path default/ordinary])}
                                 {:title "Charge"
                                  :handler #(state/dispatch-on-event % [:add-component path default/charge])}
                                 {:title "Charge group"
                                  :handler #(state/dispatch-on-event % [:add-component path default/charge-group])}
                                 {:title "Semy"
                                  :handler #(state/dispatch-on-event % [:add-component path default/semy])}]}]
                  (non-mandatory-part-of-parent? path)
                  (conj {:icon "fas fa-undo"
                         :title "Reset"
                         :handler #(state/dispatch-on-event % [:reset-field-part-reference path])})))
     :nodes (concat (when (-> component-data :type name keyword (not= :plain))
                      (->> component-data
                           :fields
                           count
                           range
                           (map (fn [idx]
                                  {:path (conj path :fields idx)}))
                           vec))
                    (->> component-data
                         :components
                         count
                         range
                         reverse
                         (map (fn [idx]
                                (let [component-path (conj path :components idx)]
                                  {:path component-path
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip "move down"
                                              :handler #(state/dispatch-on-event % [:move-element-down component-path])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec (count (:components component-data))))
                                              :tooltip "move up"
                                              :handler #(state/dispatch-on-event % [:move-element-up component-path])}
                                             {:icon "far fa-trash-alt"
                                              :tooltip "remove"
                                              :handler #(state/dispatch-on-event
                                                         % [:remove-element component-path])}]})))
                         vec))}))

(defmethod interface/component-form-data :heraldry.component/field [_component-data]
  {:form form})

(defmethod interface/form-element :field-layout-num-fields-x [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-x path value]))])

(defmethod interface/form-element :field-layout-num-fields-y [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-fields-y path value]))])

(defmethod interface/form-element :field-layout-num-base-fields [path]
  [range/range-input path
   :on-change (fn [value]
                (rf/dispatch [:set-field-layout-num-base-fields path value]))])
