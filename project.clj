(defproject caterpillar "0.1.0"
  :description "Simple crawling service"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.9.1" :exclusions [crouton]]
                 [clj-http-fake "0.7.8"]
                 [clj-time "0.6.0"]
                 [im.chit/cronj "1.0.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [enlive "1.1.5"]
                 [com.taoensso/timbre "3.1.6"]
                 [commons-codec "1.7"]
                 [opencv "2.4.8"]
                 [org.clojure/tools.reader "0.8.4"]
                 [org.clojars.r0man/environ "0.4.1-SNAPSHOT"]
                 [mysql/mysql-connector-java "5.1.28"]
                 [korma "0.3.0-RC6"]
                 [clj-sql-up "0.3.1"]
                 ]
  :plugins [[lein-localrepo "0.5.3"]
            [lein-environ "0.4.0"]
            [clj-sql-up "0.3.1"]]
  :clj-sql-up {:deps [[mysql/mysql-connector-java "5.1.28"]]}
  :jvm-opts [~(str "-Djava.library.path=opencv/x64/")]
  :env {:ocr-samples-path "resources/ocr/samples/"
        :ocr-file "resources/ocr/ocr.edn"
        :mariposa-dict-names "dict-names.clj"
        :crawl-mages "resources/img/"
        :crawl-mages-min-size 100
        :thumb-size {:width 100 :height 90}
        :subsystems [{:id :caterpillar
                      :sys-name "Caterpillar"
                      :sys-ns caterpillar.core
                      :config-file "conf/crawl.clj"}

                     {:id :mariposa
                      :sys-name "Mariposa"
                      :sys-ns caterpillar.mariposa
                      :config-file "conf/extract.clj"}

                     {:id :formiga
                      :sys-name "Formiga"
                      :sys-ns caterpillar.formiga
                      :config-file "conf/classify.clj"}

                     {:id :publisher
                      :sys-name "Publisher"
                      :sys-ns caterpillar.publisher
                      :config-file "conf/publish.clj"}

                     ]
        }
  :main caterpillar.system)
