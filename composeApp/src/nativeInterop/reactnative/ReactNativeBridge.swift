import UIKit

@objc(ReactNativeBridge)
public class ReactNativeBridge: NSObject {

    @objc public static let shared = ReactNativeBridge()

    @objc public func openReactNativeDemo() {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first(where: { $0.isKeyWindow }),
                  var topVC = window.rootViewController else {
                return
            }

            // Find topmost presented view controller
            while let presentedVC = topVC.presentedViewController {
                topVC = presentedVC
            }

            // Create and present ReactViewController using runtime class lookup
            // ReactViewController is defined in the iOS app and has access to React framework
            guard let reactVCClass = NSClassFromString("ReactViewController") as? UIViewController.Type else {
                print("ReactViewController class not found")
                return
            }

            let reactVC = reactVCClass.init()
            reactVC.modalPresentationStyle = .fullScreen
            topVC.present(reactVC, animated: true, completion: nil)
        }
    }
}
