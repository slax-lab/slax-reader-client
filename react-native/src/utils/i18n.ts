/**
 * React Native i18n工具类
 * 支持多语言动态切换
 */

import { Alert } from 'react-native';
import  { LocaleModule } from '../generated/reaktNativeToolkit/typescript/modules';

// 多语言翻译数据
// 数据结构: { "feedback_title": { "en": "Feedback", "zh": "反馈" } }
let allTranslations: Record<string, Record<string, string>> = {};
let currentLocale: string = 'en';
let isInitialized: boolean = false;
let initPromise: Promise<void> | null = null;

// 语言切换监听器
type LocaleChangeListener = (newLocale: string) => void;
const localeChangeListeners: Set<LocaleChangeListener> = new Set();

/**
 * 订阅语言切换事件
 * @param listener 监听器函数
 * @returns 取消订阅函数
 */
export function subscribeLocaleChange(listener: LocaleChangeListener): () => void {
  localeChangeListeners.add(listener);
  return () => localeChangeListeners.delete(listener);
}

/**
 * 通知所有监听器语言已切换
 */
function notifyLocaleChange(newLocale: string): void {
  localeChangeListeners.forEach(listener => {
    try {
      listener(newLocale);
    } catch (error) {
      console.error('[i18n] 监听器执行失败:', error);
    }
  });
}

/**
 * 初始化 i18n，加载所有语言的翻译数据
 * 此函数具有幂等性，多次调用只会执行一次实际初始化
 */
export async function initI18n(): Promise<void> {
  // 已经初始化完成，直接返回
  if (isInitialized) {
    console.log('[i18n] 已初始化，跳过重复调用');
    return;
  }

  // 正在初始化中，返回同一个 Promise
  if (initPromise) {
    console.log('[i18n] 正在初始化中，等待完成...');
    return initPromise;
  }

  // 开始初始化
  const startTime = Date.now();

  initPromise = (async () => {
    try {
      // 注意：不在此处调用 getCurrentLocale()
      // 因为语言可能在KMP其他页面被切换，需要每次进入RN页面时重新同步
      // 使用 syncCurrentLocale() 在 I18nProvider 初始化时同步最新语言状态
      allTranslations = await LocaleModule.getAllLanguagesStrings();

      const endTime = Date.now();
      const duration = endTime - startTime;

      const translationKeyCount = Object.keys(allTranslations).length;
      const languageCount = translationKeyCount > 0
        ? Object.keys(Object.values(allTranslations)[0] || {}).length
        : 0;

      console.log('[i18n] 初始化完成');
      console.log('[i18n] 已加载翻译键数量:', translationKeyCount);
      console.log('[i18n] 支持语言数量:', languageCount);
      console.log('[i18n] 加载耗时:', duration, 'ms');

      isInitialized = true;

      // 仅在开发环境显示调试信息
      if (__DEV__) {
        Alert.alert(
          'i18n 加载完成',
          `翻译键: ${translationKeyCount}\n支持语言: ${languageCount}\n加载耗时: ${duration}ms\n\n注意：语言将在页面加载时同步`
        );
      }
    } catch (error) {
      const endTime = Date.now();
      const duration = endTime - startTime;

      console.error('[i18n] 初始化失败:', error);
      currentLocale = 'en';
      allTranslations = {};
      isInitialized = false;
      initPromise = null;

      if (__DEV__) {
        Alert.alert(
          'i18n 加载失败',
          `错误: ${error}\n耗时: ${duration}ms`
        );
      }

      throw error;
    }
  })();

  return initPromise;
}

/**
 * 获取翻译文本
 * @param key 翻译键
 * @param locale 可选的语言代码，不传则使用当前语言
 * @returns 翻译后的文本，如果找不到则返回 key
 */
export function t(key: string, locale?: string): string {
  const lang = locale || currentLocale;
  const translation = allTranslations[key];

  if (!translation) {
    return key;
  }

  // 尝试获取指定语言的翻译
  const text = translation[lang];
  if (text) {
    return text;
  }

  // 回退到英语
  const fallback = translation['en'];
  if (fallback) {
    return fallback;
  }

  // 最后返回key
  return key;
}

/**
 * 获取带参数的翻译文本
 * @param key 翻译键
 * @param params 参数对象，如 { email: 'user@example.com' }
 * @param locale 可选的语言代码，不传则使用当前语言
 */
export async function tWithParams(
  key: string,
  params: Record<string, string>,
  locale?: string
): Promise<string> {
  try {
    // 优先使用桥接方法（支持完整的参数格式化）
    return await LocaleModule.getStringWithNamedArgs(key, params);
  } catch (error) {
    console.error(`[i18n] 获取翻译失败: ${key}`, error);

    // 回退到本地翻译 + 简单参数替换
    let text = t(key, locale);
    Object.entries(params).forEach(([paramKey, paramValue]) => {
      text = text.replace(`{${paramKey}}`, paramValue);
    });

    return text;
  }
}

/**
 * 切换语言
 * @param language 语言代码（如 "en", "zh"）
 */
export async function changeLocale(language: string): Promise<void> {
  if (language === currentLocale) {
    console.log('[i18n] 语言未变化，跳过切换');
    return;
  }

  try {
    // 调用KMP桥接方法切换语言
    await LocaleModule.changeLocale(language);

    // 更新本地状态
    currentLocale = language;

    console.log('[i18n] 语言已切换为:', language);

    // 通知所有监听器
    notifyLocaleChange(language);
  } catch (error) {
    console.error('[i18n] 切换语言失败:', error);
    throw error;
  }
}

/**
 * 获取当前语言代码（本地缓存值）
 * @returns 当前语言代码
 */
export function getCurrentLocale(): string {
  return currentLocale;
}

/**
 * 从KMP端同步最新的语言设置
 * 应在每次页面初始化时调用，以确保RN端与KMP端语言一致
 * @returns 同步后的语言代码
 */
export async function syncCurrentLocale(): Promise<string> {
  try {
    const latestLocale = await LocaleModule.getCurrentLocale();

    if (latestLocale !== currentLocale) {
      console.log(`[i18n] 同步语言: ${currentLocale} -> ${latestLocale}`);
      currentLocale = latestLocale;
      // 通知所有监听器语言已变化
      notifyLocaleChange(latestLocale);
    }

    return latestLocale;
  } catch (error) {
    console.error('[i18n] 同步语言失败:', error);
    return currentLocale;
  }
}

/**
 * 获取支持的所有语言代码
 * @returns 语言代码数组
 */
export function getSupportedLocales(): string[] {
  const firstTranslation = Object.values(allTranslations)[0];
  if (!firstTranslation) {
    return [];
  }
  return Object.keys(firstTranslation);
}
