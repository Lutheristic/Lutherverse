package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Corruption;
import com.qsr.customspd.actors.buffs.Terror;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 8abc3699d (v2.2.0), which reworked how retreating mobs behave.
 *
 * Mob.Fleeing used to drop its target when it lost sight of its enemy and then stand still, and
 * a fleeing mob with nowhere to run did nothing at all. Now losing the enemy runs an escaped()
 * hook and the mob keeps running from where it last saw them; a cornered mob that is not
 * terrified turns to fight if it can see its enemy and wanders off if it cannot. Thief and
 * CrystalMimic moved their escape code from nowhereToRun() into escaped(), so a cornered thief or
 * crystal mimic out of the hero's sight now wanders off where it used to get away, and a crystal
 * mimic that turns to fight gets its turn back.
 *
 * Each case calls the mob's own act() once on a HeadlessLevel, usually with the hero as the enemy.
 */
class MobRetreatPortTest {

	private static final long SEED = 20261112L;

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveHero() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void aFleeingMobThatLosesItsEnemyKeepsRunning() {
		openRoom();
		placeHero(27, 4);
		Rat rat = placeMob(new Rat(), 8, 4);
		int lastSeen = cell(7, 4);
		rat.state = rat.FLEEING;
		rat.target = lastSeen;

		act(rat);

		assertSame(rat.FLEEING, rat.state, stateName(rat));
		assertNotEquals(cell(8, 4), rat.pos, "the rat must keep running, not stand still");
		assertTrue(Dungeon.level.distance(rat.pos, lastSeen) > 1, "it runs away from where it last saw the enemy");
	}

	@Test
	void aCorneredMobThatSeesItsEnemyTurnsToFight() {
		deadEnd();
		Hero hero = placeHero(2, 2);
		Rat rat = placeMob(new Rat(), 1, 2);
		rat.state = rat.FLEEING;
		rat.enemy = hero;

		act(rat);

		assertEquals(cell(1, 2), rat.pos, "precondition: the rat had nowhere to run");
		assertSame(rat.HUNTING, rat.state, stateName(rat));
		assertTrue(showedRage(rat), "turning to fight shows the rage text: " + StubSprite.of(rat).statuses);
		assertEquals(1f, rat.cooldown(), "turning to fight costs an ordinary mob its turn");
	}

	@Test
	void aCorneredMobThatLostSightOfItsEnemyWandersOff() {
		deadEnd();
		Hero hero = placeHero(2, 2);
		// The hero vanishes right in front of the rat: still its enemy, no longer in view.
		hero.invisible = 1;
		Rat rat = placeMob(new Rat(), 1, 2);
		rat.state = rat.FLEEING;
		rat.enemy = hero;
		rat.target = hero.pos;

		act(rat);

		assertEquals(cell(1, 2), rat.pos, "precondition: the rat had nowhere to run");
		assertSame(rat.WANDERING, rat.state, stateName(rat));
		assertTrue(StubSprite.of(rat).statuses.isEmpty(), "no rage without an enemy in view");
	}

	@Test
	void aTerrifiedCorneredMobKeepsFleeing() {
		deadEnd();
		Hero hero = placeHero(2, 2);
		Rat rat = placeMob(new Rat(), 1, 2);
		Buff.affect(rat, Terror.class, Terror.DURATION).object = hero.id();

		act(rat);

		assertEquals(cell(1, 2), rat.pos, "precondition: the rat had nowhere to run");
		assertSame(rat.FLEEING, rat.state, "terror keeps a cornered mob from turning: " + stateName(rat));
		assertTrue(!showedRage(rat), "no rage while terrified");
	}

	// From upstream 74eb63739: allies are no longer exempt from turning to fight.
	@Test
	void aCorneredAllyTurnsToFightTheEnemyItFledFrom() {
		deadEnd();
		placeHero(18, 2);
		Rat ally = placeMob(new Rat(), 1, 2);
		Buff.affect(ally, Corruption.class);
		Rat enemy = placeMob(new Rat(), 2, 2);
		enemy.state = enemy.HUNTING;
		ally.state = ally.FLEEING;
		ally.enemy = enemy;

		act(ally);

		assertEquals(cell(1, 2), ally.pos, "precondition: the ally had nowhere to run");
		assertSame(ally.HUNTING, ally.state, stateName(ally));
		assertTrue(ally.isTargeting(enemy), "it turns on the enemy that cornered it");
	}

	@Test
	void aCrystalMimicThatTurnsToFightKeepsItsTurn() {
		deadEnd();
		Hero hero = placeHero(2, 2);
		CrystalMimic mimic = placeMob(new CrystalMimic(), 1, 2);
		mimic.adjustStats(10);
		mimic.alignment = Char.Alignment.ENEMY;
		mimic.state = mimic.FLEEING;
		mimic.enemy = hero;

		act(mimic);

		assertEquals(cell(1, 2), mimic.pos, "precondition: the mimic had nowhere to run");
		assertSame(mimic.HUNTING, mimic.state, stateName(mimic));
		assertEquals(0f, mimic.cooldown(), "crystal mimics are fast: turning to fight costs no time");
	}

