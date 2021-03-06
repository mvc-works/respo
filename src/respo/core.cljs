
(ns respo.core
  (:require [respo.controller.resolve :refer [build-deliver-event]]
            [respo.render.diff :refer [find-element-diffs]]
            [respo.render.effect :refer [collect-mounting]]
            [respo.util.format :refer [purify-element mute-element]]
            [respo.controller.client :refer [activate-instance! patch-instance!]]
            [respo.util.list
             :refer
             [pick-attrs pick-event val-exists? detect-func-in-map? filter-first]]
            [respo.util.detect :refer [component? element? effect?]]
            [respo.util.dom :refer [compare-to-dom!]]
            [respo.schema :as schema]
            [respo.util.comparator :refer [compare-xy]]
            [memof.alias :refer [tick-calling-loop! reset-calling-caches!]])
  (:require-macros [respo.core]))

(defonce *changes-logger (atom nil))

(defonce *global-element (atom nil))

(defn >> [states k]
  (let [parent-cursor (or (:cursor states) []), branch (get states k)]
    (assoc branch :cursor (conj parent-cursor k))))

(defn clear-cache! [] (tick-calling-loop!))

(defn confirm-child [x]
  (when-not (or (nil? x) (element? x) (component? x))
    (throw (js/Error. (str "Invalid data in elements tree: " (pr-str x)))))
  x)

(defn create-element [tag-name props & children]
  (assert
   (not (some sequential? children))
   (str "For rendering lists, please use list-> , got: " (pr-str children)))
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style)
                 (sort (fn [x y] (compare-xy (first x) (first y))) (:style props))
                 (list))
        event (pick-event props)
        children (->> (map-indexed vector children) (filter val-exists?))]
    (merge
     schema/element
     {:name tag-name,
      :coord nil,
      :attrs attrs,
      :style styles,
      :event event,
      :children children})))

(defn create-list-element [tag-name props child-map]
  (let [attrs (pick-attrs props)
        styles (if (contains? props :style)
                 (sort (fn [x y] (compare-xy (first x) (first y))) (:style props))
                 (list))
        event (pick-event props)]
    (merge
     schema/element
     {:name tag-name,
      :coord nil,
      :attrs attrs,
      :style styles,
      :event event,
      :children child-map})))

(def element-type (if (exists? js/Element) js/Element js/Error))

(defn extract-effects-list [m]
  (let [tree (:tree m)]
    (if (vector? tree)
      (let [node-tree (filter-first
                       (fn [x] (and (map? x) (or (component? x) (element? x))))
                       tree)
            effects-list (filter effect? tree)]
        (merge m {:tree node-tree, :effects effects-list}))
      m)))

(defn mount-app! [target element dispatch!]
  (assert (instance? element-type target) "1st argument should be an element")
  (assert (component? element) "2nd argument should be a component")
  (let [element element
        deliver-event (build-deliver-event *global-element dispatch!)
        *changes (atom [])
        collect! (fn [x]
                   (assert (= 4 (count x)) "change op should has length 3")
                   (swap! *changes conj x))]
    (comment println "mount app")
    (activate-instance! element target deliver-event)
    (collect-mounting collect! [] [] element true)
    (patch-instance! @*changes target deliver-event)
    (reset! *global-element element)))

(defn realize-ssr! [target element dispatch!]
  (assert (instance? element-type target) "1st argument should be an element")
  (assert (component? element) "2nd argument should be a component")
  (let [app-element (.-firstElementChild target)
        *changes (atom [])
        collect! (fn [x]
                   (assert (= 4 (count x)) "change op should has length 3")
                   (swap! *changes conj x))
        deliver-event (build-deliver-event *global-element dispatch!)]
    (if (nil? app-element) (throw (js/Error. "Detected no element from SSR!")))
    (compare-to-dom! (purify-element element) app-element)
    (collect-mounting collect! [] [] element true)
    (patch-instance! @*changes target deliver-event)
    (reset! *global-element (mute-element element))))

(defn rerender-app! [target element dispatch!]
  (tick-calling-loop!)
  (let [deliver-event (build-deliver-event *global-element dispatch!)
        *changes (atom [])
        collect! (fn [x]
                   (assert (= 4 (count x)) "change op should has length 3")
                   (swap! *changes conj x))]
    (comment println @*global-element)
    (comment println "Changes:" (pr-str (mapv (partial take 2) @*changes)))
    (find-element-diffs collect! [] [] @*global-element element)
    (let [logger @*changes-logger]
      (if (some? logger) (logger @*global-element element @*changes)))
    (patch-instance! @*changes target deliver-event)
    (reset! *global-element element)))

(defn render! [target markup dispatch!]
  (if (some? @*global-element)
    (rerender-app! target markup dispatch!)
    (mount-app! target markup dispatch!)))
