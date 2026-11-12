package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.stones.StoneOfAggression;
import com.qsr.customspd.mechanics.Ballistica;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 01a9218ad: a boss or miniboss that is marked by a stone of
 * aggression takes half damage from its own side's evil eye beams and warlock bolts, the way
 * Char.attack already halves their melee hits. Yog-Dzewa takes a quarter from the beams.
 */
class AggressionBossDamagePortTest {

	private static final int SIZE = 12;
	private static final int ROW = 6 * SIZE;
	private static final int SHOOTER = ROW + 2;
	private static final int TARGET = ROW + 6;
	private static final long SEED = 20261112L;

	/** A char that never dodges and records the hits it takes. It starts on the shooter's side. */
	static class Target extends Rat {
		final List<Integer> hits = new ArrayList<>();

		Target(Property rank) {
			HP = HT = 1000;
			if (rank != null) properties.add(rank);
		}

		@Override
		public int defenseSkill(Char enemy) {
			return 0;
		}

		@Override
		public void damage(int dmg, Object src) {
			hits.add(dmg);
		}
	}

	/** Yog-Dzewa, never dodging, recording the hits it takes and ignoring them otherwise. */
	static class RecordingYog extends YogDzewa {
		final List<Integer> hits = new ArrayList<>();

		@Override
		public int defenseSkill(Char enemy) {
			return 0;
		}

		@Override
		public void damage(int dmg, Object src) {
			hits.add(dmg);
		}
	}

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
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		Dungeon.levelName = prevLevelName;
	}

	private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	private static void clearChars() {
		for (Char ch : Actor.chars().toArray(new Char[0])) {
			if (ch != Dungeon.hero) Actor.remove(ch);
		}
	}

	private static <T extends Char> T placed(T target, boolean aggroed) {
		HeadlessLevel.at(target, TARGET);
		StubSprite.attach(target);
		if (aggroed) Buff.affect(target, StoneOfAggression.Aggression.class, 5f);
		return target;
	}

	/** An evil eye on the target's side fires a charged beam through it under a fixed seed. */
	private static void gaze(Char target) throws Exception {
		Eye eye = HeadlessLevel.at(new Eye(), SHOOTER);
		StubSprite.attach(eye);
		eye.beamCharged = true;
		set(Eye.class, eye, "beam", new Ballistica(SHOOTER, TARGET, Ballistica.STOP_SOLID));
		Random.pushGenerator(SEED);
		try {
			eye.deathGaze();
		} finally {
			Random.popGenerator();
		}
	}

	/** A warlock on the target's side fires a bolt at it under a fixed seed. */
	private static void bolt(Char target) {
		Warlock warlock = HeadlessLevel.at(new Warlock(), SHOOTER);
		StubSprite.attach(warlock);
		warlock.enemy = target;
		Random.pushGenerator(SEED);
		try {
			warlock.zap();
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void anAggroedBossTakesHalfFromAnEvilEyeBeam() throws Exception {
		Target unmarked = placed(new Target(Char.Property.BOSS), false);
		gaze(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.BOSS), true);
		gaze(marked);

		int full = unmarked.hits.get(0);
		assertEquals((int) (full * 0.5f), (int) marked.hits.get(0), "beam " + full + " on an aggroed boss");
	}

	@Test
	void anAggroedYogTakesAQuarterFromAnEvilEyeBeam() throws Exception {
		RecordingYog unmarked = placed(new RecordingYog(), false);
		gaze(unmarked);
		clearChars();
		RecordingYog marked = placed(new RecordingYog(), true);
		gaze(marked);

		int full = unmarked.hits.get(0);
		assertEquals((int) ((int) (full * 0.5f) * 0.5f), (int) marked.hits.get(0), "beam " + full + " on an aggroed Yog");
	}

	@Test
	void anAggroedBossTakesHalfFromAWarlockBolt() {
		Target unmarked = placed(new Target(Char.Property.BOSS), false);
		bolt(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.BOSS), true);
		bolt(marked);

		int full = unmarked.hits.get(0);
		assertEquals((int) (full * 0.5f), (int) marked.hits.get(0), "bolt " + full + " on an aggroed boss");
	}

	@Test
	void anAggroedMinibossTakesHalfFromAnEvilEyeBeam() throws Exception {
		Target unmarked = placed(new Target(Char.Property.MINIBOSS), false);
		gaze(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.MINIBOSS), true);
		gaze(marked);

		int full = unmarked.hits.get(0);
		assertEquals((int) (full * 0.5f), (int) marked.hits.get(0), "beam " + full + " on an aggroed miniboss");
	}

	@Test
	void anAggroedMinibossTakesHalfFromAWarlockBolt() {
		Target unmarked = placed(new Target(Char.Property.MINIBOSS), false);
		bolt(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.MINIBOSS), true);
		bolt(marked);

		int full = unmarked.hits.get(0);
		assertEquals((int) (full * 0.5f), (int) marked.hits.get(0), "bolt " + full + " on an aggroed miniboss");
	}

	@Test
	void anAggroedBossOnTheOtherSideTakesFullBeamDamage() throws Exception {
		// the eye's beam is only softened for a boss on the eye's own side
		Target unmarked = placed(new Target(Char.Property.BOSS), false);
		unmarked.alignment = Char.Alignment.ALLY;
		gaze(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.BOSS), true);
		marked.alignment = Char.Alignment.ALLY;
		gaze(marked);

		assertEquals(unmarked.hits.get(0), marked.hits.get(0));
	}

	@Test
	void anAggroedBossOnTheOtherSideTakesFullBoltDamage() {
		Target unmarked = placed(new Target(Char.Property.BOSS), false);
		unmarked.alignment = Char.Alignment.ALLY;
		bolt(unmarked);
		clearChars();
		Target marked = placed(new Target(Char.Property.BOSS), true);
		marked.alignment = Char.Alignment.ALLY;
		bolt(marked);

		assertEquals(unmarked.hits.get(0), marked.hits.get(0));
	}

	@Test
	void anAggroedRegularEnemyTakesFullBoltDamage() {
		// unchanged by the port: only bosses and minibosses resist their minions
		Target unmarked = placed(new Target(null), false);
		bolt(unmarked);
		clearChars();
		Target marked = placed(new Target(null), true);
		bolt(marked);

		assertEquals(unmarked.hits.get(0), marked.hits.get(0));
	}

	@Test
	void anAggroedRegularEnemyTakesFullBeamDamage() throws Exception {
		// unchanged by the port: only bosses and minibosses resist their minions
		Target unmarked = placed(new Target(null), false);
		gaze(unmarked);
		clearChars();
		Target marked = placed(new Target(null), true);
		gaze(marked);

		assertEquals(unmarked.hits.get(0), marked.hits.get(0));
	}
}
