(ns pigeon-scoops-backend.menu.responses
  (:require [spec-tools.data-spec :as ds]))

(def durations
  #{:duration/day
    :duration/week
    :duration/month})

(def menu-item-size
  {:menu-item-size/id           uuid?
   :menu-item-size/menu-item-id uuid?
   :menu-item-size/amount       number?
   :menu-item-size/amount-unit  keyword?})

(def menu-item
  {:menu-item/id        uuid?
   :menu-item/menu-id   uuid?
   :menu-item/recipe-id uuid?
   :menu-item/sizes     [menu-item-size]})

(def menu
  {:menu/id                uuid?
   :menu/name              string?
   :menu/repeats           boolean?
   :menu/active            boolean?
   :menu/duration          number?
   :menu/duration-type     keyword?
   (ds/opt :menu/items)    [menu-item]
   (ds/opt :menu/end-time) inst?})
