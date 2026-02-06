import Foundation

public typealias GoogleSignInCompletion = (String?, String?, String?, String?) -> Void

@objc(GoogleSignInBridge)
@objcMembers
public class GoogleSignInBridge: NSObject {

    @objc public static let shared = GoogleSignInBridge()

    private override init() {
        super.init()
    }

    @objc public func signIn(withServerClientId serverClientId: String, completion: @escaping GoogleSignInCompletion) {
        completion(nil, nil, nil, "Google Sign-In is not available in share extension")
    }
}
