
(ns respo.render.differ
  (:require [clojure.string :as string]))

(declare find-element-diffs)

(defn find-children-diffs [acc n-coord index old-children new-children]
  (comment
    .log
    js/console
    "diff children:"
    acc
    n-coord
    index
    old-children
    new-children)
  (cond
    (and (= 0 (count old-children)) (= 0 (count new-children))) acc
    (and (= 0 (count old-children)) (> (count new-children) 0)) (recur
                                                                  (conj
                                                                    acc
                                                                    (let 
                                                                      [entry
                                                                       (first
                                                                         new-children)
                                                                       item
                                                                       (val
                                                                         entry)]
                                                                      [:append
                                                                       n-coord
                                                                       item]))
                                                                  n-coord
                                                                  (inc
                                                                    index)
                                                                  old-children
                                                                  (rest
                                                                    new-children))
    (and (> (count old-children) 0) (= 0 (count new-children))) (recur
                                                                  (conj
                                                                    acc
                                                                    (let 
                                                                      [entry
                                                                       (first
                                                                         old-children)
                                                                       item
                                                                       (val
                                                                         entry)]
                                                                      [:rm
                                                                       (conj
                                                                         n-coord
                                                                         index)]))
                                                                  n-coord
                                                                  index
                                                                  (rest
                                                                    old-children)
                                                                  new-children)
    :else (let [first-old-entry (first old-children)
                first-new-entry (first new-children)
                old-follows (rest old-children)
                new-follows (rest new-children)]
            (case
              (compare (key first-old-entry) (key first-new-entry))
              -1
              (let [acc-after-cursor (conj
                                       acc
                                       [:rm (conj n-coord index)])]
                (recur
                  acc-after-cursor
                  n-coord
                  index
                  old-follows
                  new-children))
              1
              (let [acc-after-cursor (conj
                                       acc
                                       [:add
                                        (conj n-coord index)
                                        (val first-new-entry)])]
                (recur
                  acc-after-cursor
                  n-coord
                  (inc index)
                  old-children
                  new-follows))
              (let [acc-after-cursor (find-element-diffs
                                       acc
                                       (conj n-coord index)
                                       (val first-old-entry)
                                       (val first-new-entry))]
                (recur
                  acc-after-cursor
                  n-coord
                  (inc index)
                  old-follows
                  new-follows))))))

(defn find-style-diffs [acc coord old-style new-style]
  (if (identical? old-style new-style)
    acc
    (cond
      (and (= 0 (count old-style)) (= 0 (count new-style))) acc
      (and (= 0 (count old-style)) (> (count new-style) 0)) (let 
                                                              [entry
                                                               (first
                                                                 new-style)
                                                               follows
                                                               (rest
                                                                 new-style)]
                                                              (recur
                                                                (conj
                                                                  acc
                                                                  [:add-style
                                                                   coord
                                                                   entry])
                                                                coord
                                                                old-style
                                                                follows))
      (and (> (count old-style) 0) (= 0 (count new-style))) (let 
                                                              [entry
                                                               (first
                                                                 old-style)
                                                               follows
                                                               (rest
                                                                 old-style)]
                                                              (recur
                                                                (conj
                                                                  acc
                                                                  [:rm-style
                                                                   coord
                                                                   (key
                                                                     entry)])
                                                                coord
                                                                follows
                                                                new-style))
      :else (let [old-entry (first old-style)
                  new-entry (first new-style)
                  old-follows (rest old-style)
                  new-follows (rest new-style)]
              (case
                (compare (key old-entry) (key new-entry))
                -1
                (recur
                  (conj acc [:rm-style coord (key old-entry)])
                  coord
                  old-follows
                  new-style)
                1
                (recur
                  (conj acc [:add-style coord new-entry])
                  coord
                  old-style
                  new-follows)
                (recur
                  (if (= (val old-entry) (val new-entry))
                    acc
                    (conj acc [:replace-style coord new-entry]))
                  coord
                  old-follows
                  new-follows))))))

(defn find-props-diffs [acc coord old-props new-props]
  (comment
    .log
    js/console
    "find props:"
    acc
    coord
    old-props
    new-props
    (count old-props)
    (count new-props))
  (cond
    (and (= 0 (count old-props)) (= 0 (count new-props))) acc
    (and (= 0 (count old-props)) (> (count new-props) 0)) (recur
                                                            (conj
                                                              acc
                                                              [:add-prop
                                                               coord
                                                               (first
                                                                 new-props)])
                                                            coord
                                                            old-props
                                                            (rest
                                                              new-props))
    (and (> (count old-props) 0) (= 0 (count new-props))) (recur
                                                            (conj
                                                              acc
                                                              [:rm-prop
                                                               coord
                                                               (key
                                                                 (first
                                                                   old-props))])
                                                            coord
                                                            (rest
                                                              old-props)
                                                            new-props)
    :else (let [old-entry (first old-props)
                new-entry (first new-props)
                [old-k old-v] (first old-props)
                [new-k new-v] (first new-props)
                old-follows (rest old-props)
                new-follows (rest new-props)]
            (comment .log js/console old-k new-k old-v new-v)
            (case
              (compare old-k new-k)
              -1
              (recur
                (conj acc [:rm-prop coord old-k])
                coord
                old-follows
                new-props)
              1
              (recur
                (conj acc [:add-prop coord new-entry])
                coord
                old-props
                new-follows)
              (recur
                (if (= old-v new-v)
                  acc
                  (conj acc [:replace-prop coord new-entry]))
                coord
                old-follows
                new-follows)))))

(defn find-events-diffs [acc coord old-events new-events]
  (comment
    .log
    js/console
    "compare events:"
    (pr-str old-events)
    (pr-str new-events))
  (cond
    (and (= (count old-events) 0) (= (count new-events) 0)) acc
    (and (= (count old-events) 0) (> (count new-events) 0)) (recur
                                                              (conj
                                                                acc
                                                                [:add-event
                                                                 coord
                                                                 (first
                                                                   new-events)])
                                                              coord
                                                              old-events
                                                              (rest
                                                                new-events))
    (and (> (count old-events) 0) (= (count new-events) 0)) (recur
                                                              (conj
                                                                acc
                                                                [:rm-event
                                                                 coord
                                                                 (first
                                                                   old-events)])
                                                              coord
                                                              (rest
                                                                old-events)
                                                              new-events)
    :else (case
            (compare (first old-events) (first new-events))
            -1
            (recur
              (conj acc [:rm-event coord (first old-events)])
              coord
              (rest old-events)
              new-events)
            1
            (recur
              (conj acc [:add-event coord (first new-events)])
              coord
              old-events
              (rest new-events))
            (recur acc coord (rest old-events) (rest new-events)))))

(defn find-element-diffs [acc n-coord old-tree new-tree]
  (comment
    .log
    js/console
    "element diffing:"
    acc
    n-coord
    old-tree
    new-tree)
  (if (identical? old-tree new-tree)
    acc
    (let [old-children (:children old-tree)
          new-children (:children new-tree)]
      (if (or
            (not= (:coord old-tree) (:coord new-tree))
            (not= (:name old-tree) (:name new-tree))
            (not= (:c-name old-tree) (:c-name new-tree)))
        (conj acc [:replace n-coord new-tree])
        (-> acc
         (find-style-diffs n-coord (:style old-tree) (:style new-tree))
         (find-props-diffs n-coord (:attrs old-tree) (:attrs new-tree))
         (find-events-diffs
           n-coord
           (sort (keys (:event old-tree)))
           (sort (keys (:event new-tree))))
         (find-children-diffs n-coord 0 old-children new-children))))))
