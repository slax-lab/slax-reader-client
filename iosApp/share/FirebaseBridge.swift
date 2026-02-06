import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

@objc(FirebaseBridge)
@objcMembers
public class FirebaseBridge: NSObject {

    @objc public static let shared = FirebaseBridge()

    private override init() {
        super.init()
    }

    @objc public func configure() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
    }

    @objc public func logEvent(_ name: String, parameters: [String: Any]?) {
        Analytics.logEvent(name, parameters: parameters)
    }

    @objc public func setUserId(_ userId: String?) {
        Analytics.setUserID(userId)
    }

    @objc public func setUserProperty(_ value: String?, forName name: String) {
        Analytics.setUserProperty(value, forName: name)
    }

    @objc public func recordError(_ message: String, code: Int) {
        let error = NSError(domain: "com.slax.reader", code: code, userInfo: [NSLocalizedDescriptionKey: message])
        Crashlytics.crashlytics().record(error: error)
    }

    @objc public func log(_ message: String) {
        Crashlytics.crashlytics().log(message)
    }

    @objc public func setCrashlyticsUserId(_ userId: String?) {
        Crashlytics.crashlytics().setUserID(userId ?? "")
    }
}
