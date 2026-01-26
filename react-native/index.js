import 'react-native-get-random-values'
import React, { useState, useEffect } from 'react'; // 引入 React
import { AppRegistry, View, ActivityIndicator } from 'react-native';
import FeedbackPage from './src/Feedback';
import { initI18n } from './src/utils/i18n';

const Root = (props) => {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    initI18n()
      .catch(e => console.error(e))
      .finally(() => setReady(true));
  }, []);

  if (!ready) {
    // 这里展示 Loading
    return (
      <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  // 初始化完成后，渲染业务组件，并把原生透传过来的 props 继续传下去
  return <FeedbackPage {...props} />;
};

AppRegistry.registerComponent('RNFeedbackPage', () => Root);