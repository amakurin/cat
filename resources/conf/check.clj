{:tasks
 {:short-check
  {:sched "1 2 /1 * * * *"
   :opts {:timeout-ms 1000
          :query-hours 2
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }

  :long-check
  {:sched "1 31 1 * * * *"
   :opts {:timeout-ms 1500
          :query-hours 168
          :storage-entity-src :ads
          :storage-entity-tgt :pub}
   }
  }
 }
