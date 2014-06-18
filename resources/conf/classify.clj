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
