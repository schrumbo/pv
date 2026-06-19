package schrumbo.pv.ui.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.Util
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import schrumbo.pv.api.ProfileService
import schrumbo.pv.data.ProfileState
import schrumbo.pv.data.SkillType
import schrumbo.pv.render.FakePlayer
import schrumbo.pv.render.ItemRenderUtils
import schrumbo.pv.ui.Page
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.ClickRegistry
import schrumbo.pv.ui.component.Hover
import schrumbo.pv.ui.page.BestiaryPage
import schrumbo.pv.ui.page.CollectionsPage
import schrumbo.pv.ui.page.DungeonsPage
import schrumbo.pv.ui.page.FarmingPage
import schrumbo.pv.ui.page.FishingPage
import schrumbo.pv.ui.page.ForagingPage
import schrumbo.pv.ui.page.GeneralPage
import schrumbo.pv.ui.page.HuntingPage
import schrumbo.pv.ui.page.InventoryPage
import schrumbo.pv.ui.page.MiningPage
import schrumbo.pv.ui.page.PetsPage
import schrumbo.pv.ui.page.Placeholder
import schrumbo.pv.ui.page.RiftPage

/** Root profile-viewer screen: icon tab bar on top, the page, and a bottom bar with profile chips,
 *  player search, and an "Open in SkyCrypt" button. */
class PvScreen(target: String) : Screen(Component.literal("Profile Viewer")) {

    private var state: ProfileState = ProfileState.Loading
    private var profileIndex = 0

    private var page = Page.GENERAL
    private var dungeonMaster = false
    private var bestiaryIsland = 0
    private var bestiaryRailScroll = 0
    private var collectionCategory = 0
    private var inventoryTab = 0
    private var containerPage = 0

    private var input = ""
    private var inputFocused = false

    // Hunting page shard search.
    private var huntingQuery = ""
    private var huntingFocused = false
    private var huntingInputRect = intArrayOf(0, 0, 0, 0)

    // Bestiary island rail (scrolls independently of the mob grid when it overflows the panel).
    private var railRect = intArrayOf(0, 0, 0, 0)
    private var railMaxScroll = 0

    private var cachedEntity: LivingEntity? = null
    private var cachedEntityIndex = -1

    private var inputRect = intArrayOf(0, 0, 0, 0)
    private var skycryptRect = intArrayOf(0, 0, 0, 0)
    private val profileChipRects = mutableListOf<Pair<IntArray, Int>>()

    private val tabRects = mutableListOf<Pair<IntArray, Page>>()

    // Transform of the (possibly scaled) General main column, for click hit-testing.
    private var mainTfX = 0
    private var mainTfY = 0
    private var mainScale = 1f

    // Vertical scroll offset per page (scrollable pages render at scale 1 and scroll instead of
    // rescaling when their content — or a sub-page — overflows the panel).
    private val scrollOffsets = HashMap<Page, Int>()
    private var curMaxScroll = 0
    private var contentRect = intArrayOf(0, 0, 0, 0)

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

        val topH = 22
        val botH = 24
        tabBar(context, px + 10, py + 3, mouseX, mouseY)
        context.fill(px, py + topH, px + panelW, py + topH + 1, Theme.BORDER)

        val barTop = py + panelH - botH
        context.fill(px, barTop, px + panelW, barTop + 1, Theme.BORDER)
        bottomBar(context, px, barTop + 1, panelW, botH - 1, mouseX, mouseY)

