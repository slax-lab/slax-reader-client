import UIKit

@objc(ReactNativeBridge)
public class ReactNativeBridge: NSObject {

    @objc public static let shared = ReactNativeBridge()

    @objc public func createReactViewController(_ moduleName: String, _ initialProps: NSDictionary?) -> UIViewController? {
        guard let vcClass = NSClassFromString("ReactViewController") as? NSObject.Type else { return nil }

        if let props = initialProps as? [String: Any] {
            return vcClass.perform(NSSelectorFromString("createWithModuleName:initialProps:"), with: moduleName, with: props)?.takeUnretainedValue() as? UIViewController
        } else {
            return vcClass.perform(NSSelectorFromString("createWithModuleName:"), with: moduleName)?.takeUnretainedValue() as? UIViewController
        }
    }
}
