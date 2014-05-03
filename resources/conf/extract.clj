{:terms
 [
  {:term :area
   :formatters {:func :area}
   :pattern
   #"(?iux)
   \b
   (?: (?: s\s?-?\s?|пл(?: ощадь?[ию]?)?)?)?
   (?: ([1-9]\d{0,2}(?: [\.,]\d)?)\s?[и\\/\-\+\s]\s?)?
   (?: ([1-9]\d{0,2}(?: [\.,]\d)?)\s?[и\\/\-\+\s]\s?)?
   ([1-9]\d{0,2}(?: [\.,]\d)?)\s?
   (?: (?: кв\.?|квадрат\.?|квадратны[йх])?\s?м(?: [²2\.]|(?: етр[ао]?в?)?))
   (?![\p{IsCyrillic}\w])"
   }
  {:term :money
   :formatters {:func :money}
   :pattern
   #"(?iux)
   \b
   ([1-9]\d{0,2}(?: [\.,]\d)?)\s?
   (?: (?: \.?(\d00))|(тыс[иея]ч[аи]?|тыс\.?|ты|т\.?))
   \s?(рублей|рубл\.?|руб\.?|р\.?)?
   (?![\p{IsCyrillic}\w])"
   }
  {:term :phone
   :formatters {:func :phone}
   :pattern
   #"(?iux)
   (?<![\d])
   (?: \b(?: т|тел)[\.\:]?\s?)?
   (?: \+\s?)?
   (?: [78][\.\-\s]?)?
   (?: \(\d{3}\)\s?|(?: \d\s?[\.\-]?\s?){3})
   (?: (?: \d\s?[\.\-\s]?\s?){6,7}|(?: \d\s?[\.\-\s]?\s?){4})
   \b(?![\d])"
   }
  {:term :absurd-phone
   :pattern
   #"(?iux)
   (?<= ([89][\doOоОзЗбБЧч_[^\p{IsCyrillic}\w]]{5,20}))
   [oOоОзЗбБЧч_[^\p{IsCyrillic}\d\-\w\s\(\)\:\.]]
   (?=([\doOоОзЗбБЧч_[^\p{IsCyrillic}\w]]{5,20}))"
   }
  {:term :advert-marker
   :pattern
   #"(?iux)
   \b
   хостел|газета|успейте|помогите
   |расc?четный\s?час|сайте?|вступай(?:те)|участвуй(?:те)|участвуй(?:те)
   |большая\s?база|база\s?жилья|(?:sms|смс)\s?\-?\s?расс?ыл(?:ка|ок)
   \b"
   }
  {:term :floor
   :formatters {:func :floor}
   :pattern
   #"(?iux)
   \b
   (\d{1,2})
   (?: \s?-?\s?(?: [ыои]?й|[ое]?м))?\s?
   (?:
   (?: этаж[е]?\s?[\\/]? |эт\.?\s?[\\/]? |[\\/])\s?
   (\d{1,2})\s?-?\s?(?: х|ти|ми)?\s?(?: этажн(?: ого|ом|\.)?|эт\.?)\s?(?: дом[ае]?)?|этаж|эт\.?)\b"
   }
  {:term :appartment-type
   :formatters {:func :dictionary}
   :pattern
   #"(?iux)
   \b
   ( (?:квартир[ау]) ?\s?\-?\s?студи[яюи]) # :studio
   |(?:(?:
   ( (?:1\s?\-?\s?о?|однушку|одно?)\s?)    # :appartment1
   |( (?:2\s?\-?\s?у?х?|двушку|двух?)\s?)  # :appartment2
   |( (?:3\s?\-?\s?[её]?х?|тр[её]шку|тр[её]х?)\s?) # :appartment3
   |( (?:4\s?\-?\s?р?[её]?х?|четыр[её]х?)\s?) # :appartment4
   |( (?:5\s?\-?\s?(?:ти)?|п[ея]ти)\s?)       # :appartment5
   |( (?:6\s?\-?\s?(?:ти)?|ш[еиы]сти)\s?)     # :appartment6
   |( (?:7\s?\-?\s?(?:ми)?|с[еи]ми)\s?)       # :appartment7
   )
   (?:
   (?: комн?(?:\.|атн?(?: ую|ая|\.)?)?)\s?(?:квартирк?[уа]|квартир?\.?|кварт?\.?|кв?\.?)?
   |(?: к\.?\s?(?:квартирк?[уа]|квартир?\.?|кварт?\.?|кв?\.?))))
   |(комнат[ау])
   \b"
   }
  {:term :building-type
   :formatters {:func :dictionary}
   :pattern
   #"(?iux)
   \b
   (?:
   ( к[ие]рп\.?(?: ич)?\.?) # :brick
   |( п[ао]нел\.?ь?) # :panel
   |( бло[кч]\.?) # :block
   |( м[оа]н[оа]лит\.?) # :monolit
   |( д[ие]р[ие]в\.?(?:ян)?) # :wood
   )
   н?\.?(?: ого|ый|ом)?
   \b"
   }
  {:term :person-name
   :formatters {:func :just-match}
   :pattern #"(?x) ^\p{Lu}\p{IsCyrillic}+\b"
   }
  {:term :total
   :pattern #"(?iux)\b(пл(ощадью?|\.)?)?\s?общ(ая|ей|\.)?\s?(пл(ощадью?|\.)?)?\b"
   }
  {:term :living
   :pattern #"(?iux)\b(пл(ощадью?|\.)?)?\s?жил(ая|ой|\.)?\s?(пл(ощадью?|\.)?)?\b"
   }
  {:term :kitchen
   :pattern #"(?iux)\b(пл(ощадью?|\.)?)?\s?кухн[яи]\b"
   }
  {:term :percent
   :pattern #"(?iux)\b\d{1,3}?\s?\%"
   }
  {:term :by-fact
   :pattern
   #"(?iux)
   \b(?: по\s?факту\s?(?: [вз]а?селения|заключения|подписания|аренды|снятия|найма)?|(:? при\s?[вз]а?селении))\b"
   }
  {:term :comission
   :pattern #"(?iux)\b(комм?исс?и[яю]|комис\.?)\b"
   }
  {:term :payment
   :pattern #"(?iux)\b(о?плата|оплачиваются)\b"
   }
  {:term :to-agent
   :pattern
   #"(?iux)\b((услуги\s)?(р[ие][еэ]лторски[ехм]|агент?ски[ехм]|aгент[ау]|р[ие][еэ]лтор[ау]|агент?ств[ау])(\sуслуги)?)\b"
   }
  {:term :utilities
   :pattern #"(?iux)\b((к\.?\s?[\\/\-]?\s?у\.?)|(комм?ун\.?|комм?\.?\s?(унальны[ей])?)\s?(пл\.?(ат\.?)?(еж)?и?|усл?\.?(уги)?)|комм?уналка|кварт?плат[уа])\b"
   }
  {:term :toilet
   :pattern #"(?iux)\b(с\.?\s?[\\/\-]?\s?у\.?|сан[\\/\-\s]?узел|туал\.?(ет)?)\b"
   }
  {:term :layout
   :formatters {:func :dictionary}
   :pattern
   #"(?iux)
   \b((?: раз|от)д\.? (?: ел\.?)?[ьеё]?н?н?\.?(?: ый)?)
   |(совм\.?(?: ещ)? (?: [её]н)?\.?(?: н?ый)?|совмест?ный)\b"
   }
  {:term :deposit
   :pattern
   #"(?iux)\b(?:страховой|страх\.?|авансовый)?\s?(?:депозит|взнос|залог)
   |посл\.?(?:ед\.?)?(?:ни[йе])?\s?мес\.?(?:[яе]ц)?ы?\b"
   }
  {:term :counters
   :pattern #"(?iux)\b((по)?\s?сч[её]тчик([иу]|ам)?)\b"
   }
  {:term :water
   :pattern #"(?iux)\bвода\b"
   }
  {:term :electricity
   :pattern #"(?iux)\b(свет|электричество|электроэнергия)\b"
   }
  {:term :gas
   :pattern #"(?iux)\bгаз\b"
   }
  {:term :internet
   :pattern #"(?iux)\b((интернет\s?\(?\s?(вай\s?\-?\s?фай|wi\s?\-?\s?fi)\s?\)?)|интернет|вай\s?\-?\s?фай|wi\s?\-?\s?fi)\b"
   }
  {:term :furniture
   :pattern #"(?iux)\b(мебел([ьи]|ью)|мебе?лированн?(?: ая|ую|а)?|диван|кровать)\b"
   }
  {:term :electronics
   :pattern #"(?iux)\b(бытов(ая|ой)\s)?(техник([аи]|ой))\b"
   }
  {:term :washer
   :pattern #"(?iux)\b(сма|стир\.?(ал\.?)?(ьн[аыуо][йяею])?\s?(машин)?к?[аыий]([\s\-\\/]автомат)?|машинк?а([\s\-\\/]автомат))\b"
   }
  {:term :frige
   :pattern #"(?iux)\b(холодильник(?: а|ом)?|х\-к|хол\.|холод\.?)\b"
   }
  {:term :tv
   :pattern #"(?iux)\b(теле(визор[аы]?|ид[ие]ние|к)|(дом\.?(ашн\.?)?(ий)?)?\s?кинотеатр|(?: плазменная|full?\s?hd|фулл?\s?эйч\s?ди?)\s?панель|тв|tv)\b"
   }
  {:term :conditioner
   :pattern #"(?iux)\b(кондиционер(?:ом)?|сплит[\s\-]?система)\b"
   }
  {:term :balcony
   :pattern #"(?iux)\bб[ао]лкон[аыо]?м?\b"
   }
  {:term :loggia
   :pattern #"(?iux)\bлоджи[яие]й?\b"
   }
  {:term :bow-window
   :pattern #"(?iux)\bэркер(?:ом)?\b"
   }
  {:term :concierge
   :pattern #"(?iux)\bконсьерж(?:ом|ем|ка|кой)??\b"
   }
  {:term :security
   :pattern #"(?iux)\b(охрана|охраняем(ое|ый|ая)|видеонаблюдение|сигнализация)\b"
   }
  {:term :intercom
   :pattern #"(?iux)\b(домофон|видеодомофон|интерком)\b"
   }
  {:term :parking
   :pattern #"(?iux)\b(паркинг|парковка|(авто[\-\s]?)?стоянк[иа]|гараж)\b"
   }
  {:term :kids
   :pattern #"(?iux)\b(дет(и|ей|ьми|ски[йех]|ская|ской|ки|ками)|ребен(ок|ком)|школ[аы]?)\b"
   }
  {:term :pets
   :pattern #"(?iux)\bживотны(е|ми|х)\b"
   }
  {:term :addiction
   :pattern #"(?iux)(\bв\.?[\\/]?п\.?|вр(ед)?н?\.?(ых)?\s?прив(ыч)?(ек)?)\b"
   }
  {:term :only-russo
   :pattern
   #"(?iux)
   \b
   русс?к(?: ой|им|их|ому)|славянск(?: ой|им|ому|их)|сл[ао]в[яе]н(?: ам|е|ин)
   \b"
   }
  {:term :nationality
   :pattern
   #"(?iux)
   \b
   национальност(?: ь|и|ей)
   \b"
   }
  {:term :applicant
   :pattern
   #"(?iux)
   \bжелающи[хмей]\b"
   }
  {:term :variants
   :pattern
   #"(?iux)\bварианты?\b"
   }
  {:term :owner
   :pattern
   #"(?iux)\bот\s?собственника|[cс]обственник(?:ом)?\b"
   }
  {:term :middleman
   :pattern
   #"(?iux)\bпосредник(?: ов|а)?\b"
   }
  {:term :distrub
   :pattern
   #"(?iux)\bбеспоко(?: ить|йте)?\b"
   }
  {:term :without
   :pattern #"(?iux)\bбез|не|нет|кроме\s"
   }
  {:term :full
   :pattern #"(?iux)\bвс[еёя]\s"
   }
  {:term :any
   :pattern
   #"(?iux)\bлюб(?:ые|ой)|все[хм]\b"
   }
  ]

 :input-rules
 {:as-is
  #{:url :imgs :lat :lng :city :address :src-date :person-type}
  :exclude
  #{:url :src-id :target}
  :include-origin
  #{:description}
  }

 :grammar
 {:fact
  [:conditioner :furniture :electronics :washer :frige
   :tv :balcony :addiction :security :parking :internet
   :concierge :loggia :intercom :pets :kids
   :owner :middleman :distrub :absurd-phone

   :only-russo :nationality :variants :applicant

   :payment :comission :to-agent :by-fact :percent :advert-marker

   :electricity :water :counters :gas :utilities :deposit
   [:semicolon :fact]
   ]
  :operator
  [:without :plus :full :any [:semicolon :operator]]
  :bi-operator
  [:and [:semicolon :bi-operator]]
  :value
  [:floor :area :money :layout :phone :appartment-type :building-type :person-name [:semicolon :value]]
  :object
  [:kitchen :total :living :toilet [:semicolon :object]]
  :semicolon
  [:un]
  :terminator [:terminate [:semicolon :semicolon][:terminator :semicolon]]
  :formula [[:fact] [:value]
            [:object :value][:operator :fact]
            [:operator :formula]
            ;[:formula :operator :terminator]
            ;[:formula :operator :semicolon]
            [:formula :bi-operator :formula]]
  }

 :output-rules
 {[:operator :without :fact] [:not]
  [:operator :plus :fact :utilities] [:switch-to :plus-utilities :as-is]
  [:operator :plus :fact :electricity] [:switch-to :plus-electricity :as-is]
  [:operator :plus :fact :gas] [:switch-to :plus-gas :as-is]
  [:operator :plus :fact :water] [:switch-to :plus-water :as-is]
  [:fact :utilities][nil]
  [:fact :electricity][nil]
  [:fact :gas][nil]
  [:fact :water][nil]

  [:operator :without] []
  [:operator :without :value] [:rule-for-last]
  [:operator :full] []
  [:operator :full :value] [:rule-for-last]
  [:operator :any] []
  [:operator :any :value] [:rule-for-last]
  [:operator :plus] []
  [:operator :plus :value] [:rule-for-last]

  [:operator :full :fact :furniture] []
  [:operator :full :fact :electronics] [:split-to [:tv :frige :washer]]
  [:operator :any :fact :variants] [:switch-to :only-russo :as-not]
  [:operator :without :fact :only-russo] []
  [:operator :any :fact :nationality] [:switch-to :only-russo :as-not]
  [:operator :any :fact :applicant] [:switch-to :only-russo :as-not]
  [:fact :variants][nil]
  [:fact :applicant][nil]

  [:fact :electronics] [:split-to [:tv :frige]]
  [:value :area] [:split-to [:total-area :living-area :kitchen-area]
                  :prob-by-size {1 0.63 2 0.45 3 0.87}]
  [:value :floor] [:split-to [:floor :floors] :prob-by-size {1 0.5 2 0.9}]
  [:object :total :value :area] [:switch-to :total-area :as-first :prob-by-size 1]
  [:object :living :value :area] [:switch-to :living-area :as-first :prob-by-size 1]
  [:object :kitchen :value :area] [:switch-to :kitchen-area :as-first :prob-by-size 1]
  [:object :toilet :value :layout] [:switch-to :toilet :as-is]
  [:value :money] [:switch-to :price :as-is]
  [:value :layout] [nil]
  }
 :clean-rules
 {:to-collection [:phone]
  :boolean-and
  [:conditioner :furniture :electronics :washer :frige
   :tv :balcony :addiction :security :parking :internet
   :concierge :loggia :intercom :pets :kids :only-russo :nationality
   :owner :middleman :distrub :absurd-phone

   :payment :comission :to-agent :by-fact :percent

   :electricity :water :counters :gas :utilities :deposit
   ]
  :full-reliability [:person-name :appartment-type]
  :default :max-likelihood
  :low-reliability-bound 0
  }
 :tasks
 {:common-extraction
  {:sched "1 /1 * * * * *"
   :opts {:storage-entity :ads}
   }
  }
 }
