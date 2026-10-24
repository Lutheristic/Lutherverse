package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.npcs.RatKing;
import com.qsr.customspd.items.armor.LeatherArmor;
import com.qsr.customspd.items.armor.WarriorArmor;
import com.qsr.customspd.items.potions.elixirs.ElixirOfMight;
import com.qsr.customspd.items.rings.RingOfAccuracy;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.items.wands.WandOfTransfusion;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for the item-text hunks of upstream b6f7e7462 (v2.5.0, "made a bunch of code
 * tweaks so catalogs can appear out of game"; unchanged in v2.5.4). Each of these description /
 * stat strings dereferenced Dungeon.hero, so reading an item's text with no hero (a catalog shown
 * outside a run) was a NullPointerException. With no hero there is nothing hero-specific to say,
 * and each hunk falls back to upstream's neutral default.
 */
class ItemTextNoHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void amuletDescribesItsOriginsWhenThereIsNoHero() {
		Dungeon.hero = null;
		String desc = assertDoesNotThrow(() -> new Amulet().desc());
		assertTrue(desc.contains(Messages.get(Amulet.class, "desc_origins")),
				"no hero means no AscensionChallenge, so the plain-origins paragraph");
	}

	@Test
	void elixirOfMightUsesTheBaseHealthBoostWhenThereIsNoHero() {
		Dungeon.hero = null;
		ElixirOfMight elixir = new ElixirOfMight();
		assertEquals(Messages.get(elixir, "desc", ElixirOfMight.HTBoost.boost(20)),
				assertDoesNotThrow(elixir::desc),
				"upstream b6f7e7462: a hero-less boost is computed from the starting 20 HT");
	}

	@Test
	void wandsDescribeThemselvesWithNoHero() {
		Dungeon.hero = null;
		assertDoesNotThrow(() -> new WandOfMagicMissile().info(), "Wand.info checks the Battlemage subclass");

		WandOfTransfusion transfusion = new WandOfTransfusion();
		String stats = assertDoesNotThrow(transfusion::statsDesc, "statsDesc scales self-damage by hero HT");
		// unidentified: selfDMG (1 with no hero) shown for both bounds, then 5 charge-heal base
		assertEquals(Messages.get(transfusion, "stats_desc", 1, 1, 5, transfusion.min(0), transfusion.max(0)), stats);
	}

	@Test
	void armorInfoOmitsTheStrengthWarningWhenThereIsNoHero() {
		Dungeon.hero = null;
		LeatherArmor armor = new LeatherArmor();
		String unknown = assertDoesNotThrow(armor::info);
		assertFalse(unknown.contains(Messages.get(com.qsr.customspd.items.armor.Armor.class, "probably_too_heavy")));

		armor.identify();
		String known = assertDoesNotThrow(armor::info);
		assertFalse(known.contains(Messages.get(com.qsr.customspd.items.armor.Armor.class, "too_heavy")));
	}

	@Test
	void classArmorDescriptionNeedsNoHero() {
		Dungeon.hero = null;
		WarriorArmor armor = new WarriorArmor();
		assertDoesNotThrow(armor::desc, "ClassArmor.desc only adds the ability text when the hero owns it");
	}

	@Test
	void plantAndSeedDescriptionsNeedNoHero() {
		Dungeon.hero = null;
		String plantDesc = assertDoesNotThrow(() -> new Sungrass().desc());
		assertEquals(Messages.get(Sungrass.class, "desc"), plantDesc, "no hero, so no Warden paragraph");

		String seedDesc = assertDoesNotThrow(() -> new Sungrass.Seed().desc());
		assertEquals(Messages.get(Sungrass.class, "desc"), seedDesc, "no hero, so no Warden paragraph");
	}

	@Test
	void ratKingDescriptionNeedsNoHero() {
		Dungeon.hero = null;
		assertDoesNotThrow(() -> new RatKing().description(), "RatKing.description checks the hero's armor ability");
	}

	@Test
	void noHeroMeansNothingIsEquipped() {
		Dungeon.hero = null;
		// KindofMisc.isEquipped(hero) dereferenced the hero; Ring.info() asks it for cursed rings
		RingOfAccuracy ring = new RingOfAccuracy();
		assertFalse(assertDoesNotThrow(() -> ring.isEquipped(Dungeon.hero)));
		ring.cursed = true;
		assertDoesNotThrow(ring::info, "a cursed ring's info asks whether the (absent) hero wears it");
	}
}
