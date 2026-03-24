import SwiftUI
import FirebaseCore
import FirebaseMessaging
import GoogleSignIn
import BackgroundTasks
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

      BGTaskScheduler.shared.register(
        forTaskWithIdentifier: "silent_push_print_task",
        using: nil
      ) { task in
          task.expirationHandler = { task.setTaskCompleted(success: false) }
          let helper = KmpWorkerHelper()
          let scheduler = helper.getScheduler()
          scheduler.flushPendingProgress()
          task.setTaskCompleted(success: true)
      }

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

        let helper = KmpWorkerHelper()
        let scheduler = helper.getScheduler()
        let trigger = TaskTriggerHelperKt.createTaskTriggerOneTime(initialDelayMs: 0)
        let constraints = TaskTriggerHelperKt.createDefaultConstraints()

        Task {
            do {
                let _ = try await scheduler.enqueue(
                    id: "silent_push_print_task",
                    trigger: trigger,
                    workerClassName: "PrintWorker",
                    constraints: constraints,
                    inputJson: nil,
                    policy: .replace
                )
                print("[SilentPush] Successfully enqueued PrintWorker")
                completionHandler(.newData)
            } catch {
                print("[SilentPush] Failed to enqueue PrintWorker: \(error)")
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
