package schrumbo.pv.ui.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import schrumbo.pv.api.ProfileService
import schrumbo.pv.data.ProfileState
import schrumbo.pv.data.SkillType
import schrumbo.pv.render.FakePlayer
import schrumbo.pv.ui.Page
import schrumbo.pv.ui.SkillTab
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.ClickRegistry
import schrumbo.pv.ui.component.Hover
import schrumbo.pv.ui.page.GeneralPage
import schrumbo.pv.ui.page.Placeholder

/** Root profile-viewer screen: floating search/profile controls, page tabs, and the pages. */
class PvScreen(target: String) : Screen(Component.literal("Profile Viewer")) {

    private var state: ProfileState = ProfileState.Loading
    private var profileIndex = 0

    private var page = Page.GENERAL
    private var skillTab = SkillTab.MINING

    private var input = ""
    private var inputFocused = false

    private var cachedEntity: LivingEntity? = null
    private var cachedEntityIndex = -1

    private var inputRect = intArrayOf(0, 0, 0, 0)
    private var dropdownOpen = false
    private var dropHeaderRect = intArrayOf(0, 0, 0, 0)
    private val dropEntryRects = mutableListOf<Pair<IntArray, Int>>()

    private val tabRects = mutableListOf<Pair<IntArray, Page>>()
    private val subnavRects = mutableListOf<Pair<IntArray, SkillTab>>()

    // Transform of the (possibly scaled) General main column, for click hit-testing.
    private var mainTfX = 0
    private var mainTfY = 0
    private var mainScale = 1f

    init {
        load(target)
    }

    override fun isPauseScreen(): Boolean = false

