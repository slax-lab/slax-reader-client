import { requireNativeModule } from 'expo';

type SlaxBridgeNativeModule = {
  invoke(method: string, payload: Record<string, any>): Promise<Record<string, any>>;
};

const SlaxBridge = requireNativeModule<SlaxBridgeNativeModule>('SlaxBridge');

export function invokeNative(
  method: string,
  payload: Record<string, any>,
): Promise<Record<string, any>> {
  return SlaxBridge.invoke(method, payload);
}
