import { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { getInitialProps } from '@/constants/initial-props';

export default function IndexScreen() {
  useEffect(() => {
    const { route, ...params } = getInitialProps();
    const pathname = route ? `/${route.replace(/^\/+/, '')}` : '/feedback';
    router.replace({
      pathname: pathname as any,
      params,
    });
  }, []);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#16B998" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5F5F3',
  },
});
