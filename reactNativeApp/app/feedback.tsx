import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  SafeAreaView, Platform, StatusBar, Image, Alert,
  KeyboardAvoidingView,
} from 'react-native';
import { popToNative } from 'expo-brownfield';
import { t } from '@/constants/i18n';
import { invokeNative } from '@/modules/slax-bridge/src';
import { getInitialProps } from '@/constants/initial-props';

export default function FeedbackPage() {
  const { title, href, email, bookmarkId, entryPoint, version, language } = getInitialProps();

  const lang = language ?? 'en';

  const [feedbackText, setFeedbackText] = useState('');
  const [allowFollowUp, setAllowFollowUp] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (feedbackText.trim() === '' || isSubmitting) return;

    const environment = Platform.OS === 'ios'
      ? `iOS ${Platform.Version}`
      : `Android ${Platform.Version}`;

    setIsSubmitting(true);
    try {
      await invokeNative('feedback.submit', {
        bookmark_uuid: bookmarkId ?? '',
        entry_point: entryPoint ?? '',
        type: 'parse_error',
        content: feedbackText.trim(),
        platform: 'app',
        environment,
        version: version ?? '',
        target_url: href ?? '',
        allow_follow_up: allowFollowUp,
      });

      Alert.alert(
        t('feedback_success_title', lang),
        t('feedback_success_message', lang),
        [{ text: t('btn_ok', lang), onPress: () => handleBack() }],
      );
    } catch (e: any) {
      Alert.alert(
        t('feedback_error_title', lang),
        `${t('feedback_error_message', lang)} ${e.message ?? ''}`,
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => popToNative(true);
  const isSubmitEnabled = feedbackText.trim().length > 0 && !isSubmitting;

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView behavior="padding" style={styles.keyboardAvoidingView}>
        <View style={styles.mainContainer}>
          <View style={styles.header}>
            <TouchableOpacity style={styles.backButton} onPress={handleBack} activeOpacity={0.7}>
              <Image source={require('@/assets/images/ic_nav_back.png')} style={styles.backIcon} />
            </TouchableOpacity>
            <Text style={styles.headerTitle}>{t('feedback_title', lang)}</Text>
            <TouchableOpacity
              style={styles.submitButton}
              onPress={handleSubmit}
              activeOpacity={0.7}
              disabled={!isSubmitEnabled}
            >
              <Text style={[styles.submitButtonText, !isSubmitEnabled && styles.submitButtonTextDisabled]}>
                {isSubmitting ? t('feedback_submitting', lang) : t('feedback_submit', lang)}
              </Text>
            </TouchableOpacity>
          </View>

          {title ? (
            <View style={styles.contextArea}>
              <Text style={styles.articleTitle} numberOfLines={1}>{title}</Text>
              <Text style={styles.articleUrl} numberOfLines={1}>{href}</Text>
            </View>
          ) : null}

          <View style={styles.inputContainer}>
            <TextInput
              style={styles.textInput}
              value={feedbackText}
              onChangeText={setFeedbackText}
              placeholder={t('feedback_placeholder', lang)}
              placeholderTextColor="rgba(0, 0, 0, 0.3)"
              multiline
              textAlignVertical="top"
            />
          </View>

          <View style={styles.footer}>
            <TouchableOpacity
              style={styles.checkboxContainer}
              onPress={() => setAllowFollowUp(!allowFollowUp)}
              activeOpacity={0.7}
            >
              <Image
                source={
                  allowFollowUp
                    ? require('@/assets/images/ic_agree_enable.png')
                    : require('@/assets/images/ic_agree_disable.png')
                }
                style={styles.checkbox}
              />
              <Text style={styles.footerText}>
                {t('feedback_allow_follow_up', lang, { email: email ?? '' })}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F3',
    paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 0,
  },
  keyboardAvoidingView: { flex: 1 },
  mainContainer: { flex: 1, flexDirection: 'column' },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  backButton: { width: 40, height: 40, justifyContent: 'center', alignItems: 'flex-start' },
  backIcon: { width: 24, height: 24 },
  headerTitle: { fontSize: 17, fontWeight: '600', lineHeight: 24, color: '#0F1419' },
  submitButton: { paddingVertical: 8, backgroundColor: 'transparent', borderRadius: 8 },
  submitButtonText: { color: '#16B998', fontSize: 14, fontWeight: '600' },
  submitButtonTextDisabled: { color: '#999999' },
  contextArea: { paddingHorizontal: 12, paddingVertical: 20 },
  articleTitle: { fontSize: 16, fontWeight: '500', lineHeight: 22, color: '#0F1419', marginBottom: 4 },
  articleUrl: { fontSize: 14, lineHeight: 20, color: '#5490C2' },
  inputContainer: {
    flex: 1,
    marginHorizontal: 12,
    backgroundColor: '#FCFCFC',
    borderRadius: 16,
    paddingVertical: 20,
    paddingHorizontal: 16,
    borderColor: '#0F14190F',
    borderWidth: 1,
    minHeight: 120,
  },
  textInput: { flex: 1, fontSize: 15, lineHeight: 22.5, color: '#333', padding: 0, textAlignVertical: 'top' },
  footer: { padding: 20 },
  checkboxContainer: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center' },
  checkbox: { width: 12, height: 12 },
  footerText: { marginLeft: 4, fontSize: 14, lineHeight: 20, color: '#333333CC' },
});
