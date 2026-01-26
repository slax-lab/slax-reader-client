/**
 * React Native i18n工具类
 */

import { Alert } from 'react-native';
import  { LocaleModule } from '../generated/reaktNativeToolkit/typescript/modules';

// 翻译缓存
let translationCache: Record<string, string> = {};
let currentLocale: string = 'en';

/**
 * 初始化 i18n，批量加载所有翻译
 */
export async function initI18n(): Promise<void> {
  const startTime = Date.now();

  try {
    currentLocale = await LocaleModule.getCurrentLocale();
    translationCache = await LocaleModule.getAllStrings();

    const endTime = Date.now();
    const duration = endTime - startTime;

    console.log('[i18n] 初始化完成，当前语言:', currentLocale);
    console.log('[i18n] 已缓存翻译数量:', Object.keys(translationCache).length);
    console.log('[i18n] 加载耗时:', duration, 'ms');

    Alert.alert(
      'i18n 加载完成',
      `语言: ${currentLocale}\n翻译数量: ${Object.keys(translationCache).length}\n加载耗时: ${duration}ms`
    );
  } catch (error) {
    const endTime = Date.now();
    const duration = endTime - startTime;

    console.error('[i18n] 初始化失败:', error);
    currentLocale = 'en';
    translationCache = {};

    Alert.alert(
      'i18n 加载失败',
      `错误: ${error}\n耗时: ${duration}ms`
    );
  }
}

/**
 * 获取翻译文本
 */
export function t(key: string): string {
  return translationCache[key] || key;
}

/**
 * 获取带参数的翻译文本
 * @param key 翻译键
 * @param params 参数对象，如 { email: 'user@example.com' }
 */
export async function tWithParams(
  key: string,
  params: Record<string, string>
): Promise<string> {
  try {

    return await LocaleModule.getStringWithNamedArgs(key, params);
  } catch (error) {
    console.error(`[i18n] 获取翻译失败: ${key}`, error);

    let text = translationCache[key] || key;
    Object.entries(params).forEach(([paramKey, paramValue]) => {
      text = text.replace(`{${paramKey}}`, paramValue);
    });

    return text;
  }
}
