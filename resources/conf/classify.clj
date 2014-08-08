{:query-days 14
 :rate-bounds {:owner [-999999 0.5]
               :suspect [0.5 0.7]
               :agent [0.7 999999]}
 :appearance-bounds {:owner [-999999 2]
                     :suspect [2 3]
                     :agent [3 999999]}
 :multi-phone-bounds {0 [-999 2]
                      0.3 [2 3]
                      0.5 [3 9999]}
 :img-count-bounds {0.1 [-999 1]
                    0.35 [1 2]
                    0.25 [2 4]
                    0 [4 999]}
 :price-bounds {0.3 [-999 0]
                0.55 [0 3000]
                0.35 [3000 4000]
                0.25 [4000 6000]
                0.23 [6000 7000]
                0.2 [7000 9000]
                0.1 [9000 10000]
                0 [10000 9999999]}
 :rate-weights
 {:by-fact [0 0.9]
  :to-agent [0 0.5]
  :percent [0 0.5]
  :comission [-0.4 0.4]
  :advert-marker [0 0.6]
  :absurd-phone [0 0.8]
  :owner [0.2 -0.4]
  :middleman [-0.2 0]
  :distrub [-0.1 0]
  :person-type [0 0.5]
  }
 :tasks
 {:common-classify
  {:sched "45 /1 * * * * *"
   :opts {:city :smr
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }
  }
 }
