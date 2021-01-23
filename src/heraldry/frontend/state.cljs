(ns heraldry.frontend.state
  (:require [re-frame.core :as rf]))

;; subs


(rf/reg-sub
 :get
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :get-form-error
 (fn [db [_ path]]
   (get-in db (concat [:form-errors] path [:message]))))

;; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:example-coa  {:render-options {:component :render-options
                                           :mode      :colours
                                           :outline?  false
                                           :squiggly? false
                                           :ui        {:selectable-fields? true}}
                          :coat-of-arms   {:escutcheon :rectangle
                                           :field      {:component  :field
                                                        :content    {:tincture :argent}
                                                        :components [{:component :charge
                                                                      :variant   :default
                                                                      :field     {:component :field
                                                                                  :content   {:tincture :azure}}
                                                                      :tincture  {:eyes-and-teeth :argent
                                                                                  :armed          :or
                                                                                  :langued        :gules
                                                                                  :attired        :argent
                                                                                  :unguled        :vert
                                                                                  :beaked         :or}}]}}}
           :coat-of-arms {:escutcheon :rectangle}
           :ui           {:component-open? {[:render-options] true}}} db)))

(rf/reg-event-db
 :set
 (fn [db [_ path value]]
   (assoc-in db path value)))

(rf/reg-event-db
 :remove
 (fn [db [_ path]]
   (cond-> db
     (-> path count (= 1)) (dissoc (first path))
     (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db
 :toggle
 (fn [db [_ path]]
   (update-in db path not)))

(rf/reg-event-db
 :set-form-error
 (fn [db [_ db-path error]]
   (assoc-in db (concat [:form-errors] db-path [:message]) error)))

(rf/reg-event-fx
 :clear-form-errors
 (fn [_ [_ db-path]]
   {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]]}))

(rf/reg-event-fx
 :clear-form
 (fn [_ [_ db-path]]
   {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]
         [:dispatch [:remove db-path]]]}))

(defn dispatch-on-event [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn dispatch-on-event-sync [event effect]
  (rf/dispatch-sync effect)
  (.stopPropagation event))
