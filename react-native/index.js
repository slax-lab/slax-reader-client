import {AppRegistry} from 'react-native';
import MarkdownPage from './src/Markdown';
import ChatPage from './src/Chat';
import FeedbackPage from './src/Feedback';

AppRegistry.registerComponent('RNMarkdownPage', () => MarkdownPage);
AppRegistry.registerComponent('RNChatPage', () => ChatPage);
AppRegistry.registerComponent('RNFeedbackPage', () => FeedbackPage);
