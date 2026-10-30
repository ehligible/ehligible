import SwiftUI
import WidgetKit

@main
struct EhligibleWidgets: WidgetBundle {
    var body: some Widget {
        CountdownWidget()
    }
}

struct CountdownWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "CountdownWidget", provider: CountdownProvider()) { entry in
            CountdownWidgetView(entry: entry)
                .containerBackground(for: .widget) { Palette.background }
        }
        .configurationDisplayName(Texts.localized("widget.name"))
        .description(Texts.localized("widget.description"))
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}