	@Test
	void aCorneredThiefThatLostSightOfItsEnemyKeepsTheLoot() {
		deadEnd();
		Hero hero = placeHero(10, 2);
		hero.invisible = 1;
		Thief thief = placeMob(new Thief(), 1, 2);
		blockTheWayOut();
		thief.item = new Gold(10);
		thief.state = thief.FLEEING;
		thief.enemy = hero;
		// Last seen close by, so the roll to lose the enemy cannot fire.
		thief.target = cell(4, 2);
		assertFalse(Dungeon.level.heroFOV[thief.pos], "precondition: out of the hero's sight");
		assertTrue(Dungeon.level.distance(hero.pos, thief.pos) >= 6, "precondition: far enough to get away");

		act(thief);

		assertNotNull(thief.item, "only losing the enemy lets a thief get away with the loot");
		assertEquals(cell(1, 2), thief.pos, "the thief stays where it was cornered");
		assertSame(thief.WANDERING, thief.state, stateName(thief));
	}

	@Test
	void aCorneredCrystalMimicThatLostSightOfItsEnemyWandersOff() {
		deadEnd();
		Hero hero = placeHero(10, 2);
		hero.invisible = 1;
		CrystalMimic mimic = placeMob(new CrystalMimic(), 1, 2);
		blockTheWayOut();
		mimic.adjustStats(10);
		mimic.alignment = Char.Alignment.ENEMY;
		mimic.state = mimic.FLEEING;
		mimic.enemy = hero;
		mimic.target = cell(4, 2);
		assertFalse(Dungeon.level.heroFOV[mimic.pos], "precondition: out of the hero's sight");
		assertTrue(Dungeon.level.distance(hero.pos, mimic.pos) >= 6, "precondition: far enough to get away");

		act(mimic);

		assertTrue(Dungeon.level.mobs.contains(mimic), "only losing the enemy lets a crystal mimic get away");
		assertEquals(cell(1, 2), mimic.pos, "the mimic stays where it was cornered");
		assertSame(mimic.WANDERING, mimic.state, stateName(mimic));
	}

	// The old nowhereToRun() code got the same result here.
	@Test
	void aCrystalMimicThatGetsAwayIsGone() {
		openRoom();
		placeHero(2, 4);
		CrystalMimic mimic = placeMob(new CrystalMimic(), 20, 4);
		mimic.adjustStats(10);
		mimic.alignment = Char.Alignment.ENEMY;
		mimic.state = mimic.FLEEING;
		mimic.target = cell(19, 4);

		act(mimic);

		assertNull(mimic.enemy, "precondition: the hero is out of the mimic's sight");
		assertFalse(Dungeon.level.mobs.contains(mimic), "the mimic leaves the level");
		assertFalse(Actor.chars().contains(mimic), "and stops acting");
	}

	// The old nowhereToRun() code got the same result here.
	@Test
	void aThiefThatGetsAwayTeleportsWithTheLoot() {
		openRoom();
		placeHero(2, 4);
		Thief thief = placeMob(new Thief(), 20, 4);
		thief.item = new Gold(10);
		thief.state = thief.FLEEING;
		thief.target = cell(19, 4);

		act(thief);

		assertNull(thief.item, "the thief got away with the item");
		assertNotEquals(cell(20, 4), thief.pos, "the thief teleports off");
		assertSame(thief.WANDERING, thief.state, stateName(thief));
	}

	// From upstream f2d0a821f: a mob whose escape logic ends its fleeing still spends the turn.
	@Test
	void aThiefThatGetsAwaySpendsItsTurn() {
		openRoom();
		placeHero(2, 4);
		Thief thief = placeMob(new Thief(), 20, 4);
		thief.item = new Gold(10);
		thief.state = thief.FLEEING;
		thief.target = cell(19, 4);

		act(thief);

		assertNull(thief.item, "precondition: the thief got away");
		assertEquals(1f, thief.cooldown(), "getting away takes the thief's turn");
	}

	// ------------------------------------------------------------------------------ helpers

	/** An open 30x9 room. */
	private static void openRoom() {
		HeadlessLevel.install(30, 9);
	}

	/** A 20x5 room walled down to one corridor along row 2, closed at x = 1. */
	private static void deadEnd() {
		HeadlessLevel level = HeadlessLevel.install(20, 5);
		for (int x = 1; x < 19; x++) {
			level.map[cell(x, 1)] = Terrain.WALL;
			level.map[cell(x, 3)] = Terrain.WALL;
		}
		level.buildFlagMaps();
	}

	/** A sleeping rat in the mouth of the dead end, so a mob at x = 1 cannot run past it. */
	private static void blockTheWayOut() {
		placeMob(new Rat(), 2, 2);
	}

	private static Hero placeHero(int x, int y) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, cell(x, y));
		StubSprite.attach(hero);
		return hero;
	}

	private static <T extends Mob> T placeMob(T mob, int x, int y) {
		HeadlessLevel.at(mob, cell(x, y));
		Dungeon.level.mobs.add(mob);
		StubSprite.attach(mob);
		return mob;
	}

	private static void act(Mob mob) {
		Random.pushGenerator(SEED);
		try {
			mob.act();
		} finally {
			Random.popGenerator();
		}
	}

	private static boolean showedRage(Mob mob) {
		String rage = Messages.get(Mob.class, "rage");
		for (StubSprite.Status s : StubSprite.of(mob).statuses) {
			if (s.text.equals(rage)) return true;
		}
		return false;
	}

	private static int cell(int x, int y) {
		return y * Dungeon.level.width() + x;
	}

	private static String stateName(Mob mob) {
		return mob.state.getClass().getSimpleName();
	}
}
