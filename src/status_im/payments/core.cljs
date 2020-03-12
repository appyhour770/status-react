(ns status-im.payments.core
  (:require [re-frame.core :as re-frame]
            [status-im.utils.platform :as platform]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.http :as http]
            [taoensso.timbre :as log]
            [status-im.react-native.js-dependencies :as js-dependencies]))

(def payment-gateway "")

(def rn-iap (.-default js-dependencies/react-native-iap))

(def purchase-updated-listener (.-purchaseUpdatedListener js-dependencies/react-native-iap))
(def purchase-error-listener (.-purchaseErrorListener js-dependencies/react-native-iap))
(def finish-transaction (.-finishTransaction js-dependencies/react-native-iap))
(def init-connection (.-initConnection rn-iap))
(def request-purchase (.-requestPurchase rn-iap))

(defn clear-purchase-listeners [active-listeners]
  (loop [listener active-listeners]
    (.remove listener)))

(defn purchase-listeners []
  [(purchase-updated-listener #(re-frame/dispatch [::handle-purchase %]))
   (purchase-error-listener #(log/warn "PAYMENT ERROR:" (js->clj %)))])

(re-frame/reg-fx
 ::init-connection
 (fn []
   (-> (init-connection)
       (.then #(re-frame/dispatch [:set-in [:iap/payment :can-make-payment] %]))
       (.catch #(log/error "PAYMENT ERROR:" (js->clj %))))))

(re-frame/reg-sub
 ::can-make-payment
 (fn [db]
   (get-in db [:iap/payment :can-make-payment])))

(re-frame/reg-fx
 ::confirm-purchase
 (fn [purchase]
   (-> (finish-transaction purchase)
       (.catch #(log/warn "FINISH PAYMENT ERROR:" (js->clj %))))))

(re-frame/reg-fx
 ::request-purchase
 (fn [sku]
   (-> (request-purchase sku  false)
       (.catch #(log/warn "REQUEST PAYMENT ERROR:" (js->clj %))))))

(handlers/register-handler-fx
 ::request-payment
 (fn [_ [_ sku]]
   {::request-purchase sku}))

;; Gateway handling

(handlers/register-handler-fx
 ::handle-purchase
 (fn [_ [_ purchase-data]]
   {::call-payment-gateway purchase-data}))

(re-frame/reg-fx
 ::call-payment-gateway
 (fn [_ [_ purchase-data]]
   (prn purchase-data)))

(handlers/register-handler-fx
 ::gateway-on-success
 (fn []))

(handlers/register-handler-fx
 ::gateway-on-error
 (fn []))
