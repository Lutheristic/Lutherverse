package com.qsr.customspd.windows;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.sprites.StatueSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static com.qsr.customspd.windows.WndJournalCatalogTest.grid;
import static com.qsr.customspd.windows.WndJournalCatalogTest.headerText;
import static com.qsr.customspd.windows.WndJournalCatalogTest.icon;
import static com.qsr.customspd.windows.WndJournalCatalogTest.members;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The notes tab on the grid (upstream dd9f90c37, 995a8836c, bf973930b): a title and description,
 * then one header per visited floor, deepest first, with that floor's records under it.
 */
class WndJournalNotesGridTest {

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
		priorFont = PixelScene.pixelFont;
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-notes-grid-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			WndJournalCatalogTest.resetTabState();
		} finally {
			PixelScene.pixelFont = priorFont;
			run.close();
		}
	}

	private List<Gizmo> notesGrid() throws Exception {
		Group parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return grid((Gizmo) field(WndJournal.class, "notesTab").get(wnd));
	}

	private static List<String> headers(List<Gizmo> grid) throws Exception {
		List<String> out = new ArrayList<>();
		for (Gizmo g : grid) {
			if (g instanceof ScrollingGridPane.GridHeader) out.add(headerText(g));
		}
		return out;
	}

	/** The cells after the header with this text, up to the next header. */
	private static List<Gizmo> itemsUnder(List<Gizmo> grid, String header) throws Exception {
		assertTrue(headers(grid).contains(header), "header " + header + " is shown");
		List<Gizmo> out = new ArrayList<>();
		boolean in = false;
		for (Gizmo g : grid) {
			if (g instanceof ScrollingGridPane.GridHeader) {
				if (in) break;
				in = header.equals(headerText(g));
			} else if (in) {
				out.add(g);
			}
		}
		return out;
	}

	private static void addLandmark(String floor, Notes.Landmark landmark) {
		String prior = Dungeon.levelName;
		Dungeon.levelName = floor;
		Notes.add(landmark);
		Dungeon.levelName = prior;
	}

	private static Object texture(GeneralAsset asset) {
		return TextureCache.get(Asset.getAssetFilePath(asset));
	}

	@Test
	void headersAreTitleDescThenFloorsDeepestFirst() throws Exception {
		Dungeon.visited = new String[]{"1", "2", "3"};
		Dungeon.levelName = "2";
		addLandmark("1", Notes.Landmark.GARDEN);
		addLandmark("3", Notes.Landmark.STATUE);

		assertEquals(Arrays.asList(
				"_Adventuring Notes_",
				"As you journey through the dungeon, you will automatically record noteworthy things here.",
				"Floor 3", "_Floor 2_", "Floor 1"), headers(notesGrid()));
	}

	@Test
	void unknownFloorGoesLastUnderItsRawName() throws Exception {
		Dungeon.visited = new String[]{"1", "2", "3"};
		Dungeon.levelName = "2";
		addLandmark("1", Notes.Landmark.GARDEN);
		addLandmark("3", Notes.Landmark.STATUE);
		Notes.add(new IronKey("gone"));

		List<Gizmo> grid = notesGrid();
		List<String> headers = headers(grid);
		assertTrue(headers.contains("gone"), "the unknown floor has a header");
		assertEquals("gone", headers.get(headers.size() - 1), "the unknown floor is last, under its raw name");
		assertEquals(1, itemsUnder(grid, "gone").size(), "one cell under it");
	}

	@Test
	void depthZeroFloorShowsAsFloorZero() throws Exception {
		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = testLayout();
			Dungeon.visited = new String[]{"z0", "a5"};
			Dungeon.levelName = "a5";
			List<String> headers = headers(notesGrid());
			assertTrue(headers.size() >= 2, "the title and description headers are shown");
			assertEquals(Arrays.asList("_Floor 5_", "Floor 0"), headers.subList(2, headers.size()));
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void twoFloorsAtOneDepthGetTwoHeadersAndOnlyTheCurrentIsHighlighted() throws Exception {
		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = testLayout();
			Dungeon.visited = new String[]{"a5", "b5"};
			Dungeon.levelName = "b5";
			List<String> headers = headers(notesGrid());
			assertTrue(headers.size() >= 2, "the title and description headers are shown");
			assertEquals(Arrays.asList("Floor 5", "_Floor 5_"), headers.subList(2, headers.size()));
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void keyWithQuantityTwoShowsASecondIcon() throws Exception {
		Dungeon.visited = new String[]{"1"};
		Dungeon.levelName = "1";
		Notes.add(new IronKey("1"));
		Notes.add(new IronKey("1"));

		List<Gizmo> cells = itemsUnder(notesGrid(), "_Floor 1_");
		assertEquals(1, cells.size(), "the two keys share one cell");
		Object second = field(ScrollingGridPane.GridItem.class, "secondIcon").get(cells.get(0));
		assertTrue(second instanceof BitmapText, "the cell carries the quantity as a second icon");
		assertEquals("2", ((BitmapText) second).text());
	}

	@Test
	void itemsUnderAFloorFollowOrder() throws Exception {
		Dungeon.visited = new String[]{"1"};
		Dungeon.levelName = "1";
		Notes.add(new IronKey("1"));
		addLandmark("1", Notes.Landmark.STATUE);
		addLandmark("1", Notes.Landmark.ALCHEMY);

		List<Gizmo> cells = itemsUnder(notesGrid(), "_Floor 1_");
		assertEquals(3, cells.size());
		assertSame(texture(GeneralAsset.ICON_ALCHEMY), icon(cells.get(0)).texture, "alchemy first");
		assertSame(new Image(new StatueSprite()).texture, icon(cells.get(1)).texture, "statue second");
		assertTrue(icon(cells.get(2)) instanceof ItemSprite, "the key last");
	}

	@Test
	void clickOpensWndJournalItem() throws Exception {
		Game priorInstance = Game.instance;
		IdleCellSelector.install();
		try {
			new Game(null, null);
			WndJournalCatalogTest.setUpUi();
			GameScene scene = new GameScene();
			field(Game.class, "scene").set(Game.instance, scene);
			field(GameScene.class, "scene").set(null, scene);

			Dungeon.visited = new String[]{"1"};
			Dungeon.levelName = "1";
			addLandmark("1", Notes.Landmark.GARDEN);

			List<Gizmo> cells = itemsUnder(notesGrid(), "_Floor 1_");
			assertEquals(1, cells.size());
			ScrollingGridPane.GridItem item = (ScrollingGridPane.GridItem) cells.get(0);
			assertTrue(item.onClick(item.left() + 1, item.top() + 1), "the click lands on the cell");

			int shown = 0;
			for (Gizmo g : members(scene)) {
				if (g instanceof WndJournalItem) shown++;
			}
			assertEquals(1, shown, "clicking a landmark opens one WndJournalItem");
		} finally {
			field(GameScene.class, "scene").set(null, null);
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		}
	}

	private static DungeonLayout testLayout() {
		try (InputStream in = WndJournalNotesGridTest.class.getResourceAsStream("/notes-floor-order-layout.json")) {
			assertTrue(in != null, "test resource notes-floor-order-layout.json");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
			return Json.Default.decodeFromString(DungeonLayout.Companion.serializer(),
					new String(out.toByteArray(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
