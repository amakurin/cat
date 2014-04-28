(ns caterpillar.ocr
  (:require
   [caterpillar.tools :as tools]
   [clojure.tools.reader.edn :as edn]
   [environ.core :refer [env]]
   )
  (:import
   org.opencv.core.Core
   org.opencv.core.CvType
   org.opencv.core.Mat
   org.opencv.core.MatOfByte
   org.opencv.core.MatOfFloat
   org.opencv.core.Point
   org.opencv.core.Rect
   org.opencv.core.Scalar
   org.opencv.core.Size
   org.opencv.highgui.Highgui
   org.opencv.ml.CvKNearest
   org.opencv.utils.Converters
   org.opencv.imgproc.Imgproc
   java.awt.image.BufferedImage
   java.awt.RenderingHints
   java.awt.Color
   javax.imageio.ImageIO
   java.io.File
   java.io.ByteArrayInputStream
   java.io.InputStream
   java.net.URL
   java.util.ArrayList
   ))

(def sys (atom {:knn nil}))

(defn path-for-src [src] (str (env :ocr-samples-path) (name src) "/"))

(defn path-for-src-file [src local-file] (str (path-for-src src) local-file))

(defn ocr-data-file [] (env :ocr-file))

(defn buf-to-mat [buf type]
  (let [itype (if (= type :gray)  CvType/CV_8UC1  CvType/CV_8UC3)
        img-b  (-> buf (.getRaster) (.getDataBuffer) (.getData))
        mat (Mat. (.getHeight buf) (.getWidth buf) itype)]
    (.put mat 0 0 img-b)
    mat))

(defn convert-buffer-image-to-mat [img & [{:keys [scale type] :or {scale 3 type :rgb}}]]
  (let [itype (if (= type :gray) BufferedImage/TYPE_BYTE_GRAY BufferedImage/TYPE_3BYTE_BGR)
        w (.getWidth img)
        h (.getHeight img)
        nw (* w scale)
        nh (* h scale)
        new-frame  (BufferedImage. nw nh BufferedImage/TYPE_3BYTE_BGR)
        g (.getGraphics new-frame)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_INTERPOLATION  RenderingHints/VALUE_INTERPOLATION_BILINEAR)
      (.drawImage img 0 0 nw nh 0 0 w h nil)
      (.dispose))
    (buf-to-mat new-frame type)))

(defn convert-mat-to-buffer-image [mat]
  (let [new-mat (MatOfByte.)]
    (Highgui/imencode ".png" mat new-mat)
    (ImageIO/read (ByteArrayInputStream. (.toArray new-mat)))))

(defn read-image [src]
  (let [image (ImageIO/read src)
        result (BufferedImage. (.getWidth image nil)(.getHeight image nil) BufferedImage/TYPE_INT_RGB)
        g (.createGraphics result)]
    (.drawImage g image 0 0 (.getWidth result) (.getHeight result) Color/WHITE nil)
    result))

(defn read-image-with-crop [src bottom-crop min-size]
  (let [image (ImageIO/read src)
        width (.getWidth image nil)
        height (.getHeight image nil)]
    (if (or (< width min-size)(< height min-size))
      nil
      (let [image (ImageIO/read src)
            result (BufferedImage. width(- height bottom-crop) BufferedImage/TYPE_INT_RGB)
            g (.createGraphics result)]
        (.drawImage g (.getSubimage image 0 0 (.getWidth result) (.getHeight result))
                    0 0 (.getWidth result) (.getHeight result) Color/WHITE nil)
        result))))

(defn buf-to-file [{:keys [path format buf]}]
  (let [f (File. path)]
    (ImageIO/write buf (name format) f)))

(defn mat-to-file [mat path]
  (buf-to-file {:path path
                :format :png
                :buf (convert-mat-to-buffer-image mat)}))

(defn url-to-file [url path]
   (buf-to-file {:buf (read-image (URL. url))
                 :format :png
                 :path path}))

(defn to-gray-scale-mat [im]
  (let [mat (convert-buffer-image-to-mat im)
        gray (Mat.)]
    (Imgproc/cvtColor mat gray Imgproc/COLOR_BGR2GRAY)
    gray))

(defn preprocess [gray-scale-mat]
  (let [blur (Mat.)
        thresh (Mat.)]
    (Imgproc/GaussianBlur gray-scale-mat blur (Size. 3 3) 2)
    (Imgproc/adaptiveThreshold blur;gray-scale-mat ;
                               thresh 255
                               Imgproc/ADAPTIVE_THRESH_MEAN_C
                               Imgproc/THRESH_BINARY_INV 11 8)
    thresh))

