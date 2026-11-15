package com.qsr.customspd.windows;

import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.CustomMob;
import com.qsr.customspd.actors.mobs.GaolShade;
import com.qsr.customspd.actors.mobs.HexwardMoth;
import com.qsr.customspd.actors.mobs.Keywraith;
import com.qsr.customspd.actors.mobs.Mimic;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.TumblerWisp;
import com.qsr.customspd.actors.mobs.WardstoneSentinel;
import com.qsr.customspd.items.wands.WandOfWarding;
import com.qsr.customspd.journal.Bestiary;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.levels.traps.WornDartTrap;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.plants.Firebloom;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.tiles.DungeonTileSheet;
import com.qsr.customspd.tiles.TerrainFeaturesTilemap;
import com.qsr.customspd.ui.RedButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Scene;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.qsr.customspd.windows.WndJournalCatalogTest.catalogTab;
import static com.qsr.customspd.windows.WndJournalCatalogTest.cellFor;
import static com.qsr.customspd.windows.WndJournalCatalogTest.centered;
import static com.qsr.customspd.windows.WndJournalCatalogTest.clearSeen;
import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static com.qsr.customspd.windows.WndJournalCatalogTest.grid;
import static com.qsr.customspd.windows.WndJournalCatalogTest.headerText;
import static com.qsr.customspd.windows.WndJournalCatalogTest.members;
import static com.qsr.customspd.windows.WndJournalCatalogTest.sections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 52aa38780 (in its v2.5.4 form): the catalog tab gains a bestiary sub-tab that lists
 * every Bestiary entity under one header per section, and journal entries open in a
 * WndJournalItem.
 */
class WndJournalBestiaryTest {

	private static final int BESTIARY_IDX = 2;

