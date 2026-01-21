import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
    View,
    Text,
    ScrollView,
    TouchableOpacity,
    StyleSheet,
    Animated,
    Easing, SafeAreaView,
} from 'react-native';
import Markdown from 'react-native-markdown-display';

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
}

const PRESET_MESSAGES: ChatMessage[] = [
    {
        role: 'user',
        content: '你好！请介绍一下你自己。',
    },
    {
        role: 'assistant',
        content:
            '你好！我是一个AI助手，很高兴认识你。我可以帮助你完成各种任务，比如回答问题、写代码、创作内容等。有什么我可以帮你的吗？',
    },
    {
        role: 'user',
        content: '能帮我写一个React组件吗？',
    },
    {
        role: 'assistant',
        content: `当然可以！这是一个简单的按钮组件示例：

\`\`\`jsx
const Button = ({ children, onPress }) => {
  return (
    <TouchableOpacity
      onPress={onPress}
      style={{
        padding: 12,
        borderRadius: 8,
        backgroundColor: '#5856D6',
      }}
    >
      <Text style={{ color: '#FFF' }}>
        {children}
      </Text>
    </TouchableOpacity>
  );
};
\`\`\`

这个组件支持自定义内容和点击事件！`,
    },
    {
        role: 'user',
        content: '太棒了！还有其他建议吗？',
    },
    {
        role: 'assistant',
        content: `当然！以下是一些提升组件质量的建议：

1. **添加TypeScript类型** - 提高代码可维护性
2. **使用StyleSheet** - 更好的性能优化
3. **添加动画效果** - 提升用户体验
4. **考虑无障碍访问** - 添加accessibility属性
5. **编写单元测试** - 确保组件稳定性

需要我详细展开其中任何一点吗？`,
    },
];

const BlinkingCursor: React.FC = () => {
    const opacity = useRef(new Animated.Value(1)).current;

    useEffect(() => {
        const animation = Animated.loop(
            Animated.sequence([
                Animated.timing(opacity, {
                    toValue: 0,
                    duration: 500,
                    easing: Easing.linear,
                    useNativeDriver: true,
                }),
                Animated.timing(opacity, {
                    toValue: 1,
                    duration: 500,
                    easing: Easing.linear,
                    useNativeDriver: true,
                }),
            ])
        );
        animation.start();
        return () => animation.stop();
    }, [opacity]);

    return (
        <Animated.Text style={[styles.cursor, { opacity }]}>▊</Animated.Text>
    );
};

const userMarkdownStyles = StyleSheet.create({
    body: {
        color: '#FFFFFF',
        fontSize: 15,
        lineHeight: 22,
    },
    paragraph: {
        marginVertical: 0,
    },
});

const assistantMarkdownStyles = StyleSheet.create({
    body: {
        color: '#E4E4E7',
        fontSize: 15,
        lineHeight: 24,
    },
    paragraph: {
        marginVertical: 4,
    },
    strong: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
    code_inline: {
        backgroundColor: 'rgba(88, 86, 214, 0.3)',
        color: '#C9B1FF',
        paddingHorizontal: 6,
        paddingVertical: 2,
        borderRadius: 4,
        fontFamily: 'Menlo',
        fontSize: 13,
    },
    fence: {
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        padding: 12,
        borderRadius: 8,
        marginVertical: 8,
    },
    code_block: {
        fontFamily: 'Menlo',
        fontSize: 12,
        color: '#A5D6FF',
    },
    list_item: {
        marginVertical: 2,
    },
    ordered_list: {
        marginVertical: 8,
    },
    bullet_list: {
        marginVertical: 8,
    },
});

interface MessageBubbleProps {
    message: ChatMessage;
    isStreaming?: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({
                                                         message,
                                                         isStreaming = false,
                                                     }) => {
    const isUser = message.role === 'user';

    return (
        <View
            style={[
                styles.messageRow,
                isUser ? styles.messageRowUser : styles.messageRowAssistant,
            ]}
        >
            {!isUser && (
                <View style={styles.avatar}>
                    <Text style={styles.avatarText}>🤖</Text>
                </View>
            )}
            <View
                style={[
                    styles.messageBubble,
                    isUser ? styles.userBubble : styles.assistantBubble,
                ]}
            >
                <Markdown style={isUser ? userMarkdownStyles : assistantMarkdownStyles}>
                    {message.content}
                </Markdown>
                {isStreaming && <BlinkingCursor />}
            </View>
            {isUser && (
                <View style={styles.avatar}>
                    <Text style={styles.avatarText}>👤</Text>
                </View>
            )}
        </View>
    );
};

const ChatPage: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [currentStreamingMessage, setCurrentStreamingMessage] =
        useState<ChatMessage | null>(null);
    const [isStreaming, setIsStreaming] = useState<boolean>(false);
    const scrollViewRef = useRef<ScrollView>(null);
    const streamingRef = useRef<boolean>(false);
    const streamingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const scrollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const streamNextMessage = useCallback(
        (index: number, currentMessages: ChatMessage[]) => {
            if (!streamingRef.current || index >= PRESET_MESSAGES.length) {
                setIsStreaming(false);
                streamingRef.current = false;
                setCurrentStreamingMessage(null);
                return;
            }

            const msg = PRESET_MESSAGES[index];

            if (msg.role === 'user') {
                const newMessages = [...currentMessages, msg];
                setMessages(newMessages);
                if (streamingTimeoutRef.current) {
                    clearTimeout(streamingTimeoutRef.current);
                }
                streamingTimeoutRef.current = setTimeout(
                    () => streamNextMessage(index + 1, newMessages),
                    600
                );
            } else {
                let charIndex = 0;
                setCurrentStreamingMessage({ role: 'assistant', content: '' });

                const stream = () => {
                    if (!streamingRef.current) return;

                    if (charIndex < msg.content.length) {
                        const charsToAdd = Math.random() > 0.8 ? 4 : 2;
                        const nextIndex = Math.min(charIndex + charsToAdd, msg.content.length);
                        setCurrentStreamingMessage({
                            role: 'assistant',
                            content: msg.content.slice(0, nextIndex),
                        });
                        charIndex = nextIndex;
                        if (streamingTimeoutRef.current) {
                            clearTimeout(streamingTimeoutRef.current);
                        }
                        streamingTimeoutRef.current = setTimeout(stream, 20);
                    } else {
                        const newMessages = [...currentMessages, msg];
                        setMessages(newMessages);
                        setCurrentStreamingMessage(null);
                        if (streamingTimeoutRef.current) {
                            clearTimeout(streamingTimeoutRef.current);
                        }
                        streamingTimeoutRef.current = setTimeout(
                            () => streamNextMessage(index + 1, newMessages),
                            800
                        );
                    }
                };

                stream();
            }
        },
        []
    );

