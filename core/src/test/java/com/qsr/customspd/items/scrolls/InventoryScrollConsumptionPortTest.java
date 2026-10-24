package com.qsr.customspd.items.scrolls;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
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
 * the read to the moment an item is chosen. An upstream follow-up keeps that model honest and
 * is pinned here, observed through the scroll prompt (which needs a scene, so the prompt
 * itself is the NPE boundary the tests stop at):
 *
 *  - 295ac57f8: ScrollOfRemoveCurse.doRead must not detach up front - the consumption belongs to
 *    InventoryScroll (unknown: on read; known: on select), so a stacked read uses ONE scroll.
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
}