(defn get-contours-rects [im]
  (let [mat (.clone im)
        contours (java.util.ArrayList.)
        hierarchy (Mat.)]
    (Imgproc/findContours mat contours hierarchy
                          Imgproc/RETR_EXTERNAL Imgproc/CHAIN_APPROX_SIMPLE)
    (let [rects (map #(Imgproc/boundingRect %)contours)
          bound (*(apply max (map #(.height %) rects))0.7)]
      (->> rects
           (filter #(< bound (.height %)))
           (sort #(< (.x %1)(.x %2)))))))

(defn resize-mat [mat width height]
  (let [resized (Mat.)]
    (Imgproc/resize mat resized (Size. width height))
    resized))

(defn get-samples-mats [im rects]
  (let [samples (atom [])]
    (doseq [rect rects]
      (let [sub (.submat im rect)
            resized (Mat.)]
        (swap! samples conj (resize-mat sub 10 10))))
    @samples))

(defn hconcat [mats]
  (let [big (Mat.)]
    (Core/hconcat mats big)
    big))

(defn vconcat [mats]
  (let [big (Mat.)]
    (Core/vconcat mats big)
    big))

(defn to-float-row [sample]
  (let [converted (Mat.)]
        (.convertTo (.reshape sample 1 1) converted CvType/CV_32FC1)
    converted))

(defn samples-matrix [samples]
    (vconcat (map to-float-row samples)))

(defn process-sample [sample & [{src-field :src-field :or {src-field :local-file}}]]
  (let [src-types {:local-file #(File. (path-for-src-file (:src %) (:local-file %)))
                   :src-url #(URL. (:src-url %))}
        im (-> sample ((src-field src-types)) read-image to-gray-scale-mat preprocess)
        rects (get-contours-rects im)]
    (get-samples-mats im rects)))

(defn process-samples [samples handler & [{src-field :src-field :or {src-field :local-file}}]]
  (->> samples
       (map #(process-sample % {:src-field src-field}))
       (reduce concat)
       handler))

(defn process-responses [responses-str]
  (let [responses (Mat. 1 (count responses-str) CvType/CV_8UC1)
        mresm (Mat.)]
    (.put responses 0 0  (->> responses-str (map byte) byte-array bytes))
    (.convertTo responses mresm CvType/CV_32FC1)
    mresm))

(defn load-classifier-data []
  (edn/read-string (slurp (ocr-data-file))))

(defn prepare-train-set [train-set]
  (let [samples (->> train-set (map val) (reduce concat))]
    {:samples (process-samples samples samples-matrix)
     :responses (process-responses (apply str (map :response samples)))}))

(defn create-classifier []
  (let [{:keys [samples responses]} (prepare-train-set(load-classifier-data))
        knn (CvKNearest.)]
    (.train knn samples responses)
    (swap! sys assoc :knn knn)))

(defn convert-set [training-set sample-converter]
  (->> training-set
       (map (fn [[src tset]]
              [src (->> tset (map (sample-converter src)) vec)]))
       (into {})))

(defn converter-assoc [src]
  (fn [{:keys [src-url response local-file] :as sample}]
    (assoc
      (if local-file sample
        (merge sample {:local-file (tools/gen-file-name :png) :new? true}))
      :src src)))

(defn converter-dissoc [src]
  (fn [sample]
    (dissoc sample :new?)))

(defn update-classifier [training-set]
  (let [pre-set (convert-set training-set converter-assoc)
        post-set (convert-set pre-set converter-dissoc)]
    (doseq [path (map #(path-for-src (key %)) training-set)]
      (when-not (.exists (File. path)) (.mkdirs (File. path))))
    (doseq [{:keys [src src-url local-file]} (->> pre-set (map val) (reduce concat) (filter :new?))]
      (url-to-file src-url (path-for-src-file src local-file)))
    (spit (ocr-data-file) (pr-str post-set))
    (create-classifier)))

(defn init-sys []
  (clojure.lang.RT/loadLibrary Core/NATIVE_LIBRARY_NAME)
  (create-classifier)
  (swap! sys assoc :initialized? true))

(defn init-when-not[] (when-not (:initialized? @sys) (init-sys)))

(defn recognize [sample]
  (init-when-not)
  (let [msam (process-samples [sample] samples-matrix {:src-field :src-url})
        results (Mat.)
        nrs (Mat.)
        dists (Mat.)
        result (ArrayList.)]
    (.find_nearest (:knn @sys) msam 1 results nrs dists)
    (Converters/Mat_to_vector_float results result)
    (apply str (map #(-> % int char) result))))




;;;; Fun utils

;(def irr-unknown "http://monolith1.izrukvruki.ru/phones/ru/311760779/phone/d5f36f8aa4150d2dd6958b77f7b5af07.jpg")

;(def mail-unknown "http://cars.mail.ru/sale/decode_contacts_string_png/?plain=0&str=53616c7465645f5f881cd23e39559a2fd0b4abf50e74c5694829f721330cdfcb616b02b9280264aa29d73a05aba067ca&1249198371078819")

;(recognize {:src-url mail-unknown})


;; (defn preprocess1 [url]
;;   (let [pre (-> url (URL.) read-image to-gray-scale-mat preprocess)
;;         ;pre (preprocess im)
;;         rects (get-contours-rects pre)]
;;     (doseq [rect rects]
;;       (Core/rectangle pre
;;                       (Point. (.x rect) (.y rect))
;;                       (Point. (+ (.x rect)(.width rect)) (+ (.y rect)(.height rect)))
;;                       (Scalar. 255 255 255))
;;       )

;;     (mat-to-file pre "D:\\myimage.png")
;;     ))

;(preprocess1 mail-unknown)

;; (defn urls-to-sample-img [samples path]
;;   (let [big (process-samples samples hconcat {:src-field :src-url})]
;;     (mat-to-file big path)))


;; (defn make-img-by-urls [urls path]
;;   (->> urls
;;      (map #(-> % (URL.) read-image to-gray-scale-mat))
;;      (map #(resize-mat % 210 30))
;;      (vconcat)
;;      (#(mat-to-file % path))))

;(make-img-by-urls (map first samples)"D:\\myimage.png")

