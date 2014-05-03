{:img-crops
 {#"avito" 40 #"irr" 47 }
 :seo-rent-strings ["Снять" "Сдам"]
 :persistent-fields
 [:id
  :seoid
  :created
  :src-date
  :city
  :appartment-type
  :floor
  :floors
  :price
  :person-name
  :phone
  :address
  :district
  :metro
  :distance
  :lat
  :lng
  :total-area
  :living-area
  :kitchen-area
  :toilet
  :building-type
  :description
  :imgs

  :deposit
  :counters
  :plus-utilities
  :plus-electricity
  :plus-water
  :plus-gas

  :balcony
  :loggia
  :bow-window

  :furniture
  :internet
  :tv
  :frige
  :washer
  :conditioner

  :parking

  :intercom
  :security
  :concierge

  :only-russo
  :kids
  :pets
  :addiction]

 :tasks
 {:common-publish
  {:sched "50 /1 * * * * *"
   :opts {:storage-entity-src :ads
          :storage-entity-tgt :pub}
   }
  }
 }
