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
import { TestModule } from "./generated/reaktNativeToolkit/typescript/modules.ts";
import Markdown from 'react-native-markdown-display';

// 预设的 Markdown 内容
const MARKDOWN_CONTENT = `# 🌟 欢迎来到流sss式渲染演示

这是一个**实时流式渲染**的Markdown内容展示。

## 为什么选择流式渲染？

流式渲染提供了以下优势：

1. **更好的用户体验** - 用户无需等待全部内容加载
2. **实时反馈** - 让用户感知到系统正在工作
3. **降低感知延迟** - 首字节时间大大缩短

## 代码示例

\`\`\`javascript
const streamContent = async (text) => {
  for (const char of text) {
    await delay(20);
    setContent(prev => prev + char);
  }
};
\`\`\`

## 特性列表

- ✅ 支持标题渲染
- ✅ 支持**粗体**和*斜体*
- ✅ 支持代码块高亮
- ✅ 支持列表展示

> 💡 **提示**：流式渲染是现代AI应用的标配功能，它让交互变得更加自然流畅。

---

感谢您的观看！🎉`;

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

const markdownStyles = StyleSheet.create({
    body: {
        color: '#E4E4E7',
        fontSize: 16,
        lineHeight: 26,
    },
    heading1: {
        color: '#FFFFFF',
        fontSize: 28,
        fontWeight: '700',
        marginBottom: 16,
        marginTop: 8,
    },
    heading2: {
        color: '#FFFFFF',
        fontSize: 22,
        fontWeight: '600',
        marginBottom: 12,
        marginTop: 24,
    },
    heading3: {
        color: '#FAFAFA',
        fontSize: 18,
        fontWeight: '600',
        marginBottom: 10,
        marginTop: 20,
    },
    paragraph: {
        color: '#D4D4D8',
        marginVertical: 8,
    },
    strong: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
    em: {
        fontStyle: 'italic',
    },
    blockquote: {
        backgroundColor: 'rgba(88, 86, 214, 0.1)',
        borderLeftColor: '#5856D6',
        borderLeftWidth: 3,
        paddingLeft: 16,
        paddingVertical: 8,
        marginVertical: 12,
    },
    code_inline: {
        backgroundColor: 'rgba(88, 86, 214, 0.2)',
        color: '#C9B1FF',
        paddingHorizontal: 6,
        paddingVertical: 2,
        borderRadius: 4,
        fontFamily: 'Menlo',
        fontSize: 14,
    },
    code_block: {
        backgroundColor: 'rgba(0, 0, 0, 0.4)',
        padding: 16,
        borderRadius: 10,
        fontFamily: 'Menlo',
        fontSize: 13,
        color: '#A5D6FF',
    },
    fence: {
        backgroundColor: 'rgba(0, 0, 0, 0.4)',
        padding: 16,
        borderRadius: 10,
        marginVertical: 12,
    },
    list_item: {
        marginVertical: 4,
    },
    bullet_list: {
        marginVertical: 8,
    },
    ordered_list: {
        marginVertical: 8,
    },
    hr: {
        backgroundColor: 'rgba(255, 255, 255, 0.2)',
        height: 1,
        marginVertical: 24,
    },
});

const useCounter = () => {
    const count = TestModule.useCount();

    return {
        count,
        increment: TestModule.increment
    }
}

