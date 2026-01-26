/**
 * React Native 应用入口
 * 统一注册所有 RN 组件
 */

import 'react-native-get-random-values';
import FeedbackPage from './src/Feedback';
import { registerRNComponent } from './src/utils/registerComponent';

// 注册反馈页面组件
registerRNComponent('RNFeedbackPage', FeedbackPage);

// 如果需要注册更多组件，只需添加一行：
// registerRNComponent('RNAnotherPage', AnotherPage);
// registerRNComponent('RNSettingsPage', SettingsPage, {
//   extraInit: async () => {
//     // 可选的额外初始化逻辑
//   }
// });
