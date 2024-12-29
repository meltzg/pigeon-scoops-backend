(ns pigeon-scoops-backend.units-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.units.common :as units]))

(def tolerance 0.0001)

(deftest convert-test
  (testing "can convert units correctly and can be converted back"
    (are [val from to expected]
      (and (< (abs (- expected (units/convert val from to)))
              tolerance)
           (< (abs (- val (units/convert (units/convert val from to) to from)))
              tolerance))
      ;; mass conversion
      1 :mass/lb :mass/oz 16
      2 :mass/kg :mass/lb 4.40925
      3 :mass/kg :mass/g 3000
      4000 :mass/mg :mass/g 4
      ;; volume conversion
      1 :volume/gal :volume/qt 4
      1 :volume/qt :volume/pt 2
      1 :volume/pt :volume/c 2
      1 :volume/c :volume/floz 8
      1 :volume/floz :volume/tbsp 2
      1 :volume/tbsp :volume/tsp 3
      768 :volume/tsp :volume/gal 1
      1 :volume/l :volume/ml 1000
      1 :volume/l :volume/gal 0.2642
      2 :common/unit :common/unit 2)))

(deftest convert-invalid-test
  (testing "invalid conversions return nil"
    (are [from to]
      (nil? (units/convert 8 from to))
      :mass/oz :common/pinch
      :common/pinch :mass/oz
      :volume/tbsp :common/pinch
      :common/pinch :volume/tbsp
      :volume/c :mass/g
      :mass/kg :volume/floz
      :common/pinch :common/unit)))

(deftest scale-factor-test
  (testing "can find scale factor from one amount in one unit to another amount in another unit"
    (are [amount-from unit-from amount-to unit-to expected]
      (let [actual (units/scale-factor amount-from unit-from amount-to unit-to)]
        (if (pos? expected)
          (< (abs (- expected actual)) tolerance)
          (nil? actual)))
      1 :volume/qt 2 :volume/gal 8.0
      1 :volume/qt 0.125 :volume/gal 0.5
      2 :volume/l 5 :volume/c 0.5914699999999999
      2 :mass/lb 4 :mass/oz 0.125
      2 :mass/lb 2 :volume/l -1
      2 :volume/c 2 :mass/g -1
      2 :common/pinch 3 :common/pinch 1.5
      2 :common/pinch 3 :common/unit -1
      2 :common/pinch 3 :volume/c -1
      2 :volume/c 3 :common/unit -1
      1 :volume/c 2 :volume/c 2
      0.25 :volume/c 0.5 :volume/c 2)))

(deftest to-comparable-test
  (testing "can convert an amount to it's comparable unit amount"
    (are [val unit expected]
      (let [actual (units/to-comparable val unit)]
        (if expected
          (< (abs (- expected actual)) tolerance)
          (nil? actual)))
      2 :common/unit 2
      3 :mass/oz (units/convert 3 :mass/oz :mass/g)
      4 :volume/gal (units/convert 4 :volume/gal :volume/ml))))

(deftest to-unit-class-test
  (testing "can convert a unit type to a string representing what class of unit"
    (are [unit-type expected]
      (= (units/to-unit-class unit-type) expected)
      :common/pinch "common"
      :volume/c "volume"
      :mass/g "mass")))

(deftest add-amounts-test
  (testing "can add amounts together"
    (are [amounts expected]
      (= (apply units/add-amounts amounts) expected)
      [1 :volume/pt 3 :volume/c] [2.5 :volume/pt]
      [1 :volume/pt 3 :volume/c 2 :volume/qt 4] [6.5 :volume/pt])))

