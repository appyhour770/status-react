(ns status-im.ui.components.tabbar.core
  (:require
   [status-im.ui.components.animation :as animation]
   [status-im.ui.components.tabbar.styles :as tabs.styles]
   [reagent.core :as reagent]
   [oops.core :refer [oget]]
   [status-im.ui.components.react :as react]
   [status-im.utils.platform :as platform]
   [status-im.ui.components.icons.vector-icons :as vector-icons]
   [status-im.ui.components.badge :as badge]
   [status-im.i18n :as i18n]
   [re-frame.core :as re-frame]))

(defonce visible? (animation/create-value 0))

(defonce minimized-state (reagent/atom nil))
(defonce last-to-value (atom 1))

(defn animate
  ([visible duration to]
   (animate visible duration to nil))
  ([visible duration to callback]
   (when (not= to @last-to-value)
     (reset! last-to-value to)
     (animation/start
      (animation/timing visible
                        {:toValue         to
                         :duration        duration
                         :easing          (animation/cubic)
                         :useNativeDriver true})
      callback))))

(defn main-tab? [view-id]
  (contains?
   #{:home :wallet :open-dapp :my-profile :wallet-onboarding-setup}
   view-id))

(defn minimize-bar [route-name]
  (if (main-tab? route-name)
    (do
      (reset! minimized-state false)
      (animate visible? 150 0))
    (do
      (reset! minimized-state true)
      (animate visible? 150 1))))

(def tabs-list-data
  (->>
   [{:nav-stack           :chat-stack
     :content             {:title (i18n/label :t/chat)
                           :icon  :main-icons/message}
     :count-subscription  :chats/unread-messages-number
     :accessibility-label :home-tab-button}
    (when-not platform/desktop?
      {:nav-stack           :browser-stack
       :content             {:title (i18n/label :t/browser)
                             :icon  :main-icons/browser}
       :accessibility-label :dapp-tab-button})
    (when-not platform/desktop?
      {:nav-stack           :wallet-stack
       :content             {:title (i18n/label :t/wallet)
                             :icon  :main-icons/wallet}
       :accessibility-label :wallet-tab-button})
    {:nav-stack           :profile-stack
     :content             {:title (i18n/label :t/profile)
                           :icon  :main-icons/user-profile}
     :count-subscription  :get-profile-unread-messages-number
     :accessibility-label :profile-tab-button}]
   (remove nil?)
   (map-indexed vector)))

(defn tab []
  (fn [{:keys [icon label active? nav-stack on-press
               accessibility-label count-subscription]}]
    (let [count (when count-subscription @(re-frame/subscribe [count-subscription]))]
      [react/touchable-highlight {:style               tabs.styles/touchable-container
                                  :on-press            on-press
                                  :accessibility-label accessibility-label}
       [react/view {:style tabs.styles/tab-container}
        [react/view {:style tabs.styles/icon-container}
         [vector-icons/icon icon (tabs.styles/icon active?)]
         (when count
           (cond
             (or (pos? count) (pos? (:other count)))
             [react/view {:style (if (= nav-stack :chat-stack)
                                   tabs.styles/message-counter
                                   tabs.styles/counter)}
              [badge/message-counter (or (:other count) count) true]]
             (pos? (:public count))
             [react/view {:style tabs.styles/counter-public-container}
              [react/view {:style tabs.styles/counter-public
                           :accessibility-label :public-unread-badge}]]))]
        (when-not platform/desktop?
          [react/view {:style tabs.styles/tab-title-container}
           [react/text {:style (tabs.styles/tab-title active?)}
            label]])]])))

(defn tabs []
  (let [listeners       (atom [])
        keyboard-shown? (reagent/atom false)]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (when platform/android?
          (reset!
           listeners
           [(.addListener react/keyboard  "keyboardDidShow"
                          (fn []
                            (reset! keyboard-shown? true)))
            (.addListener react/keyboard  "keyboardDidHide"
                          (fn []
                            (reset! keyboard-shown? false)))])))
      :component-will-unmount
      (fn []
        (when (not-empty @listeners)
          (doseq [listener @listeners]
            (when listener
              (.remove listener)))))
      :reagent-render
      (fn [{:keys [navigate index inset]}]
        [react/animated-view {:style (tabs.styles/tabs-wrapper @keyboard-shown? @minimized-state inset)}
         [react/animated-view {:style (tabs.styles/animated-container visible?)}
          [react/view
           {:style tabs.styles/tabs-container}
           [react/view {:style tabs.styles/tabs}
            (for [[route-index
                   {:keys [nav-stack accessibility-label count-subscription content]}]
                  tabs-list-data

                  :let [{:keys [icon title]} content]]
              ^{:key nav-stack}
              [tab
               {:icon                icon
                :label               title
                :on-press            #(navigate (name nav-stack))
                :accessibility-label accessibility-label
                :count-subscription  count-subscription
                :active?             (= (str index) (str route-index))
                :nav-stack           nav-stack}])]]]
         [react/view
          {:style (tabs.styles/ios-titles-cover inset)}]])})))

(defn tabbar [props]
  (let [navigate (oget props "navigation" "navigate")
        index    (oget props "state" "index")]
    (reagent/as-element
     [react/safe-area-consumer
      (fn [insets]
        (reagent/as-element
         [tabs {:navigate navigate
                :index    index
                :inset    (oget insets "bottom")}]))])))
