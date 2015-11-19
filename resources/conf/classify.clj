{:query-days 14
 :rate-bounds {:owner [-999999 0.5]
               :suspect [0.5 0.7]
               :agent [0.7 999999]}
 :appearance-bounds {:owner [-999999 2]
                     :suspect [2 3]
                     :agent [3 999999]}
 :multi-phone-bounds {0 [-999 1]
                      0.3 [1 2]
                      0.5 [3 9999]}
 :img-count-bounds {0.3 [-999 1]
                    0.35 [1 2]
                    0.45 [2 3]
                    0.34 [3 4]
                    0 [4 999]}
 :price-bounds
 {:common
  {0.3 [-999 0]
   0.55 [0 6000]
   0.45 [6000 8000]
   0.2 [8000 10000]
   0.15 [10000 11000]
   0 [11000 9999999]}
  1
  {0.31 [-999 0]
   1.5 [0 10000]
   0.5 [10000 13000]
   0.3 [13000 17000]
   0.2 [17000 20000]
   0.0 [20000 22000]
   0.1 [22000 25000]
   0.4 [25000 9999999]}
  2
  {0.29 [-999 0]
   0.51 [0 14000]
   0.3 [14000 17000]
   0.0 [17000 30000]
   0.2 [30000 40000]
   0.25 [40000 50000]
   0.5 [50000 9999999]}
  8 ;комната
  {0.31 [-999 0]
   1.51 [0 5000]
   0.6 [5000 7000]
   0.25 [7000 10000]
   0.0 [10000 11000]
   0.5 [11000 14000]
   1.4 [14000 9999999]}
  }
 :rate-weights
 {:by-fact [0 0.9]
  :to-agent [0 0.5]
  :percent [0 0.5]
  :comission [-0.3 0.4]
  :advert-marker [0 0.6]
  :absurd-phone [0 0.8]
  :owner [0.2 -0.3]
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
