/**
 * i18next 配置文件
 * 配置国际化支持，使用静态 JSON 翻译文件
 * 语言设置从 KMP 端的 LocaleModule 获取
 */

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { LocaleModule } from '../generated/reaktNativeToolkit/typescript/modules';

import enCommon from '../../locales/en/common.json';
import enFeedback from '../../locales/en/feedback.json';
import zhCommon from '../../locales/zh/common.json';
import zhFeedback from '../../locales/zh/feedback.json';

const resources = {
  en: {
    common: enCommon,
    feedback: enFeedback,
  },
  zh: {
    common: zhCommon,
    feedback: zhFeedback,
  },
};

let isInitialized = false;

/**
 * 用于初始化并更新当前国际化语言
 */
export async function initI18n(): Promise<void> {
  try {
    const currentLocale = await LocaleModule.getCurrentLocale();

    if (!isInitialized) {
      await i18n
        .use(initReactI18next)
        .init({
          resources,
          lng: currentLocale,
          fallbackLng: 'en',
          defaultNS: 'common',
          ns: ['common', 'feedback'],
          interpolation: {
            escapeValue: false,
          },
          debug: process.env.NODE_ENV === 'development',
          returnObjects: false,
          saveMissing: false,
          react: {
            useSuspense: false,
          },
        });

      isInitialized = true;
      console.log('[i18n] 初始化完成，当前语言:', currentLocale);
    } else {
      if (i18n.language !== currentLocale) {
        await i18n.changeLanguage(currentLocale);
        console.log('[i18n] 语言已更新:', currentLocale);
      }
    }
  } catch (error) {
    console.error('[i18n] 初始化/更新失败:', error);

    if (!isInitialized) {
      await i18n
        .use(initReactI18next)
        .init({
          resources,
          lng: 'en',
          fallbackLng: 'en',
          defaultNS: 'common',
          ns: ['common', 'feedback'],
          interpolation: {
            escapeValue: false,
          },
          react: {
            useSuspense: false,
          },
        });
      isInitialized = true;
    }
  }
}

/**
 * 切换语言
 */
export async function changeLanguage(language: string): Promise<void> {
  try {
    await i18n.changeLanguage(language);
    await LocaleModule.changeLocale(language);
    console.log('[i18n] 语言已切换:', language);
  } catch (error) {
    console.error('[i18n] 切换语言失败:', error);
    throw error;
  }
}

export default i18n;
