package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.armor.glyphs.Viscosity;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollingListPane;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The journal's equipment catalog lists enchantments and glyphs next to the items (upstream
 * a3f2ab7a8, shown in CPDU's list view rather than upstream's grid).
 */
class WndJournalCatalogTest {

	@SuppressWarnings("unchecked")
	private static void clearSeen() throws ReflectiveOperationException {
		Field seen = Catalog.class.getDeclaredField("seen");
		seen.setAccessible(true);
		for (Catalog cat : Catalog.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
		}
	}

	private static String text(Gizmo g, Class<?> type) throws ReflectiveOperationException {
		Field label = type.getDeclaredField("label");
		label.setAccessible(true);
		return ((RenderedTextBlock) label.get(g)).text();
	}

	/**
	 * The labels of the catalog's enchantment and glyph rows, in list order, each under the title
	 * of the section it is listed in.
	 */
	@SuppressWarnings("unchecked")
	private static void collectEffectRows(Gizmo g, String[] section, Map<String, List<String>> out)
			throws ReflectiveOperationException {
		if (g instanceof ScrollingListPane.ListTitle) {
			section[0] = text(g, ScrollingListPane.ListTitle.class);
			return;
		}
		if (EFFECT_ROW.isInstance(g)) {
			List<String> rows = out.get(section[0]);
			if (rows == null) out.put(section[0], rows = new ArrayList<>());
			rows.add(text(g, ScrollingListPane.ListItem.class));
			return;
		}
		if (g instanceof Group) {
			Field members = Group.class.getDeclaredField("members");
			members.setAccessible(true);
			List<Gizmo> children = (List<Gizmo>) members.get(g);
			if (children == null) return;
			for (Gizmo child : new ArrayList<>(children)) {
				if (child != null) collectEffectRows(child, section, out);
			}
		}
	}

	private static final Class<?> EFFECT_ROW;
	static {
		try {
			EFFECT_ROW = Class.forName("com.qsr.customspd.windows.WndJournal$CatalogTab$EffectListItem");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static String effectName(Class<?> effect) {
		Object e = Reflection.newInstance(effect);
		return Messages.titleCase(e instanceof Weapon.Enchantment
				? ((Weapon.Enchantment) e).name() : ((Armor.Glyph) e).name());
	}

	/** The rows of one section: every entry listed, only the seen one named. */
	private static void assertSection(Catalog catalog, String seenName, Map<String, List<String>> rows) {
		String title = Messages.titleCase(catalog.title()) + " (" + catalog.totalSeen() + "/" + catalog.totalItems() + ")";
		List<String> section = rows.get(title);
		assertEquals(catalog.totalItems(), section == null ? 0 : section.size(), title + " rows: " + rows);

		List<String> named = new ArrayList<>();
		int hidden = 0;
		for (String label : section) {
			if (label.equals("???")) hidden++;
			else named.add(label);
		}
		assertEquals(catalog.totalItems() - 1, hidden, title + " hidden rows: " + section);
		assertEquals(Collections.singletonList(seenName), named, title + " named rows");
	}

	@Test
	void theEquipmentCatalogListsEnchantmentsAndGlyphs() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			run.enterFirstLevel();
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

			clearSeen();
			Catalog.setSeen(Blazing.class);
			Catalog.setSeen(Viscosity.class);

			Group parent = new Group();
			WndJournal wnd = new WndJournal();
			parent.add(wnd);
			try {
				Map<String, List<String>> rows = new LinkedHashMap<>();
				collectEffectRows(wnd, new String[1], rows);

				assertEquals(2, rows.size(), "effect rows sit only in the enchantment and glyph sections: " + rows.keySet());
				assertSection(Catalog.ENCHANTMENTS, effectName(Blazing.class), rows);
				assertSection(Catalog.GLYPHS, effectName(Viscosity.class), rows);

				List<Class<?>> unseen = new ArrayList<>(Catalog.ENCHANTMENTS.items());
				unseen.addAll(Catalog.GLYPHS.items());
				unseen.remove(Blazing.class);
				unseen.remove(Viscosity.class);
				for (List<String> section : rows.values()) {
					for (Class<?> effect : unseen) {
						assertFalse(section.contains(effectName(effect)), effect.getSimpleName() + " is named while unseen");
					}
				}
			} finally {
				if (wnd.parent != null) wnd.hide();
			}
		}
	}
}
