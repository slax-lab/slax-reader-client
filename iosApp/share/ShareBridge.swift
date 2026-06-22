import Foundation
import UIKit

@objc(ShareBridge)
@objcMembers
public class ShareBridge: NSObject {

    @objc public static let shared = ShareBridge()

    private override init() {
        super.init()
    }

    @objc public func share(title: String, text: String, urlString: String, imageData: Data?) {
    }
}
