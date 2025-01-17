# react-native-ad-manager

A react-native module for Google Ad Manager Banners, Interstitials and Native ads.

The banner types are implemented as components while the interstitial and rewarded video have an imperative API.

Native ads are implemented as wrapper for a native view.

## Installation

You can use npm or Yarn to install the latest beta version:

## Getting started

**npm:**

    npm i --save react-native-ad-manager

**Yarn:**

    yarn add react-native-ad-manager

### Mostly automatic installation

`$ react-native link react-native-ad-manager`

Alternatively for iOS you can install the library with CocoaPods by adding a line to your `Podfile`;

    pod 'react-native-ad-manager', path: '../node_modules/react-native-ad-manager'

### iOS

For iOS you will have to add the [Google Mobile Ads SDK](https://developers.google.com/ad-manager/mobile-ads-sdk/ios/quick-start#import_the_mobile_ads_sdk) to your Xcode project.

### Android

On Android the Ad Manager library code is part of Play Services, which is automatically added when this library is linked.

But you still have to manually update your `AndroidManifest.xml`, as described in the [Google Mobile Ads SDK documentation](https://developers.google.com/ad-manager/mobile-ads-sdk/android/quick-start#import_the_mobile_ads_sdk).

## Usage

```jsx
import {
  Banner,
  Interstitial,
  Configuration,
  PublisherBanner,
  NativeAdsManager,
} from 'react-native-ad-manager'

// use test mode for this device and do not track clicks
Configuration.setTestDevices([Configuration.simulatorId]);


// Display a GAM Publisher banner
<Banner
  adSize="fullBanner"
  adUnitID="your-ad-unit-id"
  onAdFailedToLoad={error => console.error(error)}
  onAppEvent={event => console.log(event.name, event.info)}
/>

// Display an interstitial
Interstitial.setAdUnitID('your-ad-unit-id');
Interstitial.requestAd().then(() => Interstitial.showAd());

// Native ad
import NativeAdView from './NativeAdView';
const adsManager = new NativeAdsManager('your-ad-unit-id');
<NativeAdView
    targeting={{
        customTargeting: {group: 'user_test'},
        categoryExclusions: ['media'],
        contentURL: 'test://',
        publisherProvidedID: 'provider_id',
    }}
    style={{width: '100%'}}
    adsManager={adsManager}
    validAdTypes={['native', 'template']}
    customTemplateIds={['your-template-id-1', 'your-template-id-2']}
    onAdLoaded={ad => {
        console.log(ad);
    }}
    onAdFailedToLoad={error => {
        console.log(error);
    }}
/>
```

See the NativeAdView component in the [example NativeAdView](example/NativeAdView.js).
For a full example reference to the [example project](example).


## Mobile Ads SDK v8 (Play Service Ads v20) Upgrade

Pull Requests welcome :)

- [x] Upgrade Dependencies
- [x] make compatible with react-native-firebase (tested with 12.2.0)
- [x] Test Ids via global setting -> `import { Configuration } from 'react-native-ad-manager'`
- [x] fix compilation issues
- [ ] support facebook mediation - we do not need it, might break when using react-native-fbsdk. You can easily fork and uncomment the necessary lines in .java, .m, .gradle, and podspec files
- [ ] support adaptive banners
- [ ] drop support for application leave events (dropped in SDK) -> should warn developers