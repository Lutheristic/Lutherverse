package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.npcs.Shopkeeper;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.artifacts.Artifact;
import com.qsr.customspd.items.artifacts.MasterThievesArmband;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.RedButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 008a65140 (v2.3.0): with the thieves' armband, a steal that can fail asks first,
 * since a failed steal closes the shop. Only a guaranteed steal happens on the first click.
 *
 * The shop window is built for real on floor 1 with no display. GameScene.show only needs a
 * scene to add the window to, so a bare GameScene stands in for the real one.
 */
class WndTradeItemStealWarnPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private GameScene scene;
	private MasterThievesArmband armband;
	private MasterThievesArmband.Thievery thievery;
	private Heap heap;
	private Item potion;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261112L);
		run.enterFirstLevel();
		IdleCellSelector.install();
		priorInstance = Game.instance;
		new Game(null, null);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		PixelScene.uiCamera = new Camera(0, 0, 400, 400, 1);

		armband = new MasterThievesArmband();
		armband.activate(Dungeon.hero);
		thievery = Dungeon.hero.buff(MasterThievesArmband.Thievery.class);
		assertNotNull(thievery, "test setup: the armband is active");

		int cell = Dungeon.hero.pos;
		assertNull(Dungeon.level.heaps.get(cell), "test setup: an empty cell for the shop item");
		potion = new PotionOfHealing();
		heap = new Heap();
		heap.pos = cell;
		heap.drop(potion);
		heap.type = Heap.Type.FOR_SALE;
		Dungeon.level.heaps.put(cell, heap);
	}

	@AfterEach
	void tearDown() {
		try {
			IdleCellSelector.uninstall();
		} finally {
			Game.instance = priorInstance;
			run.close();
		}
	}

	private static Field field(Class<?> owner, String name) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private void charge(int charges) throws Exception {
		field(Artifact.class, "charge").setInt(armband, charges);
	}

	private int charge() throws Exception {
		return field(Artifact.class, "charge").getInt(armband);
	}

	/** A bare GameScene, so GameScene.show has somewhere to put the confirmation. */
	private void bareGameScene() throws Exception {
		scene = new GameScene();
		field(GameScene.class, "scene").set(null, scene);
	}

	private static List<Gizmo> members(Group group) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) field(Group.class, "members").get(group);
		return members;
	}

	private static <T> List<T> all(Group group, Class<T> type) throws Exception {
		List<T> found = new ArrayList<>();
		for (Gizmo g : members(group)) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		return found;
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	private static RedButton stealButton(WndTradeItem window) throws Exception {
		List<RedButton> buttons = all(window, RedButton.class);
		assertEquals(2, buttons.size(), "test setup: buy and steal buttons");
		return buttons.get(1);
	}

	private static String text(RenderedTextBlock block) throws Exception {
		return (String) field(RenderedTextBlock.class, "text").get(block);
	}

	private WndOptions onlyConfirmation() throws Exception {
		List<WndOptions> shown = all(scene, WndOptions.class);
		assertEquals(1, shown.size(), "one confirmation is shown");
		return shown.get(0);
	}

	private static void choose(WndOptions window, int index) throws Exception {
		Method onSelect = WndOptions.class.getDeclaredMethod("onSelect", int.class);
		onSelect.setAccessible(true);
		onSelect.invoke(window, index);
	}

	@Test
	void aStealThatCanFailAsksFirstAndTakesNothing() throws Exception {
		charge(1);
		assertTrue(thievery.stealChance(potion) < 1f, "test setup: a steal that can fail");
		bareGameScene();
		WndTradeItem window = new WndTradeItem(heap);

		click(stealButton(window));

		WndOptions confirm = onlyConfirmation();
		boolean warned = false;
		for (RenderedTextBlock block : all(confirm, RenderedTextBlock.class)) {
			if (Messages.get(WndTradeItem.class, "steal_warn").equals(text(block))) warned = true;
		}
		assertTrue(warned, "the confirmation warns that a failed steal closes the shop");
		List<RedButton> options = all(confirm, RedButton.class);
		assertEquals(2, options.size(), "yes and no");
		assertEquals(Messages.get(WndTradeItem.class, "steal_warn_yes"), options.get(0).text());
		assertEquals(Messages.get(WndTradeItem.class, "steal_warn_no"), options.get(1).text());

		assertSame(potion, heap.peek(), "nothing is stolen before the player confirms");
		assertEquals(1, charge(), "no charge is spent before the player confirms");
	}

	@Test
	void sayingNoLeavesTheShopAlone() throws Exception {
		charge(1);
		Shopkeeper shopkeeper = new Shopkeeper();
		Dungeon.level.mobs.add(shopkeeper);
		bareGameScene();
		WndTradeItem window = new WndTradeItem(heap);
		click(stealButton(window));

		choose(onlyConfirmation(), 1);

		assertSame(heap, Dungeon.level.heaps.get(heap.pos), "the item is still for sale");
		assertSame(potion, heap.peek(), "the item is still for sale");
		assertEquals(Heap.Type.FOR_SALE, heap.type);
		assertTrue(Dungeon.level.mobs.contains(shopkeeper), "the shopkeeper stays");
		assertEquals(1, charge(), "no charge is spent");
	}

	@Test
	void aFailedStealAfterSayingYesClosesTheShop() throws Exception {
		charge(1);
		float chance = thievery.stealChance(potion);
		long failingSeed = 0;
		float roll;
		do {
			failingSeed++;
			Random.pushGenerator(failingSeed);
			roll = Random.Float();
			Random.popGenerator();
		} while (roll <= chance);

		Shopkeeper shopkeeper = new Shopkeeper();
		Dungeon.level.mobs.add(shopkeeper);
		bareGameScene();
		WndTradeItem window = new WndTradeItem(heap);
		click(stealButton(window));
		WndOptions confirm = onlyConfirmation();

		// the steal's roll comes from a generator whose first float fails it
		Random.pushGenerator(failingSeed);
		try {
			choose(confirm, 0);
		} finally {
			Random.popGenerator();
		}

		assertFalse(Dungeon.level.mobs.contains(shopkeeper), "the shopkeeper flees");
		assertNull(Dungeon.level.heaps.get(heap.pos), "the shop's stock goes with them");
		assertFalse(Dungeon.hero.belongings.contains(potion), "nothing was stolen");
	}

	// passes on the old code too: a guaranteed steal always happened on the first click
	@Test
	void aGuaranteedStealHappensRightAway() throws Exception {
		charge(3);
		assertEquals(1f, thievery.stealChance(potion), 0f, "test setup: a guaranteed steal");
		// no GameScene: a confirmation would have nowhere to go, and picking up needs no scene
		WndTradeItem window = new WndTradeItem(heap);

		click(stealButton(window));

		assertTrue(Dungeon.hero.belongings.contains(potion), "the item is stolen at once");
		assertNull(Dungeon.level.heaps.get(heap.pos), "the shop slot is empty");
		assertEquals(0, charge(), "the charges are spent");
	}
}
