import UIKit
import ReactNativeAppTarget

@objc(RNViewController)
public class RNViewController: NSObject {

    private static let once: Void = {
        ReactNativeHostManager.shared.initialize()
    }()

    @objc public static func create(moduleName: String, initialProps: NSDictionary?) -> UIViewController {
        _ = once
        return ReactNativeViewController(
            moduleName: moduleName,
            initialProps: initialProps as? [AnyHashable: Any]
        )
    }
}