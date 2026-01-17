import UIKit
import React
import ComposeApp

@objc(ReactViewController)
public class ReactViewController: UIViewController {

    private var bridge: RCTBridge?
    private var modulesHelper: ReactNativeModulesHelper?
    private let moduleName: String

    @objc public static func create(moduleName: String) -> ReactViewController {
        return ReactViewController(moduleName: moduleName)
    }

    @objc public init(moduleName: String) {
        self.moduleName = moduleName
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        self.moduleName = "SlaxReaderRN"
        super.init(coder: coder)
    }

    public override func viewDidLoad() {
        super.viewDidLoad()

        #if DEBUG
        let provider = RCTBundleURLProvider.sharedSettings()
        // provider.jsLocation = "192.168.6.221"
        guard let jsCodeLocation = provider.jsBundleURL(
            forBundleRoot: "index",
            fallbackExtension: nil
        ) else { return }
        #else
        guard let jsCodeLocation = Bundle.main.url(
            forResource: "main",
            withExtension: "jsbundle"
        ) else { return }
        #endif

        modulesHelper = ReactNativeModulesHelper()

        let bridge = RCTBridge(
            bundleURL: jsCodeLocation,
            moduleProvider: { [weak self] in
                self?.modulesHelper?.createNativeModules().compactMap { $0 as? RCTBridgeModule } ?? []
            },
            launchOptions: nil
        )
        self.bridge = bridge
        self.view = RCTRootView(bridge: bridge!, moduleName: moduleName, initialProperties: nil)
    }

    deinit {
        bridge?.invalidate()
        bridge = nil
        modulesHelper?.cleanup()
        modulesHelper = nil
    }
}
