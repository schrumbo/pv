package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.NamedContainer
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign

/** Inventory page: a wrapping container selector and a chest-style slot grid of the chosen container. */
object InventoryPage {

    private const val SLOT = 18
    private const val GAP = 2

    fun build(p: SkyblockProfile, width: Int, selected: Int, onTab: (Int) -> Unit): Component {
        if (p.containers.isEmpty()) {
            return Column(
                PageKit.pageHeader("Inventory", "", width),
                Text("This player has their inventory API disabled.", Theme.TEXT_MUTED),
                spacing = 8,
            )
        }
        val active = selected.coerceIn(0, p.containers.size - 1)
        val container = p.containers[active]
        val count = container.items.count { !it.isEmpty }
        return Column(
            PageKit.pageHeader("Inventory", "· ${container.name} ($count)", width),
            tabBar(p.containers, active, width, onTab),
            Spacer(0, 2),
            slotGrid(container.items, width),
            spacing = 8,
        )
    }

    private fun tabBar(containers: List<NamedContainer>, active: Int, width: Int, onTab: (Int) -> Unit): Component {
        val rows = mutableListOf<MutableList<Component>>(mutableListOf())
        var rowW = 0
        containers.forEachIndexed { i, c ->
            val btn = segButton(c.name, i == active) { onTab(i) }
            if (rowW + btn.width + 6 > width && rows.last().isNotEmpty()) {
                rows += mutableListOf<Component>(); rowW = 0
            }
            rows.last() += btn
            rowW += btn.width + 6
        }
        return Column(rows.map { Row(it, spacing = 6) }, spacing = 4)
    }

    private fun segButton(label: String, active: Boolean, onClick: () -> Unit): Component {
        val w = PageKit.font().width(label) + 16
        val btn = Frame(
            w, 14,
            Text(label, if (active) Theme.ACCENT else Theme.TEXT_MUTED),
            Theme.SURFACE_ALT,
            if (active) Theme.ACCENT else Theme.BORDER,
            HAlign.CENTER, VAlign.CENTER,
        )
        return Clickable(btn, hoverFill = Theme.HOVER, onClick = onClick)
    }

    private fun slotGrid(items: List<ItemStack>, width: Int): Component {
        val cols = ((width + GAP) / (SLOT + GAP)).coerceIn(8, 12)
        val rows = items.chunked(cols).map { rowItems ->
            Row(rowItems.map { slot(it) }, spacing = GAP)
        }
        return Column(rows, spacing = GAP)
    }

    private fun slot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 2, SLOT - 2) else Item(stack, SLOT - 2, tooltip = true)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }
}
