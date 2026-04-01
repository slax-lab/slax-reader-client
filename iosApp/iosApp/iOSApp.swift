import SwiftUI
import FirebaseCore
import FirebaseMessaging
import GoogleSignIn
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate {
    var window: UIWindow?

    func application(
      _ app: UIApplication,
      open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
      return GIDSignIn.sharedInstance.handle(url)
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
      FirebaseApp.configure()
      Messaging.messaging().delegate = self
      application.registerForRemoteNotifications()
      return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let token = fcmToken {
            print("[NotificationHelper] FCM Token refreshed: \(token)")
        }
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable : Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        print("[SilentPush] Received remote notification: \(userInfo)")

        let data = (userInfo as? [String: Any])?.compactMapValues { "\($0)" } ?? [:]

        Task {
            do {
                try await BackgroundTaskRunner.shared.onSilentPush(data: data)
                print("[SilentPush] Background task completed")
                completionHandler(.newData)
            } catch {
                print("[SilentPush] Background task failed: \(error)")
                completionHandler(.failed)
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL(perform: { url in
                    GIDSignIn.sharedInstance.handle(url)
                })
                .onAppear {
                    if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                       let window = windowScene.windows.first {
                        delegate.window = window
                    }
                }
        }
    }
}
