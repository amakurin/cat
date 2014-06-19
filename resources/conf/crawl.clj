  {:field-metas
   {:avito-list
    [{:id :url
      :selector [:a.item-link]
      :processors [{:func :get-attribute
                    :args {:attr :href}}]}
     {:id :src-date
      :selector [:div.info-date]
      :processors [{:func :as-text}
                   {:func :get-date-time}]}

     {:id :src-id
      :selector [:a.item-link]
      :processors [{:func :get-attribute
                    :args {:attr :href}}
                   {:func :find-pattern
                    :args {:pattern #"(\d+)$"}}
                   ]}
     {:id :target
      :processors [{:func :set-value
                    :args {:value :avito-item}}]}]

    :avito-item
    [
     {:id :description
      :selector [:div.description-wrapper]
      :processors [{:func :as-text}]
      }
     {:id :phone
      :selector [:div.description-wrapper]
      :processors [{:func :as-text}
                   {:func :get-phones}]
      }
     {:id :price
      :selector [:span.price-value]
      :processors [{:func :as-text}]
      }
     {:id :address
      :selector [:span.text-user-address]
      :processors [{:func :as-text}]
      }
     {:id :person-name
      :selector [:div.person-name]
      :processors [{:func :as-text}]
      }
     {:id :person-type
      :selector [:div.person-name :span.info-text]
      :processors [{:func :as-text}
;;                    {:func :set-value
;;                     :args {:value :owner}}
                   {:func :key-by-occur
                    :args {:pattern "агент"
                           :key-true :agent
                           :key-false :owner}}
                   ]
      }
     {:id :appartment-type
      :selector [:header :> [:span.semantic-text {:func :nth-of-type :args {:n 2}}]]
      :processors [{:func :as-text}]
      }
     {:id :total-area
      :selector [:header :> [:span.semantic-text {:func :nth-of-type :args {:n 3}}]]
      :processors [{:func :as-text}]
      }
     {:id :floor
      :selector [:header :> [:span.semantic-text {:func :nth-of-type :args {:n 4}}]]
      :processors [{:func :as-text}]
      }
     {:id :building-type
      :selector [:header :> [:span.semantic-text {:func :nth-of-type :args {:n 5}}]]
      :processors [{:func :as-text}]
      }
     {:id :imgs
      :selector [:li.photo-container :img.photo-self]
      :collection? true
      :processors [{:func :get-attribute
                    :args {:attr :src}}]
      }
     {:id :imgs
      :selector [:li.photo-container :span.img-pseudo]
      :collection? true
      :processors [{:func :get-attribute
                    :args {:attr :data-img-src}}]
      }
     {:id :lat
      :selector [:div#item-map]
      :processors [{:func :get-attribute
                    :args {:attr :data-coords-lat}}]
      }
     {:id :lng
      :selector [:div#item-map]
      :processors [{:func :get-attribute
                    :args {:attr :data-coords-lng}}]
      }
     {:id :phone
      :selector [:li.action-show-number :a.action-link]
      :processors [{:func :get-attribute
                    :args {:attr :href}}
                   {:func :str-format
                    :args {:fstr "%s?async"}}
                   {:func :direct-crawl-json
                    :args {:fields-meta [{:id :phone
                                          :selector [:phone1]}]}}
                   {:func :get-phone}
                   ]
      }
     ]

    :irr-list
    [{:id :url
      :selector [:td :div.add_title_wrap :a.add_title]
      :processors [{:func :get-attribute
                    :args {:attr :href}}]}
     {:id :src-id
      :processors [{:func :get-attribute
                    :args {:attr :data-item-id}}
                   ]}
     {:id :src-date
      :selector [:p.adv_data]
      :processors [{:func :as-text}
                   {:func :get-date-time}]
      }
     {:id :target
      :processors [{:func :set-value
                    :args {:value :irr-item}}]}
     {:id :price
      :selector [:div.add_cost]
      :processors [{:func :as-text}]
      }
     {:id :address
      :selector [:td :div.add_title_wrap :a.add_title]
      :processors [{:func :as-text}]
      }

     {:id :appartment-type
      :selector [:div.flat_prop :> [:div.flat_p {:func :nth-of-type :args {:n 1}}] :div.flat_p_txt]
      :processors [{:func :as-text}]
      }

     {:id :total-area
      :selector [:div.flat_prop :> [:div.flat_p {:func :nth-of-type :args {:n 2}}] :div.flat_p_txt]
      :processors [{:func :as-text}]
      }
     {:id :floor
      :selector [:div.flat_prop :> [:div.flat_p {:func :nth-of-type :args {:n 3}}] :div.flat_p_txt]
      :processors [{:func :as-text}]
      }
     ]

    :irr-item
    [
     {:id :phone
      :selector [:input#allphones]
      :processors [{:func :get-attribute
                    :args {:attr :value}}
                   {:func :decode-base64}
                   {:func :to-resource}
                   {:func :extract-field
                    :args {:flat true
                           :field-meta
                           {:id :phone
                            :selector [:img]
                            :collection? true
                            :processors [{:func :get-attribute
                                          :args {:attr :src}}
                                         {:func :recognize-image-text}
                                         {:func :get-phones-spec}]}}}
                   {:func :apply-concat}]}
     {:id :phone
      :selector [:div.content_left :p.text]
      :processors [{:func :as-text}
                   {:func :get-phones}]
      }

     {:id :description
      :selector [:div.content_left :p.text]
      :processors [{:func :as-text}]
      }
     {:id :header
      :selector [:table.title-wrap :h1]
      :processors [{:func :as-text}]
      }
     {:id :imgs
      :selector [:div.slide :a.nyroModal :img]
      :collection? true
      :processors [{:func :get-attribute
                    :args {:attr :src}}]
      }
     {:id :person-name
      :selector [[:ul.form_info {:func :nth-of-type :args {:n 1}}] :li [:p {:func :nth-of-type :args {:n 2}}]]
      :processors [{:func :as-text}]
      }
     {:id :person-type
      :selector [:div.hide :div.additional-features]
      :processors [{:func :as-text}
                   {:func :key-by-occur
                    :args {:pattern "без комиссии"
                           :key-true :owner
                           :key-false :agent}}]
      }
     {:id :lat
      :selector [:div#map_container :input#geo_x]
      :processors [{:func :get-attribute
                    :args {:attr :value}}]
      }
     {:id :lng
      :selector [:div#map_container :input#geo_y]
      :processors [{:func :get-attribute
                    :args {:attr :value}}]
      }
     ]
    }
   :targets
   {
    :avito-list
    [{:func :direct-crawl-list
      :args {:url [:data :url]
             :url-param [:data :url-param]
             :selector [:article.b-item]
             :fields-meta [:conf :field-metas :avito-list]}}
     ;{:func :scrape-items
     ; :args {:n 10}}
     ]

    :avito-item
    [{:func :direct-crawl-item
      :args {:url [:data :url]
             :fields-meta [:conf :field-metas :avito-item]}}
     ]

    :irr-list
    [{:func :direct-crawl-list
      :args {:url [:data :url]
             :url-param [:data :url-param]
             :selector [[:div.add_list :.add_type4]]
             :fields-meta [:conf :field-metas :irr-list]}}
     ;{:func :scrape-items
     ; :args {:n 5}}
     ]

    :irr-item
    [{:func :direct-crawl-item
      :args {:url [:data :url]
             :fields-meta [:conf :field-metas :irr-item]}}]

    :cian-list
    [{:func :direct-crawl-list
      :args {:url "http://www.cian.ru/cat.php?deal_type=1&obl_id=1&p=%s"
             :url-param #{1}
             :selector [:td.cat :a.n]
             :fields-meta [{:id :phone
                            :processors [{:func :as-text}
                                         {:func :get-phone}]}
                           ]}}]
    :dmir-list
    [{:func :direct-crawl-list
      :args {:url [:data :url]
             :url-param [:data :url-param]
             :selector [:li.item ]
             :fields-meta [{:id :url
                            :selector [:ul.mainlist :li :> :a]
                            :processors [{:func :get-attribute
                                          :args {:attr :href}}]
                            }
                           {:id :src-id
                            :selector [:ul.mainlist :li :> :a]
                            :processors [{:func :get-attribute
                                          :args {:attr :href}}
                                         {:func :find-pattern
                                          :args {:pattern #"(\d{5,})/?$"}}
                                         ]}
                           {:id :target
                            :processors [{:func :set-value
                                          :args {:value :dmir-item}}]}]
             }}
     ;{:func :scrape-items
     ; :args {:n 10}}
     ]

    :dmir-item
    [{:func :direct-crawl-item
      :args {:url [:data :url]
             :fields-meta [{:id :phone
                            :selector [:div.phone]
                            :processors [{:func :as-text}
                                         {:func :get-phones}
                                         ]}
                           ]}}]
    }
   :tasks
   {
    :dmir-samara-agents
    {:sched "20 /1 * * * * *"
     :opts {:target :dmir-list
            :data
            {:url "http://realty.dmir.ru/samara/rent/arenda-kvartir-v-samare/?objsrc=4205,4203&page=%s"
             :url-param #{1}}
            :merge-data {:city :smr}
            :processing
            {:steps [{:storage-entity :agents
                      :store-option :so-insert-or-update
                      :insert-or-update-key [:phone]
                      :split-by [:phone]
                      :persistent-fields-except [:src-id]
                      }]
             :pause [1 1]
             }
            }}

    :avito-samara-room
    {:sched "1 /5 * * * * *"
     :opts {:target :avito-list
            :data
            {:url "https://m.avito.ru/samara/komnaty/sdam/na_dlitelnyy_srok?page=%s"
             :url-param #{1 2}}
            :merge-data {:city :smr :appartment-type 8}
            :processing
            {:steps [
;;                      {:storage-entity :agents
;;                       :store-option :so-insert-or-update
;;                       :insert-or-update-key [:phone]
;;                       :split-by [:phone]
;;                       :filter-by {:person-type :agent}
;;                       :persistent-fields [:target :phone :url :city]
;;                       }
                     {:storage-entity :ads
;;                       :filter-by {:person-type :owner}
                      :as-edn-to :raw-edn
                      :persistent-fields [:src-id :target :city :raw-edn :url ]
                      }]
             :pause [2 10]
             }
            }}
    :avito-samara-kv
    {:sched "30 /3 * * * * *"
     :opts {:target :avito-list
            :data
            {:url "https://m.avito.ru/samara/kvartiry/sdam/na_dlitelnyy_srok?page=%s"
             :url-param #{1 2}}
            :merge-data {:city :smr}
            :processing
            {:steps [
;;                      {:storage-entity :agents
;;                       :store-option :so-insert-or-update
;;                       :insert-or-update-key [:phone]
;;                       :split-by [:phone]
;;                       :filter-by {:person-type :agent}
;;                       :persistent-fields [:target :phone :url :city]
;;                       }
                     {:storage-entity :ads
;;                       :filter-by {:person-type :owner}
                      :as-edn-to :raw-edn
                      :persistent-fields [:src-id :target :city :raw-edn :url ]
                      }]
             :pause [2 10]
             }
            }}
    :irr-samara-room
    {:sched "10 /1 * * * * *"
     :opts {:target :irr-list
            :data
            {:url "http://samara.irr.ru/real-estate/rooms-rent/search/rent_period=3674653711/list=list/page%s/"
             :url-param #{1}}
            :merge-data {:city :smr :appartment-type 8}
            :processing
            {:steps [{:storage-entity :agents
                      :store-option :so-insert-or-update
                      :insert-or-update-key [:phone]
                      :split-by [:phone]
                      :filter-by {:person-type :agent}
                      :persistent-fields [:target :phone :url :city]
                      }
                     {:storage-entity :ads
                      :filter-by {:person-type :owner}
                      :as-edn-to :raw-edn
                      :persistent-fields [:src-id :target :city :raw-edn :url ]
                      }]
             ;:pause [1 1]
             }
            }}
    :irr-samara-kv
    {:sched "40 /1 * * * * *"
     :opts {:target :irr-list
            :data
            {:url "http://samara.irr.ru/real-estate/rent/search/rent_period=3674653711/list=list/page%s/"
             :url-param #{1}}
            :merge-data {:city :smr}
            :processing
            {:steps [{:storage-entity :agents
                      :store-option :so-insert-or-update
                      :insert-or-update-key [:phone]
                      :split-by [:phone]
                      :filter-by {:person-type :agent}
                      :persistent-fields [:target :phone :url :city]
                      }
                     {:storage-entity :ads
                      :filter-by {:person-type :owner}
                      :as-edn-to :raw-edn
                      :persistent-fields [:src-id :target :city :raw-edn :url ]
                      }]
             ;:pause [1 1]
             }
            }}
    }
   }
