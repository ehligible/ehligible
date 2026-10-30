import SwiftUI
import XCTest

/// Renders the widget in each size and state, in English and French, so its look can be checked without an
/// iPhone. CI sets WIDGET_SNAPSHOT_DIR (through TEST_RUNNER_WIDGET_SNAPSHOT_DIR) and uploads the images.
final class WidgetRenderTests: XCTestCase {
    @MainActor
    func testRendersEverySizeAndState() throws {
        defer { Texts.use(language: nil) }
        for language in ["en", "fr"] {
            Texts.use(language: language)
            let renderer = ImageRenderer(content: WidgetSheet())
            renderer.scale = 2
            let image = try XCTUnwrap(renderer.uiImage, "rendering failed (\(language))")
            XCTAssertGreaterThan(image.size.width, 300)
            guard let directory = ProcessInfo.processInfo.environment["WIDGET_SNAPSHOT_DIR"] else { continue }
            try FileManager.default.createDirectory(atPath: directory, withIntermediateDirectories: true)
            let url = URL(fileURLWithPath: directory).appendingPathComponent("widget-\(language).png")
            try XCTUnwrap(image.pngData()).write(to: url)
        }
    }

    func testTextsFollowTheChosenLanguage() {
        defer { Texts.use(language: nil) }
        Texts.use(language: "fr")
        XCTAssertEqual(Texts.localized("status.in_canada"), "Au Canada")
        XCTAssertEqual(Texts.daysToGo(1), "jour à faire")
        XCTAssertEqual(Texts.daysToGo(429), "jours à faire")
        Texts.use(language: "en")
        XCTAssertEqual(Texts.localized("status.in_canada"), "In Canada")
        XCTAssertEqual(Texts.daysToGo(1), "day to go")
        XCTAssertEqual(Texts.review(2), "2 days need your review")
    }
}

/// Every state on one sheet: home-screen sizes in light and dark, then the lock-screen sizes.
private struct WidgetSheet: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                tile(SmallView(entry: countdown()), width: 170)
                tile(SmallView(entry: countdown()), width: 170, dark: true)
                tile(SmallView(entry: setup), width: 170)
            }
            tile(MediumView(entry: countdown()), width: 364)
            tile(MediumView(entry: countdown(status: .abroad, country: "FR", reviewDays: 2)), width: 364)
            tile(MediumView(entry: countdown(daysLeft: 0, celebrate: true, progress: 1)), width: 364)
            tile(MediumView(entry: countdown(status: .unknown)), width: 364, dark: true)
            HStack(spacing: 14) {
                CircularView(entry: countdown())
                    .frame(width: 76, height: 76)
                RectangularView(entry: countdown())
                    .frame(width: 172, height: 76)
            }
            .padding(14)
            .foregroundStyle(.white)
            .background(Color.black.opacity(0.85), in: RoundedRectangle(cornerRadius: 22))
            .environment(\.colorScheme, .dark)
        }
        .padding(16)
        .background(Color(white: 0.82))
    }

    private var setup: CountdownEntry {
        CountdownEntry(date: Date(), day: nil, target: .exact, reviewDays: 0, hasSnapshot: false)
    }

    private func countdown(
        daysLeft: Int = 429,
        status: WidgetSnapshot.Status = .inCanada,
        country: String? = nil,
        celebrate: Bool = false,
        reviewDays: Int = 0,
        progress: Double = 0.62
    ) -> CountdownEntry {
        CountdownEntry(
            date: Date(),
            day: WidgetSnapshot.Day(
                date: "2026-10-30",
                daysLeft: daysLeft,
                countdownDate: "2028-01-02",
                creditedDays: celebrate ? 1095 : 680,
                progress: progress,
                celebrate: celebrate,
                status: status,
                country: country
            ),
            target: .exact,
            reviewDays: reviewDays,
            hasSnapshot: true
        )
    }

    /// A widget tile as on the home screen: the system's margins, background and corners.
    private func tile<Content: View>(_ content: Content, width: CGFloat, dark: Bool = false) -> some View {
        content
            .padding(16)
            .frame(width: width, height: 170)
            .background(Palette.background)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .environment(\.colorScheme, dark ? .dark : .light)
    }
}
