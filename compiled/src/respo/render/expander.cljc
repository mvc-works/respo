
(ns respo.render.expander
  (:require [clojure.string :as string]
            [respo.util.time :refer [io-get-time]]
            [respo.util.format :refer [purify-element]]
            [respo.util.detect :refer [component? element?]]))

(defn keyword->string [x] (subs (str x) 1))

(declare render-component)

(declare render-element)

(defn render-markup [markup states build-mutate coord component-coord]
  (if (component? markup)
    (render-component markup states build-mutate coord)
    (render-element markup states build-mutate coord component-coord)))

(defn render-children [children states build-mutate coord comp-coord]
  (comment println "render children:" children)
  (->>
    children
    (map
      (fn [child-entry]
        (let [k (first child-entry)
              child-element (last child-entry)
              inner-states (or (get states k) {})]
          [k
           (if (some? child-element)
             (render-markup
               child-element
               inner-states
               build-mutate
               (conj coord k)
               comp-coord)
             nil)])))
    (filter (fn [entry] (some? (last entry))))
    (into {})))

(defn render-element [markup states build-mutate coord comp-coord]
  (let [children (:children markup)
        child-elements (render-children
                         children
                         states
                         build-mutate
                         coord
                         comp-coord)]
    (comment
      println
      "children should have order:"
      (pr-str children)
      (pr-str child-elements)
      (pr-str markup))
    (assoc
      markup
      :coord
      coord
      :c-coord
      comp-coord
      :children
      child-elements)))

(def component-cached (atom {}))

(defn render-component [markup states build-mutate coord]
  (let [cache-items [markup states coord]]
    (if (contains? @component-cached cache-items)
      (do
        (comment println "hitted cache:" coord)
        (get @component-cached cache-items))
      (let [begin-time (io-get-time)
            args (:args markup)
            component (first markup)
            init-state (:init-state markup)
            new-coord (conj coord (:name markup))
            inner-states (or (get states (:name markup)) {})
            state (or (get inner-states 'data) (apply init-state args))
            render (:render markup)
            half-render (apply render args)
            mutate (build-mutate new-coord)
            markup-tree (half-render state mutate)
            tree (render-element
                   markup-tree
                   inner-states
                   build-mutate
                   new-coord
                   new-coord)
            cost (- (io-get-time) begin-time)
            result (assoc markup :coord coord :tree tree :cost cost)]
        (comment println "markup tree:" (pr-str markup-tree))
        (comment println "component state:" coord states)
        (comment println "no cache" coord (count @component-cached))
        (swap! component-cached assoc cache-items result)
        result))))

(defn render-app [markup states build-mutate]
  (comment .info js/console "render loop, states:" (pr-str states))
  (render-markup markup states build-mutate [] []))
