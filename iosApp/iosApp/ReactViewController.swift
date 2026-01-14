import UIKit
import React
import ComposeApp


@objc(ReactViewController)
public class ReactViewController: UIViewController {

    private var bridge: RCTBridge?

    public override func viewDidLoad() {
        super.viewDidLoad()

        // Get the JavaScript bundle URL
        #if DEBUG
        guard let jsCodeLocation = RCTBundleURLProvider.sharedSettings().jsBundleURL(
            forBundleRoot: "index",
            fallbackExtension: nil
        ) else {
            print("Failed to load JavaScript bundle URL")
            return
        }
        #else
        guard let jsCodeLocation = Bundle.main.url(
            forResource: "main",
            withExtension: "jsbundle"
        ) else {
            print("Failed to load JavaScript bundle from resources")
            return
        }
        #endif

        // Create the RCTBridge with KMP module provider
        let bridge = RCTBridge(
            bundleURL: jsCodeLocation,
            moduleProvider: { [weak self] in
                return self?.getKMPModules() ?? []
            },
            launchOptions: nil
        )
        self.bridge = bridge

        // Create the RCTRootView with the registered component name
        let rootView = RCTRootView(
            bridge: bridge!,
            moduleName: "SlaxReaderRN",
            initialProperties: nil
        )

        // Set the root view as the view controller's view
        self.view = rootView
    }

    /// Get all KMP modules to expose to React Native
    private func getKMPModules() -> [RCTBridgeModule] {
        // Use the Kotlin helper to create modules with proper CoroutineScope
        let helper = ReactNativeModulesHelper()
        let modules = helper.createNativeModules()

        // Convert to RCTBridgeModule array
        return modules.compactMap { $0 as? RCTBridgeModule }
    }
}
