package com.qsr.customspd.items.bags;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.QuickSlot;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD 1c9d21f1e (v2.3.0): "fixed cases where picking up bags could
 * result in multiple quickslot placeholders".
 *
 * A quickslot holds a placeholder when its item ran out. Picking such an item up re-fills the slot
 * (Item.collect calls replacePlaceholder), but an item that arrives INSIDE a bag is never
 * collect()ed by the hero: Bag.collect only moved the bag. The slot stayed a placeholder while the
 * real item sat in the bag, and the next use created a second placeholder for it. Bag.collect now
 * assigns placeholders to the bag's contents.
 */
class BagPlaceholderPortTest {

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
		Item.updateQuickslot();
	}

	@Test
	void collectingABagFillsAQuickslotPlaceholderFromItsContents() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);

		// the slot held a healing potion that has since run out, so it is a placeholder
		PotionOfHealing gone = new PotionOfHealing();
		Dungeon.quickslot.setSlot(0, gone);
		Dungeon.quickslot.convertToPlaceholder(gone);
		assertTrue(Dungeon.quickslot.isPlaceholder(0), "test premise: slot 0 is a placeholder");

		// a bandolier found on the floor with a healing potion in it
		PotionBandolier bandolier = new PotionBandolier();
		PotionOfHealing inside = new PotionOfHealing();
		bandolier.items.add(inside);

		assertTrue(bandolier.collect(hero.belongings.backpack));

		assertFalse(Dungeon.quickslot.isPlaceholder(0), "the placeholder was replaced");
		assertSame(inside, Dungeon.quickslot.getItem(0), "slot 0 now points at the potion inside the bag");
	}
}
