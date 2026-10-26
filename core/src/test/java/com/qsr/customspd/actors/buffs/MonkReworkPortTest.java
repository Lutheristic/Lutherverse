package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.items.armor.LeatherArmor;
import com.qsr.customspd.items.armor.MailArmor;
import com.qsr.customspd.items.weapon.melee.Gloves;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import com.qsr.customspd.ui.BuffIndicator;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream bf56446d0 (v2.5.0, "various monk adjustments") and db4a922ae: monk abilities lose
 * their shared cooldown, flurry instead waits a turn and hits for 1.5x, focus lasts until it
 * parries and blocks magic too, dash reaches 4 tiles (+4 empowered), dragon kick hits for 6x
 * (9x empowered), and Unencumbered Spirit gives 50/75/100% more energy.
 */
class MonkReworkPortTest {

	private static final int W = 20;
	private static final int START = 4 * W + 2;

	private static final String JUST_HIT = "com.qsr.customspd.actors.buffs.MonkEnergy$MonkAbility$JustHitTracker";
	private static final String FOCUS_ACTIVATION = "com.qsr.customspd.actors.buffs.MonkEnergy$MonkAbility$Focus$FocusActivation";
	private static final String FOCUS_BUFF = "com.qsr.customspd.actors.buffs.MonkEnergy$MonkAbility$Focus$FocusBuff";

