package com.qsr.customspd.items.weapon.missiles;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Degrade;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream a1ffee394 (v2.3.0, "fixed thrown weapons not being affected by
 * degrade when thrown"). A thrown missile is a one-item split of its stack that is no longer in
 * the backpack, and Weapon.buffedLvl() only applies Degrade to items the hero carries, so the
 * thrown copy hit at full level while the stack was degraded. The thrown copy now asks its
 * parent stack.
 */
class MissileThrownDegradePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private MissileWeapon degradedStack(Hero[] heroOut) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		heroOut[0] = hero;

		Shuriken stack = new Shuriken();
		stack.quantity(3);
		stack.level(10);
		assertTrue(stack.collect(hero.belongings.backpack), "fixture: the stack is carried");
		Buff.affect(hero, Degrade.class, Degrade.DURATION);
		assertNotNull(hero.buff(Degrade.class));
		return stack;
	}

	@Test
	void theThrownCopyIsDegradedLikeItsStack() {
		Hero[] h = new Hero[1];
		MissileWeapon stack = degradedStack(h);
		int degraded = Degrade.reduceLevel(10);
		assertEquals(degraded, stack.buffedLvl(), "fixture: the carried stack is degraded");

		MissileWeapon thrown = (MissileWeapon) stack.split(1);

		assertEquals(degraded, thrown.buffedLvl(),
				"upstream a1ffee394: the thrown copy follows its parent's buffed level");
	}

	@Test
	void aMissileWithNoParentIsUnaffected() {
		Hero[] h = new Hero[1];
		degradedStack(h);
		Shuriken loose = new Shuriken();
		loose.level(10);
		assertEquals(10, loose.buffedLvl(), "control: items outside the pack ignore Degrade");
	}
}
