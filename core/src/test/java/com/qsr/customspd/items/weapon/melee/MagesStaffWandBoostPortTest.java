package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.ScrollEmpower;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 7a05cdb41 (v2.2.1, "fixed mage's staff melee benefiting from wand
 * boosts"). The staff overrode buffedLvl() to take the imbued wand's level when higher, so a
 * temporary wand boost (a read Scroll of Upgrade's empowerment, Wild Magic, magic missile's
 * charge) also raised the staff's melee damage. The override now covers only
 * buffedVisiblyUpgraded(), the level shown on the item.
 *
 * Upstream's second hunk rewords the Desperate Power talent description; CPDU has no such talent
 * (or message key), so that hunk is dependency-deferred.
 */
class MagesStaffWandBoostPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void aWandBoostRaisesTheShownLevelButNotTheMeleeLevel() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);

		WandOfMagicMissile wand = new WandOfMagicMissile();
		MagesStaff staff = new MagesStaff(wand);
		wand.charge(hero);
		Buff.affect(hero, ScrollEmpower.class); // +3 levels on the carried wand

		assertEquals(3, wand.buffedLvl(), "fixture: the wand is boosted by 3");
		assertEquals(3, staff.buffedVisiblyUpgraded(), "the boost is still shown on the staff");
		assertEquals(0, staff.buffedLvl(),
				"upstream 7a05cdb41: melee scaling uses the staff's own level, not the wand's boost");
	}
}
