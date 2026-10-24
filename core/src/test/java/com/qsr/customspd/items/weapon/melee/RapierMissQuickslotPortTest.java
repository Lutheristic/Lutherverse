package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 23886d73f (v2.5.0, "fixed rapier lunge into empty space not
 * updating quickslots"). Since fa66229e9 a lunge miss deducts its charge inline instead of via
 * beforeAbilityUsed (which refreshed the quickslot), and the deduction happens in the jump
 * callback, after MeleeWeapon.execute's own refresh. Without a refresh of its own the weapon's
 * "x/y" charge status stays stale. Item.updateQuickslot() raises GameScene.updateItemDisplays,
 * which is what these tests observe.
 */
class RapierMissQuickslotPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		GameScene.updateItemDisplays = false;
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero duelist(int start) {
		// Hero.speed() casts its sprite to HeroSprite (needs GL), so it is pinned as in RapierLungePortTest.
		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		return hero;
	}

	@Test
	void aLungeMissRefreshesTheQuickslot() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		Hero hero = duelist(start);
		Rapier rapier = new Rapier();
		hero.belongings.weapon = rapier;
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		int before = charger.charges;
		GameScene.updateItemDisplays = false;

		rapier.duelistAbility(hero, 4 * level.width() + 6);

		assertEquals(before - 1, charger.charges, "fixture: the miss spent a charge");
		assertTrue(GameScene.updateItemDisplays, "the spent charge is shown on the quickslot");
	}

	@Test
	void aSecondWeaponLungeMissRefreshesTheQuickslot() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		Hero hero = duelist(start);
		hero.belongings.weapon = new Sword();
		Rapier rapier = new Rapier();
		hero.belongings.secondWep = rapier;
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.secondCharges = 2;
		charger.secondPartialCharge = 0;
		GameScene.updateItemDisplays = false;

		rapier.duelistAbility(hero, 4 * level.width() + 6);

		assertEquals(1, charger.secondCharges, "fixture: the miss spent a second-pool charge");
		assertTrue(GameScene.updateItemDisplays, "the spent charge is shown on the quickslot");
	}
}
