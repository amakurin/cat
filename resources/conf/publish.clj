{:max-appearances
 {:cnt 1 :per-days 6}
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
  }
 :tasks
 {:common-extraction
  {:sched "30 /1 * * * * *"
   :opts {:city :smr
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }
  }
 }