    private fun load(target: String) {
        state = ProfileState.Loading
        cachedEntity = null
        cachedEntityIndex = -1
        ProfileService.load(target) { s ->
            state = s
            if (s is ProfileState.Loaded) profileIndex = s.selectedIndex
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        Hover.screenX = mouseX
        Hover.screenY = mouseY
        ClickRegistry.reset()
        context.fill(0, 0, width, height, Theme.BACKDROP)

        val panelW = minOf(width - 40, 760)
        val panelH = minOf(height - 40, 480)
        val px = (width - panelW) / 2
        val py = (height - panelH) / 2

        context.fill(px, py, px + panelW, py + panelH, Theme.SURFACE)
        border(context, px, py, panelW, panelH, Theme.BORDER)

        val topH = 18
        tabBar(context, px + 10, py + 5, mouseX, mouseY)
        context.fill(px, py + topH, px + panelW, py + topH + 1, Theme.BORDER)

        val pad = 12
        val contentX = px + pad
        val contentY = py + topH + 1 + pad
        val contentW = panelW - pad * 2
        val contentH = (py + panelH - pad) - contentY
        renderContent(context, contentX, contentY, contentW, contentH, mouseX, mouseY)

        // Floating controls render last so they sit above tabs and content.
        floatingControls(context, px + panelW, py, topH)
        if (dropdownOpen) dropdownOverlay(context, mouseX, mouseY)
    }

    private fun floatingControls(ctx: GuiGraphicsExtractor, panelRight: Int, py: Int, topH: Int) {
        val h = 14
        val y = py + (topH - h) / 2

        // Profile chip pinned to the far right; search to its left.
        val s = state
        var chipLeft = panelRight - 8
        if (s is ProfileState.Loaded && s.profiles.isNotEmpty()) {
            val index = profileIndex.coerceIn(0, s.profiles.size - 1)
            val label = s.profiles[index].cuteName + "  ▾"
            val w = font.width(label) + 12
            val x = panelRight - 8 - w
            ctx.fill(x, y, x + w, y + h, Theme.SURFACE_ALT)
            border(ctx, x, y, w, h, if (dropdownOpen) Theme.ACCENT else Theme.BORDER)
            ctx.text(font, label, x + 6, y + 3, Theme.TEXT, true)
            dropHeaderRect = intArrayOf(x, y, w, h)
            chipLeft = x
        } else {
            dropHeaderRect = intArrayOf(0, 0, 0, 0)
        }

        val sw = 150
        val sx = chipLeft - 6 - sw
        ctx.fill(sx, y, sx + sw, y + h, Theme.SURFACE)
        border(ctx, sx, y, sw, h, if (inputFocused) Theme.ACCENT else Theme.BORDER)
        val blink = inputFocused && (System.currentTimeMillis() / 500) % 2 == 0L
        val placeholder = input.isEmpty() && !inputFocused
        val shown = (if (placeholder) "search player…" else input) + if (blink) "_" else ""
        ctx.text(font, shown, sx + 4, y + 3, if (placeholder) Theme.TEXT_MUTED else Theme.TEXT, false)
        inputRect = intArrayOf(sx, y, sw, h)
    }

    private fun tabBar(ctx: GuiGraphicsExtractor, startX: Int, tabY: Int, mouseX: Int, mouseY: Int) {
        tabRects.clear()
        var x = startX
        for (p in Page.entries) {
            val w = font.width(p.title)
            val active = p == page
            val hovered = mouseX in x until x + w && mouseY in tabY - 2 until tabY + 11
            val color = when {
                active -> Theme.ACCENT
                hovered -> Theme.TEXT
                else -> Theme.TEXT_MUTED
            }
            ctx.text(font, p.title, x, tabY, color, true)
            if (active) ctx.fill(x, tabY + 11, x + w, tabY + 12, Theme.ACCENT)
            tabRects += intArrayOf(x, tabY - 2, w, 14) to p
            x += w + 10
        }
    }

    private fun dropdownOverlay(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        dropEntryRects.clear()
        val s = state
        if (s !is ProfileState.Loaded) return
        val (hx, hy, hw, hh) = dropHeaderRect
        val rowH = 13
        val listW = maxOf(hw, s.profiles.maxOf { font.width(it.cuteName) } + 14)
        val listX = hx + hw - listW // right-aligned with the chip
        var y = hy + hh
        val listH = s.profiles.size * rowH
        ctx.fill(listX, y, listX + listW, y + listH, Theme.SURFACE)
        border(ctx, listX, y, listW, listH, Theme.BORDER)
        for ((i, profile) in s.profiles.withIndex()) {
            val rect = intArrayOf(listX, y, listW, rowH)
            val selected = i == profileIndex
            if (hit(rect, mouseX, mouseY)) ctx.fill(listX + 1, y, listX + listW - 1, y + rowH, Theme.SURFACE_ALT)
            if (selected) ctx.fill(listX + 1, y + 2, listX + 3, y + rowH - 2, Theme.ACCENT)
            ctx.text(font, profile.cuteName, listX + 8, y + 3, if (selected) Theme.ACCENT else Theme.TEXT, true)
            dropEntryRects += rect to i
            y += rowH
        }
    }

    private fun renderContent(
        ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        subnavRects.clear()
        when (val s = state) {
            is ProfileState.Loading -> centered(ctx, "Loading…", Theme.TEXT_MUTED, x + width / 2, y + height / 2)
            is ProfileState.Error -> centered(ctx, s.message, Theme.WARN, x + width / 2, y + height / 2)
            is ProfileState.Loaded -> when (page) {
                Page.GENERAL -> renderGeneral(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.SKILLS -> renderSkills(ctx, x, y, width, height, mouseX, mouseY)
                else -> placeholder(ctx, page.title, x, y, width, height)
            }
        }
    }

    private fun renderGeneral(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)

        // Side column (player render) is fixed — never scaled, no click regions.
        val sideX = x + width - GeneralPage.SIDE_WIDTH
        GeneralPage.side(s, index, height, entityFor(s, index)).render(ctx, sideX, y, mouseX, mouseY)

        val onSkill: (SkillType) -> Unit = { st -> SkillTab.forSkill(st)?.let { skillTab = it }; page = Page.SKILLS }
        val onCatacombs: () -> Unit = { page = Page.DUNGEONS }

        val mainW = width - GeneralPage.SIDE_WIDTH - GeneralPage.GAP
        val main = GeneralPage.main(s, index, mainW, onSkill, onCatacombs)
        val scale = if (main.height > height) height.toFloat() / main.height else 1f

        mainTfX = x
        mainTfY = y
        mainScale = scale

        ctx.pose().pushMatrix()
        ctx.pose().translate(x.toFloat(), y.toFloat())
        if (scale < 1f) ctx.pose().scale(scale, scale)
        val lmx = ((mouseX - x) / scale).toInt()
        val lmy = ((mouseY - y) / scale).toInt()
        main.render(ctx, 0, 0, lmx, lmy)
        ctx.pose().popMatrix()
    }

    private fun renderSkills(
        ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val navW = 92
        ctx.fill(x + navW, y, x + navW + 1, y + height, Theme.BORDER)
        var ry = y
        val rowH = 14
        for (tab in SkillTab.entries) {
            val active = tab == skillTab
            val rect = intArrayOf(x, ry, navW, rowH)
            val hovered = hit(rect, mouseX, mouseY)
            if (active || hovered) ctx.fill(x, ry, x + navW, ry + rowH, Theme.SURFACE_ALT)
            if (active) ctx.fill(x, ry, x + 2, ry + rowH, Theme.ACCENT)
            val color = if (active) Theme.ACCENT else if (hovered) Theme.TEXT else Theme.TEXT_MUTED
            ctx.text(font, tab.title, x + 8, ry + 3, color, true)
            subnavRects += rect to tab
            ry += rowH
        }
        placeholder(ctx, skillTab.title, x + navW + 1, y, width - navW - 1, height)
    }

    private fun placeholder(ctx: GuiGraphicsExtractor, title: String, x: Int, y: Int, width: Int, height: Int) {
        val c = Placeholder.build(title)
        c.render(ctx, x + (width - c.width) / 2, y + (height - c.height) / 2, -1, -1)
    }

    private fun entityFor(s: ProfileState.Loaded, index: Int): LivingEntity? {
        val profile = s.gameProfile ?: return null
        if (cachedEntity == null || cachedEntityIndex != index) {
            cachedEntity = FakePlayer.build(profile, s.profiles[index].armor)
            cachedEntityIndex = index
        }
        return cachedEntity
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!inputFocused) return false
        val cp = event.codepoint()
        if (cp >= ' '.code && cp != 127) {
            input += event.codepointAsString()
            return true
        }
        return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (inputFocused) {
            when (event.key()) {
                257, 335 -> { submit(); return true }       // ENTER / KP_ENTER
                259 -> { if (input.isNotEmpty()) input = input.dropLast(1); return true } // BACKSPACE
                256 -> { inputFocused = false; return true } // ESCAPE
                else -> return true
            }
        }
        return super.keyPressed(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x().toInt()
        val my = event.y().toInt()

        if (hit(inputRect, mx, my)) {
            inputFocused = true
            return true
        }
        inputFocused = false

        // Header toggles (so clicking it again while open closes the menu).
        if (hit(dropHeaderRect, mx, my)) {
            dropdownOpen = !dropdownOpen
            return true
        }
        if (dropdownOpen) {
            for ((rect, index) in dropEntryRects) {
                if (hit(rect, mx, my)) {
                    profileIndex = index
                    cachedEntity = null
                    dropdownOpen = false
                    return true
                }
            }
            dropdownOpen = false // clicked outside the menu
        }

        for ((rect, p) in tabRects) {
            if (hit(rect, mx, my)) { page = p; return true }
        }
        if (page == Page.SKILLS) {
            for ((rect, tab) in subnavRects) {
                if (hit(rect, mx, my)) { skillTab = tab; return true }
            }
        }
        if (page == Page.GENERAL) {
            val lmx = ((mx - mainTfX) / mainScale).toInt()
            val lmy = ((my - mainTfY) / mainScale).toInt()
            if (ClickRegistry.fire(lmx, lmy)) return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    private fun submit() {
        val name = input.trim()
        inputFocused = false
        input = ""
        if (name.isNotEmpty()) load(name)
    }

    private fun hit(rect: IntArray, mx: Int, my: Int): Boolean =
        mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3]

    private fun centered(ctx: GuiGraphicsExtractor, text: String, color: Int, cx: Int, cy: Int) {
        ctx.text(font, text, cx - font.width(text) / 2, cy, color, true)
    }

    private fun border(c: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        c.fill(x, y, x + w, y + 1, color)
        c.fill(x, y + h - 1, x + w, y + h, color)
        c.fill(x, y, x + 1, y + h, color)
        c.fill(x + w - 1, y, x + w, y + h, color)
    }
}
