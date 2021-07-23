package com.matejdr.admanager;

import android.os.Handler;
import android.os.Looper;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.matejdr.admanager.customClasses.CustomTargeting;
import com.matejdr.admanager.enums.TargetingEnums;
import com.matejdr.admanager.enums.TargetingEnums.TargetingTypes;
import com.matejdr.admanager.utils.Targeting;

public class RNAdManagerInterstitial extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "CTKInterstitial";

    public static final String EVENT_AD_LOADED = "interstitialAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "interstitialAdFailedToLoad";
    public static final String EVENT_AD_FAILED_TO_OPEN = "interstitialAdFailedToOpen";
    public static final String EVENT_AD_OPENED = "interstitialAdOpened";
    public static final String EVENT_AD_CLOSED = "interstitialAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "interstitialAdLeftApplication";

    InterstitialAd mInterstitialAd;
    String[] testDevices;
    ReadableMap targeting;
    String adUnitId;

    CustomTargeting[] customTargeting;
    String[] categoryExclusions;
    String[] keywords;
    String contentURL;
    String publisherProvidedID;
    Location location;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNAdManagerInterstitial(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void setAdUnitID(String adUnitID) {
        this.adUnitId = adUnitID;
//        if (mInterstitialAd.getAdUnitId() == null) {
//            mInterstitialAd.setAdUnitId(adUnitID);
//        }
    }

    @ReactMethod
    public void setTestDevices(ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        this.testDevices = list.toArray(new String[list.size()]);
    }

    @ReactMethod
    public void setTargeting(ReadableMap targetingObjects) {
        this.targeting = targetingObjects;

        ReadableMapKeySetIterator targetings = targetingObjects.keySetIterator();

        if (targetings.hasNextKey()) {
            for (
                ReadableMapKeySetIterator it = targetingObjects.keySetIterator();
                it.hasNextKey();
            ) {
                String targetingType = it.nextKey();

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CUSTOMTARGETING))) {
                    ReadableMap customTargetingObject = targetingObjects.getMap(targetingType);
                    CustomTargeting[] customTargetingArray = Targeting.getCustomTargeting(customTargetingObject);
                    this.customTargeting = customTargetingArray;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CATEGORYEXCLUSIONS))) {
                    ReadableArray categoryExclusionsArray = targetingObjects.getArray(targetingType);
                    ReadableNativeArray nativeArray = (ReadableNativeArray)categoryExclusionsArray;
                    ArrayList<Object> list = nativeArray.toArrayList();
                    this.categoryExclusions = list.toArray(new String[list.size()]);
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.KEYWORDS))) {
                    ReadableArray keywords = targetingObjects.getArray(targetingType);
                    ReadableNativeArray nativeArray = (ReadableNativeArray)keywords;
                    ArrayList<Object> list = nativeArray.toArrayList();
                    this.keywords = list.toArray(new String[list.size()]);
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CONTENTURL))) {
                    String contentURL = targetingObjects.getString(targetingType);
                    this.contentURL = contentURL;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.PUBLISHERPROVIDEDID))) {
                    String publisherProvidedID = targetingObjects.getString(targetingType);
                    this.publisherProvidedID = publisherProvidedID;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.LOCATION))) {
                    ReadableMap locationObject = targetingObjects.getMap(targetingType);
                    Location location = Targeting.getLocation(locationObject);
                    this.location = location;
                }
            }
        }

    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        mInterstitialAd = null;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                  if(adUnitId == null) {
                    promise.reject("E_AD_NOT_READY", "Missing ad unit id");
                    return;
                  }
//                if (mInterstitialAd.isLoaded() || mInterstitialAd.isLoading()) {
//                    promise.reject("E_AD_ALREADY_LOADED", "Ad is already loaded.");
//                } else {
                    AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();
                    if (testDevices != null) {
                        ArrayList<String> devices = new ArrayList<String>(testDevices.length);
                        for (int i = 0; i < testDevices.length; i++) {
                            String testDevice = testDevices[i];
                            if (testDevice == "SIMULATOR") {
                                testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
                            }
                            devices.add(testDevice);
                        }
                        // TODO: move to a more central part of the package
                        RequestConfiguration configuration =
                                new RequestConfiguration.Builder().setTestDeviceIds(devices).build();
                        MobileAds.setRequestConfiguration(configuration);
                    }

                    if (customTargeting != null && customTargeting.length > 0) {
                        for (int i = 0; i < customTargeting.length; i++) {
                            String key = customTargeting[i].key;
                            if (!key.isEmpty()) {
                                if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty()) {
                                    adRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
                                } else if (customTargeting[i].values != null && !customTargeting[i].values.isEmpty()) {
                                    adRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
                                }
                            }
                        }
                    }
                    if (categoryExclusions != null && categoryExclusions.length > 0) {
                        for (int i =0; i < categoryExclusions.length; i++) {
                            String categoryExclusion = categoryExclusions[i];
                            if (!categoryExclusion.isEmpty()) {
                                adRequestBuilder.addCategoryExclusion(categoryExclusion);
                            }
                        }
                    }
                    if (keywords != null && keywords.length > 0) {
                        for (int i = 0; i < keywords.length; i++) {
                            String keyword = keywords[i];
                            if (!keyword.isEmpty()) {
                                adRequestBuilder.addKeyword(keyword);
                            }
                        }
                    }
                    if (contentURL != null) {
                        adRequestBuilder.setContentUrl(contentURL);
                    }
                    if (publisherProvidedID != null) {
                        adRequestBuilder.setPublisherProvidedId(publisherProvidedID);
                    }
                    if (location != null) {
                        adRequestBuilder.setLocation(location);
                    }

                    AdManagerAdRequest adRequest = adRequestBuilder.build();


                    InterstitialAd.load(getReactApplicationContext(), adUnitId, adRequest, new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            super.onAdLoaded(interstitialAd);
                            sendEvent(EVENT_AD_LOADED, null);
                            promise.resolve(null);
                            mInterstitialAd = interstitialAd;

                            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                    super.onAdFailedToShowFullScreenContent(adError);
                                    WritableMap error = Arguments.createMap();
                                    error.putString("message", adError.getMessage());
                                    error.putInt("code", adError.getCode());
                                    sendEvent(EVENT_AD_FAILED_TO_OPEN, null);
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    super.onAdShowedFullScreenContent();
                                    sendEvent(EVENT_AD_OPENED, null);
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent();
                                    sendEvent(EVENT_AD_CLOSED, null);
                                }

                                @Override
                                public void onAdImpression() {
                                    super.onAdImpression();
                                }
                            });
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            String errorString = "ERROR_UNKNOWN";
                            String errorMessage = loadAdError.getMessage();
                            switch (loadAdError.getCode()) {
                                case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    errorString = "ERROR_CODE_INTERNAL_ERROR";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                                    errorString = "ERROR_CODE_INVALID_REQUEST";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                                    errorString = "ERROR_CODE_NETWORK_ERROR";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                                    errorString = "ERROR_CODE_NO_FILL";
                                    break;
                            }
                            WritableMap event = Arguments.createMap();
                            WritableMap error = Arguments.createMap();
                            event.putString("message", errorMessage);
                            event.putString("type", errorString);
                            sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
                            promise.reject(errorString, errorMessage);
                        }
                    });
//                }
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(getCurrentActivity());
                    promise.resolve(null);
                } else {
                    promise.reject("E_AD_NOT_READY", "Ad is not ready.");
                }
            }
        });
    }

    @ReactMethod
    public void isReady(final Callback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                callback.invoke(mInterstitialAd != null);
            }
        });
    }
}
