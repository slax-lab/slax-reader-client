import UIKit
import ReactNativeAppTarget

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
    }()

    @objc public static func create(moduleName: String, initialProps: NSDictionary?) -> UIViewController {
        _ = once
        return ReactNativeViewController(moduleName: moduleName)
    }
}
