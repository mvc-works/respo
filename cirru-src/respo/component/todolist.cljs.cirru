
ns respo.component.todolist $ :require
  [] clojure.string :as string
  [] hsl.core :refer $ [] hsl
  [] respo.component.task :refer $ [] task-component
  [] respo.renderer.alias :refer $ [] div span input create-comp

def style-root $ {} (:color |black)
  :background-color $ hsl 120 20 93
  :line-height |24px
  :font-size |16px
  :padding |10px

def style-list $ {} (:color |black)
  :background-color $ hsl 120 20 96

def style-input $ {} (:font-size |16px)
  :line-height |24px
  :padding "|0px 8px"
  :outline |none
  :min-width |300px

def style-toolbar $ {} (:display |flex)
  :flex-direction |row
  :justify-content |center
  :width |300px
  :padding "|4px 0"

def style-button $ {} (:display |inline-block)
  :padding "|0 6px 0 6px"
  :font-family |Verdana
  :cursor |pointer
  :border-radius |4px
  :margin-left |8px

def style-panel $ {} (:display |flex)

defn clear-done (props state)
  fn (event dispatch mutate)
    .log js/console "|dispatch clear-done"
    dispatch :clear nil

defn on-focus (props state)
  fn (event dispatch mutate)
    .log js/console "|Just focused~"

defn on-text-change (props state)
  fn (simple-event dispatch mutate)
    mutate $ {} :draft (:value simple-event)

defn handle-add (props state)
  -- .log js/console "|state built inside:" (pr-str props)
    pr-str state
  fn (event dispatch mutate)
    .log js/console "|click add!" (pr-str props)
      pr-str state
    dispatch :add $ :draft state
    mutate $ {} :draft |

defn init-state (props)
  {} :draft |

defn update-state (old-state changes)
  .log js/console |changes: (pr-str old-state)
    pr-str changes
  merge old-state changes

defn render (props)
  fn (state)
    let
      (tasks $ :tasks props)
      .log js/console |tasks: $ pr-str tasks
      div ({} :style style-root)
        div ({} :style style-panel)
          input $ {} :style style-input :event
            {} :input (on-text-change props state)
              , :focus
              on-focus props state
            , :attrs
            {} :placeholder |Text

          div ({} :style style-button)
            span $ {} :event
              {} :click $ handle-add props state
              , :attrs
              {} :inner-text |Add

          div $ {} :style style-button :event
            {} :click $ clear-done props state
            , :attrs
            {} :inner-text |Clear

        div
          {} :style style-list :attrs $ {} :class-name |task-list
          ->> tasks
            map $ fn (task)
              [] (:id task)
                task-component $ {} :task task

            into $ sorted-map

        if
          > (count tasks)
            , 0
          div
            {} :style style-toolbar :attrs $ {} :spell-check true
            div
              {} :style style-button :event $ {} :click (clear-done props state)
              span $ {} :attrs
                {} $ :inner-text |Clear2

def todolist-component $ create-comp :todolist init-state update-state render
