(ns heraldry.frontend.form.charge-map
  (:require [clojure.string :as s]
            [heraldry.frontend.charge-map :as charge-map]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id full-url-for-username]]
            [re-frame.core :as rf]))

(def node-icons
  {:group    {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :facing   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :charge   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :variant  {:normal "fa-image"}})

(defn matches-word [data word]
  (cond
    (keyword? data) (-> data name s/lower-case (s/includes? word))
    (string? data)  (-> data s/lower-case (s/includes? word))
    (map? data)     (some (fn [[k v]]
                            (or (and (keyword? k)
                                     (matches-word k word)
                                     ;; this would be an attribute entry, the value
                                     ;; must be truthy as well
                                     v)
                                (matches-word v word))) data)))

(defn filter-charges [charges filter-string]
  (if (or (not filter-string)
          (-> filter-string s/trim count zero?))
    charges
    (let [words (-> filter-string
                    (s/split #" +")
                    (->> (map s/lower-case)))]
      (filterv (fn [charge]
                 (every? (fn [word]
                           (some (fn [attribute]
                                   (-> charge
                                       (get attribute)
                                       (matches-word word)))
                                 [:name :type :attitude :facing :attributes :colours :username]))
                         words))
               charges))))

(defn tree-for-charge-map [{:keys [node-type name groups charges attitudes facings variants] :as node}
                           tree-path
                           selected-charge remaining-path-to-charge
                           {:keys [still-on-path? render-variant open-all?]
                            :as   opts}]
  (let [flag-path     (conj [:ui :charge-map] tree-path)
        db-open?-path @(rf/subscribe [:get flag-path])
        open?         (or open-all?
                          (= node-type :_root)
                          (and (nil? db-open?-path)
                               still-on-path?)
                          db-open?-path)
        variant?      (= node-type :variant)]
    (cond-> [:<>]
      variant?            (conj
                           [:div.node-name {:on-click nil
                                            :style    {:color (when still-on-path? "#1b6690")
                                                       :left  0}}
                            "\u2022 " [render-variant node]])
      (and (not variant?)
           (not= node-type
                 :_root)) (conj
                           [:div.node-name.clickable
                            {:on-click #(state/dispatch-on-event % [:toggle flag-path])
                             :style    {:color (when still-on-path? "#1b6690")}}
                            (if open?
                              [:i.far {:class (-> node-icons (get node-type) :open)}]
                              [:i.far {:class (-> node-icons (get node-type) :closed)}])
                            [:<>
                             [(cond
                                (and (= node-type :variant)
                                     still-on-path?)    :b
                                (= node-type :charge)   :b
                                (= node-type :attitude) :em
                                (= node-type :facing)   :em
                                :else                   :<>) name]
                             (let [c (charge-map/count-variants node)]
                               (when (pos? c)
                                 [:span.count-badge c]))]])
      (and open?
           groups)        (conj [:ul
                                 (for [[key group] (sort-by first groups)]
                                   (let [following-path?          (and still-on-path?
                                                                       (= (first remaining-path-to-charge)
                                                                          key))
                                         remaining-path-to-charge (when following-path?
                                                                    (drop 1 remaining-path-to-charge))]
                                     ^{:key key}
                                     [:li.group
                                      [tree-for-charge-map
                                       group
                                       (conj tree-path :groups key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           charges)       (conj [:ul
                                 (for [[key charge] (sort-by first charges)]
                                   (let [following-path? (and still-on-path?
                                                              (-> remaining-path-to-charge
                                                                  count zero?)
                                                              (= (:type charge)
                                                                 (:type selected-charge)))]
                                     ^{:key key}
                                     [:li.charge
                                      [tree-for-charge-map
                                       charge
                                       (conj tree-path :charges key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           attitudes)     (conj [:ul
                                 (for [[key attitude] (sort-by first attitudes)]
                                   (let [following-path? (and still-on-path?
                                                              (-> remaining-path-to-charge
                                                                  count zero?)
                                                              (= (:key attitude)
                                                                 (:attitude selected-charge)))]
                                     ^{:key key}
                                     [:li.attitude
                                      [tree-for-charge-map
                                       attitude
                                       (conj tree-path :attitudes key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           facings)       (conj [:ul
                                 (for [[key facing] (sort-by first facings)]
                                   (let [following-path? (and still-on-path?
                                                              (-> remaining-path-to-charge
                                                                  count zero?)
                                                              (= (:key facing)
                                                                 (:facing selected-charge)))]
                                     ^{:key key}
                                     [:li.variant
                                      [tree-for-charge-map
                                       facing
                                       (conj tree-path :facings key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           variants)      (conj [:ul
                                 (for [[key variant] (sort-by (comp :name second) variants)]
                                   (let [following-path? (and still-on-path?
                                                              (-> remaining-path-to-charge
                                                                  count zero?)
                                                              (= (:key variant)
                                                                 (:facing selected-charge)))]
                                     ^{:key key}
                                     [:li.variant
                                      [tree-for-charge-map
                                       variant
                                       (conj tree-path :variants key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))]))))

(defn search-field [db-path]
  (let [current-value @(rf/subscribe [:get db-path])
        input-id      (id "input")]
    [:div {:style {:display       "inline-block"
                   :border-radius "999px"
                   :border        "1px solid #ccc"
                   :padding       "3px 6px"
                   :min-width     "10em"
                   :max-width     "20em"
                   :width         "50%"
                   :margin-bottom "0.5em"}}
     [:i.fas.fa-search]
     [:input {:id           input-id
              :name         "search"
              :type         "text"
              :value        current-value
              :autoComplete "off"
              :on-change    #(let [value (-> % .-target .-value)]
                               (rf/dispatch-sync [:set db-path value]))
              :style        {:outline     "none"
                             :border      "0"
                             :margin-left "0.5em"
                             :width       "calc(100% - 12px - 1.5em)"}}]]))

(defn charge-properties [charge]
  [:div.properties {:style {:display        "inline-block"
                            :line-height    "1.5em"
                            :vertical-align "middle"
                            :white-space    "normal"}}
   (when (-> charge :is-public not)
     [:div.tag.private [:i.fas.fa-lock] "private"])
   (when-let [attitude (-> charge
                           :attitude
                           (#(when (not= % :none) %)))]
     [:div.tag.attitude (util/translate attitude)])
   (when-let [facing (-> charge
                         :facing
                         (#(when (-> % #{:none :to-dexter} not) %)))]
     [:div.tag.facing (util/translate facing)])
   (for [attribute (->> charge
                        :attributes
                        (filter second)
                        (map first)
                        sort)]
     ^{:key attribute}
     [:div.tag.attribute (util/translate attribute)])
   (when-let [fixed-tincture (-> charge
                                 :fixed-tincture
                                 (or :none)
                                 (#(when (not= % :none) %)))]
     [:div.tag.fixed-tincture (util/translate fixed-tincture)])
   (for [modifier (->> charge
                       :colours
                       (map second)
                       (filter #(-> %
                                    #{:primary
                                      :keep
                                      :outline
                                      :eyes-and-teeth}
                                    not))
                       sort)]
     ^{:key modifier}
     [:div.tag.modifier (util/translate modifier)])])

(defn charge-tree [charges & {:keys [remove-empty-groups? hide-access-filters?
                                     link-to-charge render-variant refresh-action]}]
  [:div.tree
   (let [user-data            (user/data)
         filter-db-path       [:ui :charge-tree :filter-string]
         show-public-db-path  [:ui :charge-tree :show-public?]
         show-own-db-path     [:ui :charge-tree :show-own?]
         show-public?         @(rf/subscribe [:get show-public-db-path])
         show-own?            @(rf/subscribe [:get show-own-db-path])
         filter-string        @(rf/subscribe [:get filter-db-path])
         filtered-charges     (-> charges
                                  (filter-charges filter-string)
                                  (cond->>
                                      (not hide-access-filters?) (filter (fn [charge]
                                                                           (or (and show-public?
                                                                                    (:is-public charge))
                                                                               (and show-own?
                                                                                    (= (:username charge)
                                                                                       (:username user-data))))))))
         filtered?            (or (and (not hide-access-filters?)
                                       (not show-public?))
                                  (-> filter-string count pos?))
         remove-empty-groups? (or remove-empty-groups?
                                  filtered?)
         open-all?            filtered?
         charge-map           (charge-map/build-charge-map
                               filtered-charges
                               :remove-empty-groups? remove-empty-groups?)]
     [:<>
      [search-field filter-db-path]
      (when refresh-action
        [:a {:style    {:margin-left "0.5em"}
             :on-click #(do
                          (refresh-action)
                          (.stopPropagation %))} [:i.fas.fa-sync-alt]])
      (when (not hide-access-filters?)
        [:div
         [element/checkbox show-public-db-path "Public charges" :style {:display "inline-block"}]
         [element/checkbox show-own-db-path "Own charges" :style {:display     "inline-block"
                                                                  :margin-left "1em"}]])
      (if (empty? filtered-charges)
        [:div "None"]
        [tree-for-charge-map charge-map [] nil nil
         {:open-all?      open-all?
          :render-variant (or render-variant
                              (fn [node]
                                (let [charge   (-> node :data)
                                      username (-> charge :username)]
                                  [:div {:style {:display        "inline-block"
                                                 :white-space    "normal"
                                                 :vertical-align "top"
                                                 :line-height    "1.5em"}}
                                   [:div {:style {:display        "inline-block"
                                                  :vertical-align "top"}}
                                    [link-to-charge (-> node :data)]
                                    " by "
                                    [:a {:href   (full-url-for-username username)
                                         :target "_blank"} username]]
                                   [charge-properties charge]])))}])])])