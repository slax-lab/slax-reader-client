import UIKit
import React
import React_RCTAppDelegate
import ComposeApp

// MARK: - React Native Manager (Lazy Loaded)
class ReactNativeManager: RCTDefaultReactNativeFactoryDelegate {

    static let shared = ReactNativeManager()

    private var factory: RCTReactNativeFactory?
    private var _rootViewFactory: RCTRootViewFactory?

    override func bundleURL() -> URL? {
        #if DEBUG
        return RCTBundleURLProvider.sharedSettings().jsBundleURL(forBundleRoot: "index")
        #else
        return Bundle.main.url(forResource: "main", withExtension: "jsbundle")
        #endif
    }

    // Required by RCTBridgeDelegate
    override func sourceURL(for bridge: RCTBridge) -> URL? {
        return bundleURL()
    }

    func rootViewFactory() -> RCTRootViewFactory {
        if _rootViewFactory == nil {
            print("[ReactNativeManager] Creating RCTReactNativeFactory...")
            factory = RCTReactNativeFactory(delegate: self)
            _rootViewFactory = factory?.rootViewFactory
            print("[ReactNativeManager] RootViewFactory created")
        }
        return _rootViewFactory!
    }
}

// MARK: - React Native ViewController
@objc(ReactViewController)
public class ReactViewController: UIViewController {

    private let moduleName: String
    private let initialProps: [String: Any]?

    @objc public static func create(moduleName: String) -> ReactViewController {
        return ReactViewController(moduleName: moduleName, initialProps: nil)
    }

    @objc public static func create(moduleName: String, initialProps: [String: Any]?) -> ReactViewController {
        return ReactViewController(moduleName: moduleName, initialProps: initialProps)
    }

    public init(moduleName: String, initialProps: [String: Any]? = nil) {
        self.moduleName = moduleName
        self.initialProps = initialProps
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func loadView() {
        self.view = ReactNativeManager.shared.rootViewFactory().view(
            withModuleName: moduleName,
            initialProperties: initialProps
        )
    }
}
