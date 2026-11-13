package com.qsr.customspd.items.stones;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.items.weapon.melee.Longsword;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.RedButton;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Stones used from the inventory count in the catalog however they are applied (upstream
 * 4ff3a3365): augmenting a weapon, and a guess with a stone of intuition, whose button is an
 * anonymous class and so must not count itself.
 */
class StoneCatalogCountTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() throws Exception {
		run = HeadlessRun.start(20240904L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		hero.sprite = new StubSprite();
		new Group().add(hero.sprite);
		itemStatic("curUser").set(null, hero);
	}

	@AfterEach
	void closeRun() throws Exception {
		itemStatic("curUser").set(null, null);
		itemStatic("curItem").set(null, null);
		run.close();
	}

	private static Field itemStatic(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@Test
	void augmentingAWeaponCountsTheStone() throws Exception {
		int before = Catalog.useCount(StoneOfAugmentation.class);
		StoneOfAugmentation stone = new StoneOfAugmentation();
		stone.collect();
		itemStatic("curItem").set(null, stone);
		Weapon weapon = new Longsword();

		stone.apply(weapon, Weapon.Augment.SPEED);

		assertEquals(Weapon.Augment.SPEED, weapon.augment, "fixture: the stone took effect");
		assertEquals(before + 1, Catalog.useCount(StoneOfAugmentation.class));
	}

	private static void findGuessButton(Gizmo g, List<RedButton> out) throws Exception {
		if (g instanceof RedButton) out.add((RedButton) g);
		if (g instanceof Group) {
			Field members = Group.class.getDeclaredField("members");
			members.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<Gizmo> children = (List<Gizmo>) members.get(g);
			if (children == null) return;
			for (Gizmo child : new ArrayList<>(children)) {
				if (child != null) findGuessButton(child, out);
			}
		}
	}

	@Test
	void aGuessWithAStoneOfIntuitionCountsTheStone() throws Exception {
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

		int before = Catalog.useCount(StoneOfIntuition.class);
		StoneOfIntuition stone = new StoneOfIntuition();
		stone.collect();
		itemStatic("curItem").set(null, stone);

		Group parent = new Group();
		StoneOfIntuition.WndGuess wnd = stone.new WndGuess(new PotionOfLevitation());
		parent.add(wnd);
		List<RedButton> buttons = new ArrayList<>();
		findGuessButton(wnd, buttons);
		assertEquals(1, buttons.size(), "fixture: the window has one guess button");

		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(buttons.get(0));

		assertNotNull(hero.buff(StoneOfIntuition.IntuitionUseTracker.class), "fixture: the guess was made");
		assertEquals(before + 1, Catalog.useCount(StoneOfIntuition.class));
	}
}
