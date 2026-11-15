package com.qsr.customspd.items.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.windows.WndJournal;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import com.watabou.noosa.Camera;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.watabou.input.KeyBindings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 75532abf0: with the notes tab first, a key opens the journal on notes (0), a guide
 * page on the guide (1), an alchemy page on the alchemy guide (2) and a lore page on the
 * catalog's lore section (3). The journal button follows the same targets.
 */
class JournalTabTargetsTest {

	private HeadlessRun run;
	private final Map<Document, Map<String, Integer>> savedPages = new LinkedHashMap<>();

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20261115L);
		run.enterFirstLevel();
		Notes.reset();
		for (Document doc : Document.values()) {
			savedPages.put(doc, new LinkedHashMap<>(pages(doc)));
		}
		WndJournal.last_index = 99;
		WndJournal.CatalogTab.currentItemIdx = 0;
		WndJournal.AlchemyTab.currentPageIdx = 0;
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			WndJournal.last_index = 0;
			WndJournal.CatalogTab.currentItemIdx = 0;
			WndJournal.AlchemyTab.currentPageIdx = 0;
			for (Map.Entry<Document, Map<String, Integer>> e : savedPages.entrySet()) {
				pages(e.getKey()).clear();
				pages(e.getKey()).putAll(e.getValue());
			}
		} finally {
			run.close();
		}
	}

	private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Integer> pages(Document doc) throws Exception {
		return (Map<String, Integer>) field(Document.class, "pagesStates").get(doc);
	}

	private static String page(Document doc, int i) {
		return new ArrayList<>(doc.pageNames()).get(i);
	}

	private static void pickUp(DocumentPage item, String page) {
		item.page(page);
		item.doPickUp(Dungeon.hero, Dungeon.hero.pos);
	}

	@Test
	void aKeyOpensTheNotesTab() {
		new IronKey("1").doPickUp(Dungeon.hero, Dungeon.hero.pos);
		assertEquals(0, WndJournal.last_index);
	}

	@Test
	void aGuidePageOpensTheGuideTab() {
		pickUp(new GuidePage(), page(Document.ADVENTURERS_GUIDE, 1));
		assertEquals(1, WndJournal.last_index);
	}

	@Test
	void anAlchemyPageOpensTheAlchemyTabAtItsPage() {
		String page = page(Document.ALCHEMY_GUIDE, 1);
		pickUp(new AlchemyPage(), page);
		assertEquals(2, WndJournal.last_index);
		assertEquals(Document.ALCHEMY_GUIDE.pageIdx(page), WndJournal.AlchemyTab.currentPageIdx);
	}

	@Test
	void aLorePageOpensTheCatalogLoreSection() {
		// guard: the lore branch was already in place before the tab reorder
		pickUp(RegionLorePage.pageForDoc(Document.SEWERS_GUARD), page(Document.SEWERS_GUARD, 0));
		assertEquals(3, WndJournal.last_index);
		assertEquals(3, WndJournal.CatalogTab.currentItemIdx);
	}

	@Test
	void theJournalButtonFollowsTheSameTargets() throws Exception {
		Game priorInstance = Game.instance;
		BitmapText.Font priorFont = PixelScene.pixelFont;
		IdleCellSelector.install();
		try {
			new Game(null, null);
			setUpUi();
			PixelScene.pixelFont = new BitmapText.Font(
					TextureCache.create("test-tab-targets-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
					4, BitmapText.Font.LATIN_FULL);
			GameScene scene = new GameScene();
			field(Game.class, "scene").set(Game.instance, scene);
			field(GameScene.class, "scene").set(null, scene);

			assertEquals(2, clickWhileFlashing(Document.ALCHEMY_GUIDE, page(Document.ALCHEMY_GUIDE, 0)),
					"a flashing alchemy page opens the alchemy tab");
			assertEquals(1, clickWhileFlashing(Document.ADVENTURERS_GUIDE, page(Document.ADVENTURERS_GUIDE, 1)),
					"a flashing guide page opens the guide tab next time");
			WndJournal.CatalogTab.currentItemIdx = 0;
			assertEquals(3, clickWhileFlashing(Document.SEWERS_GUARD, page(Document.SEWERS_GUARD, 0)),
					"a flashing lore page opens the catalog next time");
			assertEquals(3, WndJournal.CatalogTab.currentItemIdx, "at its lore section");
		} finally {
			field(GameScene.class, "scene").set(null, null);
			PixelScene.pixelFont = priorFont;
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		}
	}

	private static int clickWhileFlashing(Document doc, String page) throws Exception {
		Class<?> cls = Class.forName("com.qsr.customspd.ui.MenuPane$JournalButton");
		Constructor<?> ctor = cls.getDeclaredConstructor();
		ctor.setAccessible(true);
		Object button = ctor.newInstance();
		field(cls, "flashingDoc").set(button, doc);
		field(cls, "flashingPage").set(button, page);
		WndJournal.last_index = 99;
		Method onClick = cls.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
		return WndJournal.last_index;
	}

	/** The headless display and key setup a window needs (as the journal window tests use). */
	private static void setUpUi() {
		PixelScene.uiCamera = new Camera(0, 0, 400, 400, 1);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		KeyBindings.setAllControllerBindings(SPDAction.getControllerDefaults());
	}
}
