/**
 * React Native i18n 工具类
 */

import  { LocaleModule } from '../generated/reaktNativeToolkit/typescript/modules';

let allTranslations: Record<string, Record<string, string>> = {};
let currentLocale: string = 'en';
let isInitialized: boolean = false;
let initPromise: Promise<void> | null = null;

type LocaleChangeListener = (newLocale: string) => void;
const localeChangeListeners: Set<LocaleChangeListener> = new Set();

// 翻译缓存相关状态
type TranslationChangeListener = () => void;
const translationListeners: Set<TranslationChangeListener> = new Set();
const translationCache: Record<string, string> = {}; // 缓存翻译结果
const pendingKeys: Set<string> = new Set(); // 正在请求中的key

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
 * 订阅翻译更新事件
 * @param listener 监听器函数
 * @returns 取消订阅函数
 */
export function subscribeTranslationChange(listener: TranslationChangeListener): () => void {
  translationListeners.add(listener);
  return () => translationListeners.delete(listener);
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
 * 通知所有翻译监听器数据已更新
 */
function notifyTranslationChange(): void {
  translationListeners.forEach(listener => {
    try {
      listener();
    } catch (error) {
      console.error('[i18n] 翻译监听器执行失败:', error);
    }
  });
}

/**
 * 初始化 i18n，加载所有语言的翻译数据
 */
export async function initI18n(): Promise<void> {
  if (isInitialized) {
    console.log('[i18n] 已初始化，跳过重复调用');
    return;
  }

  if (initPromise) {
    console.log('[i18n] 正在初始化中，等待完成...');
    return initPromise;
  }

  initPromise = (async () => {
    try {
      if (!allTranslations || !Object.keys(allTranslations).length) {
      }
      allTranslations = await LocaleModule.getAllLanguagesStrings();
      isInitialized = true;

      console.log('[i18n] 初始化完成');
    } catch (error) {
      console.error('[i18n] 初始化失败:', error);
      currentLocale = 'en';
      allTranslations = {};
      isInitialized = false;
      initPromise = null;

      throw error;
    }
  })();

  return initPromise;
}

/**
 * 获取本地翻译（内部辅助函数）
 * @param key 翻译键
 * @param locale 可选的语言，不传则使用当前语言
 * @returns 本地翻译文本
 */
function getLocalTranslation(key: string, locale?: string): string {
  const lang = locale || currentLocale;
  const translation = allTranslations[key];

  if (!translation) {
    return key;
  }

  const text = translation[lang];
  if (text) {
    return text;
  }

  const fallback = translation['en'];
  if (fallback) {
    return fallback;
  }

  return key;
}

/**
 * 获取翻译文本
 *
 * @param key 翻译键
 * @returns 翻译文本
 */
export function t(key: string): string {
  if (translationCache[key] !== undefined) {
    return translationCache[key];
  }

  const fallbackValue = getLocalTranslation(key);
  if (!pendingKeys.has(key)) {
    pendingKeys.add(key);

    LocaleModule.getString(key)
      .then(value => {
        translationCache[key] = value;
        notifyTranslationChange();
      })
      .catch(error => {
        console.error(`[i18n] 翻译获取失败: ${key}`, error);
        translationCache[key] = fallbackValue;
      })
      .finally(() => {
        pendingKeys.delete(key);
      });
  }

  return fallbackValue;
}

/**
 * 获取带参数的翻译文本
 * @param key 翻译键
 * @param params 参数对象，如 { email: 'user@example.com' }
 * @param locale 可选的语言，不传则使用当前语言
 */
export async function tWithParams(
  key: string,
  params: Record<string, string>,
  locale?: string
): Promise<string> {
  try {
    let text = getLocalTranslation(key, locale);
    Object.entries(params).forEach(([paramKey, paramValue]) => {
      text = text.replace(`{${paramKey}}`, paramValue);
    });

    return text
  } catch (error) {
    console.error(`[i18n] 获取翻译失败: ${key}`, error);
    return await LocaleModule.getStringWithNamedArgs(key, params);
  }
}

/**
 * 切换语言
 * @param language 语言（如 "en", "zh"）
 */
export async function changeLocale(language: string): Promise<void> {
  if (language === currentLocale) {
    return;
  }

  try {
    await LocaleModule.changeLocale(language);
    currentLocale = language;
    clearTranslationCache();
    notifyLocaleChange(language);
  } catch (error) {
    console.error('[i18n] 切换语言失败:', error);
    throw error;
  }
}

/**
 * 获取当前本地缓存语言
 * @returns 当前语言
 */
export function getCurrentLocale(): string {
  return currentLocale;
}

/**
 * 同步最新的语言设置
 * @returns 同步后的语言
 */
export async function syncCurrentLocale(): Promise<string> {
  try {
    const latestLocale = await LocaleModule.getCurrentLocale();

    if (latestLocale !== currentLocale) {
      currentLocale = latestLocale;

      clearTranslationCache();
      notifyLocaleChange(latestLocale);
    }

    return latestLocale;
  } catch (error) {
    console.error('[i18n] 同步语言失败:', error);
    return currentLocale;
  }
}

/**
 * 获取支持的所有语言
 * @returns 语言数组
 */
export function getSupportedLocales(): string[] {
  const firstTranslation = Object.values(allTranslations)[0];
  if (!firstTranslation) {
    return [];
  }
  return Object.keys(firstTranslation);
}

/**
 * 清空翻译缓存（在语言切换时调用）
 */
export function clearTranslationCache(): void {
  Object.keys(translationCache).forEach(key => delete translationCache[key]);
  console.log('[i18n] 翻译缓存已清空');
}
