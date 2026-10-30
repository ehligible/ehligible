import Foundation

/// The countdown the app precomputes for the widget (Kotlin `WidgetSnapshot` in core), read from the shared App
/// Group container. Its JSON shape is pinned by `EhligibleWidgetTests/widget-snapshot.json`, which the Kotlin
/// tests check as well.
struct WidgetSnapshot: Codable, Equatable {
    var generatedAt: Int64
    var reviewDays: Int
    var target: Target
    var days: [Day]

    enum Target: String, Codable {
        case exact = "EXACT"
        case safer = "SAFER"
    }

    enum Status: String, Codable {
        case inCanada = "IN_CANADA"
        case abroad = "ABROAD"
        case nearBorder = "NEAR_BORDER"
        case unknown = "UNKNOWN"
    }

    /// The widget's numbers for one local calendar day.
    struct Day: Codable, Equatable {
        /// "yyyy-MM-dd".
        var date: String
        var daysLeft: Int?
        var countdownDate: String?
        var creditedDays: Int
        var progress: Double
        var celebrate: Bool
        var status: Status
        var country: String?
    }

    /// Must match `iosApp/project.yml` and Kotlin's `IosWidgets`.
    static let appGroup = "group.io.github.ehligible.app"
    static let fileName = "widget.json"

    static func decode(_ data: Data) throws -> WidgetSnapshot {
        try JSONDecoder().decode(WidgetSnapshot.self, from: data)
    }

    /// The snapshot the app wrote last; nil before onboarding, or in unsigned builds without an App Group.
    static func load() -> WidgetSnapshot? {
        guard
            let url = FileManager.default
                .containerURL(forSecurityApplicationGroupIdentifier: appGroup)?
                .appendingPathComponent(fileName),
            let data = try? Data(contentsOf: url)
        else { return nil }
        return try? decode(data)
    }

    /// What to show from `now` on: today's numbers right away, each later day from its midnight, and nothing
    /// (`day == nil`: "open the app") once the precomputed days run out.
    func schedule(from now: Date, calendar: Calendar = .current) -> [(date: Date, day: Day?)] {
        let today = Self.key(for: now, calendar: calendar)
        var result: [(date: Date, day: Day?)] = []
        for day in days where day.date >= today {
            guard let start = Self.startOfDay(day.date, calendar: calendar) else { continue }
            result.append((date: max(start, now), day: day))
        }
        if let last = days.last,
           let start = Self.startOfDay(last.date, calendar: calendar),
           let end = calendar.date(byAdding: .day, value: 1, to: start),
           end > now {
            result.append((date: end, day: nil))
        }
        if result.isEmpty {
            result.append((date: now, day: nil))
        }
        return result
    }

    /// The "yyyy-MM-dd" key of the local day containing `date`.
    static func key(for date: Date, calendar: Calendar = .current) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }

    /// Local midnight at the start of a "yyyy-MM-dd" day.
    static func startOfDay(_ key: String, calendar: Calendar = .current) -> Date? {
        let parts = key.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }
}
