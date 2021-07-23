package com.matejdr.admanager;

import android.view.View;
import android.location.Location;
import android.util.Log;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.ads.mediation.admob.AdMobAdapter;
//import com.google.ads.mediation.facebook.FacebookExtras;
//import com.google.ads.mediation.facebook.FacebookAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.admanager.AppEventListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeCustomFormatAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.formats.OnAdManagerAdViewLoadedListener;
import com.google.android.gms.ads.nativead.NativeCustomFormatAd.OnCustomFormatAdLoadedListener;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import com.matejdr.admanager.customClasses.CustomTargeting;
import com.matejdr.admanager.utils.Targeting;

public class NativeAdViewGroup extends ReactViewGroup implements AppEventListener,
        LifecycleEventListener, NativeAd.OnNativeAdLoadedListener,
        OnAdManagerAdViewLoadedListener, OnCustomFormatAdLoadedListener {
    public static final String AD_TYPE_BANNER = "banner";
    public static final String AD_TYPE_NATIVE = "native";
    public static final String AD_TYPE_TEMPLATE = "template";

    protected AdLoader adLoader;
    protected ReactApplicationContext applicationContext;
    protected NativeAdView nativeAdView;
    protected AdManagerAdView publisherAdView;
    protected NativeCustomFormatAd nativeCustomFormatAd;
    protected String nativeCustomFormatAdClickableAsset;
    protected ThemedReactContext context;

    String[] testDevices;
    String adUnitID;
    AdSize[] validAdSizes;
    AdSize adSize;
    String[] customTemplateIds;
    String[] validAdTypes = new String[]{AD_TYPE_BANNER, AD_TYPE_NATIVE, AD_TYPE_TEMPLATE};
    ;

    // Targeting
    Boolean hasTargeting = false;
    CustomTargeting[] customTargeting;
    String[] categoryExclusions;
    String[] keywords;
    String contentURL;
    String publisherProvidedID;
    Location location;
    String correlator;

    /**
     * @{RCTEventEmitter} instance used for sending events back to JS
     **/
    private RCTEventEmitter mEventEmitter;

    /**
     * Creates new NativeAdView instance and retrieves event emitter
     *
     * @param context
     */
    public NativeAdViewGroup(ThemedReactContext context, ReactApplicationContext applicationContext) {
        super(context);
        this.context = context;
        this.applicationContext = applicationContext;
        this.applicationContext.addLifecycleEventListener(this);

        this.nativeAdView = new NativeAdView(context);
        this.publisherAdView = new AdManagerAdView(context);

        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
    }

    public void loadAd(RNAdManageNativeManager.AdsManagerProperties adsManagerProperties) {
        this.testDevices = adsManagerProperties.getTestDevices();
        this.adUnitID = adsManagerProperties.getAdUnitID();
    }

    private void setupAdLoader() {
        if (adLoader != null) {
            return;
        }

        final ReactApplicationContext reactContext = this.applicationContext;

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                .build();

        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        if (adSize != null) {
            adSizes.add(adSize);
        }
        if (validAdSizes != null) {
            for (int i = 0; i < validAdSizes.length; i++) {
                adSizes.add(validAdSizes[i]);
            }
        }

        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }

        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);

        List<String> validAdTypesList = Arrays.asList(validAdTypes);

