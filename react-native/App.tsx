import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
} from 'react-native';
import {TestModule} from './src/generated/reaktNativeToolkit/typescript/modules';

function App(): React.JSX.Element {
  const [greeting, setGreeting] = useState<string>('');
  const [sum, setSum] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const callHello = async () => {
    setLoading(true);
    try {
      const result = await TestModule.hello();
      setGreeting(result);
    } catch (error) {
      console.error('Error calling hello:', error);
      setGreeting('Error: ' + error);
    } finally {
      setLoading(false);
    }
  };

  const callAdd = async () => {
    setLoading(true);
    try {
      const result = await TestModule.add(5, 7);
      setSum(result);
    } catch (error) {
      console.error('Error calling add:', error);
      setSum(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // 自动调用一次
    callHello();
    callAdd();
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentInsetAdjustmentBehavior="automatic">
        <View style={styles.content}>
          <Text style={styles.title}>KMP + React Native 集成成功! 🎉</Text>
          <Text style={styles.subtitle}>
            通过 reakt-native-toolkit 调用 Kotlin Multiplatform 代码
          </Text>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>📱 TestModule.hello()</Text>
            <Text style={styles.label}>Kotlin 返回:</Text>
            <Text style={styles.result}>{greeting || '加载中...'}</Text>
            <TouchableOpacity
              style={styles.button}
              onPress={callHello}
              disabled={loading}>
              <Text style={styles.buttonText}>重新调用 hello()</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>🔢 TestModule.add(5, 7)</Text>
            <Text style={styles.label}>Kotlin 计算结果:</Text>
            <Text style={styles.result}>
              {sum !== null ? `5 + 7 = ${sum}` : '加载中...'}
            </Text>
            <TouchableOpacity
              style={styles.button}
              onPress={callAdd}
              disabled={loading}>
              <Text style={styles.buttonText}>重新计算</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.infoCard}>
            <Text style={styles.infoTitle}>✨ 集成说明</Text>
            <Text style={styles.infoText}>
              • Kotlin 代码在 commonMain 中定义{'\n'}
              • 使用 @ReactNativeModule 注解{'\n'}
              • KSP 自动生成 TypeScript 类型{'\n'}
              • React Native 直接调用 Promise API
            </Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 30,
  },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 15,
    color: '#333',
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
  result: {
    fontSize: 18,
    color: '#007AFF',
    fontWeight: '600',
    marginBottom: 15,
    padding: 10,
    backgroundColor: '#f0f8ff',
    borderRadius: 8,
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  infoCard: {
    backgroundColor: '#e8f5e9',
    borderRadius: 12,
    padding: 20,
    marginTop: 10,
  },
  infoTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10,
    color: '#2e7d32',
  },
  infoText: {
    fontSize: 14,
    color: '#1b5e20',
    lineHeight: 22,
  },
});

export default App;
