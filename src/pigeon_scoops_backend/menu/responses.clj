(ns pigeon-scoops-backend.menu.responses)

(def durations
  #{:duration/day
    :duration/week
    :duration/month})

(def menu-item-size
  {:menu-item-size/id           uuid?
   :menu-item-size/menu-item-id uuid?
   :menu-item-size/amount       number?
   :menu-item-size/amount-unit  number?})

(def menu-item
  {:menu-item/id        uuid?
   :menu-item/menu-id   uuid?
   :menu-item/recipe-id uuid?
   :menu-item/sizes     [menu-item-size]})

(def menu
  {:menu/id            uuid?
   :menu/name          string?
   :menu/repeats?      boolean?
   :menu/duration      number?
   :menu/duration-type keyword?
   :menu/end-time      string?
   :menu/items         [menu-item]})


