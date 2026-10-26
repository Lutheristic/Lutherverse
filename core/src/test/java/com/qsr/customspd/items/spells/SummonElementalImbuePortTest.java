package com.qsr.customspd.items.spells;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Elemental;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Upstream 938a78883 (v2.4.0): summoning no longer spends the summon elemental spell's imbue. Before,
 * every cast after the imbued one fell back to a newborn elemental; now an imbued spell keeps
 * summoning the imbued kind.
 *
 * Summoning places the elemental through ScrollOfTeleportation.appear, which needs the new
 * elemental to have a sprite on a stage. The spell builds the elemental from its imbue class, so
 * the fixture imbues it with a fire elemental that brings a no-op sprite of its own.
 */
class SummonElementalImbuePortTest {

	/** A fire elemental with a headless sprite, so the real summon path can place it. */
	public static class StagedFireElemental extends Elemental.FireElemental {
		{
			new Group().add(StubSprite.attach(this));
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private static Field summonClass() throws Exception {
		Field f = SummonElemental.class.getDeclaredField("summonClass");
		f.setAccessible(true);
		return f;
	}

	@Test
	void anImbuedSpellKeepsItsImbueAfterSummoning() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		curUser().set(null, hero);

		SummonElemental spell = new SummonElemental();
		summonClass().set(spell, StagedFireElemental.class);

		Random.pushGenerator(938L);
		try {
			spell.onCast(hero);
		} finally {
			Random.popGenerator();
		}

		int summoned = 0;
		for (Mob m : level.mobs) {
			if (m instanceof StagedFireElemental) summoned++;
		}
		assertEquals(1, summoned, "the cast summons the imbued elemental");
		assertSame(StagedFireElemental.class, summonClass().get(spell), "the imbue survives the summon");
	}
}
