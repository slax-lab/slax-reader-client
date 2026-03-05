const fs = require('fs');
const path = require('path');
const { withFinalizedMod, withXcodeProject, IOSConfig } = require('expo/config-plugins');

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

// Fix expo-brownfield's hardcoded -Onone optimization level
function fixBrownfieldOptimization(config) {
  return withXcodeProject(config, (config) => {
    const project = config.modResults;
    const { Target } = IOSConfig;

    // Get all native targets
    const nativeTargets = Target.getNativeTargets(project);

    for (const [, target] of nativeTargets) {
      const buildConfigListId = target.buildConfigurationList;
      const buildConfigs = IOSConfig.XcodeUtils.getBuildConfigurationsForListId(project, buildConfigListId);

      for (const [, configuration] of buildConfigs) {
        const { buildSettings } = configuration;

        // Only fix Release builds
        if (configuration.name === 'Release' && buildSettings) {
          // Fix Swift optimization
          if (buildSettings.SWIFT_OPTIMIZATION_LEVEL === '"-Onone"') {
            buildSettings.SWIFT_OPTIMIZATION_LEVEL = '"-Osize"';
            buildSettings.STRIP_INSTALLED_PRODUCT = 'YES';
            buildSettings.STRIP_SWIFT_SYMBOLS = 'YES';
            buildSettings.DEAD_CODE_STRIPPING = 'YES';
            buildSettings.DEPLOYMENT_POSTPROCESSING = 'YES';
          }

          // Add GCC optimization if missing
          if (!buildSettings.GCC_OPTIMIZATION_LEVEL) {
            buildSettings.GCC_OPTIMIZATION_LEVEL = 's';
          }
        }
      }
    }

    return config;
  });
}

function withSlaxBridgeCoreReexport(config) {
  // Fix Swift file imports
  config = withFinalizedMod(config, [
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

  // Fix Xcode build settings
  config = fixBrownfieldOptimization(config);

  return config;
}

module.exports = withSlaxBridgeCoreReexport;
