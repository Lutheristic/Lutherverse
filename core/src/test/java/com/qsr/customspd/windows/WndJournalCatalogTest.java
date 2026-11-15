package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.items.armor.glyphs.Viscosity;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingGridPane;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal's catalog tab is upstream's grid (39209884f, a3f2ab7a8, 6a313b135): a centered
 * section header, then one header per catalog followed by one cell per catalogued entry, in
 * Catalog order. Enchantments and glyphs keep their glow when seen, and unseen cells are dark
 * on a tinted background.
 */
class WndJournalCatalogTest {

	static final String TAB = "com.qsr.customspd.windows.WndJournal$CatalogTab";

	static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	static Class<?> catalogTab() throws ClassNotFoundException {
		return Class.forName(TAB);
	}

	/** The headless display, font and key setup a journal window needs. */
	static void setUpUi() {
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

	/** Puts the catalog tab back on its first sub-tab with no remembered scroll. */
	static void resetTabState() throws ReflectiveOperationException {
		field(catalogTab(), "currentItemIdx").setInt(null, 0);
		try {
			Arrays.fill((float[]) field(catalogTab(), "scrollPositions").get(null), 0f);
		} catch (NoSuchFieldException e) {
			// not there before the grid
		}
	}

	@SuppressWarnings("unchecked")
	static void clearSeen() throws ReflectiveOperationException {
		Field seen = field(Catalog.class, "seen");
		for (Catalog cat : Catalog.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
		}
	}

	@SuppressWarnings("unchecked")
	static List<Gizmo> members(Group g) throws ReflectiveOperationException {
		List<Gizmo> m = (List<Gizmo>) field(Group.class, "members").get(g);
		return m == null ? new ArrayList<Gizmo>() : m;
	}

	/** Every grid header and grid cell under g, in display order. */
	static void walkGrid(Gizmo g, List<Gizmo> out) throws ReflectiveOperationException {
		if (g instanceof ScrollingGridPane.GridHeader || g instanceof ScrollingGridPane.GridItem) {
			out.add(g);
			return;
		}
		if (g instanceof Group) {
			for (Gizmo child : new ArrayList<>(members((Group) g))) {
				if (child != null) walkGrid(child, out);
			}
		}
	}

	static List<Gizmo> grid(Gizmo root) throws ReflectiveOperationException {
		List<Gizmo> out = new ArrayList<>();
		walkGrid(root, out);
		return out;
	}

	static String headerText(Gizmo header) throws ReflectiveOperationException {
		return ((RenderedTextBlock) field(ScrollingGridPane.GridHeader.class, "text").get(header)).text();
	}

	static boolean centered(Gizmo header) throws ReflectiveOperationException {
		return field(ScrollingGridPane.GridHeader.class, "center").getBoolean(header);
	}

	static Image icon(Gizmo cell) throws ReflectiveOperationException {
		return (Image) field(ScrollingGridPane.GridItem.class, "icon").get(cell);
	}

	static ColorBlock bg(Gizmo cell) throws ReflectiveOperationException {
		return (ColorBlock) field(ScrollingGridPane.GridItem.class, "bg").get(cell);
	}

	static Object glowing(ItemSprite sprite) throws ReflectiveOperationException {
		return field(ItemSprite.class, "glowing").get(sprite);
	}

	/**
	 * The cells under each per-catalog header, one list per catalog, after checking the section
	 * header and each catalog header against the catalogs shown.
	 */
	static List<List<Gizmo>> sections(List<Gizmo> grid, String sectionKey, List<Catalog> catalogs)
			throws ReflectiveOperationException {
		assertFalse(grid.isEmpty(), "the catalog tab shows a grid");
		Gizmo first = grid.get(0);
		assertTrue(first instanceof ScrollingGridPane.GridHeader, "the grid opens with a header");
		int seen = 0, total = 0;
		for (Catalog c : catalogs) {
			seen += c.totalSeen();
			total += c.totalItems();
		}
		assertTrue(centered(first), "the section header is centered");
		assertEquals("_" + Messages.get(catalogTab(), sectionKey) + "_ (" + seen + "/" + total + ")",
				headerText(first));

		List<List<Gizmo>> out = new ArrayList<>();
		int i = 1;
		for (Catalog c : catalogs) {
			assertTrue(i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridHeader,
					"a header for " + c + " at " + i);
			assertEquals("_" + Messages.titleCase(c.title()) + "_ (" + c.totalSeen() + "/" + c.totalItems() + "):",
					headerText(grid.get(i)));
			assertFalse(centered(grid.get(i)), c + " header is not centered");
			i++;
			List<Gizmo> cells = new ArrayList<>();
			while (i < grid.size() && grid.get(i) instanceof ScrollingGridPane.GridItem) {
				cells.add(grid.get(i++));
			}
			assertEquals(c.totalItems(), cells.size(), c + " cells");
			out.add(cells);
		}
		assertEquals(grid.size(), i, "nothing follows the last catalog");
		return out;
	}

	/** The cell for cls in the given sections, by its place in Catalog order. */
	static Gizmo cellFor(Class<?> cls, List<Catalog> catalogs, List<List<Gizmo>> sections) {
		for (int s = 0; s < catalogs.size(); s++) {
			int idx = new ArrayList<>(catalogs.get(s).items()).indexOf(cls);
			if (idx >= 0) return sections.get(s).get(idx);
		}
		throw new AssertionError(cls.getSimpleName() + " is not in these catalogs");
	}

	@Test
	void theEquipmentGridListsEveryCatalogInOrder() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			run.enterFirstLevel();
			setUpUi();
			resetTabState();

			clearSeen();
			Catalog.setSeen(Blazing.class);
			Catalog.setSeen(Viscosity.class);

			Group parent = new Group();
			WndJournal wnd = new WndJournal();
			parent.add(wnd);
			try {
				List<Catalog> catalogs = Catalog.equipmentCatalogs;
				List<List<Gizmo>> sections = sections(grid(wnd), "title_equipment", catalogs);

				Gizmo blazing = cellFor(Blazing.class, catalogs, sections);
				Gizmo viscosity = cellFor(Viscosity.class, catalogs, sections);
				assertTrue(icon(blazing) instanceof ItemSprite, "the Blazing icon is an item sprite");
				assertSame(new Blazing().glowing(), glowing((ItemSprite) icon(blazing)), "Blazing glows");
				assertTrue(icon(viscosity) instanceof ItemSprite, "the Viscosity icon is an item sprite");
				assertSame(new Viscosity().glowing(), glowing((ItemSprite) icon(viscosity)), "Viscosity glows");

				for (Catalog c : Arrays.asList(Catalog.ENCHANTMENTS, Catalog.GLYPHS)) {
					for (Class<?> cls : c.items()) {
						if (cls == Blazing.class || cls == Viscosity.class) continue;
						Gizmo cell = cellFor(cls, catalogs, sections);
						ColorBlock bg = bg(cell);
						assertEquals(2f, bg.rm, cls.getSimpleName() + " bg red");
						assertEquals(1f, bg.gm, cls.getSimpleName() + " bg green");
						assertEquals(2f, bg.bm, cls.getSimpleName() + " bg blue");
						Image icon = icon(cell);
						assertEquals(0f, icon.rm, cls.getSimpleName() + " icon is dark");
						assertEquals(0f, icon.gm, cls.getSimpleName() + " icon is dark");
						assertEquals(0f, icon.bm, cls.getSimpleName() + " icon is dark");
					}
				}
			} finally {
				if (wnd.parent != null) wnd.hide();
			}
		}
	}
}
