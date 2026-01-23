import 'react-native-get-random-values'
import {AppRegistry} from 'react-native';
import MarkdownPage from './src/Markdown';
import ChatPage from "./src/Chat";

AppRegistry.registerComponent('RNMarkdownPage', () => MarkdownPage);
AppRegistry.registerComponent('RNChatPage', () => ChatPage);
