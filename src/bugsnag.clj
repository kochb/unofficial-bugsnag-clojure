(ns bugsnag
  (:use [clojure.walk :only (stringify-keys)])
  (:require [clojure.data.json :as json])
  (:import [com.bugsnag Client MetaData]
           java.io.ByteArrayInputStream))

(def bugsnag-enabled (not= "development" (System/getenv "RELEASE_STAGE")))

(def bugsnag (if bugsnag-enabled (Client. (System/getenv "BUGSNAG_ID"))))

(def set-user
  (if (not bugsnag-enabled)
    (fn [id email name])
    (fn [id email name]
      (.setUser bugsnag id email name))))

(def set-meta-data
  (if (not bugsnag-enabled)
    (fn [metadata])
    (fn [metadata]
      (doseq [[tab data] (stringify-keys metadata)]
        (doseq [[k v] data]
          (.addToTab bugsnag tab k v))))))

(def set-auto-notify
  (if (not bugsnag-enabled)
    (fn [enabled])
    (fn [enabled]
      (.setAutoNotify bugsnag enabled))))

(def set-notify-release-stages 
  (if (not bugsnag-enabled)
    (fn [stages])
    (fn [stages]
      (.setNotifyReleaseStages bugsnag (into-array stages)))))

(def set-release-stage
  (if (not bugsnag-enabled)
    (fn [stage])
    (fn [stage]
      (when bugsnag-enabled
        (.setReleaseStage bugsnag stage)))))

(def notify
  (if (not bugsnag-enabled)
    (fn [error details])
    (fn [error details]
      (let [metadata (MetaData.)]
        (doseq [[tab data] details]
          (doseq [[k v] data]
            (.addToTab metadata tab k v)))
        (.notify bugsnag
                 (if (string? error) (RuntimeException. error) error)
                 metadata)))))

(defn clean-request [request]
  (stringify-keys
    (if (and (contains? request :body) (request :body))
      (assoc request :body (slurp (request :body)))
      request)))

(defn clean-response [response]
  (stringify-keys response))

(defn notify-request [message & [request response]]
  (notify message (merge
    (when request {"Request" (clean-request request)})
    (when response {"Response" (clean-response response)}))))

(defn make-stream [string]
  (when (string? string) (ByteArrayInputStream. (.getBytes string))))

(defn copy-request [request body]
  (assoc request :body (make-stream body)))

(def wrap-bugsnag 
  (if (not bugsnag-enabled)
    (fn [handler] (fn [request] (handler request)))
    (fn [handler] (fn [request]
      (let [body (when (contains? request :body) (slurp (request :body)))]
        (try
          (let [response (handler (copy-request request body))]
            (when (contains? response :status)
              (when (<= 400 (response :status))
                (notify-request "Failed Request" (copy-request request body) response)))
            response)
          (catch Exception e
            (notify-request e (copy-request request body))
            (throw e))))))))

