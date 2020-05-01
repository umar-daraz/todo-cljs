(ns main
  (:require [reagent.core :as r]
            [cljs.core.async :refer (chan put! <! go go-loop timeout)]
            ))
(defonce state (r/atom #{}))
(def event-queue (chan))

(go-loop [[event payload] (<! event-queue)]
  (case event
    :hello (prn "Hello Event")
    :edit-todo 
    (swap! state (fn [s] 
                   (map (fn [x] 
                          (update x :title
                                     #(if (= (:id x) (:id payload)) (:title payload) % ))) s)))
    :delete-todo (swap! state (fn [s] (remove #(= (:id %) payload) s)))
    :add-todo (swap! state conj {:id (random-uuid) :title  payload}))
  (recur (<! event-queue)))

(defn todo-item [item edit-item-id]
  (let [edited-title (r/atom (:title item))]
    (fn [item edit-item-id]
      [:li.border-2.p-2.mb-2 
       [:div.flex.justify-between 
        (if (= (:id item) @edit-item-id)
          [:div 
           [:input.mr-2 
            {:type "text" :value @edited-title 
             :on-change #(reset! edited-title (-> % .-target .-value))
             :on-key-press (fn [e]
                             (if (= (.-key e) "Enter")
                               (do
                                 (put! event-queue [:edit-todo 
                                                    {:title @edited-title :id  (:id item)}])
                                 (reset! edit-item-id nil)
                                 )))}] 
           [:button.text-red-400 {:on-click #(reset! edit-item-id nil)}  "X"]]
          [:span (:title item)])
        [:div
         [:button.text-blue-400.mr-2 
          {:on-click #(reset! edit-item-id (:id item))} "edit"]
         [:button.text-red-400 
          {:on-click #(put! event-queue [:delete-todo (:id item)])} "delete" ]]]])))

(defn add-todo []
  (let [new-todo (r/atom "Work")]
    (fn []
      [:div.m-4.flex 
       [:label.mr-2 "TODO"] 
       [:input.flex-grow {:type "text" :value @new-todo 
                          :on-change  #(reset! new-todo (-> % .-target .-value))
                          :on-key-press (fn [e]
                                          (if (= (.-key e) "Enter")
                                            (put! event-queue [:add-todo @new-todo])
                                            ))
                          }]])))
(defn todos []
  (let [edit-item-id (r/atom nil)]
    (fn []
      [:div {:class "w-1/3"} 
       [:h1 {:class "text-2xl"} "Todo list"]
       [add-todo]
       (into [:ul] (for [item @state] [todo-item item edit-item-id]))
       ])))

(defn main-component []
  [:div.pl-4 
   [todos]
   ])


(defn mount [c]
  (r/render-component [c] (.getElementById js/document "app"))
  )

(defn reload! []
  (mount main-component)
  (print "Hello reload!"))

(defn main! []
  (mount main-component)
  (print "Hello Main"))