	private HeadlessLevel level;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero monk() {
		level = HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		hero.subClass = HeroSubClass.MONK;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, START);
		// unarmed damage rolls 1-(STR-8), so 9 strength always rolls exactly 1
		hero.STR = 9;
		Buff.affect(hero, MonkEnergy.class).energy = 5;
		return hero;
	}

	/** Monastic Vigor +1 empowers abilities at full energy. */
	private static MonkEnergy empower(Hero hero) {
		hero.talents.get(2).put(Talent.MONASTIC_VIGOR, 1);
		MonkEnergy energy = hero.buff(MonkEnergy.class);
		energy.energy = energy.energyCap();
		assertTrue(energy.abilitiesEmpowered(hero), "test setup: abilities are empowered");
		return energy;
	}

	private Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		level.mobs.add(rat);
		level.heroFOV[cell] = true;
		return rat;
	}

	/** Lets the hero wait out the given number of turns, everything else acting as it would. */
	private static void waitTurns(int turns) {
		float until = Actor.now() + turns;
		new TurnPump(h -> h.spendConstant(Actor.TICK)).runUntil(() -> Actor.now() >= until, 50 * turns);
	}

	@Test
	void usingAnAbilityStartsNoCooldown() {
		Hero hero = monk();
		MonkEnergy energy = hero.buff(MonkEnergy.class);
		energy.energy = 10;

		energy.abilityUsed(new MonkEnergy.MonkAbility.Dash());

		assertEquals(7f, energy.energy, 0.0001f, "test setup: the dash was paid for");
		assertEquals(0, energy.cooldown, "no shared cooldown after an ability");
		assertTrue(new MonkEnergy.MonkAbility.DragonKick().usable(energy), "another ability can follow at once");
	}

	@Test
	void flurryCannotBeUsedAgainUntilTheNextTurn() {
		Hero hero = monk();
		MonkEnergy energy = hero.buff(MonkEnergy.class);
		Rat rat = ratAt(START + 1);
		MonkEnergy.MonkAbility.Flurry flurry = new MonkEnergy.MonkAbility.Flurry();

		assertTrue(flurry.usable(energy));
		flurry.doAbility(hero, rat.pos);

		assertTrue(rat.HP < rat.HT, "test setup: the flurry hit");
		assertNotNull(hero.buff(MonkEnergy.MonkAbility.FlurryCooldownTracker.class));
		assertFalse(flurry.usable(energy), "a flurry cannot be chained into another");
		assertTrue(new MonkEnergy.MonkAbility.Dash().usable(energy), "the other abilities stay usable");

		waitTurns(1);

		assertNull(hero.buff(MonkEnergy.MonkAbility.FlurryCooldownTracker.class));
		assertTrue(flurry.usable(energy), "a turn later the flurry is back");
	}

	@Test
	void flurryHitsTwiceForOneAndAHalfTimesUnarmedDamage() {
		Hero hero = monk();
		Rat rat = ratAt(START + 1);

		new MonkEnergy.MonkAbility.Flurry().doAbility(hero, rat.pos);

		// 1 unarmed damage x1.5 rounds to 2, twice
		assertEquals(4, rat.HT - rat.HP);
	}

	@Test
	void flurryDescriptionShowsTheBoostedDamage() {
		Hero hero = monk();
		hero.STR = 12;

		String desc = new MonkEnergy.MonkAbility.Flurry().desc();

		assertTrue(desc.contains("deal 2-6 damage"), desc);
	}

	@Test
	void aFocusedMonkCannotFocusAgain() {
		Hero hero = monk();
		MonkEnergy energy = hero.buff(MonkEnergy.class);
		MonkEnergy.MonkAbility.Focus focus = new MonkEnergy.MonkAbility.Focus();

		assertTrue(focus.usable(energy));
		focus.doAbility(hero, null);

		assertNotNull(hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class));
		assertFalse(focus.usable(energy), "focus greys out while the monk is already focused");
		assertTrue(new MonkEnergy.MonkAbility.Dash().usable(energy));
	}

	@Test
	void focusLastsUntilItParries() {
		Hero hero = monk();

		new MonkEnergy.MonkAbility.Focus().doAbility(hero, null);
		waitTurns(40);

		assertNotNull(hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class),
				"focus no longer runs out after 30 turns");
	}

	@Test
	void focusParriesAMagicAttackToo() {
		Hero hero = monk();
		Rat rat = ratAt(START + 1);
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		assertFalse(Char.hit(rat, hero, true), "a magic attack on a focused monk misses");
		hero.defenseVerb();
		assertNull(hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class), "and the parry uses up the focus");
	}

	@Test
	void dashReachesFourTiles() {
		Hero hero = monk();

		new MonkEnergy.MonkAbility.Dash().doAbility(hero, START + 4);

		assertEquals(START + 4, hero.pos);
	}

	@Test
	void anEmpoweredDashReachesEightTilesButNoFurther() {
		Hero hero = monk();
		empower(hero);

		new MonkEnergy.MonkAbility.Dash().doAbility(hero, START + 9);
		assertEquals(START, hero.pos, "nine tiles is out of range");

		new MonkEnergy.MonkAbility.Dash().doAbility(hero, START + 8);
		assertEquals(START + 8, hero.pos);
	}

	@Test
	void dragonKickHitsForSixTimesUnarmedDamage() {
		Hero hero = monk();
		Rat rat = ratAt(START + 1);

		new MonkEnergy.MonkAbility.DragonKick().doAbility(hero, rat.pos);

		assertEquals(6, rat.HT - rat.HP);
	}

	@Test
	void anEmpoweredDragonKickHitsForNineTimesUnarmedDamage() {
		Hero hero = monk();
		empower(hero);
		Rat rat = ratAt(START + 1);

		new MonkEnergy.MonkAbility.DragonKick().doAbility(hero, rat.pos);

		assertEquals(9, rat.HT - rat.HP);
	}

	@Test
	void dragonKickDescriptionShowsTheBoostedDamage() {
		Hero hero = monk();
		hero.STR = 12;

		assertTrue(new MonkEnergy.MonkAbility.DragonKick().desc().contains("deals 6-24 damage"));
		empower(hero);
		assertTrue(new MonkEnergy.MonkAbility.DragonKick().desc().contains("deals _9-36 damage_"));
	}

	private static float energyFromOneRat(Hero hero) {
		MonkEnergy energy = hero.buff(MonkEnergy.class);
		energy.energy = 0;
		energy.gainEnergy(new Rat());
		return energy.energy;
	}

	@Test
	void unencumberedSpiritGivesFiftySeventyFiveAndOneHundredPercentMoreEnergy() {
		Hero hero = monk();

		hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, 1);
		hero.belongings.armor = new MailArmor();
		assertEquals(1.5f, energyFromOneRat(hero), 0.0001f, "+1 with tier 3 armor");

		hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, 2);
		hero.belongings.armor = new LeatherArmor();
		assertEquals(1.75f, energyFromOneRat(hero), 0.0001f, "+2 with tier 2 armor");

		hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, 3);
		hero.belongings.armor = new ClothArmor();
		hero.belongings.weapon = new Gloves();
		assertEquals(3f, energyFromOneRat(hero), 0.0001f, "+3 with tier 1 armor and weapon");
	}

	private static Bundlable restoreOldBuff(String className, float time) throws Exception {
		Bundle buff = new Bundle();
		buff.put("__className", className);
		buff.put("time", time);
		Bundle save = new Bundle();
		save.put("buff", buff);
		return SaveRoundtrip.writeRead(save).get("buff");
	}

	@Test
	void anOldSavesJustHitTrackerLoadsAsAPlainExpiringBuff() throws Exception {
		Bundlable restored = restoreOldBuff(JUST_HIT, 2f);

		assertNotNull(restored, "the removed tracker must still load");
		assertSame(FlavourBuff.class, restored.getClass());
		assertEquals(BuffIndicator.NONE, ((Buff) restored).icon());
	}

	@Test
	void anOldSavesFocusActivationLoadsAsAPlainExpiringBuff() throws Exception {
		Bundlable restored = restoreOldBuff(FOCUS_ACTIVATION, 0f);

		assertNotNull(restored, "the removed tracker must still load");
		assertSame(FlavourBuff.class, restored.getClass());
		assertEquals(BuffIndicator.NONE, ((Buff) restored).icon());
	}

	@Test
	void anOldSavesTimedFocusLoadsAsFocusThatLastsUntilItParries() throws Exception {
		Hero hero = monk();
		Bundlable restored = restoreOldBuff(FOCUS_BUFF, 12f);

		assertSame(MonkEnergy.MonkAbility.Focus.FocusBuff.class, restored.getClass());
		((Buff) restored).attachTo(hero);
		waitTurns(40);

		assertNotNull(hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class),
				"the old 30-turn timer no longer ends the focus");
	}
}
