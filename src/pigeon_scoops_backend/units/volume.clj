(ns pigeon-scoops-backend.units.volume)

(def floz-to-ml 29.5735)

(def us-liquid {:volume/gal  (* 128 floz-to-ml)
                :volume/qt   (* 32 floz-to-ml)
                :volume/pt   (* 16 floz-to-ml)
                :volume/c    (* 8 floz-to-ml)
                :volume/floz floz-to-ml
                :volume/tbsp (/ floz-to-ml 2)
                :volume/tsp  (/ floz-to-ml 6)})

(def metric-liquid {:volume/l  1000
                    :volume/ml 1})

(def conversion-map (merge us-liquid metric-liquid))
