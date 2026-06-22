import Foundation
import UIKit
import LinkPresentation

@objc(ShareBridge)
@objcMembers
public class ShareBridge: NSObject {

    @objc public static let shared = ShareBridge()

    private override init() {
        super.init()
    }

    @objc public func share(title: String, text: String, urlString: String, imageData: Data?) {
        let image = imageData.flatMap { UIImage(data: $0) }
        let itemSource = ShareItemSource(text: text, title: title, urlString: urlString, image: image)

        let activityVC = UIActivityViewController(activityItems: [itemSource], applicationActivities: nil)

        guard let topVC = ShareBridge.topViewController() else { return }

        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = topVC.view
            popover.sourceRect = CGRect(x: topVC.view.bounds.midX, y: topVC.view.bounds.midY, width: 0, height: 0)
        }

        topVC.present(activityVC, animated: true, completion: nil)
    }

    private static func topViewController(
        _ base: UIViewController? = UIApplication.shared.windows.first(where: { $0.isKeyWindow })?.rootViewController
    ) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return topViewController(nav.visibleViewController)
        }
        if let tab = base as? UITabBarController {
            return topViewController(tab.selectedViewController)
        }
        if let presented = base?.presentedViewController {
            return topViewController(presented)
        }
        return base
    }
}

private class ShareItemSource: NSObject, UIActivityItemSource {

    private let text: String
    private let title: String
    private let urlString: String
    private let image: UIImage?

    init(text: String, title: String, urlString: String, image: UIImage?) {
        self.text = text
        self.title = title
        self.urlString = urlString
        self.image = image
    }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        return text
    }

    func activityViewController(
        _ activityViewController: UIActivityViewController,
        itemForActivityType activityType: UIActivity.ActivityType?
    ) -> Any? {
        return text
    }

    func activityViewController(
        _ activityViewController: UIActivityViewController,
        subjectForActivityType activityType: UIActivity.ActivityType?
    ) -> String {
        return title
    }

    func activityViewControllerLinkMetadata(_ activityViewController: UIActivityViewController) -> LPLinkMetadata? {
        let metadata = LPLinkMetadata()
        metadata.title = title
        if let url = URL(string: urlString) {
            metadata.originalURL = url
        }
        if let image = image {
            metadata.iconProvider = NSItemProvider(object: image)
            metadata.imageProvider = NSItemProvider(object: image)
        }
        return metadata
    }
}
