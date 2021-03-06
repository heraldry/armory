(ns heraldry.frontend.form.tag
  (:require [clojure.string :as s]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(def value-path [:ui :tag-input-value])

(defn on-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set value-path new-value])))

(defn add-tag-clicked [path value]
  (let [tags (-> value
                 (or "")
                 s/trim
                 s/lower-case
                 (s/split #"[^a-z0-9-]+")
                 (->> (filter #(-> % count pos?))))]
    (rf/dispatch [:add-tags path tags])
    (rf/dispatch [:set value-path nil])))

(defn delete-tag-clicked [path tag]
  (rf/dispatch [:remove-tags path [tag]]))

(defn tag-view [tag & {:keys [on-delete
                              on-click
                              selected?]}]
  [:span.tag {:style {:background (if selected?
                                    "#f2bc51"
                                    "#0c6793")
                      :color (if selected?
                               "#000"
                               "#eee")
                      :cursor (when on-click
                                "pointer")}
              :on-click on-click}
   (name tag)
   (when on-delete
     [:span.delete {:on-click on-delete}
      "x"])])

(defn tags-view [tags & {:keys [on-delete
                                on-click
                                selected]}]
  [:div.tags
   (for [tag (sort tags)]
     ^{:key tag}
     [:<>
      [tag-view tag
       :on-delete (when on-delete
                    #(on-delete tag))
       :on-click (when on-click
                   #(on-click tag))
       :selected? (get selected tag)]
      " "])])

(defn form [path]
  (let [value @(rf/subscribe [:get value-path])
        tags @(rf/subscribe [:get path])
        on-click (fn [event]
                   (.preventDefault event)
                   (.stopPropagation event)
                   (add-tag-clicked path value))
        id (util/id "tag-name")]
    [:<>
     [:div.pure-control-group
      [:label {:for   id
               :style {:width "6em"}} "Tag"]
      [:input {:id        id
               :value     value
               :on-change on-change
               :on-key-press (fn [event]
                               (when (-> event .-code (= "Enter"))
                                 (on-click event)))
               :type      "text"
               :style     {:margin-right "0.5em"}}]
      [:button
       {:disabled (-> value (or "") s/trim count zero?)
        :on-click on-click
        :type "button"}
       "Add"]
      [:div {:style {:padding-top "10px"}}
       [tags-view (keys tags)
        :on-delete #(delete-tag-clicked path %)]]]]))