        val pad = 12
        val contentX = px + pad
        val contentY = py + topH + 1 + pad
        val contentW = panelW - pad * 2
        val contentH = (barTop - pad) - contentY
        contentRect = intArrayOf(contentX, contentY, contentW, contentH)
        curMaxScroll = 0
        renderContent(context, contentX, contentY, contentW, contentH, mouseX, mouseY)
    }

    /** Bottom bar: floating profile chips on the left, player search + Open-in-SkyCrypt on the right. */
    private fun bottomBar(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, mouseX: Int, mouseY: Int) {
        profileChipRects.clear()
        val ch = 16
        val cy = y + (h - ch) / 2
        val s = state

        // Open in SkyCrypt — far right, only when a profile is loaded.
        val skyLabel = "Open in SkyCrypt"
        val skyW = font.width(skyLabel) + 14
        val skyX = x + w - 8 - skyW
        if (s is ProfileState.Loaded && skyCryptUrl() != null) {
            val hovered = hit(intArrayOf(skyX, cy, skyW, ch), mouseX, mouseY)
            ctx.fill(skyX, cy, skyX + skyW, cy + ch, if (hovered) Theme.ACCENT else Theme.SURFACE_ALT)
            border(ctx, skyX, cy, skyW, ch, Theme.ACCENT)
            ctx.text(font, skyLabel, skyX + 7, cy + 4, if (hovered) Theme.SURFACE else Theme.ACCENT, false)
            skycryptRect = intArrayOf(skyX, cy, skyW, ch)
        } else {
            skycryptRect = intArrayOf(0, 0, 0, 0)
        }

        // Player search — left of the SkyCrypt button.
        val sw = 150
        val sx = skyX - 6 - sw
        ctx.fill(sx, cy, sx + sw, cy + ch, Theme.SURFACE)
        border(ctx, sx, cy, sw, ch, if (inputFocused) Theme.ACCENT else Theme.BORDER)
        val blink = inputFocused && (System.currentTimeMillis() / 500) % 2 == 0L
        val placeholder = input.isEmpty() && !inputFocused
        val shown = (if (placeholder) "search player…" else input) + if (blink) "_" else ""
        ctx.text(font, shown, sx + 5, cy + 4, if (placeholder) Theme.TEXT_MUTED else Theme.TEXT, false)
        inputRect = intArrayOf(sx, cy, sw, ch)

        // Floating profile chips — fill from the left, stopping before the search box.
        if (s !is ProfileState.Loaded) return
        var chipX = x + 10
        for ((i, profile) in s.profiles.withIndex()) {
            val cw = font.width(profile.cuteName) + 14
            if (chipX + cw > sx - 8) break
            val active = i == profileIndex.coerceIn(0, s.profiles.size - 1)
            val hovered = hit(intArrayOf(chipX, cy, cw, ch), mouseX, mouseY)
            ctx.fill(chipX, cy, chipX + cw, cy + ch, Theme.SURFACE_ALT)
            border(ctx, chipX, cy, cw, ch, if (active) Theme.ACCENT else Theme.BORDER)
            val color = if (active) Theme.ACCENT else if (hovered) Theme.TEXT else Theme.TEXT_MUTED
            ctx.text(font, profile.cuteName, chipX + 7, cy + 4, color, true)
            profileChipRects += intArrayOf(chipX, cy, cw, ch) to i
            chipX += cw + 6
        }
    }

    /** sky.shiiyu.moe URL for the current player + profile, or null when no name is resolved. */
    private fun skyCryptUrl(): String? {
        val s = state as? ProfileState.Loaded ?: return null
        val name = s.gameProfile?.name ?: return null
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        return "https://sky.shiiyu.moe/stats/$name/${s.profiles[index].cuteName}"
    }

    private fun tabBar(ctx: GuiGraphicsExtractor, startX: Int, iconY: Int, mouseX: Int, mouseY: Int) {
        tabRects.clear()
        val size = 16
        val gap = 7
        var x = startX
        for (p in Page.entries) {
            val rect = intArrayOf(x, iconY, size, size)
            val active = p == page
            val hovered = hit(rect, mouseX, mouseY)
            ItemRenderUtils.renderItem(ctx, icon(p.icon), x, iconY, 1f)
            when {
                active -> ctx.fill(x, iconY + size, x + size, iconY + size + 1, Theme.ACCENT)
                hovered -> ctx.fill(x, iconY + size, x + size, iconY + size + 1, Theme.BORDER)
            }
            if (hovered) {
                ctx.setComponentTooltipForNextFrame(font, listOf(Component.literal(p.title)), Hover.screenX, Hover.screenY)
            }
            tabRects += rect to p
            x += size + gap
        }
    }

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }

    private fun renderContent(
        ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        when (val s = state) {
            is ProfileState.Loading -> centered(ctx, "Loading…", Theme.TEXT_MUTED, x + width / 2, y + height / 2)
            is ProfileState.Error -> centered(ctx, s.message, Theme.WARN, x + width / 2, y + height / 2)
            is ProfileState.Loaded -> when (page) {
                Page.GENERAL -> renderGeneral(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.BESTIARY -> renderBestiary(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.CATACOMBS -> renderDungeons(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.COLLECTIONS -> renderCollections(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.MINING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> MiningPage.build(p, w) }
                Page.FISHING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> FishingPage.build(p, w) }
                Page.FARMING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> FarmingPage.build(p, w) }
                Page.HUNTING -> renderHunting(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.FORAGING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> ForagingPage.build(p, w) }
                Page.PETS -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> PetsPage.build(p, w) }
                Page.INVENTORY -> renderInventory(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.RIFT -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> RiftPage.build(p, w) }
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

        val onSkill: (SkillType) -> Unit = { st -> Page.forSkill(st)?.let { page = it } }
        val onCatacombs: () -> Unit = { page = Page.CATACOMBS }

        val mainW = width - GeneralPage.SIDE_WIDTH - GeneralPage.GAP
        renderScaledFill(ctx, x, y, mainW, height, mouseX, mouseY) { w, h ->
            GeneralPage.main(s, index, w, h, onSkill, onCatacombs)
        }
    }

    private fun renderBestiary(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        val islands = BestiaryPage.islands(s.profiles[index])
        if (islands.isEmpty()) {
            renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ -> BestiaryPage.header(islands, w) }
            return
        }
        val active = bestiaryIsland.coerceIn(0, islands.size - 1)

        val header = BestiaryPage.header(islands, width)
        header.render(ctx, x, y, mouseX, mouseY)
        val bodyY = y + header.height + 8
        val bodyH = height - header.height - 8

        // Island rail — scrolls on its own when taller than the panel, so every island stays reachable.
        val rail = BestiaryPage.rail(islands, active) { bestiaryIsland = it; scrollOffsets[Page.BESTIARY] = 0 }
        railMaxScroll = (rail.height - bodyH).coerceAtLeast(0)
        bestiaryRailScroll = bestiaryRailScroll.coerceIn(0, railMaxScroll)
        railRect = intArrayOf(x, bodyY, BestiaryPage.RAIL_W, bodyH)

        val before = ClickRegistry.regions.size
        ctx.enableScissor(x, bodyY, x + BestiaryPage.RAIL_W, bodyY + bodyH)
        rail.render(ctx, x, bodyY - bestiaryRailScroll, mouseX, mouseY)
        ctx.disableScissor()
        // Drop rail regions scrolled outside the viewport so they can't be clicked through the chrome.
        val regs = ClickRegistry.regions
        var i = before
        while (i < regs.size) {
            if (regs[i].y + regs[i].h <= bodyY || regs[i].y >= bodyY + bodyH) regs.removeAt(i) else i++
        }
        if (railMaxScroll > 0) {
            val barW = 2
            val tx = x + BestiaryPage.RAIL_W - barW
            ctx.fill(tx, bodyY, tx + barW, bodyY + bodyH, Theme.SURFACE_ALT)
            val thumbH = (bodyH * bodyH / rail.height).coerceIn(12, bodyH)
            val thumbY = bodyY + (bodyH - thumbH) * bestiaryRailScroll / railMaxScroll
            ctx.fill(tx, thumbY, tx + barW, thumbY + thumbH, Theme.BORDER)
        }

        val gx = x + BestiaryPage.RAIL_W + 14
        renderScrolled(ctx, gx, bodyY, width - BestiaryPage.RAIL_W - 14, bodyH, mouseX, mouseY) { w, _ ->
            BestiaryPage.grid(islands[active], w)
        }
    }

    /** Renders a page that needs no per-page state — built from the active profile and width. */
    private fun renderStatic(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
        build: (schrumbo.pv.data.SkyblockProfile, Int) -> schrumbo.pv.ui.component.Component,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ -> build(s.profiles[index], w) }
    }

    private fun renderHunting(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        val p = s.profiles[index]

        val header = HuntingPage.header(p, width)
        header.render(ctx, x, y, mouseX, mouseY)
        val cy = y + header.height + 6

        // Search bar — filters the shard grid by name (empty = only owned shards).
        val boxH = 16
        ctx.fill(x, cy, x + width, cy + boxH, Theme.SURFACE)
        border(ctx, x, cy, width, boxH, if (huntingFocused) Theme.ACCENT else Theme.BORDER)
        val blink = huntingFocused && (System.currentTimeMillis() / 500) % 2 == 0L
        val placeholder = huntingQuery.isEmpty() && !huntingFocused
        val shown = (if (placeholder) "search attribute / mob / shard…" else huntingQuery) + if (blink) "_" else ""
        ctx.text(font, shown, x + 5, cy + 4, if (placeholder) Theme.TEXT_MUTED else Theme.TEXT, false)
        huntingInputRect = intArrayOf(x, cy, width, boxH)

        val bodyY = cy + boxH + 8
        renderScrolled(ctx, x, bodyY, width, height - (bodyY - y), mouseX, mouseY) { w, _ ->
            HuntingPage.grid(p, huntingQuery, w)
        }
    }

    private fun renderInventory(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        val p = s.profiles[index]
        if (InventoryPage.isEmpty(p)) {
            renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ -> InventoryPage.disabled(w) }
            return
        }
        val active = inventoryTab.coerceIn(0, InventoryPage.entryCount(p) - 1)

        // Fixed rail (never scrolls); clicks resolve at scale 1 — renderScrolled sets that transform.
        InventoryPage.rail(p, active) { inventoryTab = it; containerPage = 0; scrollOffsets[Page.INVENTORY] = 0 }
            .render(ctx, x, y, mouseX, mouseY)

        val gx = x + InventoryPage.RAIL_W + 14
        val gw = width - InventoryPage.RAIL_W - 14
        val header = InventoryPage.gridHeader(p, active, gw)
        header.render(ctx, gx, y, mouseX, mouseY)

        val gy = y + header.height + 8
        renderScrolled(ctx, gx, gy, gw, height - header.height - 8, mouseX, mouseY) { w, _ ->
            InventoryPage.body(p, active, w, containerPage) { containerPage = it; scrollOffsets[Page.INVENTORY] = 0 }
        }
    }

    private fun renderCollections(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        val cats = CollectionsPage.categories(s.profiles[index])
        if (cats.isEmpty()) {
            renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ -> CollectionsPage.header(cats, w) }
            return
        }
        val active = collectionCategory.coerceIn(0, cats.size - 1)

        val header = CollectionsPage.header(cats, width)
        header.render(ctx, x, y, mouseX, mouseY)
        val bodyY = y + header.height + 8

        CollectionsPage.rail(cats, active) { collectionCategory = it; scrollOffsets[Page.COLLECTIONS] = 0 }
            .render(ctx, x, bodyY, mouseX, mouseY)

        val gx = x + CollectionsPage.RAIL_W + 14
        renderScrolled(ctx, gx, bodyY, width - CollectionsPage.RAIL_W - 14, height - header.height - 8, mouseX, mouseY) { w, _ ->
            CollectionsPage.grid(cats[active], w)
        }
    }

    private fun renderDungeons(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        renderScaledFill(ctx, x, y, width, height, mouseX, mouseY) { w, _ ->
            DungeonsPage.build(s.profiles[index], w, dungeonMaster) { dungeonMaster = it }
        }
    }

    /**
     * Renders a click-aware page at scale 1 inside a scissor-clipped viewport; content taller than
     * [availH] scrolls vertically via the mouse wheel instead of being rescaled. This keeps a single,
     * stable scale across a page's sub-pages — switching island/tab/category never resizes anything.
     */
    private fun renderScrolled(
        ctx: GuiGraphicsExtractor, x: Int, y: Int, availW: Int, availH: Int, mouseX: Int, mouseY: Int,
        build: (Int, Int) -> schrumbo.pv.ui.component.Component,
    ) {
        val barGap = 6
        val content = build(availW - barGap, availH)
        val maxScroll = (content.height - availH).coerceAtLeast(0)
        curMaxScroll = maxScroll
        val off = scrollOffsets.getOrDefault(page, 0).coerceIn(0, maxScroll)
        scrollOffsets[page] = off

        // Scrollable pages draw at absolute coords (scale 1); clicks need no transform.
        mainTfX = 0
        mainTfY = 0
        mainScale = 1f

        ctx.enableScissor(x, y, x + availW, y + availH)
        content.render(ctx, x, y - off, mouseX, mouseY)
        ctx.disableScissor()

        if (maxScroll > 0) {
            val barW = 3
            val trackX = x + availW - barW
            ctx.fill(trackX, y, trackX + barW, y + availH, Theme.SURFACE_ALT)
            val thumbH = (availH * availH / content.height).coerceIn(14, availH)
            val thumbY = y + (availH - thumbH) * off / maxScroll
            ctx.fill(trackX, thumbY, trackX + barW, thumbY + thumbH, Theme.BORDER)
        }
    }

    /**
     * Renders a click-aware page so it fills [availW]×[availH]. The page is built once; if it's
     * taller than [availH] it's rebuilt at a proportionally wider logical width and then scaled down
     * uniformly — so the down-scale fills the width instead of leaving an empty strip on the right.
     */
    private fun renderScaledFill(
        ctx: GuiGraphicsExtractor, x: Int, y: Int, availW: Int, availH: Int, mouseX: Int, mouseY: Int,
        build: (Int, Int) -> schrumbo.pv.ui.component.Component,
    ) {
        var content = build(availW, availH)
        var scale = 1f
        if (content.height > availH) {
            val first = availH.toFloat() / content.height
            content = build((availW / first).toInt().coerceAtLeast(availW), availH)
            scale = availH.toFloat() / content.height
        }
        mainTfX = x
        mainTfY = y
        mainScale = scale

        ctx.pose().pushMatrix()
        ctx.pose().translate(x.toFloat(), y.toFloat())
        if (scale < 1f) ctx.pose().scale(scale, scale)
        val lmx = ((mouseX - x) / scale).toInt()
        val lmy = ((mouseY - y) / scale).toInt()
        content.render(ctx, 0, 0, lmx, lmy)
        ctx.pose().popMatrix()
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
        val cp = event.codepoint()
        if (cp < ' '.code || cp == 127) return false
        if (inputFocused) { input += event.codepointAsString(); return true }
        if (huntingFocused) { huntingQuery += event.codepointAsString(); scrollOffsets[Page.HUNTING] = 0; return true }
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
        if (huntingFocused) {
            when (event.key()) {
                259 -> { if (huntingQuery.isNotEmpty()) { huntingQuery = huntingQuery.dropLast(1); scrollOffsets[Page.HUNTING] = 0 }; return true }
                256, 257, 335 -> { huntingFocused = false; return true } // ESCAPE / ENTER
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
            huntingFocused = false
            return true
        }
        if (page == Page.HUNTING && hit(huntingInputRect, mx, my)) {
            huntingFocused = true
            inputFocused = false
            return true
        }
        inputFocused = false
        huntingFocused = false

        if (hit(skycryptRect, mx, my)) {
            skyCryptUrl()?.let { Util.getPlatform().openUri(it) }
            return true
        }
        for ((rect, index) in profileChipRects) {
            if (hit(rect, mx, my)) {
                profileIndex = index
                cachedEntity = null
                return true
            }
        }

        for ((rect, p) in tabRects) {
            if (hit(rect, mx, my)) { page = p; return true }
        }
        // Page click regions live in the (possibly scaled) content space; only fire when the cursor
        // is inside the content viewport so regions scrolled under the chrome stay inert.
        if (hit(contentRect, mx, my)) {
            val lmx = ((mx - mainTfX) / mainScale).toInt()
            val lmy = ((my - mainTfY) / mainScale).toInt()
            if (ClickRegistry.fire(lmx, lmy)) return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (page == Page.BESTIARY && railMaxScroll > 0 && hit(railRect, mouseX.toInt(), mouseY.toInt())) {
            val step = font.lineHeight * 3
            bestiaryRailScroll = (bestiaryRailScroll - (scrollY * step).toInt()).coerceIn(0, railMaxScroll)
            return true
        }
        if (curMaxScroll > 0 && hit(contentRect, mouseX.toInt(), mouseY.toInt())) {
            val step = font.lineHeight * 3
            val cur = scrollOffsets.getOrDefault(page, 0)
            scrollOffsets[page] = (cur - (scrollY * step).toInt()).coerceIn(0, curMaxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
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
