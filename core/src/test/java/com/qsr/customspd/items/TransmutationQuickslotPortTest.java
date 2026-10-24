package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.QuickSlot;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.scrolls.ScrollOfTransmutation;
import com.qsr.customspd.items.wands.Wand;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD 241292cf4 (v2.3.1): "fixed cases where transmutation
 * would not preserve quickslots".
 *
 * ScrollOfTransmutation.onItemSelected re-points the transmuted item's quickslot at the result.
 * After collecting the result it used to swap {@code result} for
 * {@code belongings.getSimilar(result)} unconditionally; getSimilar matches on class alone, so a
 * NON-stackable result (a wand) whose class the hero already carried was replaced by that other,
 * pre-existing wand, and the quickslot ended up pointing at the wrong item. The swap only makes
 * sense for stackable results, where collect() merged the new item into the existing stack.
 *
 * The scenario forces the collision deterministically: the hero carries one wand of EVERY class,
 * so whichever class the transmutation rolls, a same-class wand already sits in the pack.
 */
class TransmutationQuickslotPortTest {

	private Hero prevHero;
	private QuickSlot prevQuickslot;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
		prevQuickslot = Dungeon.quickslot;
		Dungeon.quickslot = new QuickSlot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = prevHero;
		Dungeon.quickslot = prevQuickslot;
		Item.curUser = null;
		Item.curItem = null;
	}

	private static void select(ScrollOfTransmutation scroll, Item item) throws Exception {
		Method m = ScrollOfTransmutation.class.getDeclaredMethod("onItemSelected", Item.class);
		m.setAccessible(true);
		m.invoke(scroll, item);
	}

	@Test
	void quickslotFollowsTheNewWandNotAnExistingSameClassWand() throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		Item.curUser = hero;

		// one wand of every class, so any rolled class collides with a pre-existing instance
		Set<Item> preexisting = Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>());
		WandOfMagicMissile source = null;
		for (Class<?> c : com.qsr.customspd.items.Generator.Category.WAND.classes) {
			Wand w = (Wand) Reflection.newInstance(c);
			hero.belongings.backpack.items.add(w);
			preexisting.add(w);
			if (w instanceof WandOfMagicMissile) source = (WandOfMagicMissile) w;
		}
		assertNotNull(source, "the pack must hold the wand being transmuted");

		ScrollOfTransmutation scroll = new ScrollOfTransmutation();
		Item.curItem = scroll;
		Dungeon.quickslot.setSlot(0, source);
		assertTrue(Dungeon.quickslot.getSlot(source) == 0);

		Random.pushGenerator(20261023L);
		try {
			select(scroll, source);
		} finally {
			Random.popGenerator();
		}

		Item inSlot = Dungeon.quickslot.getItem(0);
		assertNotNull(inSlot, "the quickslot must still be filled after transmutation");
		assertTrue(hero.belongings.contains(inSlot), "the quickslot item must be in the pack");
		assertFalse(preexisting.contains(inSlot),
				"the quickslot must follow the NEW wand, not a same-class wand that was already carried");
		assertTrue(inSlot instanceof Wand);
		assertFalse(inSlot.getClass() == WandOfMagicMissile.class,
				"transmutation always changes the wand's class");
	}
}
