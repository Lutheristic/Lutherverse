package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Amok;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Charm;
import com.qsr.customspd.actors.buffs.Chill;
import com.qsr.customspd.actors.buffs.Corruption;
import com.qsr.customspd.actors.buffs.Dread;
import com.qsr.customspd.actors.buffs.Frost;
import com.qsr.customspd.actors.buffs.Paralysis;
import com.qsr.customspd.actors.buffs.Sleep;
import com.qsr.customspd.actors.buffs.Slow;
import com.qsr.customspd.actors.buffs.Speed;
import com.qsr.customspd.actors.buffs.Terror;
import com.qsr.customspd.actors.buffs.Vertigo;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.wands.WandOfRegrowth;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 54367bae2: the demon spawner, pylons, the rot heart, Yog-Dzewa
 * and the regrowth lotus are STATIC instead of keeping their own immunity lists. On top of the
 * mob behaviour debuffs and stuns they already shrugged off, they can no longer be chilled, slowed,
 * sped up, frozen or charmed.
 */
class StaticImmunitiesPortTest {

	private static final int SIZE = 10;

	private static final List<Class<? extends Buff>> STATIC_IMMUNE = Arrays.asList(
			Chill.class, Slow.class, Speed.class, Frost.class, Charm.class,
			Paralysis.class, Amok.class, Sleep.class, Dread.class, Terror.class, Vertigo.class);

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private String prevLevelName;

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

	/** The debuffs from the list that still attach to the char. */
	private static List<String> attachable(Char ch) {
		StubSprite.attach(ch);
		HeadlessLevel.at(ch, 5 * SIZE + 5);
		List<String> stuck = new ArrayList<>();
		for (Class<? extends Buff> debuff : STATIC_IMMUNE) {
			Buff.affect(ch, debuff);
			if (ch.buff(debuff) != null) {
				stuck.add(debuff.getSimpleName());
				ch.buff(debuff).detach();
			}
		}
		return stuck;
	}

	@Test
	void theDemonSpawnerShrugsOffEveryStunAndStateDebuff() {
		DemonSpawner spawner = new DemonSpawner();
		assertEquals("[]", attachable(spawner).toString());
		assertTrue(Char.hasProp(spawner, Char.Property.STATIC));
	}

	@Test
	void anActivePylonShrugsOffEveryStunAndStateDebuff() {
		Pylon pylon = new Pylon();
		pylon.alignment = Char.Alignment.ENEMY; // an inactive pylon takes no buffs at all
		assertEquals("[]", attachable(pylon).toString());
		assertTrue(Char.hasProp(pylon, Char.Property.STATIC));
	}

	@Test
	void theRotHeartShrugsOffEveryStunAndStateDebuff() {
		RotHeart heart = new RotHeart();
		assertEquals("[]", attachable(heart).toString());
		assertTrue(Char.hasProp(heart, Char.Property.STATIC));
	}

	@Test
	void yogDzewaShrugsOffEveryStunAndStateDebuff() {
		YogDzewa yog = new YogDzewa();
		assertEquals("[]", attachable(yog).toString());
		assertTrue(Char.hasProp(yog, Char.Property.STATIC));
	}

	@Test
	void theRegrowthLotusIsImmuneToEveryStunAndStateDebuff() {
		// the lotus refuses every buff in add(), so its immunity list is what is checked here
		WandOfRegrowth.Lotus lotus = new WandOfRegrowth.Lotus();
		List<String> notImmune = new ArrayList<>();
		for (Class<? extends Buff> debuff : STATIC_IMMUNE) {
			if (!lotus.isImmune(debuff)) notImmune.add(debuff.getSimpleName());
		}
		assertEquals("[]", notImmune.toString());
		assertTrue(Char.hasProp(lotus, Char.Property.STATIC));
		assertTrue(lotus.isImmune(Corruption.class), "static covers the ally buffs the lotus listed itself");
	}
}
