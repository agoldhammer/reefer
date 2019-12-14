(ns reefer.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [reefer.core-test]))

(doo-tests 'reefer.core-test)
