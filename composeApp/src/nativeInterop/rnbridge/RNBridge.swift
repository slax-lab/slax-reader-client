import Foundation
import UIKit

@objc public class RNBridge: NSObject {

    @objc public static func createReactViewController(_ moduleName: String, _ initialProps: NSDictionary?) -> UIViewController? {
        // RNViewController is in the main app target, can import ReactNativeAppTarget directly
        guard let vcClass = NSClassFromString("RNViewController") as? NSObject.Type else {
            print("[RNBridge] RNViewController class not found")
            return nil
        }

        let selector = NSSelectorFromString("createWithModuleName:initialProps:")
        guard vcClass.responds(to: selector) else {
            print("[RNBridge] RNViewController does not respond to create selector")
            return nil
        }

        let result = vcClass.perform(selector, with: moduleName, with: initialProps)
        return result?.takeUnretainedValue() as? UIViewController
    }
}
