import '@expo/metro-runtime';

import { App } from 'expo-router/build/qualified-entry';
import { registerRootComponent } from 'expo';
import { setInitialProps } from './constants/initial-props';

function Root(props: Record<string, any>) {
  if (props && Object.keys(props).length > 0) {
    setInitialProps(props);
  }
  return <App />;
}

registerRootComponent(Root);
