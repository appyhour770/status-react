(ns status-im.ui.screens.routing.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [status-im.ui.screens.profile.tribute-to-talk.views :as tr-to-talk]
            [status-im.ui.screens.hardwallet.pin.views :as hardwallet.pin]
            [status-im.ui.screens.add-new.new-public-chat.view :as new-public-chat]
            [status-im.ui.screens.wallet.components.views :as wallet.components]
            [status-im.ui.screens.qr-scanner.views :as qr-scanner]
            [status-im.ui.screens.stickers.views :as stickers]
            [status-im.ui.screens.home.views :as home]
            [status-im.ui.screens.chat.views :as chat]
            [status-im.ui.screens.keycard.views :as keycard]
            [status-im.ui.screens.wallet.transactions.views :as wallet-transactions]
            [status-im.ui.screens.routing.intro-login-stack :as intro-login-stack]
            [status-im.ui.screens.routing.chat-stack :as chat-stack]
            [status-im.ui.screens.routing.wallet-stack :as wallet-stack]
            [status-im.ui.screens.routing.profile-stack :as profile-stack]
            [status-im.ui.screens.routing.browser-stack :as browser-stack]
            [status-im.chat.models.loading :as chat.loading]
            [status-im.ui.components.tabbar.core :as tabbar]
            [status-im.ui.screens.routing.core :as navigation]))

(defonce main-stack (navigation/create-stack))
(defonce modals-stack (navigation/create-stack))
(defonce bottom-tabs (navigation/create-bottom-tabs))

;; TODO:  Add two-pane navigator on chat-stack
(defn tabs []
  [bottom-tabs {:initial-route-name :chat-stack
                :lazy               false
                :header-mode        :none
                :tab-bar            tabbar/tabbar}
   [{:name      :chat-stack
     :no-wrap   true
     :component chat-stack/chat-stack}
    {:name      :browser-stack
     :no-wrap   true
     :component browser-stack/browser-stack}
    {:name      :wallet-stack
     :no-wrap   true
     :component wallet-stack/wallet-stack}
    {:name      :profile-stack
     :no-wrap   true
     :component profile-stack/profile-stack}]])

(defn modals []
  [modals-stack {:mode                     :modal
                 :header-mode              :none
                 :defaultNavigationOptions {:gestureEnabled true}}
   [{:name      :chat-modal
     :on-focus  [::chat.loading/load-messages]
     :component chat/chat-modal}
    {:name      :stickers-pack-modal
     :component stickers/pack-modal}
    {:name      :tribute-learn-more
     :component tr-to-talk/learn-more}
    {:name      :wallet-transactions-filter
     :component wallet-transactions/filter-history}
    {:name      :welcome
     :component home/welcome}
    {:name      :keycard-welcome
     :component keycard/welcome}
    {:name      :new-chat
     :component tabs}
    {:name      :new-public-chat
     :component new-public-chat/new-public-chat}
    {:name      :contact-code
     :component wallet.components/contact-code}
    {:name      :qr-scanner
     :no-wrap   true
     :component qr-scanner/qr-scanner}]])

(defn get-main-component [_]
  (fn []
    [main-stack {:header-mode :none
                 ;; :mode        :modal
}
     [(if @(re-frame/subscribe [:multiaccount/logged-in?])
        {:name      :tabs
         :no-wrap   true
         :component tabs}
        {:name      :intro-stack
         :no-wrap   true
         :component intro-login-stack/intro-stack})
      {:name      :modals
       :no-wrap   true
       :component modals}]]))
