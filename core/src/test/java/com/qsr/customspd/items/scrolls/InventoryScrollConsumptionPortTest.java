package com.qsr.customspd.items.scrolls;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.TormentedSpirit;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 2879e9ae5 moved the "consume the scroll" step of inventory scrolls from the start of
 * the read to the moment an item is chosen. Two upstream follow-ups keep that model honest and
 * are pinned here, both observed through the scroll prompt (which needs a scene, so the prompt
 * itself is the NPE boundary the tests stop at):
 *
 *  - 295ac57f8: ScrollOfRemoveCurse.doRead must not detach up front - the consumption belongs to
 *    InventoryScroll (unknown: on read; known: on select), so a stacked read uses ONE scroll.
 *  - 335414182 (kept by v2.5.4): cleansing a TormentedSpirit with Remove Curse never reaches
 *    InventoryScroll, so that branch consumes the scroll itself.
 *  - 538914864: cancelling the item prompt of a KNOWN, non-anonymous inventory scroll costs no
 *    time and consumes nothing (only anonymous reads, which already spent the scroll, spend a turn).
 */
class InventoryScrollConsumptionPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		setStatic("curUser", null);
		setStatic("curItem", null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setStatic(String name, Object value) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	private static Hero hero() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		StubSprite.attach(hero);
		Scroll.initLabels();
		return hero;
	}

	/**
	 * Reads the scroll as Scroll.execute would after its guards (Item.execute itself needs a scene
	 * for GameScene.cancel); the item prompt needs one too, so the read ends at GameScene.selectItem.
	 */
	private static void readUntilPrompt(Hero hero, Scroll scroll) throws Exception {
		setStatic("curUser", hero);
		setStatic("curItem", scroll);
		try {
			scroll.doRead();
		} catch (NullPointerException e) {
			boolean fromPrompt = false;
			for (StackTraceElement el : e.getStackTrace()) {
				if (el.getClassName().endsWith("GameScene") && el.getMethodName().equals("selectItem")) {
					fromPrompt = true;
					break;
				}
			}
			assertTrue(fromPrompt, "the only acceptable failure is the missing scene at the item prompt: " + e);
		}
	}

	private static <T extends Scroll> T stackOf(Hero hero, T scroll, int quantity, boolean known) {
		scroll.quantity(quantity);
		if (known) {
			scroll.identify();
		}
		assertEquals(known, scroll.isKnown());
		scroll.collect(hero.belongings.backpack);
		return scroll;
	}

	@Test
	void readingAStackOfUnknownRemoveCurseSpendsOneScroll() throws Exception {
		Hero hero = hero();
		ScrollOfRemoveCurse scroll = stackOf(hero, new ScrollOfRemoveCurse(), 3, false);

		readUntilPrompt(hero, scroll);

		assertEquals(2, scroll.quantity(), "an unknown scroll is used up when read, once");
	}

	@Test
	void openingTheItemPromptOfAKnownRemoveCurseStackSpendsNothingYet() throws Exception {
		Hero hero = hero();
		ScrollOfRemoveCurse scroll = stackOf(hero, new ScrollOfRemoveCurse(), 3, true);

		readUntilPrompt(hero, scroll);

		assertEquals(3, scroll.quantity(), "a known scroll is only used up once an item is chosen");
	}

	/**
	 * A TormentedSpirit whose cleanse only records the call: the real one drops a prize heap and
	 * animates it (heap.sprite.drop()), which needs a scene. Everything up to the cleanse - which
	 * is where the scroll is consumed - is the real ScrollOfRemoveCurse.doRead.
	 */
	public static final class RecordingSpirit extends TormentedSpirit {
		int cleansed = 0;

		@Override
		public void cleanse() {
			cleansed++;
		}
	}

	/**
	 * Real ScrollOfRemoveCurse except for readAnimation, whose last step casts the reader's sprite
	 * to HeroSprite (a GL-textured sprite the headless harness cannot build). The override keeps
	 * every non-sprite effect of Scroll.readAnimation (dispel, spend, busy).
	 */
	public static final class HeadlessRemoveCurse extends ScrollOfRemoveCurse {
		@Override
		protected void readAnimation() {
			com.qsr.customspd.actors.buffs.Invisibility.dispel();
			curUser.spend(TIME_TO_READ);
			curUser.busy();
		}
	}

	private static RecordingSpirit spiritNextTo(Hero hero) {
		RecordingSpirit spirit = HeadlessLevel.at(new RecordingSpirit(), hero.pos + 1);
		StubSprite.attach(spirit);
		// Flare.show adds the flare to the reader sprite's parent group.
		new com.watabou.noosa.Group().add(hero.sprite);
		return spirit;
	}

	/**
	 * Merge fix: the spirit branch of doRead never calls super.doRead(), so its own detach is the
	 * only thing that consumes the scroll (v2.5.4 ScrollOfRemoveCurse, re-added upstream in
	 * 335414182). One port removed it under the old "Scroll.execute detaches first" model while
	 * another removed that up-front detach (2879e9ae5), leaving no consumer.
	 */
	@Test
	void cleansingATormentedSpiritConsumesExactlyOneKnownScroll() throws Exception {
		Hero hero = hero();
		HeadlessLevel.at(hero, hero.pos);
		RecordingSpirit spirit = spiritNextTo(hero);
		HeadlessRemoveCurse scroll = stackOf(hero, new HeadlessRemoveCurse(), 3, true);

		setStatic("curUser", hero);
		setStatic("curItem", scroll);
		scroll.doRead();

		assertEquals(1, spirit.cleansed, "fixture: the read took the spirit branch");
		assertEquals(2, scroll.quantity(), "cleansing the spirit uses up one scroll of the stack");
		assertTrue(hero.belongings.backpack.items.contains(scroll), "the rest of the stack stays in the pack");
	}

	@Test
	void cleansingATormentedSpiritConsumesASingleUnknownScroll() throws Exception {
		Hero hero = hero();
		HeadlessLevel.at(hero, hero.pos);
		RecordingSpirit spirit = spiritNextTo(hero);
		HeadlessRemoveCurse scroll = stackOf(hero, new HeadlessRemoveCurse(), 1, false);

		setStatic("curUser", hero);
		setStatic("curItem", scroll);
		scroll.doRead();

		assertEquals(1, spirit.cleansed, "fixture: the read took the spirit branch");
		assertTrue(!hero.belongings.backpack.items.contains(scroll), "the only scroll is used up");
	}

	@Test
	void cancellingTheItemPromptOfAKnownScrollCostsNoTimeAndNoScroll() throws Exception {
		Hero hero = hero();
		ScrollOfIdentify scroll = stackOf(hero, new ScrollOfIdentify(), 3, true);
		readUntilPrompt(hero, scroll);
		float before = hero.cooldown();

		scroll.itemSelector.onSelect(null);

		assertEquals(before, hero.cooldown(), 0.0001f, "cancelling a known inventory scroll takes no time");
		assertEquals(3, scroll.quantity(), "and consumes nothing");
	}
}
