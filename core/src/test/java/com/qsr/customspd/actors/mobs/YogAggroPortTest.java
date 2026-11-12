package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for the Yog-Dzewa part of upstream 01a9218ad and its null check from
 * c2b163ae0: Yog passes its target on to the minions around it, but no longer sets them on one
 * of their own (a minion marked by a stone of aggression, say) and no longer directs minions
 * that have been turned to the other side.
 */
class YogAggroPortTest {

	private static final int SIZE = 12;

	private Hero hero;
	private YogDzewa yog;
	private YogDzewa.Larva marked;
	private YogDzewa.Larva bystander;

	private String prevLevelName;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevLevelName = Dungeon.levelName;
		Dungeon.levelName = "25"; // Yog seeds its fist order from the level name
		HeadlessLevel.install(SIZE, SIZE);
		hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);

		yog = mob(new YogDzewa(), 6 * SIZE + 6);
		marked = mob(new YogDzewa.Larva(), 6 * SIZE + 8);
		bystander = mob(new YogDzewa.Larva(), 8 * SIZE + 6);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		Dungeon.levelName = prevLevelName;
	}

	private static <T extends Mob> T mob(T mob, int cell) {
		HeadlessLevel.at(mob, cell);
		StubSprite.attach(mob);
		Dungeon.level.mobs.add(mob);
		return mob;
	}

	@Test
	void yogDoesNotTurnItsMinionsOnEachOther() {
		yog.aggro(marked);

		assertFalse(bystander.isTargeting(marked), "a larva is not set on another larva");
	}

	@Test
	void yogDoesNotDirectAMinionThatChangedSides() {
		bystander.alignment = Char.Alignment.ALLY;

		yog.aggro(hero);

		assertFalse(bystander.isTargeting(hero), "a larva on the hero's side is left alone");
		assertTrue(marked.isTargeting(hero), "Yog's own larvae still go for the hero");
	}

	@Test
	void yogStillSetsItsMinionsOnTheHero() {
		// unchanged by the port: Yog sets the minions around it on the hero
		yog.aggro(hero);

		assertTrue(marked.isTargeting(hero));
		assertTrue(bystander.isTargeting(hero));
	}

	@Test
	void yogStillSetsItsMinionsOnAMinionThatChangedSides() {
		// unchanged by the port: a corrupted larva is an enemy like any other
		marked.alignment = Char.Alignment.ALLY;

		yog.aggro(marked);

		assertTrue(bystander.isTargeting(marked));
	}

	@Test
	void yogCanBeToldToDropItsTarget() {
		// aggro(null) is how an expiring stone of aggression clears enemy targets
		bystander.aggro(hero);

		yog.aggro(null);

		assertTrue(bystander.isTargeting(null), "the minions drop their target with Yog");
	}
}
