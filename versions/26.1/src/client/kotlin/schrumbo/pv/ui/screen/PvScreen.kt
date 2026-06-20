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
import schrumbo.pv.data.GardenData
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
import schrumbo.pv.ui.page.CombatPage
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
    private var combatSub = CombatPage.Sub.BESTIARY
    private var bestiaryIsland = 0
    private var collectionCategory = 0
    private var inventoryTab = 0
    private var containerPage = 0

    private var input = ""
    private var inputFocused = false

    // Hunting page shard search.
    private var huntingQuery = ""
    private var huntingFocused = false
    private var huntingInputRect = intArrayOf(0, 0, 0, 0)

    private var cachedEntity: LivingEntity? = null
    private var cachedEntityIndex = -1

    private var inputRect = intArrayOf(0, 0, 0, 0)
    private var skycryptRect = intArrayOf(0, 0, 0, 0)
    private val profileChipRects = mutableListOf<Pair<IntArray, Int>>()

    private val tabRects = mutableListOf<Pair<IntArray, Page>>()
    private val combatSubRects = mutableListOf<Pair<IntArray, CombatPage.Sub>>()
    private val SUB_RAIL_W = 20   // rail depth, mirrors the top strip depth (topH)

    // Independent scroll viewports, keyed by a stable id (a page may host several — e.g. the Mobs
    // sub-page has separate kills/deaths columns). Each region keeps its offset across frames; its
    // geometry is refreshed every frame. Every region's scrollbar is wheel-scrollable and drag-able.
    private val scrollRegions = HashMap<String, ScrollRegion>()
    private val activeScroll = ArrayList<ScrollRegion>()   // regions drawn this frame
    private var dragRegion: ScrollRegion? = null
    private var dragGrab = 0                                // cursor offset within the grabbed thumb
    private var contentRect = intArrayOf(0, 0, 0, 0)

    /** A scrollable viewport: a persistent [offset] plus this-frame geometry for wheel/drag hit-tests. */
    private class ScrollRegion {
        var offset = 0
        var x = 0; var y = 0; var w = 0; var h = 0; var contentH = 0
        var thumbY = 0; var thumbH = 0
        val maxScroll get() = (contentH - h).coerceAtLeast(0)
        val hasBar get() = maxScroll > 0
    }

    private fun resetScroll(vararg ids: String) { for (id in ids) scrollRegions[id]?.offset = 0 }

    // Garden is a separate endpoint, fetched lazily the first time the Farming page is opened for a
    // profile. Key present = already requested; value null = still loading or unavailable.
    private val gardens = HashMap<String, GardenData?>()

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

        // The panel is centered: left margin == right margin, top margin == bottom margin. The left
        // sub-rail sits inside the left margin (reserved on every page so nothing shifts); the panel is
        // shrunk just enough that the rail still clears the screen edge by [edge].
        val edge = 20
        val railW = SUB_RAIL_W
        val panelW = minOf(width - 2 * (railW + edge), 680)
        val panelH = minOf(height - 2 * edge, 430)
        val px = (width - panelW) / 2
        val py = (height - panelH) / 2

        // Main category tabs run across the TOP; the content panel is the surface below them and the
        // bottom controls float under it (both on the backdrop, no shared chrome behind them). The left
        // edge of the content is reserved for per-page sub-category bookmarks (rails).
        val topH = 20
        val botH = 16
        val floatGap = 6
        val panelTop = py + topH
        val panelBottom = py + panelH - botH - floatGap

        context.fill(px, panelTop, px + panelW, panelBottom, Theme.SURFACE)
        border(context, px, panelTop, panelW, panelBottom - panelTop, Theme.BORDER)

        tabBar(context, px + 10, py, panelTop, mouseX, mouseY)
        bottomBar(context, px, panelBottom + floatGap, panelW, botH, mouseX, mouseY)

        // Pages with sub-categories get a rail OUTSIDE the panel on the left (on the backdrop, like
        // the top tabs sit above the panel); the active chip merges rightward into the surface.
        if (page == Page.COMBAT) subTabRail(context, px - SUB_RAIL_W, panelTop, mouseX, mouseY)

        val pad = 12
        val contentX = px + pad
        val contentY = panelTop + pad
        val contentW = panelW - pad * 2
        val contentH = (panelBottom - pad) - contentY
        contentRect = intArrayOf(contentX, contentY, contentW, contentH)
        activeScroll.clear()
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

    /**
     * Main-category folder tabs across the top from [topY] down to [lineY] (the body's top edge). Every
     * tab shows its icon; the active tab also shows its label and fills the body surface (no bottom
     * border) so it reads as an open folder merging into the page. Inactive tabs are raised chips.
     */
    private fun tabBar(ctx: GuiGraphicsExtractor, startX: Int, topY: Int, lineY: Int, mouseX: Int, mouseY: Int) {
        tabRects.clear()
        val iconSize = 14
        val padX = 5
        val gap = 4
        val tabH = lineY - topY
        var x = startX
        for (p in Page.entries) {
            val active = p == page
            val labelW = if (active) 3 + font.width(p.title) else 0
            val tabW = padX * 2 + iconSize + labelW
            val rect = intArrayOf(x, topY, tabW, tabH)
            val hovered = hit(rect, mouseX, mouseY)

            // Active tab fills over the body edge (merge); inactive tabs are raised chips.
            val bottom = if (active) lineY + 1 else lineY
            ctx.fill(x, topY, x + tabW, bottom, if (active) Theme.SURFACE else if (hovered) Theme.BORDER else Theme.SURFACE_ALT)
            ctx.fill(x, topY, x + tabW, topY + 1, if (active) Theme.ACCENT else Theme.BORDER)
            ctx.fill(x, topY, x + 1, lineY, Theme.BORDER)
            ctx.fill(x + tabW - 1, topY, x + tabW, lineY, Theme.BORDER)

            val iy = topY + (tabH - iconSize) / 2
            ItemRenderUtils.renderItem(ctx, icon(p.icon), x + padX, iy, iconSize / 16f)
            if (active) {
                ctx.text(font, p.title, x + padX + iconSize + 3, topY + (tabH - font.lineHeight) / 2, Theme.TEXT, false)
            } else if (hovered) {
                ctx.setComponentTooltipForNextFrame(font, listOf(Component.literal(p.title)), Hover.screenX, Hover.screenY)
            }
            tabRects += rect to p
            x += tabW + gap
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
                Page.COMBAT -> renderCombat(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.CATACOMBS -> renderDungeons(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.COLLECTIONS -> renderCollections(ctx, s, x, y, width, height, mouseX, mouseY)
                Page.MINING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> MiningPage.build(p, w) }
                Page.FISHING -> renderStatic(ctx, s, x, y, width, height, mouseX, mouseY) { p, w -> FishingPage.build(p, w) }
                Page.FARMING -> renderFarming(ctx, s, x, y, width, height, mouseX, mouseY)
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
        renderScrolled(ctx, x, y, mainW, height, mouseX, mouseY) { w, h ->
            GeneralPage.main(s, index, w, h, onSkill, onCatacombs)
        }
    }

    /** Combat tab: the left sub-tab rail is drawn as chrome (see [subTabRail]); here only the body. */
    private fun renderCombat(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        when (combatSub) {
            CombatPage.Sub.BESTIARY -> renderBestiary(ctx, s, x, y, width, height, mouseX, mouseY)
            CombatPage.Sub.MOBS -> renderMobs(ctx, s.profiles[index], x, y, width, height, mouseX, mouseY)
            CombatPage.Sub.CRIMSON -> renderScrolled(ctx, x, y, width, height, mouseX, mouseY, "crimson") { w, _ -> CombatPage.crimson(s.profiles[index], w) }
        }
    }

    /** Mobs sub-page: kills and deaths as two side-by-side columns that scroll independently. */
    private fun renderMobs(
        ctx: GuiGraphicsExtractor, p: schrumbo.pv.data.SkyblockProfile,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val m = p.combat.mobs
        if (m.kills.isEmpty() && m.deaths.isEmpty()) {
            centered(ctx, "No combat stats", Theme.TEXT_MUTED, x + width / 2, y + height / 2)
            return
        }
        val gap = 14
        val colW = (width - gap) / 2
        renderScrolled(ctx, x, y, colW, height, mouseX, mouseY, "mobs_kills") { w, _ ->
            CombatPage.mobsColumn("Kills", m.totalKills, m.kills, w)
        }
        renderScrolled(ctx, x + colW + gap, y, colW, height, mouseX, mouseY, "mobs_deaths") { w, _ ->
            CombatPage.mobsColumn("Deaths", m.totalDeaths, m.deaths, w)
        }
    }

    /**
     * Sub-category rail attached to the panel's left edge — same folder-chip look as [tabBar] but
     * rotated: the accent sits on the outer (left) edge, the active chip opens rightward into the
     * content surface (no right border). Icon-only; the label shows as a hover tooltip.
     */
    private fun subTabRail(ctx: GuiGraphicsExtractor, railX: Int, topY: Int, mouseX: Int, mouseY: Int) {
        combatSubRects.clear()
        val iconSize = 14
        val chipH = 24            // chip length along the rail (mirrors an icon-only top tab's width)
        val gap = 4
        val lead = 10             // lead-in from the corner (mirrors tabBar's startX = px + 10)
        val innerX = railX + SUB_RAIL_W   // the panel's left border edge; the rail merges here
        var cy = topY + lead
        for (sub in CombatPage.Sub.entries) {
            val active = sub == combatSub
            val rect = intArrayOf(railX, cy, SUB_RAIL_W, chipH)
            val hovered = hit(rect, mouseX, mouseY)
            // Active opens rightward 1px into the surface; inactive is closed by the panel border.
            val right = if (active) innerX + 1 else innerX
            ctx.fill(railX, cy, right, cy + chipH, if (active) Theme.SURFACE else if (hovered) Theme.BORDER else Theme.SURFACE_ALT)
            ctx.fill(railX, cy, railX + 1, cy + chipH, if (active) Theme.ACCENT else Theme.BORDER) // outer (left) accent
            ctx.fill(railX, cy, innerX, cy + 1, Theme.BORDER)                                      // top side
            ctx.fill(railX, cy + chipH - 1, innerX, cy + chipH, Theme.BORDER)                      // bottom side

            val ix = railX + (SUB_RAIL_W - iconSize) / 2
            ItemRenderUtils.renderItem(ctx, icon(sub.icon), ix, cy + (chipH - iconSize) / 2, iconSize / 16f)
            if (hovered && !active) {
                ctx.setComponentTooltipForNextFrame(font, listOf(Component.literal(sub.label)), Hover.screenX, Hover.screenY)
            }
            combatSubRects += rect to sub
            cy += chipH + gap
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

        // Header + horizontal island selector are fixed at the top; only the mob grid scrolls.
        val header = BestiaryPage.header(islands, width, active)
        header.render(ctx, x, y, mouseX, mouseY)
        var cy = y + header.height + 6

        val selector = BestiaryPage.selector(islands, active, width) { bestiaryIsland = it; resetScroll("COMBAT") }
        selector.render(ctx, x, cy, mouseX, mouseY)
        cy += selector.height + 8

        renderScrolled(ctx, x, cy, width, height - (cy - y), mouseX, mouseY) { w, _ ->
            BestiaryPage.grid(islands[active], w)
        }
    }

    private fun renderFarming(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        val p = s.profiles[index]
        val pid = p.profileId
        if (pid.isNotEmpty() && !gardens.containsKey(pid)) {
            gardens[pid] = null // mark requested before the async call so we only fetch once
            ProfileService.loadGarden(pid) { g -> gardens[pid] = g }
        }
        val garden = gardens[pid]
        renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ -> FarmingPage.build(p, garden, w) }
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
        InventoryPage.rail(p, active) { inventoryTab = it; containerPage = 0; resetScroll("INVENTORY") }
            .render(ctx, x, y, mouseX, mouseY)

        val gx = x + InventoryPage.RAIL_W + 8
        val gw = width - InventoryPage.RAIL_W - 8
        val header = InventoryPage.gridHeader(p, active, gw)
        header.render(ctx, gx, y, mouseX, mouseY)

        val gy = y + header.height + 8
        renderScrolled(ctx, gx, gy, gw, height - header.height - 8, mouseX, mouseY) { w, _ ->
            InventoryPage.body(p, active, w, containerPage) { containerPage = it; resetScroll("INVENTORY") }
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

        CollectionsPage.rail(cats, active) { collectionCategory = it; resetScroll("COLLECTIONS") }
            .render(ctx, x, bodyY, mouseX, mouseY)

        val gx = x + CollectionsPage.RAIL_W + 8
        renderScrolled(ctx, gx, bodyY, width - CollectionsPage.RAIL_W - 8, height - header.height - 8, mouseX, mouseY) { w, _ ->
            CollectionsPage.grid(cats[active], w)
        }
    }

    private fun renderDungeons(
        ctx: GuiGraphicsExtractor, s: ProfileState.Loaded,
        x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int,
    ) {
        val index = profileIndex.coerceIn(0, s.profiles.size - 1)
        renderScrolled(ctx, x, y, width, height, mouseX, mouseY) { w, _ ->
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
        id: String = page.name,
        build: (Int, Int) -> schrumbo.pv.ui.component.Component,
    ) {
        val barGap = 6
        val content = build(availW - barGap, availH)
        val r = scrollRegions.getOrPut(id) { ScrollRegion() }
        r.x = x; r.y = y; r.w = availW; r.h = availH; r.contentH = content.height
        r.offset = r.offset.coerceIn(0, r.maxScroll)
        activeScroll += r

        ctx.enableScissor(x, y, x + availW, y + availH)
        content.render(ctx, x, y - r.offset, mouseX, mouseY)
        ctx.disableScissor()

        if (r.hasBar) {
            val barW = 3
            val trackX = x + availW - barW
            ctx.fill(trackX, y, trackX + barW, y + availH, Theme.SURFACE_ALT)
            r.thumbH = (availH * availH / content.height).coerceIn(14, availH)
            r.thumbY = y + (availH - r.thumbH) * r.offset / r.maxScroll
            ctx.fill(trackX, r.thumbY, trackX + barW, r.thumbY + r.thumbH, if (dragRegion === r) Theme.ACCENT else Theme.BORDER)
        }
    }

    /** Moves a region's offset so its thumb top sits at the dragged cursor position. */
    private fun updateDrag(r: ScrollRegion, mouseY: Int) {
        val travel = r.h - r.thumbH
        if (travel <= 0) return
        val ty = (mouseY - dragGrab).coerceIn(r.y, r.y + travel)
        r.offset = ((ty - r.y).toLong() * r.maxScroll / travel).toInt()
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
        if (huntingFocused) { huntingQuery += event.codepointAsString(); resetScroll("HUNTING"); return true }
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
                259 -> { if (huntingQuery.isNotEmpty()) { huntingQuery = huntingQuery.dropLast(1); resetScroll("HUNTING") }; return true }
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
        if (page == Page.COMBAT) {
            for ((rect, sub) in combatSubRects) {
                if (hit(rect, mx, my)) { combatSub = sub; resetScroll("COMBAT", "mobs_kills", "mobs_deaths", "crimson"); return true }
            }
        }
        // Scrollbar drag: grab the thumb (or jump the track) of any region under the cursor.
        for (r in activeScroll.asReversed()) {
            if (!r.hasBar) continue
            val trackX = r.x + r.w - 3
            if (mx >= trackX - 4 && mx <= r.x + r.w && my >= r.y && my < r.y + r.h) {
                dragGrab = if (my >= r.thumbY && my < r.thumbY + r.thumbH) my - r.thumbY else r.thumbH / 2
                dragRegion = r
                updateDrag(r, my)
                return true
            }
        }
        // Page click regions live in content space; only fire when the cursor is inside the content
        // viewport so regions scrolled under the chrome stay inert.
        if (hit(contentRect, mx, my)) {
            if (ClickRegistry.fire(mx, my)) return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        for (r in activeScroll.asReversed()) {
            if (r.hasBar && mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h) {
                val step = font.lineHeight * 3
                r.offset = (r.offset - (scrollY * step).toInt()).coerceIn(0, r.maxScroll)
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val r = dragRegion ?: return super.mouseDragged(event, dragX, dragY)
        updateDrag(r, event.y().toInt())
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragRegion != null) { dragRegion = null; return true }
        return super.mouseReleased(event)
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