    const startChat = useCallback(() => {
        setMessages([]);
        setCurrentStreamingMessage(null);
        setIsStreaming(true);
        streamingRef.current = true;
        streamNextMessage(0, []);
    }, [streamNextMessage]);

    const stopChat = useCallback(() => {
        streamingRef.current = false;
        if (streamingTimeoutRef.current) {
            clearTimeout(streamingTimeoutRef.current);
            streamingTimeoutRef.current = null;
        }
        setIsStreaming(false);
        setCurrentStreamingMessage(null);
    }, []);

    useEffect(() => {
        if (scrollViewRef.current) {
            if (scrollTimeoutRef.current) {
                clearTimeout(scrollTimeoutRef.current);
            }
            scrollTimeoutRef.current = setTimeout(() => {
                scrollViewRef.current?.scrollToEnd({ animated: true });
            }, 100);
        }
    }, [messages, currentStreamingMessage]);

    useEffect(() => {
        return () => {
            streamingRef.current = false;
            if (streamingTimeoutRef.current) {
                clearTimeout(streamingTimeoutRef.current);
            }
            if (scrollTimeoutRef.current) {
                clearTimeout(scrollTimeoutRef.current);
            }
        };
    }, []);

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.title}>💬 Chat 流式对话</Text>
                <TouchableOpacity
                    style={[styles.button, isStreaming && styles.buttonStop]}
                    onPress={isStreaming ? stopChat : startChat}
                    activeOpacity={0.7}
                >
                    <Text style={styles.buttonText}>
                        {isStreaming ? '停止' : '开始演示'}
                    </Text>
                </TouchableOpacity>
            </View>

            <ScrollView
                ref={scrollViewRef}
                style={styles.chatArea}
                contentContainerStyle={styles.chatContainer}
                showsVerticalScrollIndicator={false}
            >
                {messages.length === 0 && !currentStreamingMessage ? (
                    <View style={styles.placeholder}>
                        <Text style={styles.placeholderIcon}>💭</Text>
                        <Text style={styles.placeholderText}>
                            点击上方按钮开始对话演示
                        </Text>
                    </View>
                ) : (
                    <>
                        {messages.map((msg, index) => (
                            <MessageBubble key={index} message={msg} />
                        ))}
                        {currentStreamingMessage && (
                            <MessageBubble
                                message={currentStreamingMessage}
                                isStreaming={true}
                            />
                        )}
                    </>
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0A0A0F',
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(255, 255, 255, 0.08)',
    },
    title: {
        fontSize: 20,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    button: {
        paddingHorizontal: 20,
        paddingVertical: 10,
        backgroundColor: '#5856D6',
        borderRadius: 10,
    },
    buttonStop: {
        backgroundColor: '#EF4444',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 14,
        fontWeight: '600',
    },
    chatArea: {
        flex: 1,
        margin: 16,
        backgroundColor: 'rgba(255, 255, 255, 0.02)',
        borderRadius: 16,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
    },
    chatContainer: {
        padding: 16,
        flexGrow: 1,
    },
    placeholder: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: 400,
    },
    placeholderIcon: {
        fontSize: 48,
        marginBottom: 16,
        opacity: 0.5,
    },
    placeholderText: {
        color: 'rgba(255, 255, 255, 0.3)',
        fontSize: 16,
    },
    messageRow: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        marginVertical: 8,
    },
    messageRowUser: {
        justifyContent: 'flex-end',
    },
    messageRowAssistant: {
        justifyContent: 'flex-start',
    },
    avatar: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: 'rgba(255, 255, 255, 0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginHorizontal: 8,
    },
    avatarText: {
        fontSize: 18,
    },
    messageBubble: {
        maxWidth: '75%',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 16,
    },
    userBubble: {
        backgroundColor: '#5856D6',
        borderBottomRightRadius: 4,
    },
    assistantBubble: {
        backgroundColor: 'rgba(255, 255, 255, 0.08)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
        borderBottomLeftRadius: 4,
    },
    cursor: {
        color: '#5856D6',
        fontSize: 16,
        marginLeft: 2,
    },
});

export default ChatPage;
