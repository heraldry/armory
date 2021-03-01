(ns heraldry.frontend.form.component
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.charge :as charge]
            [heraldry.frontend.form.division :as division-form]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.form.geometry :as geometry]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

;; components


(declare form-for-field)

(defn form-for-coat-of-arms [db-path]
  [element/component db-path :coat-of-arms "Coat of Arms" nil
   [escutcheon/form (conj db-path :escutcheon) "Default Escutcheon" :label-width "11em"]
   [form-for-field (conj db-path :field)]])

(defn form-for-content [path]
  [:div.form-content
   [tincture/form (conj path :tincture)]])

(defn ordinary-type-choice [path key display-name & {:keys [current]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:component :field
                                   :content {:tincture :argent}
                                   :components [{:component :ordinary
                                                 :type key
                                                 :escutcheon (if (= key :escutcheon) :heater nil)
                                                 :field {:content {:tincture (if (= current key) :or :azure)}}}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-ordinary-type [path]
  (let [ordinary-type @(rf/subscribe [:get (conj path :type)])]
    [:div.setting
     [:label "Type"]
     " "
     [element/submenu path "Select Ordinary" (get ordinary/ordinary-map ordinary-type) {:min-width "17.5em"}
      (for [[display-name key] ordinary/choices]
        ^{:key key}
        [ordinary-type-choice path key display-name :current ordinary-type])]]))

(defn form-for-ordinary [path & {:keys [parent-field]}]
  (let [ordinary @(rf/subscribe [:get path])]
    [element/component
     path :ordinary (-> ordinary :type util/translate-cap-first) nil
     [:div.settings
      [form-for-ordinary-type path]
      (let [ordinary-options (ordinary-options/options ordinary)]
        [:<>
         (when (:escutcheon ordinary-options)
           [escutcheon/form (conj path :escutcheon)])
         (when (:line ordinary-options)
           [line/form (conj path :line) :options (:line ordinary-options)])
         (when (:opposite-line ordinary-options)
           [line/form (conj path :opposite-line)
            :options (:opposite-line ordinary-options)
            :defaults (options/sanitize (:line ordinary) (:line ordinary-options))
            :title "Opposite Line"])
         (when (:diagonal-mode ordinary-options)
           [element/select (conj path :diagonal-mode) "Diagonal"
            (-> ordinary-options :diagonal-mode :choices)
            :default (-> ordinary-options :diagonal-mode :default)])
         (when (:origin ordinary-options)
           [position/form (conj path :origin)
            :title "Origin"
            :options (:origin ordinary-options)])
         (when (:geometry ordinary-options)
           [geometry/form (conj path :geometry)
            (:geometry ordinary-options)
            :current (:geometry ordinary)])])
      [element/checkbox (conj path :hints :outline?) "Outline"]]
     [form-for-field (conj path :field) :parent-field parent-field]]))

(defn form-for-layout [field-path & {:keys [title options] :or {title "Layout"}}]
  (let [layout-path (conj field-path :division :layout)
        division @(rf/subscribe [:get (conj field-path :division)])
        layout (:layout division)
        division-type (:type division)
        current-data (:layout (options/sanitize-or-nil division (division-options/options division)))
        effective-data (:layout (options/sanitize division (division-options/options division)))
        link-name (util/combine
                   ", "
                   [(cond
                      (= division-type :paly) (str (:num-fields-x effective-data) " fields")
                      (#{:barry
                         :bendy
                         :bendy-sinister} division-type) (str (:num-fields-y effective-data) " fields"))
                    (when (and (:num-base-fields current-data)
                               (not= (:num-base-fields current-data) 2))
                      (str (:num-base-fields effective-data) " base fields"))
                    (when (or (:offset-x current-data)
                              (:offset-y current-data))
                      (str "shifted"))
                    (when (or (:stretch-x current-data)
                              (:stretch-y current-data))
                      (str "stretched"))
                    (when (:rotation current-data)
                      (str "rotated"))])
        link-name (if (-> link-name count (= 0))
                    "Default"
                    link-name)]
    [:div.setting
     [:label title]
     " "
     [element/submenu layout-path title link-name {}
      (when (-> options :num-base-fields)
        [element/range-input-with-checkbox (conj layout-path :num-base-fields) "Base fields"
         (-> options :num-base-fields :min)
         (-> options :num-base-fields :max)
         :default (options/get-value (:num-base-fields layout) (:num-base-fields options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    (:num-fields-x layout)
                                    (:num-fields-y layout)
                                    value]))])
      (when (-> options :num-fields-x)
        [element/range-input-with-checkbox (conj layout-path :num-fields-x) "x-Subfields"
         (-> options :num-fields-x :min)
         (-> options :num-fields-x :max)
         :default (options/get-value (:num-fields-x layout) (:num-fields-x options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    value
                                    (:num-fields-y layout)
                                    (:num-base-fields layout)]))])
      (when (-> options :num-fields-y)
        [element/range-input-with-checkbox (conj layout-path :num-fields-y) "y-Subfields"
         (-> options :num-fields-y :min)
         (-> options :num-fields-y :max)
         :default (options/get-value (:num-fields-y layout) (:num-fields-y options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    (:num-fields-x layout)
                                    value
                                    (:num-base-fields layout)]))])
      (when (-> options :offset-x)
        [element/range-input-with-checkbox (conj layout-path :offset-x) "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :step 0.01
         :default (options/get-value (:offset-x layout) (:offset-x options))])
      (when (-> options :offset-y)
        [element/range-input-with-checkbox (conj layout-path :offset-y) "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :step 0.01
         :default (options/get-value (:offset-y layout) (:offset-y options))])
      (when (-> options :stretch-x)
        [element/range-input-with-checkbox (conj layout-path :stretch-x) "Stretch x"
         (-> options :stretch-x :min)
         (-> options :stretch-x :max)
         :step 0.01
         :default (options/get-value (:stretch-x layout) (:stretch-x options))])
      (when (-> options :stretch-y)
        [element/range-input-with-checkbox (conj layout-path :stretch-y) "Stretch y"
         (-> options :stretch-y :min)
         (-> options :stretch-y :max)
         :step 0.01
         :default (options/get-value (:stretch-y layout) (:stretch-y options))])
      (when (-> options :rotation)
        [element/range-input-with-checkbox (conj layout-path :rotation) "Rotation"
         (-> options :rotation :min)
         (-> options :rotation :max)
         :step 5
         :default (options/get-value (:rotation layout) (:rotation options))])]]))

