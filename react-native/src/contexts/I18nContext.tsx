/**
 * i18n Context - 提供响应式的国际化支持
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { subscribeTranslationChange, getCurrentLocale, syncCurrentLocale, t as tFunc, tWithParams as tWithParamsFunc } from '../utils/i18n';

/**
 * i18n Context 类型定义
 */
interface I18nContextValue {
  /** 当前语言代码 */
  locale: string;
  /** 获取翻译文本 */
  t: (key: string) => string;
  /** 获取带参数的翻译文本 */
  tWithParams: (key: string, params: Record<string, string>) => Promise<string>;
}

const I18nContext = createContext<I18nContextValue | null>(null);
interface I18nProviderProps {
  children: ReactNode;
}

/**
 * i18n Provider - 管理语言状态并提供给子组件
 */
export function I18nProvider({ children }: I18nProviderProps) {
  const [locale, setLocaleState] = useState<string>(() => getCurrentLocale());
  const [, forceUpdate] = useState(0);

  // 同步最新语言状态
  useEffect(() => {
    const syncLanguage = async () => {
      try {
        const latestLocale = await syncCurrentLocale();
        setLocaleState(latestLocale);
        console.log('[I18nProvider] 已同步KMP端语言:', latestLocale);
      } catch (error) {
        console.error('[I18nProvider] 同步语言失败:', error);
      }
    };

    syncLanguage();
  }, []);

  // 订阅翻译更新事件
  useEffect(() => {
    const unsubscribe = subscribeTranslationChange(() => {
      console.log('[I18nProvider] 翻译数据已更新，触发重新渲染');
      forceUpdate(prev => prev + 1);
    });

    return () => {
      console.log('[I18nProvider] 取消订阅翻译更新事件');
      unsubscribe();
    };
  }, []);

  const t = useCallback((key: string): string => {
    return tFunc(key);
  }, []);

  const tWithParams = useCallback(async (key: string, params: Record<string, string>): Promise<string> => {
    return tWithParamsFunc(key, params, locale);
  }, [locale]);

  const value: I18nContextValue = {
    locale,
    t,
    tWithParams,
  };

  return (
    <I18nContext.Provider value={value}>
      {children}
    </I18nContext.Provider>
  );
}

/**
 * useI18n Hook
 */
export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext);

  if (!context) {
    throw new Error('useI18n 必须在 I18nProvider 内部使用');
  }

  return context;
}
