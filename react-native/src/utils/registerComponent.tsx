import React, { useState, useEffect, ComponentType } from 'react';
import { View, ActivityIndicator, AppRegistry } from 'react-native';
import { initI18n } from './i18n';
import { I18nProvider } from '../contexts/I18nContext';

/**
 * 组件注册配置选项
 */
export interface RegisterComponentOptions {
  showLoadingIndicator?: boolean;
  loadingComponent?: ComponentType;
}

const DefaultLoadingComponent: React.FC = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <ActivityIndicator size="large" />
  </View>
);

function createComponentWrapper<P extends object>(
  Component: ComponentType<P>,
  options: RegisterComponentOptions = {}
): ComponentType<P> {
  const {
    showLoadingIndicator = true,
    loadingComponent: LoadingComponent = DefaultLoadingComponent,
  } = options;

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

    if (!ready && showLoadingIndicator) {
      return <LoadingComponent />;
    }

    return (
      <I18nProvider>
        <Component {...props} />
      </I18nProvider>
    );
  };

  return WrappedComponent;
}

/**
 * 注册 React Native 组件到 AppRegistry
 */
export function registerRNComponent<P extends object>(
  componentName: string,
  Component: ComponentType<P>,
  options?: RegisterComponentOptions
): void {
  const WrappedComponent = createComponentWrapper(Component, options);
  AppRegistry.registerComponent(componentName, () => WrappedComponent);
}
