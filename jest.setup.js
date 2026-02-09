/* eslint-env jest */

jest.mock('react-native-vision-camera', () => ({
  Camera: 'Camera',
  useCameraDevice: jest.fn(),
  useCameraPermission: jest.fn(() => ({
    hasPermission: true,
    requestPermission: jest.fn(),
  })),
}));

jest.mock('react-native-gesture-handler', () => ({
  State: {},
  PanGestureHandler: 'PanGestureHandler',
  BaseButton: 'BaseButton',
  Directions: {},
}));

jest.mock('react-native-reanimated', () => {
  const Reanimated = require('react-native-reanimated/mock');
  Reanimated.default.call = () => {};
  return Reanimated;
});
