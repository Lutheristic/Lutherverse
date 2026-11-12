package com.qsr.customspd.actors.hero.abilities.duelist;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.armor.DuelistArmor;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * upstream b34bf4d9a (v2.3.0): elemental strike reaches 4 tiles before talents, and elemental
 * reach adds 1 to 4 on top of that.
 */
class ElementalStrikeRangePortTest {

	private static final int W = 14;
	private static final int ROW = 4 * W;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		Messages.setup(Languages.ENGLISH);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** Strikes east along a row with no weapon, and returns the rat standing `distance` tiles out. */
	private static Rat strikeWithARatAt(int distance, int reachPoints) {
		HeadlessLevel level = HeadlessLevel.install(W, 9);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		hero.talents.get(3).put(Talent.ELEMENTAL_REACH, reachPoints);
		Dungeon.hero = hero;
		StubSprite sprite = StubSprite.attach(hero);
		// the cone's missiles are recycled from the hero sprite's parent
		new Group().add(sprite);
		HeadlessLevel.at(hero, ROW + 1);
		Arrays.fill(level.heroFOV, true);

		Rat rat = HeadlessLevel.at(new Rat(), ROW + 1 + distance);
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		level.mobs.add(rat);

		ClassArmor armor = ClassArmor.upgrade(hero, new DuelistArmor());
		armor.charge = 100;
		new ElementalStrike().activate(armor, hero, ROW + W - 2);
		return rat;
	}

	@Test
	void theBaseStrikeReachesFourTiles() {
		Rat rat = strikeWithARatAt(4, 0);
		assertTrue(rat.HP < rat.HT, "a rat 4 tiles away is inside the cone");
	}

	@Test
	void theBaseStrikeStopsShortOfFiveTiles() {
		Rat rat = strikeWithARatAt(5, 0);
		assertEquals(rat.HT, rat.HP, "a rat 5 tiles away is outside the cone");
	}

	@Test
	void eachPointOfReachAddsATile() {
		Rat rat = strikeWithARatAt(5, 1);
		assertTrue(rat.HP < rat.HT, "one point of elemental reach brings 5 tiles into range");
	}

	@Test
	void theTalentTextMatchesTheNewRange() {
		String desc = Talent.ELEMENTAL_REACH.desc(false);
		assertTrue(desc.contains("_5 tiles_ from 4"), desc);
		assertTrue(desc.contains("_8 tiles_ from 4"), desc);
	}
}
