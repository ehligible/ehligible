import ComposeApp
import SwiftUI
import UIKit
import WidgetKit

@main
struct EhligibleApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

/// Starts the Kotlin side at every launch, including when iOS relaunches the app in the background for a
/// location event.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        EhligibleIos.shared.start(host: WidgetHost())
        return true
    }
}

/// What Kotlin can't do itself: WidgetKit has no Objective-C API.
final class WidgetHost: NSObject, IosHost {
    func reloadWidgets() {
        WidgetCenter.shared.reloadAllTimelines()
    }
}

/// Hosts the shared Compose Multiplatform UI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        EhligibleIos.shared.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
