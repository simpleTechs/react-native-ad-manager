//
//  RNAdManagerConfiguration.m
//  react-native-ad-manager
//
//  Created by Mathis on 23.07.21.
//

#import "RNAdManagerConfiguration.h"
#import "RNAdManagerUtils.h"
#import <React/RCTUtils.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@implementation RNAdManagerConfiguration


RCT_EXPORT_MODULE(CTKAdManagerConfiguration)

RCT_EXPORT_METHOD(setTestDevices:(NSArray *)testDevices)
{
    NSArray* devices = RNAdManagerProcessTestDevices(testDevices, kGADSimulatorID);
    GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = devices;
}

@end
