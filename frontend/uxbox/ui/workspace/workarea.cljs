(ns uxbox.ui.workspace.workarea
  (:require [sablono.core :as html :refer-macros [html]]
            [rum.core :as rum]
            [uxbox.router :as r]
            [uxbox.rstore :as rs]
            [uxbox.state :as s]
            [uxbox.shapes :as shapes]
            [uxbox.library.icons :as _icons]
            [uxbox.ui.mixins :as mx]
            [uxbox.ui.util :as util]
            [uxbox.data.projects :as dp]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.ui.workspace.rules :as wr]
            [uxbox.ui.workspace.toolboxes :as toolboxes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Coordinates Debug
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- coordenates-render
  [own]
  (let [[x y] (rum/react wb/mouse-position)]
    (html
     [:div {:style {:position "absolute" :left "80px" :top "20px"}}
      [:table
       [:tbody
        [:tr [:td "X:"] [:td x]]
        [:tr [:td "Y:"] [:td y]]]]])))

(def coordinates
  (util/component
   {:render coordenates-render
    :name "coordenates"
    :mixins [rum/reactive]}))

(defn background-render
  []
  (html
   [:rect
    {:x 0 :y 0 :width "100%" :height "100%" :fill "white"}]))

(def background
  (util/component
   {:render background-render
    :name "background"
    :mixins [mx/static]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:static grid-color "#cccccc")

(defn grid-render
  [own enabled? zoom]
  (letfn [(vertical-line [position value padding]
            (let [ticks-mod (/ 100 zoom)
                  step-size (/ 10 zoom)]
              (if (< (mod value ticks-mod) step-size)
                (html [:line {:key position
                              :y1 padding
                              :y2 wb/viewport-width
                              :x1 position
                              :x2 position
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.75}])
                (html [:line {:key position
                              :y1 padding
                              :y2 wb/viewport-width
                              :x1 position
                              :x2 position
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.25}]))))
          (horizontal-line [position value padding]
            (let [ticks-mod (/ 100 zoom)
                  step-size (/ 10 zoom)]
              (if (< (mod value ticks-mod) step-size)
                (html [:line {:key position
                              :y1 position
                              :y2 position
                              :x1 padding
                              :x2 wb/viewport-height
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.75}])
                (html [:line {:key position
                              :y1 position
                              :y2 position
                              :x1 padding
                              :x2 wb/viewport-height
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.25}]))))]
    (let [padding (* 20 zoom)
          ticks-mod (/ 100 zoom)
          step-size (/ 10 zoom)
          vertical-ticks (range (- padding wb/document-start-y)
                                (- wb/viewport-height wb/document-start-y padding)
                                step-size)
          horizontal-ticks (range (- padding wb/document-start-x)
                                  (- wb/viewport-width wb/document-start-x padding)
                                  step-size)]
      (html
       [:g.grid
        {:style {:display (if enabled? "block" "none")}}
        (for [tick vertical-ticks]
          (let [position (+ tick wb/document-start-x)
                line (vertical-line position tick padding)]
            (rum/with-key line (str "tick-" tick))))
        (for [tick horizontal-ticks]
          (let [position (+ tick wb/document-start-y)
                line (horizontal-line position tick padding)]
            (rum/with-key line (str "tick-" tick))))]))))

(def grid
  (util/component
   {:render grid-render
    :name "grid"
    :mixins [mx/static]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Shape
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private
  selection-circle-style
  {:fillOpacity "0.5"
   :strokeWidth "1px"
   :vectorEffect "non-scaling-stroke"
   :cursor "move"})

(def ^:private
  default-selection-props
  {:r 4 :style selection-circle-style
   :fill "lavender"
   :stroke "gray"})

(defn- shape-render
  [own shape]
  (let [local (:rum/local own)
        x 30
        y 30
        width 100
        height 100]
    (html
     [:g
      (shapes/render shape {:x x :y y :width width :height height})
      [:g {:class "controls"}
       [:rect {:x x :y y :width width :height height
               :style {:stroke "black" :fill "transparent"
                       :stroke-opacity "0.5"}}]
       [:circle (merge default-selection-props
                       {:cx x :cy y})]
       [:circle (merge default-selection-props
                       {:cx (+ x width) :cy y})]
       [:circle (merge default-selection-props
                       {:cx x :cy (+ y height)})]
       [:circle (merge default-selection-props
                       {:cx (+ x width) :cy (+ y height)})]]])))

(def shape
  (util/component
   {:render shape-render
    :name "shape"
    :mixins [mx/static]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canvas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas-render
  []
  (let [page (rum/react wb/page-state)
        page-width (:width page)
        page-height (:height page)
        ;; selection-uuids (rum/react selected-ids)
        ;; selected-shapes (rum/react selected-shapes)
        ;; raw-shapes (into []
        ;;                  (comp
        ;;                   (filter :shape/visible?)
        ;;                   (filter #(not (contains? selection-uuids (:shape/uuid %))))
        ;;                   (map :shape/data))
        ;;                  shapes)
        item (first _icons/+external+)
        ]
    (html
     [:svg#page-canvas
      {:x wb/document-start-x
       :y wb/document-start-y
       :ref "canvas"
       :width page-width
       :height page-height
       ;; :on-mouse-down cs/on-mouse-down
       ;; :on-mouse-up cs/on-mouse-up
       }
      (background)
      [:svg#page-layout
       (shape item)]
      #_(apply vector :svg#page-layout (map shapes/shape->svg raw-shapes))
      #_(when-let [shape (rum/react drawing)]
          (shapes/shape->drawing-svg shape))
      #_(when-not (empty? selected-shapes)
          (let [rs selected-shapes]
            (vec (cons :g
                       (concat
                        (map shapes/shape->selected-svg rs)
                        (map shapes/shape->svg rs))))))])))

(def canvas
  (util/component
   {:render canvas-render
    :name "canvas"
    :mixins [rum/reactive wb/mouse-mixin]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Viewport
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn viewport-render
  []
  (let [workspace (rum/react wb/workspace-state)
        zoom 1]
    (html
     [:svg#viewport
      {:width wb/viewport-height
       :height wb/viewport-width}
      [:g.zoom
       {:transform (str "scale(" zoom ", " zoom ")")}
       (canvas)
       (grid (:grid-enabled workspace false) zoom)]])))

(def viewport
  (util/component
   {:render viewport-render
    :name "viewport"
    :mixins [rum/reactive]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aside
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn aside-render
  [own]
  (let [workspace (rum/react wb/workspace-state)]
    (html
     [:aside#settings-bar.settings-bar
      [:div.settings-bar-inside
       (when (:draw-toolbox-enabled workspace false)
         (toolboxes/draw-tools))
       (when (:icons-toolbox-enabled workspace false)
         (toolboxes/icons))
       (when (:layers-toolbox-enabled workspace false)
         (toolboxes/layers))]])))

(def aside
  (util/component
   {:render aside-render
    :name "aside"
    :mixins [rum/reactive]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Work Area
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn working-area-render
  [own]
  (let [workspace (rum/react wb/workspace-state)]
    (html
     [:section.workspace-canvas
      {:class (when (empty? (:toolboxes workspace)) "no-tool-bar")
       :on-scroll (constantly nil)}

      #_(when (:selected page)
          (element-options conn
                           page-cursor
                           project-cursor
                           zoom-cursor
                           shapes-cursor))
      (coordinates)
      (viewport)])))

(def workarea
  (util/component
   {:render working-area-render
    :name "workarea"
    :mixins [rum/reactive]}))

