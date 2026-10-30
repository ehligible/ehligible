import XCTest

/// The fixture is the same file the Kotlin tests encode and decode (`WidgetSnapshotFixtureTest`).
final class WidgetSnapshotTests: XCTestCase {
    private var toronto: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/Toronto")!
        return calendar
    }

    private func fixture() throws -> WidgetSnapshot {
        let bundle = Bundle(for: WidgetSnapshotTests.self)
        let url = try XCTUnwrap(bundle.url(forResource: "widget-snapshot", withExtension: "json"))
        return try WidgetSnapshot.decode(Data(contentsOf: url))
    }

    func testDecodesTheSnapshotWrittenByKotlin() throws {
        let snapshot = try fixture()
        XCTAssertEqual(snapshot.generatedAt, 1_793_289_600_000)
        XCTAssertEqual(snapshot.target, .safer)
        XCTAssertEqual(snapshot.reviewDays, 2)
        XCTAssertEqual(snapshot.days.map(\.date), ["2026-10-29", "2026-10-30", "2026-10-31"])
        XCTAssertEqual(snapshot.days[0].daysLeft, 436)
        XCTAssertEqual(snapshot.days[0].countdownDate, "2028-01-08")
        XCTAssertEqual(snapshot.days[0].progress, 0.6091, accuracy: 0.00001)
        XCTAssertEqual(snapshot.days[0].status, .abroad)
        XCTAssertEqual(snapshot.days[0].country, "FR")
        XCTAssertEqual(snapshot.days[1].status, .inCanada)
        XCTAssertNil(snapshot.days[1].country)
        XCTAssertNil(snapshot.days[2].daysLeft)
        XCTAssertNil(snapshot.days[2].countdownDate)
        XCTAssertEqual(snapshot.days[2].status, .unknown)
    }

    func testSchedulesEachDayFromItsMidnightThenRunsOut() throws {
        let snapshot = try fixture()
        let calendar = toronto
        let now = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 10, day: 30, hour: 9)))
        let schedule = snapshot.schedule(from: now, calendar: calendar)
        XCTAssertEqual(schedule.map { $0.day?.date }, ["2026-10-30", "2026-10-31", nil])
        XCTAssertEqual(schedule[0].date, now)
        XCTAssertEqual(schedule[1].date, calendar.date(from: DateComponents(year: 2026, month: 10, day: 31)))
        XCTAssertEqual(schedule[2].date, calendar.date(from: DateComponents(year: 2026, month: 11, day: 1)))
    }

    func testAStaleSnapshotShowsNothing() throws {
        let snapshot = try fixture()
        let calendar = toronto
        let later = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 11, day: 20, hour: 9)))
        let schedule = snapshot.schedule(from: later, calendar: calendar)
        XCTAssertEqual(schedule.count, 1)
        XCTAssertNil(schedule[0].day)
        XCTAssertEqual(schedule[0].date, later)
    }

    func testDayKeysUseTheLocalCalendar() throws {
        let calendar = toronto
        // 03:00 UTC on Oct 30 is still Oct 29 in Toronto.
        let date = try XCTUnwrap(ISO8601DateFormatter().date(from: "2026-10-30T03:00:00Z"))
        XCTAssertEqual(WidgetSnapshot.key(for: date, calendar: calendar), "2026-10-29")
    }
}
