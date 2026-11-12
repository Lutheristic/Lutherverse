package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfLiquidFlame;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfMagicMapping;
import com.qsr.customspd.items.stones.StoneOfClairvoyance;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.AlchemyScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 47fd533ca (v2.5.0): turning a potion or scroll into energy identifies it, and so does
 * turning a scroll into runestones. In the alchemy scene the old name fades into the new one
 * over the energy line; anywhere else the item is identified on the spot.
 *
 * The scene is built for real on floor 1 with no display, as in AlchemySceneUiPortTest.
 */
class EnergizeIdentifiesPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private AlchemyScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		run.enterFirstLevel();
		StubSprite.attach(Dungeon.hero);
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
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			Game.instance = priorInstance;
			run.close();
		}
	}

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	private void openAlchemy() throws Exception {
		scene = new AlchemyScene();
		scene.create();
		Field f = Game.class.getDeclaredField("scene");
		f.setAccessible(true);
		f.set(Game.instance, scene);
	}

	private static <T> List<T> all(Group group, Class<T> type) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) read(group, Group.class, "members");
		List<T> found = new ArrayList<>();
		for (Gizmo g : members) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		return found;
	}

	private static String label(IconTitle title) throws Exception {
		RenderedTextBlock block = (RenderedTextBlock) read(title, IconTitle.class, "tfLabel");
		return (String) read(block, RenderedTextBlock.class, "text");
	}

	private static <T extends Item> T unknown(T item) {
		assertFalse(item.isIdentified(), "test setup: " + item.getClass().getSimpleName() + " starts unknown");
		return item;
	}

	private static <T extends Item> T carried(T item) {
		assertTrue(item.collect(Dungeon.hero.belongings.backpack), "test setup: the hero picks it up");
		return item;
	}

	@Test
	void energizingAPotionIdentifiesIt() {
		Potion potion = carried(unknown(new PotionOfLiquidFlame()));
		int energy = Dungeon.energy;

		WndEnergizeItem.energizeAll(potion);

		assertTrue(potion.isIdentified(), "the potion is identified");
		assertTrue(new PotionOfLiquidFlame().isKnown(), "every potion of its kind is known now");
		assertFalse(Dungeon.hero.belongings.contains(potion), "the potion is gone");
		assertEquals(energy + 6, Dungeon.energy, "it turned into energy");
	}

	@Test
	void energizingOneScrollOfAStackIdentifiesTheRest() {
		Scroll scrolls = new ScrollOfMagicMapping();
		scrolls.quantity(2);
		carried(unknown(scrolls));

		WndEnergizeItem.energizeOne(scrolls);

		assertEquals(1, scrolls.quantity(), "one of the two is energized");
		assertTrue(scrolls.isIdentified(), "the scroll left over is identified");
	}

	@Test
	void energizingAtTheAlchemyPotShowsTheOldNameGivingWayToTheNew() throws Exception {
		openAlchemy();
		int titlesBefore = all(scene, IconTitle.class).size();
		Potion potion = carried(unknown(new PotionOfLiquidFlame()));
		String oldName = Messages.titleCase(potion.title());

		WndEnergizeItem.energizeAll(potion);

		assertTrue(potion.isIdentified(), "the potion is identified");
		List<IconTitle> titles = all(scene, IconTitle.class);
		assertEquals(titlesBefore + 2, titles.size(), "the old and new names are shown");
		IconTitle before = titles.get(titles.size() - 2);
		IconTitle after = titles.get(titles.size() - 1);
		assertEquals(oldName, label(before));
		assertEquals(Messages.titleCase(potion.title()), label(after));
		assertFalse(oldName.equals(label(after)), "the new name is the real one");
		assertEquals(1f, before.alpha(), 0f, "the old name starts fully shown");
		assertEquals(-0.5f, after.alpha(), 0f, "the new name waits half a second before it shows");
	}

	@Test
	void theNamesFadeAwayOnTheirOwn() throws Exception {
		openAlchemy();
		int titlesBefore = all(scene, IconTitle.class).size();
		WndEnergizeItem.energizeAll(carried(unknown(new PotionOfLiquidFlame())));
		List<IconTitle> titles = all(scene, IconTitle.class);
		assertEquals(titlesBefore + 2, titles.size(), "the old and new names are shown");
		IconTitle before = titles.get(titles.size() - 2);
		IconTitle after = titles.get(titles.size() - 1);
		List<NinePatch> backgrounds = all(scene, NinePatch.class);
		NinePatch background = backgrounds.get(backgrounds.size() - 1);

		Game.elapsed = 0.25f;
		for (int i = 0; i < 3; i++) before.update();
		assertNotNull(before.parent, "the old name is still fading out");
		before.update();
		assertEquals(null, before.parent, "the old name is gone after a second");

		// the new name fades in for a second and a half, then out for one more
		for (int i = 0; i < 6; i++) after.update();
		assertEquals(1f, after.alpha(), 0.001f, "the new name is fully shown");
		for (int i = 0; i < 3; i++) after.update();
		assertNotNull(after.parent, "the new name is still fading out");
		assertEquals(after.alpha(), background.alpha(), 0.001f, "the box fades with it");
		after.update();
		assertEquals(null, after.parent, "the new name is gone");
		assertEquals(null, background.parent, "and so is its box");
	}

	@Test
	void anItemAlreadyKnownShowsNoNames() throws Exception {
		openAlchemy();
		int titlesBefore = all(scene, IconTitle.class).size();
		Potion potion = new PotionOfLiquidFlame();
		potion.identify();

		WndEnergizeItem.energizeAll(carried(potion));

		assertEquals(titlesBefore, all(scene, IconTitle.class).size(), "nothing to reveal");
	}

	@Test
	void makingRunestonesAtThePotShowsTheScrollsName() throws Exception {
		openAlchemy();
		int titlesBefore = all(scene, IconTitle.class).size();
		Scroll scroll = unknown(new ScrollOfMagicMapping());
		ArrayList<Item> ingredients = new ArrayList<>();
		ingredients.add(scroll);

		Item stones = new Scroll.ScrollToStone().brew(ingredients);

		assertTrue(stones instanceof StoneOfClairvoyance, "test setup: two stones of the scroll's kind");
		assertTrue(scroll.isIdentified(), "the scroll is identified");
		assertEquals(titlesBefore + 2, all(scene, IconTitle.class).size(), "the old and new names are shown");
	}

	// passes on the old code too: away from the pot the recipe always identified the scroll
	@Test
	void makingRunestonesAwayFromThePotStillIdentifies() {
		Scroll scroll = unknown(new ScrollOfMagicMapping());
		ArrayList<Item> ingredients = new ArrayList<>();
		ingredients.add(scroll);

		new Scroll.ScrollToStone().brew(ingredients);

		assertTrue(scroll.isIdentified(), "the scroll is identified");
	}
}
