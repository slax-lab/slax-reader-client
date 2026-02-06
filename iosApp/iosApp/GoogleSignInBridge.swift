import Foundation
import GoogleSignIn

public typealias GoogleSignInCompletion = (String?, String?, String?, String?) -> Void

@objc(GoogleSignInBridge)
@objcMembers
public class GoogleSignInBridge: NSObject {

    @objc public static let shared = GoogleSignInBridge()

    private var completionHandler: GoogleSignInCompletion?

    private override init() {
        super.init()
    }

    @objc public func signIn(withServerClientId serverClientId: String, completion: @escaping GoogleSignInCompletion) {
        self.completionHandler = completion

        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            completion(nil, nil, nil, "No root view controller available")
            return
        }

        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            completion(nil, nil, nil, "GIDClientID not found in Info.plist")
            return
        }

        let config = GIDConfiguration(clientID: clientID, serverClientID: serverClientId)
        GIDSignIn.sharedInstance.configuration = config

        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { [weak self] result, error in
            if let error = error {
                let errorMessage = error.localizedDescription
                completion(nil, nil, nil, errorMessage)
                return
            }

            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                completion(nil, nil, nil, "Failed to get ID token")
                return
            }

            let email = user.profile?.email
            let displayName = user.profile?.name

            completion(idToken, email, displayName, nil)
        }
    }
}
