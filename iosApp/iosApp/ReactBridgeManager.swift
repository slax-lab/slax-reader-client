import UIKit
import React
import ComposeApp

@objc(ReactBridgeManager)
public class ReactBridgeManager: NSObject, RCTBridgeDelegate {

    @objc public static let shared = ReactBridgeManager()

    private var bridge: RCTBridge?
    private var modulesHelper: ReactNativeModulesHelper?

    private override init() {
        super.init()
    }

    @objc public func createRootView(moduleName: String, initialProperties: [String: Any]? = nil) -> RCTRootView? {
        if bridge == nil {
            modulesHelper = ReactNativeModulesHelper()
            bridge = RCTBridge(delegate: self, launchOptions: nil)
            print("[ReactBridgeManager] Bridge initialized (lazy)")
        }

        guard let bridge = bridge else {
            print("[ReactBridgeManager] ERROR: Bridge initialization failed")
            return nil
        }

        return RCTRootView(
            bridge: bridge,
            moduleName: moduleName,
            initialProperties: initialProperties
        )
    }

    public func sourceURL(for bridge: RCTBridge!) -> URL! {
        #if DEBUG
        let provider = RCTBundleURLProvider.sharedSettings()
        // provider.jsLocation = "192.168.6.221"
        return provider.jsBundleURL(forBundleRoot: "index")
        #else
        return Bundle.main.url(forResource: "main", withExtension: "jsbundle")
        #endif
    }

    public func extraModules(for bridge: RCTBridge!) -> [RCTBridgeModule]! {
        return modulesHelper?.createNativeModules().compactMap { $0 as? RCTBridgeModule } ?? []
    }
}
