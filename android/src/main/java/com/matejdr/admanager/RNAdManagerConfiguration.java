package com.matejdr.admanager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.module.annotations.ReactModule;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.ArrayList;

@ReactModule(name = "CTKAdManagerConfiguration")
public class RNAdManagerConfiguration extends ReactContextBaseJavaModule {
  public static final String REACT_CLASS = "CTKAdManagerConfiguration";

  public RNAdManagerConfiguration(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @ReactMethod
  public void setTestDevices(ReadableArray testDevices) {
    ArrayList<String> list = (ArrayList<String>) ((Object) testDevices.toArrayList());
    RequestConfiguration configuration =
          new RequestConfiguration.Builder().setTestDeviceIds(list).build();
    MobileAds.setRequestConfiguration(configuration);
  }

}
