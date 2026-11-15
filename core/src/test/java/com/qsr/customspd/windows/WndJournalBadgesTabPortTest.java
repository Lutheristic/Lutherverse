package com.qsr.customspd.windows;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.effects.BadgeBanner;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.RenderThreadCalls;
import com.qsr.customspd.ui.BadgesGrid;
import com.qsr.customspd.ui.RedButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.qsr.customspd.ui.ScrollingListPane;
import com.qsr.customspd.ui.StyledButton;
import com.qsr.customspd.ui.Window;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Scene;
import com.qsr.customspd.ui.Button;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static com.qsr.customspd.windows.WndJournalCatalogTest.members;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Upstream 3dbcfef05, 42c24a948 and 3aab64684 (PixelScene hunk) put a badges tab in the journal,
 * and ebaace5f9 lets it and the guide and catalog tabs work outside a run: the tabs lay out from
 * their own origin, updateList is public, and guide pages open in front of whatever scene is up.
 */
class WndJournalBadgesTabPortTest {

	private static final String BADGES_TAB = "com.qsr.customspd.windows.WndJournal$BadgesTab";

	private HeadlessRun run;
	private Hero hero;
	private Game priorInstance;
	private BitmapText.Font priorFont;
	private Camera priorMain;
	private int priorZoom;
	private Scene scene;
	private WndJournal wnd;
	private Map<String, Integer> savedGuide;

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20261115L);
		hero = Dungeon.hero;
		WndJournalCatalogTest.setUpUi();
		WndJournalCatalogTest.resetTabState();
		WndJournal.last_index = 0;
		savedGuide = new LinkedHashMap<>(guidePages());
		priorInstance = Game.instance;
		priorFont = PixelScene.pixelFont;
		priorMain = Camera.main;
		priorZoom = PixelScene.defaultZoom;
		// PixelScene.align divides by this; the game sets it from the display at startup
		PixelScene.defaultZoom = 1;
		IdleCellSelector.install();
		new Game(null, null);
		WndJournalCatalogTest.setUpUi();
		scene = new Scene();
		field(Game.class, "scene").set(Game.instance, scene);
		if (Camera.main == null) Camera.main = new Camera(0, 0, 400, 400, 1);
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-badges-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			setGlobal(false);
			WndJournalCatalogTest.resetTabState();
			WndJournal.last_index = 0;
			BadgeBanner.showing.clear();
			guidePages().clear();
			guidePages().putAll(savedGuide);
			Dungeon.hero = hero;
			PixelScene.pixelFont = priorFont;
			Camera.main = priorMain;
			PixelScene.defaultZoom = priorZoom;
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Integer> guidePages() throws Exception {
		return (Map<String, Integer>) field(Document.class, "pagesStates").get(Document.ADVENTURERS_GUIDE);
	}

	private static Class<?> badgesTabClass() {
		try {
			return Class.forName(BADGES_TAB);
		} catch (ClassNotFoundException e) {
			fail("WndJournal has no BadgesTab class");
			return null;
		}
	}

	private static Field badgesField(String name) {
		try {
			return field(badgesTabClass(), name);
		} catch (ReflectiveOperationException e) {
			fail("BadgesTab has no field " + name);
			return null;
		}
	}

	private static void setGlobal(boolean value) throws Exception {
		try {
			field(Class.forName(BADGES_TAB), "global").setBoolean(null, value);
		} catch (ClassNotFoundException | NoSuchFieldException e) {
			// nothing to reset before the badges tab exists
		}
	}

	private static boolean global() throws Exception {
		Field f = badgesField("global");
		assertTrue(Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()),
				"BadgesTab.global is public static");
		return f.getBoolean(null);
	}

	private Component badgesTabOf(WndJournal w) throws Exception {
		Field f;
		try {
			f = field(WndJournal.class, "badgesTab");
		} catch (NoSuchFieldException e) {
			fail("WndJournal has no badgesTab field");
			return null;
		}
		Object tab = f.get(w);
		assertNotNull(tab, "the journal builds its badges tab");
		assertTrue(badgesTabClass().isInstance(tab), "badgesTab is a WndJournal.BadgesTab");
		return (Component) tab;
	}

	private WndJournal openJournal() {
		Group parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return wnd;
	}

	private static void click(Button b) throws Exception {
		Method m = Button.class.getDeclaredMethod("onClick");
		m.setAccessible(true);
		m.invoke(b);
	}

	private static int textColor(StyledButton b) throws Exception {
		RenderedTextBlock text = (RenderedTextBlock) field(StyledButton.class, "text").get(b);
		return field(RenderedTextBlock.class, "color").getInt(text);
	}

	private static Method publicUpdateList(Class<?> tab) {
		try {
			Method m = tab.getMethod("updateList");
			assertTrue(Modifier.isPublic(m.getModifiers()), tab.getSimpleName() + ".updateList is public");
			return m;
		} catch (NoSuchMethodException e) {
			fail(tab.getSimpleName() + ".updateList is not public");
			return null;
		}
	}

	@Test
	void inARunTheFifthTabIsBadgesWithThisRunAndOverall() throws Exception {
		assertNotNull(Dungeon.hero, "a run is under way");
		openJournal();
		Component tab = badgesTabOf(wnd);
		List<?> tabs = (List<?>) field(WndTabbed.class, "tabs").get(wnd);
		assertEquals(5, tabs.size(), "notes, guide, alchemy, catalog and badges");
		Image icon = (Image) field(WndTabbed.IconTab.class, "icon").get(tabs.get(4));
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ICON_BADGES)), icon.texture,
				"the fifth tab shows the badges icon");

		wnd.select(4);
		assertEquals(4, WndJournal.last_index, "selecting the badges tab remembers it");
		assertTrue(tab.visible, "the badges tab shows");
		assertTrue(tab.active, "the badges tab is active");

		List<String> labels = new ArrayList<>();
		for (Gizmo g : members(tab)) {
			if (g instanceof RedButton) labels.add(((RedButton) g).text());
		}
		List<String> expected = new ArrayList<>();
		expected.add(Messages.get(badgesTabClass(), "this_run"));
		expected.add(Messages.get(badgesTabClass(), "overall"));
		assertEquals(expected, labels, "a This Run and an Overall button");
		assertEquals("This Run", labels.get(0));
		assertEquals("Overall", labels.get(1));
		assertEquals("Badges", Messages.get(badgesTabClass(), "title"));
	}

	@Test
	void outOfARunTheBadgesTabShowsOnlyTheOverallGrid() throws Exception {
		Dungeon.hero = null;
		Component tab = (Component) badgesTabClass().getConstructor().newInstance();
		tab.setRect(0, 0, 126, 160);

		List<RenderedTextBlock> texts = new ArrayList<>();
		List<BadgesGrid> grids = new ArrayList<>();
		for (Gizmo g : members(tab)) {
			assertFalse(g instanceof RedButton, "no This Run / Overall buttons with no run");
			if (g instanceof RenderedTextBlock) texts.add((RenderedTextBlock) g);
			if (g instanceof BadgesGrid) grids.add((BadgesGrid) g);
		}
		assertEquals(1, texts.size(), "one title");
		assertEquals("Your Badges", texts.get(0).text());
		assertEquals(1, grids.size(), "one badge grid");
		assertTrue(grids.get(0).visible, "the overall grid shows");
		assertEquals(tab.top() + 14, grids.get(0).top(), 0.001f, "the grid sits under the title");
		assertSame(grids.get(0), badgesField("badgesGlobal").get(tab), "it is the overall grid");
	}

	@Test
	void theGlobalFlagPicksTheShownList() throws Exception {
		Field badgesLocal = badgesField("badgesLocal");
		Field badgesGlobal = badgesField("badgesGlobal");
		Field btnLocal = badgesField("btnLocal");
		Field btnGlobal = badgesField("btnGlobal");

		for (boolean startGlobal : new boolean[]{true, false}) {
			badgesField("global").setBoolean(null, startGlobal);
			if (wnd != null) wnd.hide();
			openJournal();
			Component tab = badgesTabOf(wnd);
			Component local = (Component) badgesLocal.get(tab);
			Component overall = (Component) badgesGlobal.get(tab);
			RedButton bLocal = (RedButton) btnLocal.get(tab);
			RedButton bGlobal = (RedButton) btnGlobal.get(tab);

			assertShows(startGlobal, local, overall, bLocal, bGlobal, "opened with global=" + startGlobal);

			click(bLocal);
			assertFalse(global(), "This Run clears the flag");
			assertShows(false, local, overall, bLocal, bGlobal, "after This Run");

			click(bGlobal);
			assertTrue(global(), "Overall sets the flag");
			assertShows(true, local, overall, bLocal, bGlobal, "after Overall");
		}
	}

	private static void assertShows(boolean global, Component local, Component overall,
			RedButton bLocal, RedButton bGlobal, String when) throws Exception {
		assertEquals(!global, local.visible, when + ": this run's badges visible");
		assertEquals(!global, local.active, when + ": this run's badges active");
		assertEquals(global, overall.visible, when + ": overall badges visible");
		assertEquals(global, overall.active, when + ": overall badges active");
		assertEquals(global ? Window.WHITE : Window.TITLE_COLOR, textColor(bLocal), when + ": This Run colour");
		assertEquals(global ? Window.TITLE_COLOR : Window.WHITE, textColor(bGlobal), when + ": Overall colour");
	}

	private void showBadge(Badges.Badge badge, Badges.BadgeType type, boolean seedGlobal,
			boolean expectGlobal) throws Exception {
		assertEquals(type, badge.type, badge + " is a " + type + " badge");
		WndJournal.last_index = 0;
		badgesField("global").setBoolean(null, seedGlobal);
		List<Callback> calls;
		try (RenderThreadCalls capture = RenderThreadCalls.capture()) {
			PixelScene.showBadge(badge);
			calls = capture.callbacks();
		}
		assertFalse(calls.isEmpty(), "showBadge posts to the render thread");
		for (Callback c : calls) c.call();
		assertEquals(4, WndJournal.last_index, badge + " opens the journal on its badges tab");
		assertEquals(expectGlobal, global(), badge + " picks the " + (expectGlobal ? "overall" : "this run") + " list");
	}

	@Test
	void aBadgePopupOpensTheJournalOnBadgesWithItsScope() throws Exception {
		showBadge(Badges.Badge.UNLOCK_MAGE, Badges.BadgeType.LOCAL, true, false);
		showBadge(Badges.Badge.GAMES_PLAYED_1, Badges.BadgeType.GLOBAL, false, true);
		showBadge(Badges.Badge.RESEARCHER_1, Badges.BadgeType.JOURNAL, false, true);
	}

	@Test
	void theGuideTabOpensPagesInFrontOutOfARun() throws Exception {
		Dungeon.hero = null;
		assertTrue(Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO), "the intro page is found");
		WndJournal.GuideTab tab = new WndJournal.GuideTab();
		scene.add(tab);
		tab.setRect(0, 0, 126, 160);
		publicUpdateList(WndJournal.GuideTab.class);
		tab.updateList();

		ScrollingListPane list = (ScrollingListPane) field(WndJournal.GuideTab.class, "list").get(tab);
		String title = Messages.titleCase(Document.ADVENTURERS_GUIDE.pageTitle(Document.GUIDE_INTRO));
		ScrollingListPane.ListItem item = null;
		List<Gizmo> found = new ArrayList<>();
		collect(list, ScrollingListPane.ListItem.class, found);
		for (Gizmo g : found) {
			RenderedTextBlock label = (RenderedTextBlock) field(ScrollingListPane.ListItem.class, "label").get(g);
			if (title.equals(label.text())) item = (ScrollingListPane.ListItem) g;
		}
		assertNotNull(item, "the guide lists the intro page");

		//an empty slot in the scene: add() would fill it, addToFront() appends
		Gizmo gone = new Gizmo();
		scene.add(gone);
		scene.add(new Gizmo());
		scene.erase(gone);
		assertTrue(members(scene).contains(null), "the scene has an empty slot behind the end");

		assertTrue(item.onClick(item.left() + 1, item.top() + 1), "the click lands on the page");
		List<Gizmo> m = members(scene);
		Gizmo last = m.isEmpty() ? null : m.get(m.size() - 1);
		assertTrue(last instanceof WndStory, "the page opens as the scene's front window, got "
				+ (last == null ? "nothing" : last.getClass().getSimpleName()));
	}

	private static void collect(Gizmo g, Class<?> type, List<Gizmo> out) throws Exception {
		if (type.isInstance(g)) {
			out.add(g);
			return;
		}
		if (g instanceof Group) {
			for (Gizmo child : new ArrayList<>(members((Group) g))) {
				if (child != null) collect(child, type, out);
			}
		}
	}

	@Test
	void theLoreTabIsStillGone() {
		assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.qsr.customspd.windows.WndJournal$LoreTab"));
	}

	@Test
	void theCatalogTabLaysOutFromItsOrigin() throws Exception {
		WndJournal.CatalogTab tab = new WndJournal.CatalogTab();
		scene.add(tab);
		tab.setRect(10, 20, 126, 160);
		RedButton[] buttons = (RedButton[]) field(WndJournal.CatalogTab.class, "itemButtons").get(tab);
		ScrollingGridPane grid = (ScrollingGridPane) field(WndJournal.CatalogTab.class, "grid").get(tab);

		assertEquals(10, buttons[0].left(), 0.001f, "the first section button starts at the tab's left");
		assertEquals(20, buttons[0].top(), 0.001f, "the first section button starts at the tab's top");
		assertEquals(10, grid.left(), 0.001f, "the grid starts at the tab's left");
		assertEquals(buttons[buttons.length - 1].bottom() + 1, grid.top(), 0.001f, "the grid sits under the buttons");

		publicUpdateList(WndJournal.CatalogTab.class);
		tab.updateList();
		assertEquals(10, grid.left(), 0.001f, "the grid keeps the tab's left after a refresh");
		assertEquals(buttons[buttons.length - 1].bottom() + 1, grid.top(), 0.001f, "and its place under the buttons");
		assertEquals(160 - buttons[buttons.length - 1].height() - 1, grid.height(), 0.001f, "the grid fills the rest");
	}
}
