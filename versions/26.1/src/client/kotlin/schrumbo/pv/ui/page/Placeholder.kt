package schrumbo.pv.ui.page

import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text

/** Centered "coming soon" stand-in for pages that have no content yet. */
object Placeholder {
    fun build(title: String): Component = Column(
        Text(title.uppercase(), Theme.TEXT),
        Spacer(0, 2),
        Text("coming soon", Theme.TEXT_MUTED),
        spacing = 1,
        align = HAlign.CENTER,
    )
}
