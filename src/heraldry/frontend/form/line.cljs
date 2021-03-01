(ns heraldry.frontend.form.line
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn line-type-choice [path key display-name & {:keys [current]}]
  (let [options          (line/options {:type key})
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :flag
                           :field      {:component :field
                                        :division  {:type   :per-fess
                                                    :line   {:type  key
                                                             :width (* 2 (options/get-value nil (:width options)))}
                                                    :fields [{:content {:tincture :argent}}
                                                             {:content {:tincture (if (= key current) :or :azure)}}]}}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style               {:width  "6.5em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 80"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-line-type [path & {:keys [options can-disable? default value]}]
  (let [line  @(rf/subscribe [:get path])
        value (or value
                  (options/get-value (:type line) (:type options)))]
    [:div.setting
     [:label "Type"]
     [:div.other {:style {:display "inline-block"}}
      (when can-disable?
        [:input {:type      "checkbox"
                 :checked   (some? (:type line))
                 :on-change #(let [new-checked? (-> % .-target .-checked)]
                               (if new-checked?
                                 (rf/dispatch [:set (conj path :type) default])
                                 (rf/dispatch [:remove (conj path :type)])))}])
      (if (some? (:type line))
        [element/submenu (conj path :type) "Select Line Type" (get line/line-map value) {:min-width "25em"}
         (for [[display-name key] (-> options :type :choices)]
           ^{:key display-name}
           [line-type-choice (conj path :type) key display-name :current value])]
        (when can-disable?
          [:span {:style {:color "#ccc"}} (get line/line-map value)
           " (inherited)"]))]]))

(defn form [path & {:keys [title options defaults] :or {title "Line"}}]
  (let [line                     @(rf/subscribe [:get path])
        line-type                (or (:type line)
                                     (:type defaults))
        line-eccentricity        (or (:eccentricity line)
                                     (:eccentricity defaults))
        line-height              (or (:height line)
                                     (:height defaults))
        line-width               (or (:width line)
                                     (:width defaults))
        line-offset              (or (:offset line)
                                     (:offset defaults))
        fimbriation-mode         (or (-> line :fimbriation :mode)
                                     (-> defaults :fimbriation :mode))
        fimbriation-tincture-1   (or (-> line :fimbriation :tincture-1)
                                     (-> defaults :fimbriation :tincture-1))
        fimbriation-tincture-2   (or (-> line :fimbriation :tincture-2)
                                     (-> defaults :fimbriation :tincture-2))
        current-fimbriation-mode (options/get-value fimbriation-mode
                                                    (-> options :fimbriation :mode))]
    [:div.setting
     [:label title]
     " "
     [element/submenu path title (get line/line-map line-type) {}
      [form-for-line-type path :options options
       :can-disable? (some? defaults)
       :value line-type
       :default (:type defaults)]
      (when (:eccentricity options)
        [element/range-input-with-checkbox (conj path :eccentricity) "Eccentricity"
         (-> options :eccentricity :min)
         (-> options :eccentricity :max)
         :step 0.01
         :default (or (:eccentricity defaults)
                      (options/get-value line-eccentricity (:eccentricity options)))])
      (when (:height options)
        [element/range-input-with-checkbox (conj path :height) "Height"
         (-> options :height :min)
         (-> options :height :max)
         :step 0.01
         :default (or (:height defaults)
                      (options/get-value line-height (:height options)))])
      (when (:width options)
        [element/range-input-with-checkbox (conj path :width) "Width"
         (-> options :width :min)
         (-> options :width :max)
         :default (or (:width defaults)
                      (options/get-value line-width (:width options)))
         :display-function #(str % "%")])
      (when (:offset options)
        [element/range-input-with-checkbox (conj path :offset) "Offset"
         (-> options :offset :min)
         (-> options :offset :max)
         :step 0.01
         :default (or (:offset defaults)
                      (options/get-value line-offset (:offset options)))])
      (when (:flipped? options)
        [element/checkbox (conj path :flipped?) "Flipped"])
      (when (:fimbriation options)
        (let [fimbriation-path (conj path :fimbriation)
              link-name        (case fimbriation-mode
                                 :single (util/combine ", " ["single"
                                                             (util/translate-cap-first fimbriation-tincture-1)])
                                 :double (util/combine ", " ["double"
                                                             (util/translate-cap-first fimbriation-tincture-2)
                                                             (util/translate-cap-first fimbriation-tincture-1)])
                                 "None")]
          [:div.setting
           [:label "Fimbriation"]
           " "
           [element/submenu fimbriation-path "Fimbriation" link-name {}
            (when (-> options :fimbriation :mode)
              [element/select (conj fimbriation-path :mode) "Mode"
               (-> options :fimbriation :mode :choices)
               :default (-> options :fimbriation :mode :default)])
            (when (and (not= current-fimbriation-mode :none)
                       (-> options :fimbriation :alignment))
              [element/select (conj fimbriation-path :alignment) "Alignment"
               (-> options :fimbriation :alignment :choices)
               :default (-> options :fimbriation :alignment :default)])
            (when (and (not= current-fimbriation-mode :none)
                       (-> options :fimbriation :outline?))
              [element/checkbox (conj fimbriation-path :outline?) "Outline"])
            (when (and (#{:single :double} current-fimbriation-mode)
                       (-> options :fimbriation :thickness-1))
              [element/range-input (conj fimbriation-path :thickness-1)
               (str "Thickness"
                    (when (#{:double} current-fimbriation-mode) " 1"))
               (-> options :fimbriation :thickness-1 :min)
               (-> options :fimbriation :thickness-1 :max)
               :default (or (-> defaults :fimbriation :thickness-1)
                            (options/get-value (-> line :fimbriation :thickness-1)
                                               (-> options :fimbriation :thickness-1)))
               :step 0.1
               :display-function #(str % "%")])
            (when (and (#{:single :double} fimbriation-mode)
                       (-> options :fimbriation :tincture-1))
              [tincture/form (conj fimbriation-path :tincture-1)
               :label (str "Tincture"
                           (when (#{:double} current-fimbriation-mode) " 1"))])
            (when (and (#{:double} current-fimbriation-mode)
                       (-> options :fimbriation :thickness-2))
              [element/range-input (conj fimbriation-path :thickness-2) "Thickness 2"
               (-> options :fimbriation :thickness-2 :min)
               (-> options :fimbriation :thickness-2 :max)
               :default (or (-> defaults :fimbriation :thickness-2)
                            (options/get-value (-> line :fimbriation :thickness-2)
                                               (-> options :fimbriation :thickness-2)))
               :step 0.1
               :display-function #(str % "%")])
            (when (and (#{:double} current-fimbriation-mode)
                       (-> options :fimbriation :tincture-2))
              [tincture/form (conj fimbriation-path :tincture-2)
               :label "Tincture 2"])]]))]]))