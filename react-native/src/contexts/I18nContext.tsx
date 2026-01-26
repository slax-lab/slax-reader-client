/**
 * i18n Context - 提供响应式的国际化支持
 * 当语言切换时，所有使用 useI18n Hook 的组件会自动重新渲染
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { subscribeLocaleChange, getCurrentLocale, syncCurrentLocale, changeLocale, t as tFunc, tWithParams as tWithParamsFunc } from '../utils/i18n';

/**
 * i18n Context 类型定义
 */
interface I18nContextValue {
  /** 当前语言代码 */
  locale: string;
  /** 切换语言 */
  setLocale: (language: string) => Promise<void>;
  /** 获取翻译文本 */
  t: (key: string) => string;
  /** 获取带参数的翻译文本 */
  tWithParams: (key: string, params: Record<string, string>) => Promise<string>;
}

/**
 * i18n Context
 */
const I18nContext = createContext<I18nContextValue | null>(null);

/**
 * i18n Provider Props
 */
interface I18nProviderProps {
  children: ReactNode;
}

/**
 * i18n Provider - 管理语言状态并提供给子组件
 */
export function I18nProvider({ children }: I18nProviderProps) {
  // 语言状态
  const [locale, setLocaleState] = useState<string>(() => getCurrentLocale());

  // 初始化时同步KMP端的最新语言状态
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
  }, []); // 仅在mount时执行一次

  // 订阅语言切换事件
  useEffect(() => {
    console.log('[I18nProvider] 订阅语言切换事件');

    const unsubscribe = subscribeLocaleChange((newLocale) => {
      console.log('[I18nProvider] 语言切换通知:', newLocale);
      setLocaleState(newLocale);
    });

    return () => {
      console.log('[I18nProvider] 取消订阅语言切换事件');
      unsubscribe();
    };
  }, []);

  // 切换语言的包装函数
  const setLocale = useCallback(async (language: string) => {
    try {
      await changeLocale(language);
      // changeLocale 会自动触发 subscribeLocaleChange 监听器
      // 监听器会调用 setLocaleState 更新状态
    } catch (error) {
      console.error('[I18nProvider] 切换语言失败:', error);
      throw error;
    }
  }, []);

  // 翻译函数（响应式）
  const t = useCallback((key: string): string => {
    // locale 变化时，这个函数会被重新创建，从而触发使用它的组件重新渲染
    return tFunc(key, locale);
  }, [locale]);

  // 带参数的翻译函数（响应式）
  const tWithParams = useCallback(async (key: string, params: Record<string, string>): Promise<string> => {
    return tWithParamsFunc(key, params, locale);
  }, [locale]);

  const value: I18nContextValue = {
    locale,
    setLocale,
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
 * useI18n Hook - 获取 i18n 上下文
 *
 * @example
 * ```tsx
 * const { locale, t, setLocale } = useI18n();
 *
 * return (
 *   <View>
 *     <Text>{t('feedback_title')}</Text>
 *     <Button title="切换语言" onPress={() => setLocale('zh')} />
 *   </View>
 * );
 * ```
 */
export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext);

  if (!context) {
    throw new Error('useI18n 必须在 I18nProvider 内部使用');
  }

  return context;
}
