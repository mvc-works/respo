
(ns respo.comp.wrap
  (:require [respo.alias :refer [create-comp div]]
            [respo.comp.text :refer [comp-text]]))

(defn render []
  (fn [state mutate!] (comp-text "pure component component" nil)))

(def comp-wrap (create-comp :wrap render))
