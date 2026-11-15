package com.qsr.customspd.windows;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.journal.AlchemyPage;
import com.qsr.customspd.items.journal.DocumentPage;
import com.qsr.customspd.items.journal.GuidePage;
import com.qsr.customspd.items.journal.RegionLorePage;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.qsr.customspd.ui.Window;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Scene;
import com.watabou.noosa.Visual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.qsr.customspd.windows.WndJournalCatalogTest.catalogTab;
import static com.qsr.customspd.windows.WndJournalCatalogTest.centered;
import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static com.qsr.customspd.windows.WndJournalCatalogTest.grid;
import static com.qsr.customspd.windows.WndJournalCatalogTest.headerText;
import static com.qsr.customspd.windows.WndJournalCatalogTest.members;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 52aa38780 (in its v2.5.4 form) and 3dbcfef05: lore pages move from their own journal
 * tab into a lore section of the catalog tab, one grid cell per page.
 */
class WndJournalLoreTest {

	private static final int LORE_IDX = 3;

	private HeadlessRun run;
	private WndJournal wnd;
	private Game priorInstance;
	private BitmapText.Font priorFont;
	private Scene scene;
	private final Map<Document, Map<String, Integer>> savedPages = new LinkedHashMap<>();

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20261115L);
		run.enterFirstLevel();
		WndJournalCatalogTest.setUpUi();
		WndJournalCatalogTest.resetTabState();
		WndJournal.last_index = 0;
		for (Document doc : Document.values()) {
			savedPages.put(doc, new LinkedHashMap<>(pages(doc)));
		}
		priorInstance = Game.instance;
		priorFont = PixelScene.pixelFont;
		IdleCellSelector.install();
		new Game(null, null);
		WndJournalCatalogTest.setUpUi();
		scene = new Scene();
		field(Game.class, "scene").set(Game.instance, scene);
		// the game builds this from the pixel font texture at startup; a blank grid of glyphs stands in
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-lore-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			WndJournalCatalogTest.resetTabState();
			WndJournal.last_index = 0;
			for (Map.Entry<Document, Map<String, Integer>> e : savedPages.entrySet()) {
				pages(e.getKey()).clear();
				pages(e.getKey()).putAll(e.getValue());
			}
			PixelScene.pixelFont = priorFont;
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Integer> pages(Document doc) throws Exception {
		return (Map<String, Integer>) field(Document.class, "pagesStates").get(doc);
	}

	private static void loseAllLore() {
		for (Document doc : Document.values()) {
			if (!doc.isLoreDoc()) continue;
			for (String page : doc.pageNames()) doc.deletePage(page);
		}
	}

	private static String firstPage(Document doc) {
		return doc.pageNames().iterator().next();
	}

	private WndJournal open(int subTab) throws Exception {
		field(catalogTab(), "currentItemIdx").setInt(null, subTab);
		Group parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return wnd;
	}

	/** The cells under each lore document's header, after checking every header. */
	private List<List<Gizmo>> loreSections() throws Exception {
		List<Gizmo> grid = grid(wnd);
		assertFalse(grid.isEmpty(), "the lore section shows a grid");
		int seen = 0, total = 0;
		for (Document doc : Document.values()) {
			if (!doc.isLoreDoc()) continue;
			for (String page : doc.pageNames()) {
				total++;
				if (doc.isPageFound(page)) seen++;
			}
		}
		Gizmo first = grid.get(0);
		assertTrue(first instanceof ScrollingGridPane.GridHeader, "the grid opens with a header");
		assertTrue(centered(first), "the section header is centered");
		assertEquals("_" + Messages.get(catalogTab(), "title_lore") + "_ (" + seen + "/" + total + ")",
				headerText(first));

		List<List<Gizmo>> out = new ArrayList<>();
		int i = 1;
		for (Document doc : Document.values()) {
			if (!doc.isLoreDoc()) continue;
			int docSeen = 0;
			for (String page : doc.pageNames()) {
				if (doc.isPageFound(page)) docSeen++;
			}
			String name = doc.anyPagesFound() ? Messages.titleCase(doc.title()) : "???";
			assertTrue(i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridHeader,
					"a header for " + doc + " at " + i);
			assertEquals("_" + name + "_ (" + docSeen + "/" + doc.pageNames().size() + "):",
					headerText(grid.get(i)), doc + " header");
			i++;
			List<Gizmo> cells = new ArrayList<>();
			while (i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridItem) {
				cells.add(grid.get(i++));
			}
			assertEquals(doc.pageNames().size(), cells.size(), doc + " has one cell per page");
			out.add(cells);
		}
		assertEquals(grid.size(), i, "nothing follows the last lore document");
		return out;
	}

	private static List<Gizmo> cellsOf(Document doc, List<List<Gizmo>> sections) {
		int s = 0;
		for (Document d : Document.values()) {
			if (!d.isLoreDoc()) continue;
			if (d == doc) return sections.get(s);
			s++;
		}
		throw new AssertionError(doc + " is not a lore document");
	}

	private static Visual secondIcon(Gizmo cell) throws Exception {
		return (Visual) field(ScrollingGridPane.GridItem.class, "secondIcon").get(cell);
	}

	private static ColorBlock bg(Gizmo cell) throws Exception {
		return (ColorBlock) field(ScrollingGridPane.GridItem.class, "bg").get(cell);
	}

	private static void assertBg(Gizmo cell, float r, float g, float b, String what) throws Exception {
		ColorBlock bg = bg(cell);
		assertEquals(r, bg.rm, 0.001f, what + " red");
		assertEquals(g, bg.gm, 0.001f, what + " green");
		assertEquals(b, bg.bm, 0.001f, what + " blue");
	}

	private <T> T clickForPopup(Gizmo cell, Class<T> type) throws Exception {
		ScrollingGridPane.GridItem item = (ScrollingGridPane.GridItem) cell;
		assertTrue(item.onClick(item.left() + 1, item.top() + 1), "the click lands on the cell");
		List<Gizmo> shown = new ArrayList<>();
		for (Gizmo g : members(scene)) {
			if (g instanceof Window) shown.add(g);
		}
		assertEquals(1, shown.size(), "one popup opens");
		assertTrue(type.isInstance(shown.get(0)), "the popup is a " + type.getSimpleName()
				+ ", not " + shown.get(0).getClass().getSimpleName());
		return type.cast(shown.get(0));
	}

	@Test
	void theJournalHasFourTabsAndNoLoreTab() throws Exception {
		open(0);
		List<?> tabs = (List<?>) field(WndTabbed.class, "tabs").get(wnd);
		assertEquals(4, tabs.size(), "guide, alchemy, notes and catalog");
		assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.qsr.customspd.windows.WndJournal$LoreTab"));
	}

	@Test
	void theLoreSectionListsEveryLorePage() throws Exception {
		loseAllLore();
		Document.SEWERS_GUARD.findPage(firstPage(Document.SEWERS_GUARD));
		open(LORE_IDX);
		List<List<Gizmo>> sections = loreSections();

		List<Gizmo> unfound = cellsOf(Document.PRISON_WARDEN, sections);
		for (Gizmo cell : unfound) {
			assertNull(secondIcon(cell), "an unfound page shows no page number");
			assertBg(cell, 2f, 1f, 2f, "an unfound page");
		}
	}

	@Test
	void aFoundPageShowsItsPageNumber() throws Exception {
		loseAllLore();
		Document doc = Document.SEWERS_GUARD;
		List<String> names = new ArrayList<>(doc.pageNames());
		String second = names.get(1);
		doc.findPage(second);
		open(LORE_IDX);
		Gizmo cell = cellsOf(doc, loreSections()).get(1);

		Visual icon = secondIcon(cell);
		assertNotNull(icon, "a found page carries a second icon");
		assertTrue(icon instanceof BitmapText, "the second icon is a page number");
		assertEquals(Integer.toString(doc.pageIdx(second) + 1), ((BitmapText) icon).text());
		assertEquals("2", ((BitmapText) icon).text(), "page numbers are 1-based");
		assertBg(cell, 1f, 1f, 2f, "an unread page");
	}

	@Test
	void openingAFoundPageShowsItAndMarksItRead() throws Exception {
		loseAllLore();
		Document doc = Document.SEWERS_GUARD;
		String page = firstPage(doc);
		doc.findPage(page);
		assertFalse(doc.isPageRead(page));
		open(LORE_IDX);
		Gizmo cell = cellsOf(doc, loreSections()).get(0);

		clickForPopup(cell, WndStory.class);
		assertTrue(doc.isPageRead(page), "opening a page reads it");
		assertBg(cell, 1f, 1f, 1f, "a page just read");
	}

	@Test
	void anUnfoundPageSaysItIsNotFound() throws Exception {
		loseAllLore();
		open(LORE_IDX);
		Gizmo cell = cellsOf(Document.CITY_WARLOCK, loreSections()).get(0);

		WndJournalItem popup = clickForPopup(cell, WndJournalItem.class);
		List<RenderedTextBlock> texts = new ArrayList<>();
		IconTitle title = null;
		for (Gizmo g : members(popup)) {
			if (g instanceof RenderedTextBlock) texts.add((RenderedTextBlock) g);
			if (g instanceof IconTitle) title = (IconTitle) g;
		}
		assertNotNull(title, "the popup has a title bar");
		assertEquals("???", ((RenderedTextBlock) field(IconTitle.class, "tfLabel").get(title)).text());
		assertEquals(1, texts.size(), "one message block");
		assertEquals(Messages.get(catalogTab(), "not_seen_lore"), texts.get(0).text());
		assertFalse(Document.CITY_WARLOCK.isPageFound(firstPage(Document.CITY_WARLOCK)),
				"looking at an unfound page does not find it");
	}

	private static int pickUp(DocumentPage page) {
		page.page(firstPage(page.document()));
		page.doPickUp(Dungeon.hero, Dungeon.hero.pos);
		return WndJournal.last_index;
	}

	@Test
	void pickingUpALorePageOpensTheCatalogLoreSection() throws Exception {
		loseAllLore();
		WndJournal.last_index = 0;
		WndJournal.CatalogTab.currentItemIdx = 0;
		assertEquals(3, pickUp(RegionLorePage.pageForDoc(Document.SEWERS_GUARD)), "a lore page opens the catalog");
		assertEquals(3, WndJournal.CatalogTab.currentItemIdx, "at its lore section");

		WndJournal.CatalogTab.currentItemIdx = 0;
		assertEquals(1, pickUp(new AlchemyPage()), "an alchemy page still opens the alchemy tab");
		assertEquals(0, WndJournal.CatalogTab.currentItemIdx);
		WndJournal.last_index = 3;
		assertEquals(0, pickUp(new GuidePage()), "a guide page still opens the guide tab");
		assertEquals(0, WndJournal.CatalogTab.currentItemIdx);
	}
}
