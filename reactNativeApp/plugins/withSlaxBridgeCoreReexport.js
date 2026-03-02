const fs = require('fs');
const path = require('path');
const { withFinalizedMod } = require('expo/config-plugins');

function getBrownfieldTargetName(config) {
  const plugins = config?.plugins ?? [];
  for (const plugin of plugins) {
    if (Array.isArray(plugin) && plugin[0] === 'expo-brownfield') {
      return plugin[1]?.ios?.targetName || 'ReactNativeAppTarget';
    }
  }
  return 'ReactNativeAppTarget';
}

function injectSlaxBridgeCoreReexport(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Cannot find generated file: ${filePath}`);
  }

  let patched = fs.readFileSync(filePath, 'utf8');

  patched = patched.replace('@_exported import SlaxBridgeCore', 'internal import SlaxBridgeCore');
  if (!patched.includes('import SlaxBridgeCore')) {
    if (patched.includes('import Network\n')) {
      patched = patched.replace('import Network\n', 'import Network\ninternal import SlaxBridgeCore\n');
    } else if (patched.includes('import UIKit\n')) {
      patched = patched.replace('import UIKit\n', 'internal import SlaxBridgeCore\nimport UIKit\n');
    } else {
      patched = `internal import SlaxBridgeCore\n${patched}`;
    }
  }

  if (!patched.includes('public static func setSlaxBridgeHandler(')) {
    const anchor = '  public static let shared = ReactNativeHostManager()\n';
    const bridgeApi = `${anchor}
  public typealias SlaxBridgeCallback = (Result<[String: Any], Error>) -> Void
  public typealias SlaxBridgeHandler = (String, [String: Any], @escaping SlaxBridgeCallback) -> Void

  public static func setSlaxBridgeHandler(_ handler: @escaping SlaxBridgeHandler) {
    SlaxBridgeRegistry.handler = handler
  }
`;
    if (patched.includes(anchor)) {
      patched = patched.replace(anchor, bridgeApi);
    } else {
      throw new Error(`Cannot find insertion point for SlaxBridge handler API: ${filePath}`);
    }
  }

  fs.writeFileSync(filePath, patched);
}

function withSlaxBridgeCoreReexport(config) {
  return withFinalizedMod(config, [
    'ios',
    (modConfig) => {
      const targetName = getBrownfieldTargetName(modConfig);
      const hostManagerPath = path.join(
        modConfig.modRequest.platformProjectRoot,
        targetName,
        'ReactNativeHostManager.swift'
      );

      injectSlaxBridgeCoreReexport(hostManagerPath);
      return modConfig;
    },
  ]);
}

module.exports = withSlaxBridgeCoreReexport;
