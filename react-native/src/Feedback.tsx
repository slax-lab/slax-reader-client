import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    SafeAreaView,
    KeyboardAvoidingView,
    Platform,
    StatusBar,
    Image,
} from 'react-native';

interface FeedbackProps {
    articleTitle?: string;
    articleUrl?: string;
    userEmail?: string;
    onSubmit?: (feedback: FeedbackData) => void;
    onBack?: () => void;
}

interface FeedbackData {
    feedbackText: string;
    allowFollowUp: boolean;
    userEmail?: string;
}

const FeedbackPage: React.FC<FeedbackProps> = ({
    articleTitle = '中文数据占比突破80%！国产大模型加速去...',
    articleUrl = 'https://www.pinterest.com/',
    userEmail = 'user@example.com',
    onSubmit,
    onBack,
}) => {
    const [feedbackText, setFeedbackText] = useState<string>('');
    const [allowFollowUp, setAllowFollowUp] = useState<boolean>(true);

    const handleSubmit = () => {
        if (feedbackText.trim() === '') {
            return;
        }

        const data: FeedbackData = {
            feedbackText: feedbackText.trim(),
            allowFollowUp,
            userEmail: allowFollowUp ? userEmail : undefined,
        };

        onSubmit?.(data);
    };

    const handleBack = () => {
        onBack?.();
    };

    const toggleFollowUp = () => {
        setAllowFollowUp(!allowFollowUp);
    };

    const isSubmitEnabled = feedbackText.trim().length > 0;

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView
                style={styles.keyboardAvoidingView}
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}
            >
                <View style={styles.mainContainer}>
                    <View style={styles.header}>
                        <TouchableOpacity
                            style={styles.backButton}
                            onPress={handleBack}
                            activeOpacity={0.7}
                        >
                            <Image
                                source={require('./assets/images/ic_nav_back.png')}
                                style={styles.backIcon}
                            />
                        </TouchableOpacity>

                        <Text style={styles.headerTitle}>Feedback</Text>

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
                                提交
                            </Text>
                        </TouchableOpacity>
                    </View>

                    <View style={styles.contextArea}>
                        <Text style={styles.articleTitle} numberOfLines={1}>
                            {articleTitle}
                        </Text>
                        <Text style={styles.articleUrl} numberOfLines={1}>
                            {articleUrl}
                        </Text>
                    </View>

                    <View style={styles.inputContainer}>
                        <TextInput
                            style={styles.textInput}
                            value={feedbackText}
                            onChangeText={setFeedbackText}
                            placeholder="请输入您的反馈..."
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
                                    ? require('./assets/images/ic_agree_enable.png')
                                    : require('./assets/images/ic_agree_disable.png')
                                }
                                style={styles.checkbox}
                            />
                            <Text style={styles.footerText}>
                                Allow follow-up at {userEmail}
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