//                AdSize[] validAdSizes = new AdSize[]{AdSize.BANNER,
//                        AdSize.FULL_BANNER,
//                        AdSize.LARGE_BANNER,
//                        AdSize.LEADERBOARD,
//                        AdSize.MEDIUM_RECTANGLE,
//                        AdSize.WIDE_SKYSCRAPER,
//                        AdSize.SMART_BANNER,
//                        AdSize.FLUID};

        Log.e("validAdTypes", validAdTypesList.toString());
        AdLoader.Builder builder = new AdLoader.Builder(reactContext, adUnitID);
        if (validAdTypesList.contains(AD_TYPE_NATIVE)) {
            Log.e("validAdTypes", AD_TYPE_NATIVE);
            builder.forNativeAd(NativeAdViewGroup.this);
        }
        if (adSizesArray.length > 0 && validAdTypesList.contains(AD_TYPE_BANNER)) {
            Log.e("validAdTypes", AD_TYPE_BANNER);
            builder.forAdManagerAdView(NativeAdViewGroup.this, adSizesArray);
        }
        if (customTemplateIds != null && customTemplateIds.length > 0 && validAdTypesList.contains(AD_TYPE_TEMPLATE)) {
            Log.e("validAdTypes", AD_TYPE_TEMPLATE);
            for (int i = 0; i < customTemplateIds.length; i++) {
                String curCustomTemplateID = customTemplateIds[i];
                if (!curCustomTemplateID.isEmpty()) {
                    builder.forCustomFormatAd(curCustomTemplateID, NativeAdViewGroup.this, null);
                }
            }
            // builder.forCustomTemplateAd(customTemplateIds, NativeAdView.this, null);
        }
        builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                String errorMessage = "Unknown error";
                switch (adError.getCode()) {
                    case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", adError.getMessage());
                event.putMap("error", error);
                sendEvent(RNAdManagerNativeViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdLoaded() {
                // sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, null);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                sendEvent(RNAdManagerNativeViewManager.EVENT_AD_CLICKED, null);
            }

            @Override
            public void onAdOpened() {
                sendEvent(RNAdManagerNativeViewManager.EVENT_AD_OPENED, null);
            }

            @Override
            public void onAdClosed() {
                sendEvent(RNAdManagerNativeViewManager.EVENT_AD_CLOSED, null);
            }

//            @Override
//            public void onAdLeftApplication() {
//                sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LEFT_APPLICATION, null);
//            }
        }).withNativeAdOptions(adOptions);

        adLoader = builder.build();
    }

    public void reloadAd() {
        this.setupAdLoader();

        if (adLoader != null) {
            UiThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();
                    if (testDevices != null) {
                        List<String> devices = new ArrayList<>(testDevices.length);
                        for (int i = 0; i < testDevices.length; i++) {
                            String testDevice = testDevices[i];
                            if (testDevice == "SIMULATOR") {
                                testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
                            }
                            devices.add(
                                    testDevice
                            );
                        }
                        // TODO: move to a more central part of the package
                        RequestConfiguration configuration =
                                new RequestConfiguration.Builder().setTestDeviceIds(devices).build();
                        MobileAds.setRequestConfiguration(configuration);
                    }

                    if (correlator == null) {
                        correlator = (String) Targeting.getCorelator(adUnitID);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("correlator", correlator);

                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);

//                    Bundle fbExtras = new FacebookExtras()
//                        .setNativeBanner(true)
//                        .build();
//
//                    adRequestBuilder.addNetworkExtrasBundle(FacebookAdapter.class, fbExtras);

                    // Targeting
                    if (hasTargeting) {
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
                            for (int i = 0; i < categoryExclusions.length; i++) {
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
                    }

                    AdManagerAdRequest adRequest = adRequestBuilder.build();
                    adLoader.loadAd(adRequest);
                }
            });
        }
    }

    public void registerViewsForInteraction(List<View> clickableViews) {
        if (nativeCustomFormatAd != null && nativeCustomFormatAdClickableAsset != null) {
            try {
                for (View view : clickableViews) {
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            nativeCustomFormatAd.performClick(nativeCustomFormatAdClickableAsset);
                        }
                    });
                }
            } catch (Exception e) {
            }
        } else if (nativeAdView != null) {
            int viewWidth = this.getMeasuredWidth();
            int viewHeight = this.getMeasuredHeight();

            int left = 0;
            int top = 0;

            if (viewHeight <= 0) {
                viewHeight = 1500;
            }

            nativeAdView.getLayoutParams().width = viewWidth;
            nativeAdView.getLayoutParams().height = viewHeight;

            nativeAdView.measure(viewWidth, viewHeight);
            nativeAdView.layout(left, top, left + viewWidth, top + viewHeight);

            View tmpView = new View(context);
            tmpView.layout(left, top, left + viewWidth, top + viewHeight);
            nativeAdView.addView(tmpView);

            tmpView.getLayoutParams().width = viewWidth;
            tmpView.getLayoutParams().height = viewHeight;

            nativeAdView.setCallToActionView(tmpView);

//            try {
//                for (View view : clickableViews) {
//
//                    ((ViewGroup) view.getParent()).removeView(view);
//                    nativeAdView.addView(view);
//                    nativeAdView.setCallToActionView(view);
//                }
//            } catch (Exception e) {}
        }
    }

    @Override
    public void onNativeAdLoaded(NativeAd unifiedNativeAd) {
        nativeAdView.setNativeAd(unifiedNativeAd);
        removeAllViews();
        addView(nativeAdView);

        setNativeAd(unifiedNativeAd);
    }

    @Override
    public void onAdManagerAdViewLoaded(AdManagerAdView adView) {
        this.publisherAdView = adView;
        removeAllViews();
        this.addView(adView);
        if (adView == null) {
            sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, null);
            return;
        }
        int width = adView.getAdSize().getWidthInPixels(context);
        int height = adView.getAdSize().getHeightInPixels(context);
        int left = adView.getLeft();
        int top = adView.getTop();
        adView.measure(width, height);
        adView.layout(left, top, left + width, top + height);
        sendOnSizeChangeEvent(adView);
        WritableMap ad = Arguments.createMap();
        ad.putString("type", AD_TYPE_BANNER);

        WritableMap gadSize = Arguments.createMap();
        gadSize.putDouble("width", adView.getAdSize().getWidth());
        gadSize.putDouble("height", adView.getAdSize().getHeight());

        ad.putMap("gadSize", gadSize);

        //ad.putString("gadSize", adView.getAdSize().toString());
        sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, ad);
    }

    @Override
    public void onCustomFormatAdLoaded(NativeCustomFormatAd nativeCustomFormatAd) {
        this.nativeCustomFormatAd = nativeCustomFormatAd;
        removeAllViews();

        setNativeAd(nativeCustomFormatAd);
    }

    /**
     * Called by the view manager when ad is loaded. Sends serialised
     * version of a native ad back to Javascript.
     *
     * @param nativeCustomFormatAd
     */
    private void setNativeAd(NativeCustomFormatAd nativeCustomFormatAd) {
        if (nativeCustomFormatAd == null) {
            sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, null);
            return;
        }

        WritableMap ad = Arguments.createMap();
        ad.putString("type", AD_TYPE_TEMPLATE);
        ad.putString("templateID", nativeCustomFormatAd.getCustomFormatId());
        for (String assetName : nativeCustomFormatAd.getAvailableAssetNames()) {
            if (nativeCustomFormatAd.getText(assetName) != null) {
                if (nativeCustomFormatAdClickableAsset == null && nativeCustomFormatAd.getText(assetName).length() > 0) {
                    nativeCustomFormatAdClickableAsset = assetName;
                }
                ad.putString(assetName, nativeCustomFormatAd.getText(assetName).toString());
            } else if (nativeCustomFormatAd.getImage(assetName) != null) {
                WritableMap imageMap = Arguments.createMap();
                imageMap.putString("uri", nativeCustomFormatAd.getImage(assetName).getUri().toString());
                imageMap.putInt("width", nativeCustomFormatAd.getImage(assetName).getDrawable().getBounds().width());
                imageMap.putInt("height", nativeCustomFormatAd.getImage(assetName).getDrawable().getBounds().height());
                imageMap.putDouble("scale", nativeCustomFormatAd.getImage(assetName).getScale());
                ad.putMap(assetName, imageMap);
            }
        }

        sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, ad);

        nativeCustomFormatAd.recordImpression();
    }

    private void setNativeAd(NativeAd unifiedNativeAd) {
        if (unifiedNativeAd == null) {
            sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, null);
            return;
        }

        WritableMap ad = Arguments.createMap();
        ad.putString("type", AD_TYPE_NATIVE);
        if (unifiedNativeAd.getHeadline() == null) {
            ad.putString("headline", null);
        } else {
            ad.putString("headline", unifiedNativeAd.getHeadline());
        }

        if (unifiedNativeAd.getBody() == null) {
            ad.putString("bodyText", null);
        } else {
            ad.putString("bodyText", unifiedNativeAd.getBody());
        }

        if (unifiedNativeAd.getCallToAction() == null) {
            ad.putString("callToActionText", null);
        } else {
            ad.putString("callToActionText", unifiedNativeAd.getCallToAction());
        }

        if (unifiedNativeAd.getAdvertiser() == null) {
            ad.putString("advertiserName", null);
        } else {
            ad.putString("advertiserName", unifiedNativeAd.getAdvertiser());
        }

        if (unifiedNativeAd.getStarRating() == null) {
            ad.putString("starRating", null);
        } else {
            ad.putDouble("starRating", unifiedNativeAd.getStarRating());
        }

        if (unifiedNativeAd.getStore() == null) {
            ad.putString("storeName", null);
        } else {
            ad.putString("storeName", unifiedNativeAd.getStore());
        }

        if (unifiedNativeAd.getPrice() == null) {
            ad.putString("price", null);
        } else {
            ad.putString("price", unifiedNativeAd.getPrice());
        }

        if (unifiedNativeAd.getIcon() == null) {
            ad.putString("icon", null);
        } else {
            WritableMap icon = Arguments.createMap();
            icon.putString("uri", unifiedNativeAd.getIcon().getUri().toString());
            icon.putInt("width", unifiedNativeAd.getIcon().getDrawable().getBounds().width());
            icon.putInt("height", unifiedNativeAd.getIcon().getDrawable().getBounds().height());
            icon.putDouble("scale", unifiedNativeAd.getIcon().getScale());
            ad.putMap("icon", icon);
        }

        if (unifiedNativeAd.getImages().size() == 0) {
            ad.putArray("images", null);
        } else {
            WritableArray images = Arguments.createArray();
            for (NativeAd.Image image : unifiedNativeAd.getImages()) {
                WritableMap imageMap = Arguments.createMap();
                imageMap.putString("uri", image.getUri().toString());
                imageMap.putInt("width", image.getDrawable().getBounds().width());
                imageMap.putInt("height", image.getDrawable().getBounds().height());
                imageMap.putDouble("scale", image.getScale());
                images.pushMap(imageMap);
            }
            ad.putArray("images", images);
        }

