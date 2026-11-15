package com.qsr.customspd.windows;

import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Upstream 75532abf0 and dd9f90c37: the journal opens on its notes tab, the tabs run notes,
 * guide, alchemy, catalog, and each tab's hover text is its section title.
 */
class WndJournalTabsTest {

	private static final String[] TAB_FIELDS = {"notesTab", "guideTab", "alchemyTab", "catalogTab"};

	private HeadlessRun run;
	private WndJournal wnd;
	private BitmapText.Font priorFont;

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20261115L);
		run.enterFirstLevel();
		WndJournalCatalogTest.setUpUi();
		WndJournalCatalogTest.resetTabState();
		Notes.reset();
		WndJournal.last_index = 0;
		priorFont = PixelScene.pixelFont;
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-journal-tabs-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			WndJournalCatalogTest.resetTabState();
			WndJournal.last_index = 0;
		} finally {
			PixelScene.pixelFont = priorFont;
			run.close();
		}
	}

	private WndJournal open() {
		Group parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return wnd;
	}

	@SuppressWarnings("unchecked")
	private static List<Button> tabs(WndJournal wnd) throws Exception {
		return (List<Button>) field(WndTabbed.class, "tabs").get(wnd);
	}

	@Test
	void tabsAreNotesGuideAlchemyCatalog() throws Exception {
		for (int i = 0; i < TAB_FIELDS.length; i++) {
			WndJournal.last_index = i;
			WndJournal w = open();
			for (int j = 0; j < TAB_FIELDS.length; j++) {
				Component tab = (Component) field(WndJournal.class, TAB_FIELDS[j]).get(w);
				assertEquals(i == j, tab.visible, "last_index " + i + ": " + TAB_FIELDS[j] + " visible");
			}
			assertEquals(i, WndJournal.last_index, "selecting tab " + i + " keeps its index");
			w.hide();
		}
	}

	@Test
	void hoverTextIsEachTabTitle() throws Exception {
		List<Button> tabs = tabs(open());
		assertEquals(4, tabs.size());
		Method hover = Button.class.getDeclaredMethod("hoverText");
		hover.setAccessible(true);
		String[] expected = {"Adventuring Notes", "Dungeon Guide", "Alchemy Guide", "Catalogs"};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], hover.invoke(tabs.get(i)), "tab " + i + " hover text");
		}
	}

	@Test
	void notesTabIconIsJournal() throws Exception {
		List<Button> tabs = tabs(open());
		Image icon = (Image) field(WndTabbed.IconTab.class, "icon").get(tabs.get(0));
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ICON_JOURNAL)), icon.texture,
				"tab 0 shows the journal icon");
		Image alchemy = (Image) field(WndTabbed.IconTab.class, "icon").get(tabs.get(2));
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ICON_ALCHEMY)), alchemy.texture,
				"tab 2 shows the alchemy icon");
	}
}
