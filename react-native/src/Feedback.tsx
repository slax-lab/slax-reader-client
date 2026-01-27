import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    SafeAreaView,
    Platform,
    StatusBar,
    Image,
    Alert,
    KeyboardAvoidingView
} from 'react-native';
import { FeedbackModule } from './generated/reaktNativeToolkit/typescript/modules';
import { NavigationModule } from "./generated/reaktNativeToolkit/typescript/modules";
import type { com } from './generated/reaktNativeToolkit/typescript/models';
import { useTranslation } from 'react-i18next';
import { getImageSource} from "./resouces";

interface FeedbackProps {
    title?: string;
    href?: string;
    email?: string;
    bookmarkId?: string;
    entryPoint?: string;
    version: string;
}

const FeedbackPage: React.FC<FeedbackProps> = (props: FeedbackProps) => {
    const { title, href, email, bookmarkId, entryPoint, version } = props;
    const { t } = useTranslation();
    const [feedbackText, setFeedbackText] = useState<string>('');
    const [allowFollowUp, setAllowFollowUp] = useState<boolean>(true);
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    const handleSubmit = () => {
        if (feedbackText.trim() === '' || isSubmitting) {
            return;
        }

        const environment = Platform.OS === 'ios'
            ? `iOS ${Platform.Version}`
            : `Android ${Platform.Version}`;

        const feedbackParams: com.slax.reader.data.network.dto.FeedbackParams = {
            bookmark_uuid: bookmarkId?.toString() ?? '',
            entry_point: entryPoint ?? '',
            type: 'parse_error',
            content: feedbackText.trim(),
            platform: 'app',
            environment: environment,
            version,
            target_url: href ?? '',
            allow_follow_up: allowFollowUp
        };

        setIsSubmitting(true);

        FeedbackModule.sendFeedback(feedbackParams).then(res => {
            Alert.alert(
                t('feedback:success_title'),
                t('feedback:success_message'),
                [{ text: t('common:btn_ok'), onPress: () => handleBack() }]
            );
        }).catch(e => {
            console.error('提交反馈失败:', e);
            Alert.alert(
                t('feedback:error_title'),
                `${t('feedback:error_message')} ${e}`
            );
        }).finally(() => {
            setIsSubmitting(false);
        });
    };

    const handleBack = () => {
        NavigationModule.goBack()
    };

    const toggleFollowUp = () => {
        setAllowFollowUp(!allowFollowUp);
    };

    const isSubmitEnabled = feedbackText.trim().length > 0 && !isSubmitting;

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView
                behavior="padding"
                style={styles.keyboardAvoidingView}
            >
                <View style={styles.mainContainer}>
                <View style={styles.header}>
                    <TouchableOpacity
                        style={styles.backButton}
                        onPress={handleBack}
                        activeOpacity={0.7}
                    >
                        <Image
                            source={getImageSource('ic_nav_back')}
                            style={styles.backIcon}
                        />
                    </TouchableOpacity>

                    <Text style={styles.headerTitle}>{t('feedback:title')}</Text>

                    <TouchableOpacity
                        style={styles.submitButton}
                        onPress={handleSubmit}
                        activeOpacity={0.7}
                        disabled={!isSubmitEnabled}
                    >
                        <Text style={[
                            styles.submitButtonText,
                            !isSubmitEnabled && styles.submitButtonTextDisabled
                        ]}>
                            {isSubmitting ? t('feedback:submitting') : t('feedback:submit')}
                        </Text>
                    </TouchableOpacity>
                </View>

                {title && (
                    <View style={styles.contextArea}>
                        <Text style={styles.articleTitle} numberOfLines={1}>
                            {title}
                        </Text>
                        <Text style={styles.articleUrl} numberOfLines={1}>
                            {href}
                        </Text>
                    </View>
                )}

                <View style={styles.inputContainer}>
                    <TextInput
                        style={styles.textInput}
                        value={feedbackText}
                        onChangeText={setFeedbackText}
                        placeholder={t('feedback:placeholder')}
                        placeholderTextColor="rgba(0, 0, 0, 0.3)"
                        multiline={true}
                        textAlignVertical="top"
                    />
                </View>

                <View style={styles.footer}>
                    <TouchableOpacity
                        style={styles.checkboxContainer}
                        onPress={toggleFollowUp}
                        activeOpacity={0.7}
                    >
                        <Image
                            source={allowFollowUp
                                ? getImageSource('ic_agree_enable')
                                : getImageSource('ic_agree_disable')
                            }
                            style={styles.checkbox}
                        />
                        <Text style={styles.footerText}>
                            {t('feedback:allow_follow_up', { email: email || '' })}
                        </Text>
                    </TouchableOpacity>
                </View>
            </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F3FF',
        paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 0,
    },
    keyboardAvoidingView: {
        flex: 1,
    },
    mainContainer: {
        flex: 1,
        flexDirection: 'column',
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingVertical: 10,
    },
    backButton: {
        width: 40,
        height: 40,
        justifyContent: 'center',
        alignItems: 'flex-start',
    },
    backIcon: {
        width: 24,
        height: 24,
    },
    headerTitle: {
        fontSize: 17,
        fontWeight: '600',
        lineHeight: 24,
        color: '#0F1419',
    },
    submitButton: {
        paddingHorizontal: 0,
        paddingVertical: 8,
        backgroundColor: 'transparent',
        borderRadius: 8
    },
    submitButtonText: {
        color: '#16B998',
        fontSize: 14,
        fontWeight: '600',
    },
    submitButtonTextDisabled: {
        color: '#999999',
    },
    contextArea: {
        paddingHorizontal: 12,
        paddingVertical: 20,
    },
    articleTitle: {
        fontSize: 16,
        fontWeight: '500',
        lineHeight: 22,
        color: '#0F1419',
        marginBottom: 4,
    },
    articleUrl: {
        fontSize: 14,
        lineHeight: 20,
        color: '#5490C2',
    },
    inputContainer: {
        flex: 1,
        marginHorizontal: 12,
        marginVertical: 0,
        backgroundColor: '#FCFCFCFF',
        borderRadius: 16,
        paddingVertical: 20,
        paddingHorizontal: 16,
        borderColor: '#0F14190F',
        borderWidth: 1,
        minHeight: 120,
    },
    textInput: {
        flex: 1,
        fontSize: 15,
        lineHeight: 22.5,
        color: '#333',
        padding: 0,
        textAlignVertical: 'top',
    },
    footer: {
        padding: 20
    },
    checkboxContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
    },
    checkbox: {
        width: 12,
        height: 12,
    },
    footerText: {
        marginLeft: 4,
        fontSize: 14,
        lineHeight: 20,
        color: '#333333CC',
    },
});

export default FeedbackPage;
