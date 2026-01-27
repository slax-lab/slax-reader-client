import React, { ComponentType, useState, useEffect } from 'react';
import { View, ActivityIndicator, AppRegistry } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { I18nextProvider } from 'react-i18next';
import i18n, { initI18n } from '../i18n/config';

/**
 * 默认加载组件
 */
const DefaultLoadingComponent: React.FC = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <ActivityIndicator size="large" />
  </View>
);

function createComponentWrapper<P extends object>(
  Component: ComponentType<P>
): ComponentType<P> {
  const WrappedComponent: React.FC<P> = (props) => {
    const [ready, setReady] = useState(false);

    useEffect(() => {
      const initialize = async () => {
        try {
          await initI18n();
        } catch (error) {
          console.error('[registerComponent] 初始化失败:', error);
        } finally {
          setReady(true);
        }
      };

      initialize();
    }, []);

    if (!ready) {
      return <DefaultLoadingComponent />;
    }

    return (
      <SafeAreaProvider>
        <I18nextProvider i18n={i18n}>
          <Component {...props} />
        </I18nextProvider>
      </SafeAreaProvider>
    );
  };

  return WrappedComponent;
}

/**
 * 注册 React Native 组件到 AppRegistry
 */
export function registerRNComponent<P extends object>(
  componentName: string,
  Component: ComponentType<P>
): void {
  const WrappedComponent = createComponentWrapper(Component);
  AppRegistry.registerComponent(componentName, () => WrappedComponent);
}