const MarkdownPage: React.FC = () => {
    const [displayedContent, setDisplayedContent] = useState<string>('');
    const [isStreaming, setIsStreaming] = useState<boolean>(false);
    const [helloMessage, setHelloMessage] = useState<string>('');
    const scrollViewRef = useRef<ScrollView>(null);
    const streamingRef = useRef<boolean>(false);
    const streamingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const scrollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const { count, increment } = useCounter();

    useEffect(() => {
        TestModule.hello().then(setHelloMessage).catch(console.error);
    }, []);

    const startStreaming = useCallback(() => {
        const fullContent = helloMessage
            ? `> 🔗 **来自原生是模块**: ${helloMessage}\n\n${MARKDOWN_CONTENT}`
            : MARKDOWN_CONTENT;

        setDisplayedContent('');
        setIsStreaming(true);
        streamingRef.current = true;
        let index = 0;

        const stream = () => {
            if (!streamingRef.current) return;

            if (index < fullContent.length) {
                const charsToAdd = Math.random() > 0.7 ? 3 : 1;
                const nextIndex = Math.min(index + charsToAdd, fullContent.length);
                setDisplayedContent(fullContent.slice(0, nextIndex));
                index = nextIndex;
                if (streamingTimeoutRef.current) {
                    clearTimeout(streamingTimeoutRef.current);
                }
                streamingTimeoutRef.current = setTimeout(stream, 15);
            } else {
                setIsStreaming(false);
                streamingRef.current = false;
            }
        };

        stream();
    }, [helloMessage]);

    const stopStreaming = useCallback(() => {
        streamingRef.current = false;
        if (streamingTimeoutRef.current) {
            clearTimeout(streamingTimeoutRef.current);
            streamingTimeoutRef.current = null;
        }
        setIsStreaming(false);
    }, []);

    useEffect(() => {
        if (scrollViewRef.current && displayedContent) {
            if (scrollTimeoutRef.current) {
                clearTimeout(scrollTimeoutRef.current);
            }
            scrollTimeoutRef.current = setTimeout(() => {
                scrollViewRef.current?.scrollToEnd({ animated: true });
            }, 0);
        }
    }, [displayedContent]);

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
                <Text style={styles.title}>📝 Markdown 流式渲染</Text>
                <TouchableOpacity
                    style={[styles.button, isStreaming && styles.buttonStop]}
                    onPress={isStreaming ? stopStreaming : startStreaming}
                    activeOpacity={0.7}
                >
                    <Text style={styles.buttonText}>
                        {isStreaming ? '停止' : '开始演示'}
                    </Text>
                </TouchableOpacity>
            </View>

            <View style={styles.counterContainer}>
                <Text style={styles.counterLabel}>Counter 测试:</Text>
                <View style={styles.counterContent}>
                    <Text style={styles.counterValue}>{ count ?? 0 }</Text>
                    <TouchableOpacity
                        style={styles.counterButton}
                        onPress={ increment }
                        activeOpacity={0.7}
                    >
                        <Text style={styles.counterButtonText}>+1</Text>
                    </TouchableOpacity>
                </View>
            </View>

            <ScrollView
                ref={scrollViewRef}
                style={styles.contentArea}
                contentContainerStyle={styles.contentContainer}
                showsVerticalScrollIndicator={false}
            >
                {displayedContent ? (
                    <View>
                        <Markdown style={markdownStyles}>{displayedContent}</Markdown>
                        {isStreaming && <BlinkingCursor />}
                    </View>
                ) : (
                    <View style={styles.placeholder}>
                        <Text style={styles.placeholderIcon}>✨</Text>
                        <Text style={styles.placeholderText}>
                            点击上方按钮开始流式渲染演示
                        </Text>
                        {helloMessage ? (
                            <Markdown style={markdownStyles}>{helloMessage}</Markdown>
                        ) : null}
                    </View>
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
    contentArea: {
        flex: 1,
        margin: 16,
        backgroundColor: 'rgba(255, 255, 255, 0.02)',
        borderRadius: 16,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
    },
    contentContainer: {
        padding: 20,
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
    cursor: {
        color: '#5856D6',
        fontSize: 18,
        marginLeft: 2,
    },
    counterContainer: {
        marginHorizontal: 16,
        marginTop: 12,
        padding: 12,
        backgroundColor: 'rgba(88, 86, 214, 0.1)',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: 'rgba(88, 86, 214, 0.3)',
    },
    counterLabel: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
        marginBottom: 8,
    },
    counterContent: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    counterValue: {
        fontSize: 24,
        fontWeight: '700',
        color: '#5856D6',
    },
    counterButton: {
        paddingHorizontal: 20,
        paddingVertical: 10,
        backgroundColor: '#5856D6',
        borderRadius: 10,
    },
    counterButtonText: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '600',
    },
});

export default MarkdownPage;
