package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Bleeding;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Degrade;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.quest.Pickaxe;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins upstream 998cc211c, dafbb6f85 and 729a86295 (v2.4.0): damage-dealing weapon abilities
 * state their damage range, and the boost is a flat amount that grows with the weapon's level
 * instead of a damage multiplier. The expected numbers are written out by hand so that a wrong
 * boost formula cannot agree with itself.
 */
class DuelistAbilityDamagePortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A rat with no armor and plenty of health, so the damage it takes is the attack's damage. */
	public static class SturdyRat extends Rat {
		{
			HP = HT = 1000;
		}

		@Override
		public int drRoll() {
			return 0;
		}
	}

	public static class FixedRollMace extends Mace {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollGreataxe extends Greataxe {
		@Override
		public int damageRoll(Char owner) {
			return 10;
		}
	}

	public static class FixedRollSpear extends Spear {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollRapier extends Rapier {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollKatana extends Katana {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollSword extends Sword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollSai extends Sai {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollSickle extends Sickle {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollWarScythe extends WarScythe {
		@Override
		public int damageRoll(Char owner) {
			return 10;
		}
	}

	public static class FixedRollPickaxe extends Pickaxe {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollHandAxe extends HandAxe {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollBattleAxe extends BattleAxe {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollWarHammer extends WarHammer {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollGlaive extends Glaive {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollShortsword extends Shortsword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollWornShortsword extends WornShortsword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollLongsword extends Longsword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollGreatsword extends Greatsword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollGloves extends Gloves {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollGauntlet extends Gauntlet {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	/** Stands in for the rigid-skinned enemies the pickaxe pierces. */
	public static class RigidRat extends SturdyRat {
		{
			properties.add(Property.INORGANIC);
		}
	}

	/** Its own damage roll stays in place, since the spin boost is added there; min and max are pinned instead. */
	public static class FixedRangeFlail extends Flail {
		@Override
		public int min(int lvl) {
			return 10;
		}

		@Override
		public int max(int lvl) {
			return 10;
		}
	}

	private static Hero heroOnFixture() {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.STR = 20; // meets every requirement, so surprise attacks are allowed
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		return hero;
	}

	private static <T extends MeleeWeapon> T identifiedAt(T weapon, int level) {
		weapon.level(level);
		weapon.identify();
		return weapon;
	}

	private static SturdyRat ratAt(int cell) {
		SturdyRat rat = HeadlessLevel.at(new SturdyRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;
		return rat;
	}

	// The weapon is secretly made +3 first: unidentified, it must still show the +0 numbers. That
	// also tells the typical text from the known one where both say "typically" (greataxe).
	private static void assertTypicalRange(MeleeWeapon unidentified, int min, int max) {
		unidentified.level(3);
		String info = unidentified.abilityInfo();
		assertTrue(info.contains("typically"), info);
		assertTrue(info.contains("_" + min + "-" + max), info);
	}

	// not checked for "typically": upstream's greataxe ability_desc says it too
	private static void assertKnownRange(MeleeWeapon identified, int min, int max) {
		String info = identified.abilityInfo();
		assertTrue(info.contains("_" + min + "-" + max), info);
	}

	@Test
	void heavyBlowWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		// +0 unidentified: base range plus the level 0 boost
		assertTypicalRange(new Mace(), 3 + 4, 16 + 4);
		assertTypicalRange(new HandAxe(), 2 + 3, 12 + 3);
		assertTypicalRange(new BattleAxe(), 4 + 4, 20 + 4);
		assertTypicalRange(new WarHammer(), 5 + 4, 24 + 4);
		assertTypicalRange(new Greataxe(), 5 + 13, 45 + 13);

		// +3 identified: upgraded range plus the boost at +3
		assertKnownRange(identifiedAt(new Mace(), 3), 6 + 7, 28 + 7);
		assertKnownRange(identifiedAt(new HandAxe(), 3), 5 + 6, 21 + 6);
		assertKnownRange(identifiedAt(new BattleAxe(), 3), 7 + 7, 35 + 7);
		assertKnownRange(identifiedAt(new WarHammer(), 3), 8 + 7, 42 + 7);
		assertKnownRange(identifiedAt(new Greataxe(), 3), 8 + 18, 63 + 18);
	}

	@Test
	void heavyBlowOnASurprisedRatAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollMace mace = identifiedAt(new FixedRollMace(), 3);
		hero.belongings.weapon = mace;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);
		assertTrue(rat.surprisedBy(hero), "fixture: the rat has not noticed the hero");

		mace.duelistAbility(hero, cell);

		// a roll of 5 plus 4+3 at +3; the old 1.4x multiplier made it 7
		assertEquals(12, rat.HT - rat.HP);
	}

	@Test
	void retributionAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		hero.HP = hero.HT / 4; // retribution needs the hero below half health
		FixedRollGreataxe axe = new FixedRollGreataxe();
		hero.belongings.weapon = axe;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		axe.duelistAbility(hero, cell);

		// a roll of 10 plus 13 at +0; the old 1.5x multiplier made it 15
		assertEquals(23, rat.HT - rat.HP);
	}

	@Test
	void spikeAndLungeWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		assertTypicalRange(new Spear(), 2 + 5, 20 + 5);
		assertTypicalRange(new Glaive(), 5 + 7, 40 + 7);
		assertTypicalRange(new Rapier(), 1 + 3, 8 + 3);
		assertTypicalRange(new Katana(), 4 + 4, 20 + 4);

		assertKnownRange(identifiedAt(new Spear(), 3), 5 + 8, 32 + 8);
		assertKnownRange(identifiedAt(new Glaive(), 3), 8 + 11, 64 + 11);
		assertKnownRange(identifiedAt(new Rapier(), 3), 4 + 6, 14 + 6);
		assertKnownRange(identifiedAt(new Katana(), 3), 7 + 7, 35 + 7);
	}

	@Test
	void spikeAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSpear spear = new FixedRollSpear();
		hero.belongings.weapon = spear;
		int cell = 4 * W + 6; // in reach, not adjacent
		SturdyRat rat = ratAt(cell);

		spear.duelistAbility(hero, cell);

		// a roll of 5 plus 5 at +0; the old 1.45x multiplier made it 7
		assertEquals(10, rat.HT - rat.HP);
	}

	@Test
	void katanaLungeAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollKatana katana = new FixedRollKatana();
		hero.belongings.weapon = katana;
		int cell = 4 * W + 6; // one tile between them
		SturdyRat rat = ratAt(cell);

		katana.duelistAbility(hero, cell);

		assertEquals(4 * W + 5, hero.pos, "fixture: the hero lunged next to the rat");
		// a roll of 5 plus 4 at +0; the old 1.35x multiplier made it 7
		assertEquals(9, rat.HT - rat.HP);
	}

	@Test
	void rapierLungeBoostUsesTheBuffedLevel() {
		Hero hero = heroOnFixture();
		FixedRollRapier rapier = identifiedAt(new FixedRollRapier(), 4);
		hero.belongings.weapon = rapier;
		Buff.affect(hero, Degrade.class, 30f);
		assertEquals(3, rapier.buffedLvl(), "fixture: degrade lowers a +4 rapier to +3");
		int cell = 4 * W + 6;
		SturdyRat rat = ratAt(cell);

		rapier.duelistAbility(hero, cell);

		// a roll of 5 plus 3+3; the boost read the unbuffed level and made it 12
		assertEquals(11, rat.HT - rat.HP);
	}

	@Test
	void cleaveWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		assertTypicalRange(new Sword(), 3 + 3, 20 + 3);
		assertTypicalRange(new Shortsword(), 2 + 3, 15 + 3);
		assertTypicalRange(new WornShortsword(), 1 + 2, 10 + 2);
		assertTypicalRange(new Longsword(), 4 + 4, 25 + 4);
		assertTypicalRange(new Greatsword(), 5 + 4, 30 + 4);

		assertKnownRange(identifiedAt(new Sword(), 3), 6 + 5, 32 + 5);
		assertKnownRange(identifiedAt(new Shortsword(), 3), 5 + 4, 24 + 4);
		assertKnownRange(identifiedAt(new WornShortsword(), 3), 4 + 4, 16 + 4);
		assertKnownRange(identifiedAt(new Longsword(), 3), 7 + 6, 40 + 6);
		assertKnownRange(identifiedAt(new Greatsword(), 3), 8 + 6, 48 + 6);
	}

	@Test
	void flailDescribesItsBoostPerSpin() {
		heroOnFixture();
		String typical = new Flail().abilityInfo();
		assertTrue(typical.contains("typically deal _+7 damage_"), typical);
		String known = identifiedAt(new Flail(), 3).abilityInfo();
		assertTrue(known.contains("deal _+11 damage_"), known);
	}

	@Test
	void cleaveAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSword sword = new FixedRollSword();
		hero.belongings.weapon = sword;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		sword.duelistAbility(hero, cell);

		// a roll of 5 plus 3 at +0; the old 1.27x multiplier made it 6
		assertEquals(8, rat.HT - rat.HP);
	}

	@Test
	void flailSpinsAddAFlatBoostToTheNextAttack() {
		Hero hero = heroOnFixture();
		FixedRangeFlail flail = new FixedRangeFlail();
		hero.belongings.weapon = flail;
		hero.STR = flail.STRReq(); // no bonus damage from excess strength
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		flail.duelistAbility(hero, null);
		flail.duelistAbility(hero, null);
		assertEquals(2, hero.buff(Flail.SpinAbilityTracker.class).spins, "fixture: two spins");
		hero.attack(rat);

		// 10 plus 7 per spin at +0; the old +33% per spin made it 17
		assertEquals(24, rat.HT - rat.HP);
	}

	@Test
	void comboStrikeWeaponsDescribeTheirBoostPerHit() {
		heroOnFixture();
		assertTrue(new Sai().abilityInfo().contains("typically deals _+3 damage_"), new Sai().abilityInfo());
		assertTrue(new Gloves().abilityInfo().contains("typically deals _+2 damage_"), new Gloves().abilityInfo());
		assertTrue(new Gauntlet().abilityInfo().contains("typically deals _+4 damage_"), new Gauntlet().abilityInfo());

		String sai = identifiedAt(new Sai(), 3).abilityInfo();
		assertTrue(sai.contains("deals _+4 damage_"), sai);
		String gloves = identifiedAt(new Gloves(), 3).abilityInfo();
		assertTrue(gloves.contains("deals _+3 damage_"), gloves);
		String gauntlet = identifiedAt(new Gauntlet(), 3).abilityInfo();
		assertTrue(gauntlet.contains("deals _+6 damage_"), gauntlet);
	}

	@Test
	void harvestWeaponsDescribeTheirBleedRange() {
		heroOnFixture();
		assertTypicalRange(new Sickle(), 2 + 1, 20 + 1);
		assertTypicalRange(new WarScythe(), 5, 40);

		assertKnownRange(identifiedAt(new Sickle(), 3), 5 + 2, 29 + 2);
		assertKnownRange(identifiedAt(new WarScythe(), 3), 8, 58);
	}

	@Test
	void pickaxeDescribesItsDamageAgainstRigidEnemies() {
		heroOnFixture();
		assertKnownRange(new Pickaxe(), 2 + 8, 15 + 8);
		assertKnownRange(identifiedAt(new Pickaxe(), 3), 5 + 14, 24 + 14);
	}

	@Test
	void comboStrikeAddsAFlatBoostPerRecentHit() {
		Hero hero = heroOnFixture();
		FixedRollSai sai = new FixedRollSai();
		hero.belongings.weapon = sai;
		Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
		combo.addHit();
		combo.addHit();
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		sai.duelistAbility(hero, cell);

		// a roll of 5 plus 3 for each of two hits at +0; the old +40% per hit made it 9
		assertEquals(11, rat.HT - rat.HP);
	}

	@Test
	void sickleHarvestBleedTakesAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSickle sickle = identifiedAt(new FixedRollSickle(), 3);
		hero.belongings.weapon = sickle;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		sickle.duelistAbility(hero, cell);

		assertEquals(rat.HT, rat.HP, "fixture: the harvest deals its damage as bleeding");
		// a roll of 5 plus 2 at +3; the old 1.1x multiplier made it 6
		assertEquals(7f, rat.buff(Bleeding.class).level(), 0.0001f);
	}

	@Test
	void warScytheHarvestBleedsItsNormalDamage() {
		Hero hero = heroOnFixture();
		FixedRollWarScythe scythe = new FixedRollWarScythe();
		hero.belongings.weapon = scythe;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		scythe.duelistAbility(hero, cell);

		// the old shared 1.1x multiplier made it 11
		assertEquals(10f, rat.buff(Bleeding.class).level(), 0.0001f);
	}

	@Test
	void pickaxePierceAddsAFlatBoostAgainstRigidEnemies() {
		Hero hero = heroOnFixture();
		FixedRollPickaxe pickaxe = new FixedRollPickaxe();
		hero.belongings.weapon = pickaxe;
		int cell = 4 * W + 5;
		SturdyRat rat = HeadlessLevel.at(new RigidRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;

		((MeleeWeapon) pickaxe).duelistAbility(hero, cell);

		// a roll of 5 plus 8 at +0; the old 2x multiplier made it 10
		assertEquals(13, rat.HT - rat.HP);
	}

	/** Uses the ability of a fresh +3 weapon on a sturdy rat at the cell and returns the damage dealt. */
	private static int damageAtPlusThree(Supplier<MeleeWeapon> make, int cell, Consumer<Hero> setup) {
		Hero hero = heroOnFixture();
		MeleeWeapon weapon = identifiedAt(make.get(), 3);
		hero.belongings.weapon = weapon;
		setup.accept(hero);
		SturdyRat rat = ratAt(cell);
		weapon.duelistAbility(hero, cell);
		return rat.HT - rat.HP;
	}

	private static void expect(List<String> wrong, String name, int expected, int actual) {
		if (expected != actual) wrong.add(name + ": expected " + expected + ", got " + actual);
	}

	@Test
	void everyDamageAbilityAddsItsBoostAtPlusThree() {
		int adjacent = 4 * W + 5;
		int twoAway = 4 * W + 6;
		Consumer<Hero> none = hero -> { };
		Consumer<Hero> belowHalfHealth = hero -> hero.HP = hero.HT / 4;
		Consumer<Hero> twoRecentHits = hero -> {
			Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
			combo.addHit();
			combo.addHit();
		};
		List<String> wrong = new ArrayList<>();

		// heavy blow on a rat that has not noticed the hero: a roll of 5 plus the boost at +3
		expect(wrong, "hand axe", 5 + 6, damageAtPlusThree(FixedRollHandAxe::new, adjacent, none));
		expect(wrong, "battle axe", 5 + 7, damageAtPlusThree(FixedRollBattleAxe::new, adjacent, none));
		expect(wrong, "war hammer", 5 + 7, damageAtPlusThree(FixedRollWarHammer::new, adjacent, none));
		// retribution: a roll of 10 plus 12.5+1.75*3, rounded
		expect(wrong, "greataxe", 10 + 18, damageAtPlusThree(FixedRollGreataxe::new, adjacent, belowHalfHealth));

		// spike and lunge reach a rat two tiles away
		expect(wrong, "spear", 5 + 8, damageAtPlusThree(FixedRollSpear::new, twoAway, none));
		expect(wrong, "glaive", 5 + 11, damageAtPlusThree(FixedRollGlaive::new, twoAway, none));
		expect(wrong, "katana", 5 + 7, damageAtPlusThree(FixedRollKatana::new, twoAway, none));

		// cleave
		expect(wrong, "sword", 5 + 5, damageAtPlusThree(FixedRollSword::new, adjacent, none));
		expect(wrong, "shortsword", 5 + 4, damageAtPlusThree(FixedRollShortsword::new, adjacent, none));
		expect(wrong, "worn shortsword", 5 + 4, damageAtPlusThree(FixedRollWornShortsword::new, adjacent, none));
		expect(wrong, "longsword", 5 + 6, damageAtPlusThree(FixedRollLongsword::new, adjacent, none));
		expect(wrong, "greatsword", 5 + 6, damageAtPlusThree(FixedRollGreatsword::new, adjacent, none));

		// combo strike after two recent hits: the boost counts once per hit
		expect(wrong, "sai", 5 + 2 * 4, damageAtPlusThree(FixedRollSai::new, adjacent, twoRecentHits));
		expect(wrong, "studded gloves", 5 + 2 * 3, damageAtPlusThree(FixedRollGloves::new, adjacent, twoRecentHits));
		expect(wrong, "stone gauntlet", 5 + 2 * 6, damageAtPlusThree(FixedRollGauntlet::new, adjacent, twoRecentHits));

		assertTrue(wrong.isEmpty(), String.join("\n", wrong));
	}

	@Test
	void heavyBlowBoostFollowsTheAugment() {
		heroOnFixture();
		HandAxe info = identifiedAt(new HandAxe(), 3);
		info.augment = Weapon.Augment.DAMAGE;
		// 5-21 at +3, plus 6, times 1.5 rounded
		assertKnownRange(info, 17, 41);

		Consumer<Hero> none = hero -> { };
		int dealt = damageAtPlusThree(() -> {
			FixedRollHandAxe axe = new FixedRollHandAxe();
			axe.augment = Weapon.Augment.DAMAGE;
			return axe;
		}, 4 * W + 5, none);
		// a roll of 5 plus 6*1.5 rounded; without the augment the boost would be 6
		assertEquals(5 + 9, dealt);
	}

	@Test
	void flailSpinBoostScalesWithLevelAndAugment() {
		int[] dealt = new int[2];
		Weapon.Augment[] augments = {Weapon.Augment.NONE, Weapon.Augment.DAMAGE};
		for (int i = 0; i < 2; i++) {
			Hero hero = heroOnFixture();
			FixedRangeFlail flail = identifiedAt(new FixedRangeFlail(), 3);
			flail.augment = augments[i];
			hero.belongings.weapon = flail;
			hero.STR = flail.STRReq(); // no bonus damage from excess strength
			SturdyRat rat = ratAt(4 * W + 5);

			flail.duelistAbility(hero, null);
			flail.duelistAbility(hero, null);
			hero.attack(rat);
			dealt[i] = rat.HT - rat.HP;
		}

		// 10 plus 11 per spin at +3
		assertEquals(10 + 2 * 11, dealt[0], "no augment");
		// the roll and the boost both take the 1.5x damage augment: 15 plus 17 per spin
		assertEquals(15 + 2 * 17, dealt[1], "damage augment");
	}

	@Test
	void pickaxePierceBoostScalesWithLevel() {
		Hero hero = heroOnFixture();
		FixedRollPickaxe pickaxe = identifiedAt(new FixedRollPickaxe(), 3);
		hero.belongings.weapon = pickaxe;
		int cell = 4 * W + 5;
		SturdyRat rat = HeadlessLevel.at(new RigidRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;

		((MeleeWeapon) pickaxe).duelistAbility(hero, cell);

		// a roll of 5 plus 8+2*3
		assertEquals(19, rat.HT - rat.HP);
	}

	@Test
	void pickaxePierceAddsNothingAgainstOtherEnemies() {
		Hero hero = heroOnFixture();
		FixedRollPickaxe pickaxe = new FixedRollPickaxe();
		hero.belongings.weapon = pickaxe;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		((MeleeWeapon) pickaxe).duelistAbility(hero, cell);

		assertEquals(5, rat.HT - rat.HP);
	}
}
