import SwiftUI
import UIKit
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
        return true
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
