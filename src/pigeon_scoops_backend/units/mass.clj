(ns pigeon-scoops-backend.units.mass)

(def oz-to-g 28.3495)

(def us-mass {:mass/lb (* 16 oz-to-g)
              :mass/oz oz-to-g})

(def metric-mass {:mass/kg 1000
                  :mass/g  1
                  :mass/mg (/ 1 1000)})

(def conversion-map (merge us-mass metric-mass))