//        Bundle extras = unifiedNativeAd.getExtras();
//        if (extras.containsKey(FacebookAdapter.KEY_SOCIAL_CONTEXT_ASSET)) {
//            String socialContext = (String) extras.get(FacebookAdapter.KEY_SOCIAL_CONTEXT_ASSET);
//            ad.putString("socialContext", socialContext);
//        }

        sendEvent(RNAdManagerNativeViewManager.EVENT_AD_LOADED, ad);
    }


    private void sendOnSizeChangeEvent(AdManagerAdView adView) {
        int width;
        int height;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
        AdSize adSize = adView.getAdSize();
        if (adSize == AdSize.SMART_BANNER) {
            width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(reactContext));
            height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(reactContext));
        } else {
            width = adSize.getWidth();
            height = adSize.getHeight();
        }
        event.putString("type", "banner");
        event.putDouble("width", width);
        event.putDouble("height", height);
        sendEvent(RNAdManagerNativeViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        mEventEmitter.receiveEvent(getId(), name, event);
    }

    public void setCustomTemplateIds(String[] customTemplateIds) {
        this.customTemplateIds = customTemplateIds;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    public void setValidAdTypes(String[] adTypes) {
        Log.e("validAdTypes_s", adTypes.toString());
        this.validAdTypes = adTypes;
    }

    // Targeting
    public void setCustomTargeting(CustomTargeting[] customTargeting) {
        this.customTargeting = customTargeting;
    }

    public void setCategoryExclusions(String[] categoryExclusions) {
        this.categoryExclusions = categoryExclusions;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public void setPublisherProvidedID(String publisherProvidedID) {
        this.publisherProvidedID = publisherProvidedID;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setCorrelator(String correlator) {
        this.correlator = correlator;
    }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNAdManagerNativeViewManager.EVENT_APP_EVENT, event);
    }

    @Override
    public void onHostResume() {
//        if (this.nativeAdView != null) {
//            this.nativeAdView.resume();
//        }
        if (this.publisherAdView != null) {
            this.publisherAdView.resume();
        }
//        if (this.nativeCustomFormatAd != null) {
//            this.nativeCustomFormatAd.resume();
//        }
    }

    @Override
    public void onHostPause() {
//        if (this.nativeAdView != null) {
//            this.nativeAdView.pause();
//        }
        if (this.publisherAdView != null) {
            this.publisherAdView.pause();
        }
//        if (this.nativeCustomFormatAd != null) {
//            this.nativeCustomFormatAd.pause();
//        }
    }

    @Override
    public void onHostDestroy() {
        if (this.nativeAdView != null) {
            this.nativeAdView.destroy();
        }
        if (this.publisherAdView != null) {
            this.publisherAdView.destroy();
        }
        if (this.nativeCustomFormatAd != null) {
            this.nativeCustomFormatAd.destroy();
        }
        if (this.nativeCustomFormatAd != null) {
            this.nativeCustomFormatAd.destroy();
        }
        if (this.adLoader != null) {
            this.adLoader = null;
        }
    }
}
