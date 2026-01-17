import UIKit

@objc(ReactNativeBridge)
public class ReactNativeBridge: NSObject {

    @objc public static let shared = ReactNativeBridge()

    @objc public func createReactViewController(_ moduleName: String) -> UIViewController? {
        guard let vcClass = NSClassFromString("ReactViewController") as? NSObject.Type else { return nil }
        return vcClass.perform(NSSelectorFromString("createWithModuleName:"), with: moduleName)?.takeUnretainedValue() as? UIViewController
    }
}
