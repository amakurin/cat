{:tasks
 {:short-check
  {:sched "1 1 /1 * * * *"
   :opts {:timeout-ms 100
          :query-hours 2
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }

  :long-check
  {:sched "1 1 1 * * * *"
   :opts {:timeout-ms 300
          :query-hours 336
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }
  }
 }
