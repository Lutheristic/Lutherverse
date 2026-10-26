package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.LiquidMetal;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.IconButton;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The alchemy scene built for real on a started run, with no display: a stub platform lays text
 * out without measuring it and Game's screen size is set by hand, so positions are the scene's
 * own arithmetic. Each test names the upstream change it pins.
 */
class AlchemySceneUiPortTest {

	private HeadlessRun run;
	private AlchemyScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261026L);
		run.enterFirstLevel();
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			run.close();
		}
	}

	/** 1500x1000 at the default zoom is a 375x250 camera: landscape, and wide enough to split. */
	private AlchemyScene createScene() {
		return createScene(1500, 1000);
	}

	private AlchemyScene createScene(int width, int height) {
		Game.width = Game.dispWidth = width;
		Game.height = Game.dispHeight = height;
		scene = new AlchemyScene();
		scene.create();
		return scene;
	}

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	private static Item inputItem(int slot) throws Exception {
		Object button = ((Object[]) read(null, AlchemyScene.class, "inputs"))[slot];
		Method item = button.getClass().getDeclaredMethod("item");
		item.setAccessible(true);
		return (Item) item.invoke(button);
	}

	private static void combine(AlchemyScene scene, int slot) throws Exception {
		Method combine = AlchemyScene.class.getDeclaredMethod("combine", int.class);
		combine.setAccessible(true);
		combine.invoke(scene, slot);
	}

	/** Puts fifteen liquid metal on the bench: enough for one telekinetic grab, with five over. */
	private void benchFifteenMetal() throws Exception {
		Dungeon.energy = 20;
		Item metal = new LiquidMetal().quantity(15);
		Dungeon.hero.belongings.backpack.items.add(metal);
		ArrayList<Item> recipe = new ArrayList<>();
		recipe.add(new LiquidMetal().quantity(10));
		scene.populate(recipe, Dungeon.hero.belongings);
		assertEquals(15, inputItem(0).quantity(), "test setup: the whole stack is on the bench");
	}

	// upstream 5bef46bc9
	@Test
	void cancelStaysUsableWhileIngredientsAreLeftAfterCrafting() throws Exception {
		createScene();
		benchFifteenMetal();
		IconButton cancel = (IconButton) read(scene, AlchemyScene.class, "cancel");
		assertTrue(cancel.active, "test setup: cancel is usable with ingredients on the bench");

		combine(scene, 0);

		assertNotNull(inputItem(0), "the unused metal stays on the bench");
		assertEquals(5, inputItem(0).quantity(), "the craft takes ten of the fifteen");
		assertTrue(cancel.active, "cancel must still return the five left over");
	}

	// upstream 5bef46bc9
	@Test
	void cancelIsOffOnceCraftingEmptiesTheBench() throws Exception {
		createScene();
		benchFifteenMetal();
		inputItem(0).quantity(10);

		combine(scene, 0);

		assertNull(inputItem(0), "every ingredient was used");
		IconButton cancel = (IconButton) read(scene, AlchemyScene.class, "cancel");
		assertFalse(cancel.active, "nothing is left to cancel");
	}
}
