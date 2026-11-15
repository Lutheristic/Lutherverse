package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.rings.Ring;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.journal.Bestiary;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.journal.Journal;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.tiles.DungeonTileSheet;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.qsr.customspd.ui.StyledButton;
import com.qsr.customspd.windows.IconTitle;
import com.qsr.customspd.windows.WndJournal;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The badges scene became the journal scene upstream (ebaace5f9, 53139c9e5, 80abc8376, 602fa2706,
 * 2b2bf9dfd, d1c2d87d5): an icon title over one toast panel holding the badges, catalog, guide or
 * alchemy tab, with four tab buttons under it, built with no run. CPDU leaves a run that a mode
 * keeps in memory alone.
 */
class JournalScenePortTest {

	private HeadlessRun run;
	private PixelScene scene;
	private Map<Object, SmartTexture> textures;
	private Map<Object, SmartTexture> earlierTextures;
	private final List<Runnable> restores = new ArrayList<>();
	private Game priorInstance;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261115L);
		@SuppressWarnings("unchecked")
		Map<Object, SmartTexture> cache = (Map<Object, SmartTexture>) read(null, TextureCache.class, "all");
		textures = cache;
		earlierTextures = new LinkedHashMap<>(textures);
		textures.clear();
		// the game is up at the title, with the journal as its scene (set in createJournal)
		priorInstance = Game.instance;
		new Game(null, null);
		// the game compiles this sheet from the pack at startup; a blank one of the same size stands in
		TextureCache.create(Assets.Environment.TERRAIN_FEATURES, 256, 128);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		Badges.loadGlobal();
		Journal.loadGlobal();
		keepJournalState();
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			destroyScene();
			Dungeon.runKeptInMemory = false;
			setLastIdx(0);
			WndJournal.CatalogTab.currentItemIdx = 0;
			WndJournal.AlchemyTab.currentPageIdx = 0;
			for (Runnable r : restores) r.run();
			Game.instance = priorInstance;
		} finally {
			if (textures != null) {
				textures.clear();
				textures.putAll(earlierTextures);
			}
			run.close();
		}
	}

	// helpers

	private void destroyScene() {
		if (scene != null) {
			PixelScene s = scene;
			scene = null;
			s.destroy();
		}
	}

	/** 1500x1000 at the default zoom is a wide camera, so the panel takes the landscape width. */
	private PixelScene createJournal(int lastIdx) throws Exception {
		destroyScene();
		setLastIdx(lastIdx);
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
		PixelScene s = new JournalScene();
		scene = s;
		Field current = Game.class.getDeclaredField("scene");
		current.setAccessible(true);
		current.set(Game.instance, s);
		s.create();
		return s;
	}

	private static void setLastIdx(int idx) throws Exception {
		Field f = JournalScene.class.getDeclaredField("lastIDX");
		f.setAccessible(true);
		f.setInt(null, idx);
	}

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> members(Group group) throws Exception {
		List<Gizmo> m = (List<Gizmo>) read(group, Group.class, "members");
		return m == null ? new ArrayList<Gizmo>() : m;
	}

	private static <T> List<T> all(Group group, Class<T> type) throws Exception {
		List<T> found = new ArrayList<>();
		for (Gizmo g : members(group)) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		return found;
	}

	private static void walk(Gizmo g, Class<?> type, List<Gizmo> out) throws Exception {
		if (type.isInstance(g)) {
			out.add(g);
			return;
		}
		if (g instanceof Group) {
			for (Gizmo child : new ArrayList<>(members((Group) g))) {
				if (child != null) walk(child, type, out);
			}
		}
	}

	private static SmartTexture texture(GeneralAsset asset) {
		return TextureCache.get(Asset.getAssetFilePath(asset));
	}

	private List<NinePatch> toastPanels() throws Exception {
		List<NinePatch> toasts = new ArrayList<>();
		for (NinePatch p : all(scene, NinePatch.class)) {
			if (p.texture == texture(GeneralAsset.TOAST)) toasts.add(p);
		}
		return toasts;
	}

	/** Snapshots the journal's seen, encounter and page maps so a test can mark them freely. */
	@SuppressWarnings("unchecked")
	private void keepJournalState() throws Exception {
		for (Catalog cat : Catalog.values()) {
			keep((Map<Object, Object>) read(cat, Catalog.class, "seen"));
			keep((Map<Object, Object>) read(cat, Catalog.class, "useCount"));
		}
		for (Bestiary b : Bestiary.values()) {
			keep((Map<Object, Object>) read(b, Bestiary.class, "seen"));
			keep((Map<Object, Object>) read(b, Bestiary.class, "encounterCount"));
		}
		for (Document doc : Document.values()) {
			keep((Map<Object, Object>) read(doc, Document.class, "pagesStates"));
		}
	}

	private void keep(final Map<Object, Object> map) {
		final Map<Object, Object> copy = new LinkedHashMap<>(map);
		restores.add(new Runnable() {
			@Override public void run() {
				map.clear();
				map.putAll(copy);
			}
		});
	}

	/** Marks every catalog item, bestiary entity and document page seen, straight in the maps. */
	@SuppressWarnings("unchecked")
	private static void seeEverything() throws Exception {
		for (Catalog cat : Catalog.values()) {
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) read(cat, Catalog.class, "seen")).entrySet()) {
				e.setValue(true);
			}
		}
		for (Bestiary b : Bestiary.values()) {
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) read(b, Bestiary.class, "seen")).entrySet()) {
				e.setValue(true);
			}
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) read(b, Bestiary.class, "encounterCount")).entrySet()) {
				e.setValue(1);
			}
		}
		for (Document doc : Document.values()) {
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) read(doc, Document.class, "pagesStates")).entrySet()) {
				e.setValue(Document.READ);
			}
		}
	}

	/** A floor played and left: the game scene had set up its tile variance, which stays behind. */
	private static void enterAndLeaveFirstLevel(HeadlessRun r) {
		r.enterFirstLevel();
		assertNotNull(Dungeon.level, "test setup: a level left over from the run");
		DungeonTileSheet.setupVariance(Dungeon.level.map.length, Dungeon.seedCurDepth());
	}

	private static Object handler(Class<?> type) throws Exception {
		return read(null, type, "handler");
	}

	// tests

	@Test
	void theTitleIsAJournalIconTitle() throws Exception {
		createJournal(0);
		List<IconTitle> titles = all(scene, IconTitle.class);
		assertEquals(1, titles.size(), "the scene title is its only icon title");
		IconTitle title = titles.get(0);
		Image icon = (Image) read(title, IconTitle.class, "imIcon");
		assertSame(texture(GeneralAsset.ICON_JOURNAL), icon.texture, "journal icon");
		assertEquals((Camera.main.width - title.reqWidth()) / 2f, title.left(), 0.5f, "centred on its own width");
		assertEquals((20 - title.height()) / 2f, title.top(), 0.5f, "centred in the top 20");
		assertEquals(200f, title.width(), 0.01f, "laid out to at most 200 wide");
	}

	@Test
	void thePanelIsOneToastAt20() throws Exception {
		createJournal(0);
		List<NinePatch> toasts = toastPanels();
		assertEquals(1, toasts.size(), "one toast panel");
		assertEquals(20f, toasts.get(0).y, 0.01f);
	}

	@Test
	void eachOfTheFourTabsBuildsWithNoRun() throws Exception {
		Class<?>[] tabs = {WndJournal.BadgesTab.class, WndJournal.CatalogTab.class,
				WndJournal.GuideTab.class, WndJournal.AlchemyTab.class};
		seeEverything();
		// the alchemy guide's thrown weapon page, whose recipes show MissileWeapon.PlaceHolder slots
		WndJournal.AlchemyTab.currentPageIdx = 6;
		for (int idx = 0; idx < 4; idx++) {
			final int i = idx;
			assertDoesNotThrow(() -> createJournal(i), "lastIDX " + i);
			assertNull(Dungeon.hero, "lastIDX " + i + ": no hero");
			for (Class<?> tab : tabs) {
				assertEquals(tab == tabs[i] ? 1 : 0, all(scene, tab).size(), "lastIDX " + i + ": " + tab.getSimpleName());
			}

			NinePatch panel = toastPanels().get(0);
			List<StyledButton> buttons = all(scene, StyledButton.class);
			assertEquals(4, buttons.size(), "lastIDX " + i + ": four tab buttons");
			SmartTexture[] icons = {texture(GeneralAsset.ICON_BADGES), texture(GeneralAsset.ICON_CATALOG),
					new ItemSprite(GeneralAsset.MASTERY, null).texture, texture(GeneralAsset.ICON_ALCHEMY)};
			// added to the back in order, so the scene holds them alchemy first
			for (int b = 0; b < 4; b++) {
				StyledButton button = buttons.get(3 - b);
				assertSame(icons[b], button.icon().texture, "lastIDX " + i + ": button " + b + " icon");
				assertEquals(panel.y + panel.height() - 3, button.top(), 1f, "lastIDX " + i + ": button " + b + " on the panel bottom");
				assertEquals(b == i ? 25f : 20f, button.height(), 0.01f, "lastIDX " + i + ": button " + b + " height");
			}
			assertEquals(panel.x, buttons.get(3).left(), 1f, "lastIDX " + i + ": first button at the panel's left");
			for (int b = 1; b < 4; b++) {
				assertTrue(buttons.get(3 - b).left() > buttons.get(4 - b).left(), "lastIDX " + i + ": buttons run left to right");
			}
		}
		// the catalog's consumables too, so every catalog item is built out of a run
		WndJournal.CatalogTab.currentItemIdx = 1;
		assertDoesNotThrow(() -> createJournal(1), "catalog consumables");
	}

	@Test
	void enteringTheJournalClearsAFinishedRun() throws Exception {
		assertNotNull(Dungeon.hero, "test setup: a run with a hero");
		assertNotNull(handler(Potion.class), "test setup: potion handler");
		assertNotNull(handler(Scroll.class), "test setup: scroll handler");
		assertNotNull(handler(Ring.class), "test setup: ring handler");
		assertFalse(Dungeon.runKeptInMemory);
		createJournal(0);
		assertNull(Dungeon.hero, "hero");
		assertNull(handler(Potion.class), "potion handler");
		assertNull(handler(Scroll.class), "scroll handler");
		assertNull(handler(Ring.class), "ring handler");
	}

	@Test
	void aRunKeptInMemorySurvivesTheJournal() throws Exception {
		Hero hero = Dungeon.hero;
		Object potions = handler(Potion.class);
		Object scrolls = handler(Scroll.class);
		Object rings = handler(Ring.class);
		assertNotNull(hero, "test setup: a run with a hero");
		Dungeon.runKeptInMemory = true;
		for (int idx = 0; idx < 2; idx++) {
			createJournal(idx);
			assertSame(hero, Dungeon.hero, "lastIDX " + idx + ": hero");
			assertSame(potions, handler(Potion.class), "lastIDX " + idx + ": potion handler");
			assertSame(scrolls, handler(Scroll.class), "lastIDX " + idx + ": scroll handler");
			assertSame(rings, handler(Ring.class), "lastIDX " + idx + ": ring handler");
			if (idx == 0) {
				List<WndJournal.BadgesTab> tabs = all(scene, WndJournal.BadgesTab.class);
				assertEquals(1, tabs.size(), "badges tab");
				assertNotNull(read(tabs.get(0), WndJournal.BadgesTab.class, "btnLocal"), "This Run button");
				assertNotNull(read(tabs.get(0), WndJournal.BadgesTab.class, "btnGlobal"), "Overall button");
			}
		}
	}

	@Test
	void aStaleLevelDoesNotBreakTheCatalog() throws Exception {
		enterAndLeaveFirstLevel(run);
		assertDoesNotThrow(() -> createJournal(1));
		assertEquals(1, all(scene, WndJournal.CatalogTab.class).size(), "the catalog tab is built");
		assertNotNull(Dungeon.level, "the level is left as it was");
	}

	@Test
	void theBestiaryAndLoreBuildFromAFreshLaunch() throws Exception {
		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.layout = null;
		Dungeon.levelName = null;
		seeEverything();
		buildBestiaryAndLore("fresh launch");

		HeadlessRun stale = HeadlessRun.start(20261116L);
		try {
			enterAndLeaveFirstLevel(stale);
			seeEverything();
			buildBestiaryAndLore("stale level");
		} finally {
			destroyScene();
			stale.close();
		}
	}

	private void buildBestiaryAndLore(String state) throws Exception {
		for (int section = 2; section <= 3; section++) {
			WndJournal.CatalogTab.currentItemIdx = section;
			final String what = state + ", catalog section " + section;
			assertDoesNotThrow(() -> createJournal(1), what);
			List<WndJournal.CatalogTab> tabs = all(scene, WndJournal.CatalogTab.class);
			assertEquals(1, tabs.size(), what);
			List<Gizmo> cells = new ArrayList<>();
			walk(tabs.get(0), ScrollingGridPane.GridItem.class, cells);
			assertFalse(cells.isEmpty(), what + ": the grid holds entries");
		}
	}

	@Test
	void aMissingLevelNameSeedsLikeANonNumericOne() {
		long seed = Dungeon.seed;
		try {
			Dungeon.seed = 123456789L;
			assertEquals(Dungeon.seedForDepth(4, "sewers_x"), assertDoesNotThrow(() -> Dungeon.seedForDepth(4, null)));
		} finally {
			Dungeon.seed = seed;
		}
	}

	@Test
	void theSceneKeysAreInEnglish() throws Exception {
		assertEquals("Journal", Messages.get(JournalScene.class, "title"));
		Properties props = new Properties();
		// tests run from the assets folder
		InputStream in = new FileInputStream("messages/scenes/scenes.properties");
		try {
			props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} finally {
			in.close();
		}
		assertTrue(props.containsKey("scenes.journalscene.title"), "journalscene.title present");
		assertFalse(props.containsKey("scenes.badgesscene.title"), "badgesscene.title gone");
	}
}