(defn form-for-field [path & {:keys [parent-field title-prefix]}]
  (let [division (-> @(rf/subscribe [:get path])
                     :division)
        division-type (-> division
                          :type
                          (or :none))
        field @(rf/subscribe [:get path])
        counterchanged? (and @(rf/subscribe [:get (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division)))
        root-field? (= path [:coat-of-arms :field])]
    [element/component path :field (cond
                                     (:counterchanged? field) "Counterchanged"
                                     (= division-type :none) (-> field :content :tincture util/translate-tincture util/upper-case-first)
                                     :else (-> division-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [element/checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [element/checkbox (conj path :counterchanged?) "Counterchanged"
         :disabled? (not (division/counterchangable? (-> parent-field :division)))])
      (when (not counterchanged?)
        [:<>
         [division-form/form path]
         (let [division-options (division-options/options (:division field))]
           [:<>
            (when (-> division-options :origin)
              [position/form (conj path :division :origin)
               :title "Origin"
               :options (:origin division-options)])
            (when (-> division-options :diagonal-mode)
              [element/select (conj path :division :diagonal-mode) "Diagonal"
               (-> division-options :diagonal-mode :choices)
               :default (-> division-options :diagonal-mode :default)])
            (when (:layout division-options)
              [form-for-layout path :options (:layout division-options)])
            (when (:line division-options)
              [line/form (conj path :division :line) :options (:line division-options)])])
         (if (= division-type :none)
           [form-for-content (conj path :content)]
           [element/checkbox (conj path :division :hints :outline?) "Outline"])])]
     (cond
       (#{:chequy
          :lozengy} division-type) [:div.parts.components {:style {:margin-bottom "0.5em"}}
                                    [:ul
                                     (let [tinctures @(rf/subscribe [:get (conj path :division :fields)])]
                                       (for [idx (range (count tinctures))]
                                         ^{:key idx}
                                         [:li
                                          [tincture/form (conj path :division :fields idx :content :tincture)
                                           :label (str "Tincture " (inc idx))]]))]]
       (not counterchanged?) [:div.parts.components
                              [:ul
                               (let [content @(rf/subscribe [:get (conj path :division :fields)])
                                     mandatory-part-count (division/mandatory-part-count division)]
                                 (for [[idx part] (map-indexed vector content)]
                                   (let [part-path (conj path :division :fields idx)
                                         part-name (division/part-name division-type idx)
                                         ref (:ref part)]
                                     ^{:key idx}
                                     [:li
                                      [:div
                                       (if ref
                                         [element/component part-path :ref (str "Same as " (division/part-name division-type ref)) part-name]
                                         [form-for-field part-path :title-prefix part-name])]
                                      [:div {:style {:padding-left "10px"}}
                                       (if ref
                                         [:a {:on-click #(do (state/dispatch-on-event % [:set part-path (get content ref)])
                                                             (state/dispatch-on-event % [:ui-component-open part-path]))}
                                          [:i.far.fa-edit]]
                                         (when (>= idx mandatory-part-count)
                                           [:a {:on-click #(state/dispatch-on-event % [:set (conj part-path :ref)
                                                                                       (-> (division/default-fields division)
                                                                                           (get idx)
                                                                                           :ref)])}
                                            [:i.far.fa-times-circle]]))]])))]])
     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/ordinary])}
       [:i.fas.fa-plus] " Add ordinary"]
      " "
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/charge])}
       [:i.fas.fa-plus] " Add charge"]]
     [:div.components
      [:ul
       (let [components @(rf/subscribe [:get (conj path :components)])]
         (for [[idx component] (reverse (map-indexed vector components))]
           (let [component-path (conj path :components idx)]
             ^{:key idx}
             [:li
              [:div {:style {:padding-right "10px"
                             :white-space "nowrap"}}
               [:a (if (zero? idx)
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-component-down component-path])})
                [:i.fas.fa-chevron-down]]
               " "
               [:a (if (= idx (dec (count components)))
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-component-up component-path])})
                [:i.fas.fa-chevron-up]]]
              [:div
               (if (-> component :component (= :ordinary))
                 [form-for-ordinary component-path :parent-field field]
                 [charge/form component-path
                  :parent-field field
                  :form-for-field form-for-field])]
              [:div {:style {:padding-left "10px"}}
               (when (not (and (-> component :component (= :charge))
                               (-> component :type keyword? not)))
                 [:a {:on-click #(state/dispatch-on-event % [:remove-component component-path])}
                  [:i.far.fa-trash-alt]])]])))]]]))

(defn form-attribution [db-path]
  (let [attribution-options [["None (No sharing)" :none]
                             ["CC Attribution" :cc-attribution]
                             ["CC Attribution-ShareAlike" :cc-attribution-share-alike]
                             ["Public Domain" :public-domain]]
        license-nature @(rf/subscribe [:get (conj db-path :nature)])]
    [element/component db-path :attribution "Attribution / License" nil
     [element/select (conj db-path :license) "License" attribution-options]
     [element/radio-select (conj db-path :nature) [["Own work" :own-work]
                                                   ["Derivative" :derivative]]
      :default :own-work]
     (when (= license-nature :derivative)
       [:<>
        [element/select (conj db-path :source-license) "Source license" (assoc-in
                                                                         attribution-options
                                                                         [0 0]
                                                                         "None")]
        [element/text-field (conj db-path :source-name) "Source name"]
        [element/text-field (conj db-path :source-link) "Source link"]
        [element/text-field (conj db-path :source-creator-name) "Creator name"]
        [element/text-field (conj db-path :source-creator-link) "Creator link"]])
     [:div {:style {:margin-bottom "1em"}} " "]]))
