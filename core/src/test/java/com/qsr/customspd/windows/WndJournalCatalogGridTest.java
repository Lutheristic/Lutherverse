package com.qsr.customspd.windows;

import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.items.weapon.melee.MagesStaff;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.RedButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Scene;
import com.watabou.noosa.Visual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.qsr.customspd.windows.WndJournalCatalogTest.catalogTab;
import static com.qsr.customspd.windows.WndJournalCatalogTest.cellFor;
import static com.qsr.customspd.windows.WndJournalCatalogTest.clearSeen;
import static com.qsr.customspd.windows.WndJournalCatalogTest.field;
import static com.qsr.customspd.windows.WndJournalCatalogTest.glowing;
import static com.qsr.customspd.windows.WndJournalCatalogTest.grid;
import static com.qsr.customspd.windows.WndJournalCatalogTest.icon;
import static com.qsr.customspd.windows.WndJournalCatalogTest.members;
import static com.qsr.customspd.windows.WndJournalCatalogTest.sections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalog grid's cells (upstream a3f2ab7a8, 6a313b135, 40afe2a22, cba48b81c, eb51e6daf,
 * b6f7e7462): second icons on seen items, the mage's staff crop, scroll memory per sub-tab, the
 * catalog tab icon, and the popup a cell opens in and out of the game scene.
 */
class WndJournalCatalogGridTest {

