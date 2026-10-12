import SwiftUI
import UIKit
import BackgroundTasks
import DroolShared
import GoogleMaps
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

private final class FirebaseObservabilityHandler: NSObject, IosObservabilityHandler {
    func setCollectionEnabled(enabled: Bool) {
        Analytics.setAnalyticsCollectionEnabled(enabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
    }

    func logEvent(
        name: String,
        parameterName: String?,
        parameterValue: String?
    ) {
        var parameters: [String: Any]?
        if let parameterName, let parameterValue {
            parameters = [parameterName: parameterValue]
        }
        Analytics.logEvent(name, parameters: parameters)
    }

    func recordNonFatal(errorType: String, operation: String) {
        let error = NSError(
            domain: "com.github.apkelly.drool.nonfatal",
            code: 1,
            userInfo: [
                "error_type": errorType,
                "operation": operation
            ]
        )
        Crashlytics.crashlytics().record(error: error)
    }
}

private final class AppDelegate: NSObject, UIApplicationDelegate {
    private let assignmentPollingHandler = AssignmentPollingHandler()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
            [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Analytics.setAnalyticsCollectionEnabled(false)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        IosObservabilityRegistry.shared.register(
            handler: FirebaseObservabilityHandler()
        )
        assignmentPollingHandler.register()
        IosAssignmentPollingSchedulerRegistry.shared.register(
            handler: assignmentPollingHandler
        )
        return true
    }
}

private final class AssignmentPollingHandler:
    NSObject,
    IosAssignmentPollingSchedulerHandler {
    func register() {
        let registered = BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            self.handle(refreshTask)
        }
        if !registered {
            NSLog("Unable to register referee assignment background refresh")
        }
    }

    func schedule(intervalSeconds: Double) {
        UserDefaults.standard.set(
            intervalSeconds,
            forKey: Self.intervalDefaultsKey
        )
        BGTaskScheduler.shared.cancel(
            taskRequestWithIdentifier: Self.taskIdentifier
        )
        guard intervalSeconds > 0 else {
            return
        }
        let request = BGAppRefreshTaskRequest(
            identifier: Self.taskIdentifier
        )
        request.earliestBeginDate = Date(
            timeIntervalSinceNow: intervalSeconds
        )
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            NSLog(
                "Unable to schedule referee assignment refresh: %@",
                error.localizedDescription
            )
        }
    }

    private func handle(_ task: BGAppRefreshTask) {
        let completion = BackgroundTaskCompletion(task: task)
        let interval = UserDefaults.standard.double(
            forKey: Self.intervalDefaultsKey
        )
        schedule(intervalSeconds: interval)
        task.expirationHandler = {
            IosAssignmentPollBridge.shared.cancel()
            completion.finish(success: false)
        }
        IosAssignmentPollBridge.shared.execute { successful in
            completion.finish(success: successful.boolValue)
        }
    }

    private static let taskIdentifier =
        "com.github.apkelly.drool.referee-assignment-poll"
    private static let intervalDefaultsKey =
        "referee-assignment-poll-interval"
}

private final class BackgroundTaskCompletion {
    private let task: BGTask
    private let lock = NSLock()
    private var completed = false

    init(task: BGTask) {
        self.task = task
    }

    func finish(success: Bool) {
        lock.lock()
        defer { lock.unlock() }
        guard !completed else {
            return
        }
        completed = true
        task.setTaskCompleted(success: success)
    }
}

private final class NativeGoogleMapViewFactory: NSObject, IosGoogleMapViewFactory {
    func createMap(latitude: Double, longitude: Double) -> UIView {
        let camera = GMSCameraPosition(
            latitude: latitude,
            longitude: longitude,
            zoom: 15
        )
        let options = GMSMapViewOptions()
        options.camera = camera
        let mapView = GMSMapView(options: options)
        mapView.settings.setAllGesturesEnabled(false)
        let marker = GMSMarker(position: CLLocationCoordinate2D(
            latitude: latitude,
            longitude: longitude
        ))
        marker.map = mapView
        return mapView
    }
}

@main
struct DroolApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        if let apiKey = Bundle.main.object(
            forInfoDictionaryKey: "GOOGLE_MAPS_API_KEY"
        ) as? String,
           !apiKey.isEmpty,
           !apiKey.hasPrefix("$(") {
            GMSServices.provideAPIKey(apiKey)
            IosGoogleMapRegistry.shared.register(
                factory: NativeGoogleMapViewFactory()
            )
        }
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(
        _ uiViewController: UIViewController,
        context: Context
    ) {
    }
}
