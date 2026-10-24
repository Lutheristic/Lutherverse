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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream b107ca1de (v2.2.0, "fixed single scrolls of transmutation working
 * on themselves") and e7dbe79c1 (v2.5.4, "fixed 2x unided SOTrans not being usable on its own
 * stack"), both follow-ups of 2879e9ae5.
 *
 * Since 2879e9ae5 a KNOWN inventory scroll stays in the pack until an item is chosen, so a lone
 * known scroll of transmutation was offered as its own target: choosing it detached it and then
 * transmuted it, turning one scroll of transmutation into a different scroll for one turn. An
 * UNKNOWN scroll is already consumed when read, so what is left of its stack is a real target.
 */
class TransmutationSelfTargetPortTest {

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
		Scroll.initLabels(); // scroll identity is global; forget what earlier tests identified
		return hero;
	}

	private static ScrollOfTransmutation stack(Hero hero, int quantity, boolean known) {
		ScrollOfTransmutation scroll = new ScrollOfTransmutation();
		scroll.quantity(quantity);
		if (known) scroll.identify();
		assertEquals(known, scroll.isKnown());
		scroll.collect(hero.belongings.backpack);
		return scroll;
	}

	/** Reads up to the item prompt, which needs a scene: GameScene.selectItem is the boundary. */
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

	@Test
	void aLoneKnownScrollOfTransmutationCannotTargetItself() throws Exception {
		Hero hero = hero();
		ScrollOfTransmutation scroll = stack(hero, 1, true);
		readUntilPrompt(hero, scroll);

		assertTrue(hero.belongings.backpack.items.contains(scroll), "fixture: a known scroll is not consumed yet");
		assertFalse(scroll.itemSelector.itemSelectable(scroll),
				"upstream b107ca1de: the last scroll of transmutation is not offered as its own target");
	}

	@Test
	void otherScrollsStayTransmutable() throws Exception {
		Hero hero = hero();
		ScrollOfTransmutation scroll = stack(hero, 1, true);
		ScrollOfIdentify other = new ScrollOfIdentify();
		other.collect(hero.belongings.backpack);
		readUntilPrompt(hero, scroll);

		assertTrue(scroll.itemSelector.itemSelectable(other));
	}

	@Test
	void aKnownStackCanTransmuteOneOfItsOwnScrolls() throws Exception {
		Hero hero = hero();
		ScrollOfTransmutation scroll = stack(hero, 2, true);
		readUntilPrompt(hero, scroll);

		assertTrue(scroll.itemSelector.itemSelectable(scroll), "the read uses one, the other is a real target");
	}

	@Test
	void anUnknownPairCanTransmuteTheScrollLeftInItsStack() throws Exception {
		Hero hero = hero();
		ScrollOfTransmutation scroll = stack(hero, 2, false);
		readUntilPrompt(hero, scroll);

		assertEquals(1, scroll.quantity(), "fixture: the unknown read already used one scroll");
		assertTrue(scroll.itemSelector.itemSelectable(scroll),
				"upstream e7dbe79c1: the scroll left in the stack is a real target");
	}
}
