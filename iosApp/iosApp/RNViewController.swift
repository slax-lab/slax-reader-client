import UIKit
import ReactNativeAppTarget
import ComposeApp
import SlaxBridgeCore

@objc(RNViewController)
public class RNViewController: NSObject {

    private static let once: Void = {
        #if DEBUG
        let frameworkBundle = Bundle(for: ReactNativeHostManager.self)
        if let ipURL = frameworkBundle.url(forResource: "ip", withExtension: "txt"),
           let ip = try? String(contentsOf: ipURL, encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines),
           !ip.isEmpty {
            UserDefaults.standard.set(ip, forKey: "RCT_jsLocation")
        }
        #endif
        ReactNativeHostManager.shared.initialize()

        SlaxBridgeRegistry.handler = { method, payload, callback in
            ReactNativeMessageDispatcher.shared.invoke(method: method, payload: payload) { result, error in
                if let error = error {
                    callback(.failure(error))
                } else {
                    callback(.success(result as? [String: Any] ?? [:]))
                }
            }
        }
    }()

    @objc public static func create(moduleName: String, initialProps: NSDictionary?) -> UIViewController {
        _ = once
        return ReactNativeViewController(
            moduleName: moduleName,
            initialProps: initialProps as? [AnyHashable: Any]
        )
    }
}
