import { NativeEventEmitter, NativeModules } from 'react-native';
import { createErrorFromErrorData } from './utils';

const { CTKAdManagerConfiguration } = NativeModules;

function setTestDevices(testDevices) {
  if(!Array.isArray(testDevices) || testDevices.find(t => typeof t !== 'string')) {
    throw new Error('test devices must a a string array ')
  }
  CTKAdManagerConfiguration.setTestDevices(testDevices)
}

export default {
  setTestDevices,
  simulatorId: 'SIMULATOR',
};
