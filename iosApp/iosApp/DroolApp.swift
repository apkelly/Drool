import SwiftUI
import UIKit
import DroolShared
import GoogleMaps

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