	private HeadlessRun run;
	private WndJournal wnd;
	private Game priorInstance;
	private Scene scene;
	private final Map<Bestiary, Map<Class<?>, Boolean>> savedSeen = new LinkedHashMap<>();
	private final Map<Bestiary, Map<Class<?>, Integer>> savedCounts = new LinkedHashMap<>();

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20240611L);
		run.enterFirstLevel();
		WndJournalCatalogTest.setUpUi();
		WndJournalCatalogTest.resetTabState();
		for (Bestiary b : Bestiary.values()) {
			savedSeen.put(b, new LinkedHashMap<>(seenMap(b)));
			savedCounts.put(b, new LinkedHashMap<>(countMap(b)));
		}
		priorInstance = Game.instance;
		IdleCellSelector.install();
		new Game(null, null);
		WndJournalCatalogTest.setUpUi();
		scene = new Scene();
		field(Game.class, "scene").set(Game.instance, scene);
		// the game compiles this sheet from the pack at startup; a blank one of the same size stands in
		TextureCache.create(Assets.Environment.TERRAIN_FEATURES, 256, 128);
		// in a run the game scene has already built the level's feature layer
		DungeonTileSheet.setupVariance(Dungeon.level.map.length, Dungeon.seedCurDepth());
		new TerrainFeaturesTilemap(Dungeon.level.plants, Dungeon.level.traps);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			WndJournalCatalogTest.resetTabState();
			for (Map.Entry<Bestiary, Map<Class<?>, Boolean>> e : savedSeen.entrySet()) {
				seenMap(e.getKey()).putAll(e.getValue());
			}
			for (Map.Entry<Bestiary, Map<Class<?>, Integer>> e : savedCounts.entrySet()) {
				countMap(e.getKey()).putAll(e.getValue());
			}
			field(TerrainFeaturesTilemap.class, "instance").set(null, null);
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<Class<?>, Boolean> seenMap(Bestiary b) throws Exception {
		return (Map<Class<?>, Boolean>) field(Bestiary.class, "seen").get(b);
	}

	@SuppressWarnings("unchecked")
	private static Map<Class<?>, Integer> countMap(Bestiary b) throws Exception {
		return (Map<Class<?>, Integer>) field(Bestiary.class, "encounterCount").get(b);
	}

	private static void setAllSeen(boolean value) throws Exception {
		for (Bestiary b : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : seenMap(b).entrySet()) {
				e.setValue(value);
			}
		}
	}

	private WndJournal open(int subTab) throws Exception {
		field(catalogTab(), "currentItemIdx").setInt(null, subTab);
		Group parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return wnd;
	}

	private Object tab() throws Exception {
		return field(WndJournal.class, "catalogTab").get(wnd);
	}

	private void updateList() throws Exception {
		Method m = catalogTab().getDeclaredMethod("updateList");
		m.setAccessible(true);
		m.invoke(tab());
	}

	/** The cells under each Bestiary section header, after checking every header. */
	private List<List<Gizmo>> bestiarySections() throws Exception {
		List<Gizmo> grid = grid(wnd);
		assertFalse(grid.isEmpty(), "the bestiary shows a grid");
		int seen = 0, total = 0;
		for (Bestiary b : Bestiary.values()) {
			seen += b.totalSeen();
			total += b.totalEntities();
		}
		Gizmo first = grid.get(0);
		assertTrue(first instanceof ScrollingGridPane.GridHeader, "the grid opens with a header");
		assertTrue(centered(first), "the section header is centered");
		assertEquals("_" + Messages.get(catalogTab(), "title_bestiary") + "_ (" + seen + "/" + total + ")",
				headerText(first));

		List<List<Gizmo>> out = new ArrayList<>();
		int i = 1;
		int cellCount = 0;
		for (Bestiary b : Bestiary.values()) {
			assertTrue(i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridHeader,
					"a header for " + b + " at " + i);
			assertEquals("_" + Messages.titleCase(b.title()) + "_ (" + b.totalSeen() + "/" + b.totalEntities() + "):",
					headerText(grid.get(i)));
			i++;
			List<Gizmo> cells = new ArrayList<>();
			while (i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridItem) {
				cells.add(grid.get(i++));
			}
			assertEquals(b.totalEntities(), cells.size(), b + " cells");
			cellCount += cells.size();
			out.add(cells);
		}
		assertEquals(grid.size(), i, "nothing follows the last section");
		assertEquals(total, cellCount, "one cell per Bestiary entity");
		return out;
	}

	private static Gizmo bestiaryCell(Class<?> cls, List<List<Gizmo>> sections) {
		Bestiary[] all = Bestiary.values();
		for (int s = 0; s < all.length; s++) {
			int idx = new ArrayList<>(all[s].entities()).indexOf(cls);
			if (idx >= 0) return sections.get(s).get(idx);
		}
		throw new AssertionError(cls.getSimpleName() + " is not in the bestiary");
	}

	private WndTitledMessage clickForPopup(Gizmo cell) throws Exception {
		ScrollingGridPane.GridItem item = (ScrollingGridPane.GridItem) cell;
		assertTrue(item.onClick(item.left() + 1, item.top() + 1), "the click lands on the cell");
		List<WndTitledMessage> shown = new ArrayList<>();
		for (Gizmo g : members(scene)) {
			if (g instanceof WndTitledMessage) shown.add((WndTitledMessage) g);
		}
		assertEquals(1, shown.size(), "one popup opens");
		return shown.get(0);
	}

	private static String message(WndTitledMessage w) throws Exception {
		List<RenderedTextBlock> texts = new ArrayList<>();
		for (Gizmo g : members(w)) {
			if (g instanceof RenderedTextBlock) texts.add((RenderedTextBlock) g);
		}
		assertEquals(1, texts.size(), "one message block");
		return texts.get(0).text();
	}

	/** A value the cell's click handler captured, such as its title, description or mob. */
	private static Object captured(Gizmo cell, String name) throws Exception {
		for (Field f : cell.getClass().getDeclaredFields()) {
			if (f.getName().contains(name)) {
				f.setAccessible(true);
				return f.get(cell);
			}
		}
		throw new AssertionError("no captured " + name + " on " + cell.getClass());
	}

	/** The title an entity cell will show, read from the click handler's captured value. */
	private static String cellTitle(Gizmo cell) throws Exception {
		return (String) captured(cell, "finalTitle");
	}

	@Test
	void theCatalogHasFourSubTabs() throws Exception {
		open(0);
		RedButton[] buttons = (RedButton[]) field(catalogTab(), "itemButtons").get(tab());
		assertEquals(4, buttons.length, "equipment, consumables, bestiary and lore");
	}

	@Test
	void theBestiaryListsEverySectionInOrder() throws Exception {
		setAllSeen(false);
		Bestiary.setSeen(Mimic.class);
		Bestiary.setSeen(Firebloom.class);
		open(0);
		field(catalogTab(), "currentItemIdx").setInt(null, BESTIARY_IDX);
		updateList();
		bestiarySections();
	}

	@Test
	void unseenEntitiesSayHowToFindThem() throws Exception {
		setAllSeen(false);
		open(BESTIARY_IDX);
		List<List<Gizmo>> sections = bestiarySections();

		Object[][] cases = {
				{Mimic.class, "not_seen_enemy"},
				{WandOfWarding.Ward.class, "not_seen_ally"},
				{WornDartTrap.class, "not_seen_trap"},
				{Firebloom.class, "not_seen_plant"},
		};
		for (Object[] c : cases) {
			WndTitledMessage popup = clickForPopup(bestiaryCell((Class<?>) c[0], sections));
			assertTrue(popup instanceof WndJournalItem, c[0] + " opens a journal entry window");
			assertEquals(Messages.get(catalogTab(), (String) c[1]), message(popup), c[0] + " message");
			popup.hide();
		}
	}

	@Test
	void everySeenEntityBuildsAndLutherverseMobsStayOut() throws Exception {
		setAllSeen(true);
		open(BESTIARY_IDX);
		List<List<Gizmo>> sections = bestiarySections();

		List<Class<? extends Mob>> excluded = Arrays.asList(Keywraith.class, GaolShade.class,
				HexwardMoth.class, TumblerWisp.class, WardstoneSentinel.class);
		List<String> excludedNames = new ArrayList<>();
		for (Class<? extends Mob> cls : excluded) {
			excludedNames.add(Messages.titleCase(Reflection.newInstance(cls).name()));
		}
		for (Bestiary b : Bestiary.values()) {
			for (Class<?> cls : b.entities()) {
				assertFalse(excluded.contains(cls), cls.getSimpleName() + " is not in the bestiary");
				assertFalse(CustomMob.class.isAssignableFrom(cls), cls.getSimpleName() + " is a pack mob");
			}
		}
		for (List<Gizmo> cells : sections) {
			for (Gizmo cell : cells) {
				String title = cellTitle(cell);
				assertFalse(title == null || title.equals("???"), "every seen cell has a real title");
				assertFalse(excludedNames.contains(title), title + " is a Lutherverse-only mob");
			}
		}
	}

	@Test
	void buildingTheBestiaryDoesNotMarkTheSentrySeen() throws Exception {
		setAllSeen(false);
		assertFalse(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class));
		open(BESTIARY_IDX);
		bestiarySections();
		assertFalse(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class),
				"the display sentry does not count as an encounter");
	}

	@Test
	void wardsAreShownAtTheirDisplayTiers() throws Exception {
		setAllSeen(true);
		open(BESTIARY_IDX);
		List<List<Gizmo>> sections = bestiarySections();
		WandOfWarding.Ward sentry = (WandOfWarding.Ward) captured(
				bestiaryCell(WandOfWarding.Ward.WardSentry.class, sections), "finalMob");
		WandOfWarding.Ward ward = (WandOfWarding.Ward) captured(
				bestiaryCell(WandOfWarding.Ward.class, sections), "finalMob");
		assertEquals(5, sentry.tier, "the sentry entry shows a tier 5 sentry");
		assertEquals(2, ward.tier, "the ward entry shows a tier 2 ward");
	}

	@Test
	void repeatEncountersAreCountedInTheDescription() throws Exception {
		setAllSeen(true);
		Object[][] cases = {
				{Mimic.class, "enemy_count"},
				{WornDartTrap.class, "trap_count"},
				{Firebloom.class, "plant_count"},
		};
		for (Object[] c : cases) {
			Bestiary.countEncounters((Class<?>) c[0], 2);
		}
		open(BESTIARY_IDX);
		List<List<Gizmo>> sections = bestiarySections();
		for (Object[] c : cases) {
			Class<?> cls = (Class<?>) c[0];
			assertEquals(2, Bestiary.encounterCount(cls), cls.getSimpleName() + " count");
			String desc = (String) captured(bestiaryCell(cls, sections), "finalDesc");
			assertTrue(desc.endsWith("\n\n" + Messages.get(catalogTab(), (String) c[1], 2)),
					cls.getSimpleName() + " description ends with its encounter count: " + desc);
		}
		String once = (String) captured(bestiaryCell(WandOfWarding.Ward.class, sections), "finalDesc");
		assertFalse(once.contains("a total of"), "an entity met at most once has no count line");
	}

	@Test
	void equipmentCellsOpenAJournalEntryWindow() throws Exception {
		clearSeen();
		open(0);
		List<List<Gizmo>> sections = sections(grid(wnd), "title_equipment", Catalog.equipmentCatalogs);
		Class<?> first = Catalog.equipmentCatalogs.get(0).items().iterator().next();
		WndTitledMessage popup = clickForPopup(cellFor(first, Catalog.equipmentCatalogs, sections));
		assertTrue(popup instanceof WndJournalItem, "an equipment cell opens a journal entry window");
		assertEquals(Messages.get(catalogTab(), "not_seen_item"), message(popup));
	}
}
