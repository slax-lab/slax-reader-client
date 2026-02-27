type Translations = Record<string, Record<string, string>>;

const translations: Translations = {
  'feedback_title': { zh: '反馈', en: 'Feedback' },
  'feedback_submit': { zh: '提交', en: 'Submit' },
  'feedback_submitting': { zh: '提交中...', en: 'Submitting...' },
  'feedback_placeholder': { zh: '请输入您的反馈...', en: 'Please enter your feedback...' },
  'feedback_allow_follow_up': { zh: '允许通过 {email} 进行跟进', en: 'Allow follow-up at {email}' },
  'feedback_success_title': { zh: '成功', en: 'Success' },
  'feedback_success_message': { zh: '反馈已提交，感谢您的反馈！', en: 'Feedback submitted. Thank you!' },
  'feedback_error_title': { zh: '错误', en: 'Error' },
  'feedback_error_message': { zh: '提交反馈失败，请稍后重试', en: 'Failed to submit feedback. Please try again later.' },
  'btn_ok': { zh: '好的', en: 'OK' },
};

const FALLBACK_LOCALE = 'en';

export function t(key: string, locale: string, args?: Record<string, string>): string {
  let text = translations[key]?.[locale] ?? translations[key]?.[FALLBACK_LOCALE] ?? key;
  if (args) {
    Object.entries(args).forEach(([k, v]) => {
      text = text.replace(`{${k}}`, v);
    });
  }
  return text;
}
