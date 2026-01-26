/**
 * React Native 组件注册辅助工具
 * 提供统一的组件注册接口，自动处理初始化逻辑
 */

import React, { useState, useEffect, ComponentType } from 'react';
import { View, ActivityIndicator, AppRegistry } from 'react-native';
import { initI18n } from './i18n';
import { I18nProvider } from '../contexts/I18nContext';

/**
 * 组件注册配置选项
 */
export interface RegisterComponentOptions {
  /** 是否显示加载指示器，默认 true */
  showLoadingIndicator?: boolean;
  /** 自定义加载组件 */
  loadingComponent?: ComponentType;
  /** 额外的初始化函数 */
  extraInit?: () => Promise<void>;
}

/**
 * 默认加载组件
 */
const DefaultLoadingComponent: React.FC = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <ActivityIndicator size="large" />
  </View>
);

/**
 * 创建带初始化逻辑的组件包装器
 * @param Component 要包装的业务组件
 * @param options 配置选项
 * @returns 包装后的组件
 */
function createComponentWrapper<P extends object>(
  Component: ComponentType<P>,
  options: RegisterComponentOptions = {}
): ComponentType<P> {
  const {
    showLoadingIndicator = true,
    loadingComponent: LoadingComponent = DefaultLoadingComponent,
    extraInit,
  } = options;

  const WrappedComponent: React.FC<P> = (props) => {
    const [ready, setReady] = useState(false);

    useEffect(() => {
      const initialize = async () => {
        try {
          // 初始化 i18n（具有幂等性，不会重复初始化）
          await initI18n();

          // 执行额外的初始化逻辑
          if (extraInit) {
            await extraInit();
          }
        } catch (error) {
          console.error('[registerComponent] 初始化失败:', error);
        } finally {
          setReady(true);
        }
      };

      initialize();
    }, []);

    // 显示加载指示器
    if (!ready && showLoadingIndicator) {
      return <LoadingComponent />;
    }

    // 渲染业务组件，使用 I18nProvider 包裹以支持响应式语言切换
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
 * 自动处理 i18n 初始化等通用逻辑
 *
 * @param componentName 组件注册名称（原生端使用此名称）
 * @param Component 要注册的 React 组件
 * @param options 可选配置
 *
 * @example
 * // 基础用法
 * registerRNComponent('RNFeedbackPage', FeedbackPage);
 *
 * @example
 * // 自定义加载组件
 * registerRNComponent('RNFeedbackPage', FeedbackPage, {
 *   loadingComponent: CustomLoading
 * });
 *
 * @example
 * // 添加额外初始化逻辑
 * registerRNComponent('RNFeedbackPage', FeedbackPage, {
 *   extraInit: async () => {
 *     await initAnalytics();
 *   }
 * });
 */
export function registerRNComponent<P extends object>(
  componentName: string,
  Component: ComponentType<P>,
  options?: RegisterComponentOptions
): void {
  const WrappedComponent = createComponentWrapper(Component, options);
  AppRegistry.registerComponent(componentName, () => WrappedComponent);
  console.log(`[registerComponent] 已注册组件: ${componentName}`);
}
