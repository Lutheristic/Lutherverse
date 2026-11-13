package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.items.armor.glyphs.Viscosity;
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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@SuppressWarnings("unchecked")
	private static void collectLabels(Gizmo g, List<String> out) throws ReflectiveOperationException {
		if (g instanceof ScrollingListPane.ListItem) {
			Field label = ScrollingListPane.ListItem.class.getDeclaredField("label");
			label.setAccessible(true);
			out.add(((RenderedTextBlock) label.get(g)).text());
		}
		if (g instanceof Group) {
			Field members = Group.class.getDeclaredField("members");
			members.setAccessible(true);
			List<Gizmo> children = (List<Gizmo>) members.get(g);
			if (children == null) return;
			for (Gizmo child : new ArrayList<>(children)) {
				if (child != null) collectLabels(child, out);
			}
		}
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
				List<String> labels = new ArrayList<>();
				collectLabels(wnd, labels);

				assertTrue(labels.contains(Messages.titleCase(new Blazing().name())), "seen enchantment: " + labels);
				assertTrue(labels.contains(Messages.titleCase(new Viscosity().name())), "seen glyph: " + labels);
				int hidden = 0;
				for (String label : labels) if (label.equals("???")) hidden++;
				int unseenEffects = Catalog.ENCHANTMENTS.totalItems() - 1 + Catalog.GLYPHS.totalItems() - 1;
				assertTrue(hidden >= unseenEffects, hidden + " hidden entries, expected at least " + unseenEffects);
			} finally {
				if (wnd.parent != null) wnd.hide();
			}
		}
	}
}
