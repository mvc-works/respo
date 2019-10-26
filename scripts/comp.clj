
(defmacro defcomp [comp-name params & body]
  `(defmacro ~comp-name [~@params]
    `(merge respo.schema/component
      {:args (list ~@~params) ,
       :name ~(keyword comp-name),
       :render (fn [~@~params]
                 (defn ~~(symbol (str "call-" comp-name)) [~~'%cursor] ~@~body))})))

(defcomp comp-a [a] (div {}))

(println (macroexpand-1 '(comp-a 1)))
