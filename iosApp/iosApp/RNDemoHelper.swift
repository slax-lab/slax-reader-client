import UIKit
import React

@objc public class RNDemoHelper: NSObject {

    /// Open React Native Demo screen
    @objc public static func openReactNativeDemo() {
        DispatchQueue.main.async {
            guard let rootViewController = getRootViewController() else {
                print("Failed to get root view controller")
                return
            }

            let reactVC = ReactViewController()
            reactVC.modalPresentationStyle = .fullScreen

            rootViewController.present(reactVC, animated: true, completion: nil)
        }
    }

    private static func getRootViewController() -> UIViewController? {
        // Get the key window
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first(where: { $0.isKeyWindow }) else {
            return nil
        }

        // Get the root view controller
        var rootViewController = window.rootViewController

        // If it's a navigation controller, get the top view controller
        while let presentedViewController = rootViewController?.presentedViewController {
            rootViewController = presentedViewController
        }

        return rootViewController
    }
}