	private HeadlessRun run;
	private WndJournal wnd;
	private Group parent;

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20240611L);
		run.enterFirstLevel();
		WndJournalCatalogTest.setUpUi();
		WndJournalCatalogTest.resetTabState();
		clearSeen();
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (wnd != null && wnd.parent != null) wnd.hide();
			WndJournalCatalogTest.resetTabState();
		} finally {
			run.close();
		}
	}

	private WndJournal open(int subTab) throws Exception {
		field(catalogTab(), "currentItemIdx").setInt(null, subTab);
		parent = new Group();
		wnd = new WndJournal();
		parent.add(wnd);
		return wnd;
	}

	private void close() {
		if (wnd != null && wnd.parent != null) wnd.hide();
		wnd = null;
	}

	private List<List<Gizmo>> equipment() throws Exception {
		return sections(grid(wnd), "title_equipment", Catalog.equipmentCatalogs);
	}

	private List<List<Gizmo>> consumables() throws Exception {
		return sections(grid(wnd), "title_consumables", Catalog.consumableCatalogs);
	}

	private static Visual secondIcon(Gizmo cell) throws Exception {
		return (Visual) field(ScrollingGridPane.GridItem.class, "secondIcon").get(cell);
	}

	private Object tab() throws Exception {
		return field(WndJournal.class, "catalogTab").get(wnd);
	}

	private void clickSubTab(int idx) throws Exception {
		RedButton[] buttons = (RedButton[]) field(catalogTab(), "itemButtons").get(tab());
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(buttons[idx]);
	}

	private static void click(Gizmo cell) {
		ScrollingGridPane.GridItem item = (ScrollingGridPane.GridItem) cell;
		assertTrue(item.onClick(item.left() + 1, item.top() + 1), "the click lands on the cell");
	}

	private static <T> List<T> all(Group group, Class<T> type) throws Exception {
		List<T> found = new ArrayList<>();
		for (Gizmo g : members(group)) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		return found;
	}

	private static String title(WndTitledMessage w) throws Exception {
		IconTitle t = all(w, IconTitle.class).get(0);
		return ((RenderedTextBlock) field(IconTitle.class, "tfLabel").get(t)).text();
	}

	private static Image popupIcon(WndTitledMessage w) throws Exception {
		IconTitle t = all(w, IconTitle.class).get(0);
		return (Image) field(IconTitle.class, "imIcon").get(t);
	}

	private static String message(WndTitledMessage w) throws Exception {
		List<RenderedTextBlock> texts = all(w, RenderedTextBlock.class);
		assertEquals(1, texts.size(), "one message block");
		return texts.get(0).text();
	}

	@Test
	void aSeenItemWithAnIconShowsItTopRight() throws Exception {
		Catalog.setSeen(PotionOfHealing.class);
		open(1);
		Gizmo cell = cellFor(PotionOfHealing.class, Catalog.consumableCatalogs, consumables());
		Visual second = secondIcon(cell);
		assertNotNull(second, "a seen potion of healing shows its icon");
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ITEM_ICON_POTION_HEALING)),
				((Image) second).texture, "the potion of healing icon");
		ScrollingGridPane.GridItem item = (ScrollingGridPane.GridItem) cell;
		assertEquals(item.left() + item.width() - second.width(), second.x, "flush right");
		assertEquals(item.top(), second.y, "flush top");
		close();

		clearSeen();
		open(1);
		cell = cellFor(PotionOfHealing.class, Catalog.consumableCatalogs, consumables());
		assertNull(secondIcon(cell), "an unseen item shows no icon");
	}

	@Test
	void aSeenMagesStaffLosesItsParticleRows() throws Exception {
		Catalog.setSeen(MagesStaff.class);
		open(0);
		Gizmo cell = cellFor(MagesStaff.class, Catalog.equipmentCatalogs, equipment());
		assertEquals(14f, icon(cell).height(), "the seen staff is cropped by 2 rows");
		close();

		clearSeen();
		open(0);
		cell = cellFor(MagesStaff.class, Catalog.equipmentCatalogs, equipment());
		assertEquals(16f, icon(cell).height(), "the unseen staff is not cropped");
	}

	@Test
	void eachSubTabRemembersItsScroll() throws Exception {
		open(0);
		clickSubTab(1);
		ScrollingGridPane grid = (ScrollingGridPane) field(catalogTab(), "grid").get(tab());
		grid.scrollTo(0, 60);
		grid.update();
		assertEquals(60f, grid.content().camera.scroll.y, "test setup: the consumables grid scrolls to 60");

		clickSubTab(0);
		assertEquals(0f, grid.content().camera.scroll.y, "equipment opens at its own scroll");
		clickSubTab(1);
		assertEquals(60f, grid.content().camera.scroll.y, "consumables come back where they were");
	}

	@Test
	void theCatalogTabUsesTheCatalogIcon() throws Exception {
		open(0);
		@SuppressWarnings("unchecked")
		List<Object> tabs = (List<Object>) field(WndTabbed.class, "tabs").get(wnd);
		Image icon = (Image) field(Class.forName("com.qsr.customspd.windows.WndTabbed$IconTab"), "icon")
				.get(tabs.get(3));
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ICON_CATALOG)), icon.texture,
				"tab 3 shows the catalog icon");

		File png = new File(GeneralAsset.ICON_CATALOG.getPath());
		assertTrue(png.isFile(), "catalog.png exists");
		BufferedImage img = ImageIO.read(png);
		assertEquals(13, img.getWidth());
		assertEquals(16, img.getHeight());
		boolean opaque = false;
		for (int y = 0; y < img.getHeight() && !opaque; y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				if ((img.getRGB(x, y) >>> 24) != 0) {
					opaque = true;
					break;
				}
			}
		}
		assertTrue(opaque, "catalog.png has an opaque pixel");
	}

	@Test
	void cellsOpenAPopupInAndOutOfTheGameScene() throws Exception {
		Game priorInstance = Game.instance;
		IdleCellSelector.install();
		try {
			new Game(null, null);
			WndJournalCatalogTest.setUpUi();
			GameScene scene = new GameScene();
			field(Game.class, "scene").set(Game.instance, scene);
			field(GameScene.class, "scene").set(null, scene);

			Catalog.setSeen(Blazing.class);
			open(0);
			List<List<Gizmo>> sections = equipment();

			Class<?> unseen = null;
			for (Class<?> cls : Catalog.ENCHANTMENTS.items()) {
				if (cls != Blazing.class) {
					unseen = cls;
					break;
				}
			}
			click(cellFor(unseen, Catalog.equipmentCatalogs, sections));
			List<WndTitledMessage> shown = all(scene, WndTitledMessage.class);
			assertEquals(1, shown.size(), "clicking an unseen enchantment opens one popup");
			assertEquals("???", title(shown.get(0)));
			assertEquals(Messages.get(catalogTab(), "not_seen_enchantment"), message(shown.get(0)));
			shown.get(0).hide();

			click(cellFor(Blazing.class, Catalog.equipmentCatalogs, sections));
			shown = all(scene, WndTitledMessage.class);
			assertEquals(1, shown.size(), "clicking Blazing opens one popup");
			Weapon.Enchantment blazing = new Blazing();
			assertEquals(Messages.titleCase(blazing.name()), title(shown.get(0)));
			assertEquals(blazing.desc(), message(shown.get(0)));
			Image popupIcon = popupIcon(shown.get(0));
			assertTrue(popupIcon instanceof ItemSprite, "the popup icon is an item sprite");
			assertSame(blazing.glowing(), glowing((ItemSprite) popupIcon), "the popup icon glows");
			shown.get(0).hide();

			// outside the game scene the popup goes on top of whatever scene is showing
			field(GameScene.class, "scene").set(null, null);
			Scene other = new Scene();
			field(Game.class, "scene").set(Game.instance, other);
			click(cellFor(Blazing.class, Catalog.equipmentCatalogs, sections));
			assertEquals(1, all(other, WndTitledMessage.class).size(), "the popup opens on the current scene");
			assertEquals(0, all(scene, WndTitledMessage.class).size(), "and not on the game scene");
		} finally {
			field(GameScene.class, "scene").set(null, null);
			Game.instance = priorInstance;
			IdleCellSelector.uninstall();
		}
	}
}
