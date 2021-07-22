#import "RNAdManagerInterstitial.h"
#import "RNAdManagerUtils.h"

#import <React/RCTUtils.h>

static NSString *const kEventAdLoaded = @"interstitialAdLoaded";
static NSString *const kEventAdFailedToLoad = @"interstitialAdFailedToLoad";
static NSString *const kEventAdOpened = @"interstitialAdOpened";
static NSString *const kEventAdFailedToOpen = @"interstitialAdFailedToOpen";
static NSString *const kEventAdClosed = @"interstitialAdClosed";
static NSString *const kEventAdLeftApplication = @"interstitialAdLeftApplication";

@implementation RNAdManagerInterstitial
{
    GADInterstitialAd  *_interstitial;
    NSString *_adUnitID;
    NSArray *_testDevices;
    NSDictionary *_targeting;
    BOOL _wasShown;

    RCTPromiseResolveBlock _requestAdResolve;
    RCTPromiseRejectBlock _requestAdReject;
    BOOL hasListeners;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE(CTKInterstitial)

- (NSArray<NSString *> *)supportedEvents
{
    return @[
             kEventAdLoaded,
             kEventAdFailedToLoad,
             kEventAdOpened,
             kEventAdFailedToOpen,
             kEventAdClosed,
             kEventAdLeftApplication ];
}

#pragma mark exported methods

RCT_EXPORT_METHOD(setAdUnitID:(NSString *)adUnitID)
{
    _adUnitID = adUnitID;
}

RCT_EXPORT_METHOD(setTestDevices:(NSArray *)testDevices)
{
    _testDevices = RNAdManagerProcessTestDevices(testDevices, kGADSimulatorID);
}

RCT_EXPORT_METHOD(setTargeting:(NSDictionary *)targeting)
{
    _targeting = targeting;
}

RCT_EXPORT_METHOD(requestAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    _requestAdResolve = nil;
    _requestAdReject = nil;

    if (_wasShown || _interstitial == nil) {
        _requestAdResolve = resolve;
        _requestAdReject = reject;
        _wasShown = NO;
        
        GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = _testDevices;

        GAMRequest *request = [GAMRequest request];
        
        if (_targeting != nil) {
            NSDictionary *customTargeting = [_targeting objectForKey:@"customTargeting"];
            if (customTargeting != nil) {
                request.customTargeting = customTargeting;
            }
            NSArray *categoryExclusions = [_targeting objectForKey:@"categoryExclusions"];
            if (categoryExclusions != nil) {
                request.categoryExclusions = categoryExclusions;
            }
            NSArray *keywords = [_targeting objectForKey:@"keywords"];
            if (keywords != nil) {
                request.keywords = keywords;
            }
            NSString *contentURL = [_targeting objectForKey:@"contentURL"];
            if (contentURL != nil) {
                request.contentURL = contentURL;
            }
            NSString *publisherProvidedID = [_targeting objectForKey:@"publisherProvidedID"];
            if (publisherProvidedID != nil) {
                request.publisherProvidedID = publisherProvidedID;
            }
            NSDictionary *location = [_targeting objectForKey:@"location"];
            if (location != nil) {
                CGFloat latitude = [[location objectForKey:@"latitude"] doubleValue];
                CGFloat longitude = [[location objectForKey:@"longitude"] doubleValue];
                CGFloat accuracy = [[location objectForKey:@"accuracy"] doubleValue];
                [request setLocationWithLatitude:latitude longitude:longitude accuracy:accuracy];
            }
        }
        
        [GADInterstitialAd loadWithAdUnitID:_adUnitID
            request:request
            completionHandler:^(GADInterstitialAd *ad, NSError *error) {
            if (error) {
              NSLog(@"Failed to load interstitial ad with error: %@", [error localizedDescription]);
              return;
            }
            self->_interstitial = ad;
            self->_interstitial.fullScreenContentDelegate = self;
        }];
        
    
    } else {
        reject(@"E_AD_ALREADY_LOADED", @"Ad is already loaded.", nil);
    }
}
- (BOOL) check_ready
{
    UIViewController *rootController = [UIApplication sharedApplication].delegate.window.rootViewController;
    NSError* err; // TODO: handle error?
    return [_interstitial canPresentFromRootViewController:rootController error:&err];
}

RCT_EXPORT_METHOD(showAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([self check_ready]) {
        UIViewController *rootController = [UIApplication sharedApplication].delegate.window.rootViewController;
        [_interstitial presentFromRootViewController:rootController];
        _wasShown = YES;
        resolve(nil);
    }
    else {
        reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
    }
}

RCT_EXPORT_METHOD(isReady:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNumber numberWithBool:[self check_ready]]]);
}

- (void)startObserving
{
    hasListeners = YES;
}

- (void)stopObserving
{
    hasListeners = NO;
}

#pragma mark GADFullScreenContentDelegate

- (void)interstitialDidReceiveAd:(__unused GADInterstitialAd *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdLoaded body:nil];
    }
    _requestAdResolve(nil);
}

- (void)interstitial:(__unused GADInterstitialAd *)interstitial didFailToReceiveAdWithError:(NSError *)error
{
    if (hasListeners) {
        NSDictionary *jsError = RCTJSErrorFromCodeMessageAndNSError(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
        [self sendEventWithName:kEventAdFailedToLoad body:jsError];
    }
    _requestAdReject(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
    _interstitial = nil;
}

- (void)interstitialWillPresentScreen:(__unused GADInterstitialAd *)ad
{
    if (hasListeners){
        [self sendEventWithName:kEventAdOpened body:nil];
    }
}

- (void)interstitialDidFailToPresentScreen:(__unused GADInterstitialAd *)ad
{
    if (hasListeners){
        [self sendEventWithName:kEventAdFailedToOpen body:nil];
    }
}

- (void)interstitialWillDismissScreen:(__unused GADInterstitialAd *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdClosed body:nil];
    }
}

- (void)interstitialWillLeaveApplication:(__unused GADInterstitialAd *)ad
{
    if (hasListeners) {
        [self sendEventWithName:kEventAdLeftApplication body:nil];
    }
}

@end
