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
import com.watabou.utils.Random;
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
 * instead of a damage multiplier. The numbers are 11a410f07's (v2.4.0) retune, which raised the
 * boosts and turned harvest into pure bleeding, with 753b340f0's (v2.4.2) spin and harvest buffs
 * and ea3d9db28's (v2.5.0) lunge, cleave, spike and retribution buffs and lash boost removal. The
 * expected numbers are written out by hand so that a wrong boost formula cannot agree with
 * itself.
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

	public static class FixedRollWhip extends Whip {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	/** Dodges anything short of a guaranteed hit. */
	public static class ElusiveRat extends SturdyRat {
		@Override
		public int defenseSkill(Char enemy) {
			return 100_000;
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
		assertTypicalRange(new Mace(), 3 + 5, 16 + 5);
		assertTypicalRange(new HandAxe(), 2 + 4, 12 + 4);
		assertTypicalRange(new BattleAxe(), 4 + 5, 20 + 5);
		assertTypicalRange(new WarHammer(), 5 + 6, 24 + 6);
		assertTypicalRange(new Greataxe(), 5 + 15, 45 + 15);

		// +3 identified: upgraded range plus the boost at +3 (1.5*3 rounds up to 5)
		assertKnownRange(identifiedAt(new Mace(), 3), 6 + 10, 28 + 10);
		assertKnownRange(identifiedAt(new HandAxe(), 3), 5 + 9, 21 + 9);
		assertKnownRange(identifiedAt(new BattleAxe(), 3), 7 + 10, 35 + 10);
		assertKnownRange(identifiedAt(new WarHammer(), 3), 8 + 11, 42 + 11);
		assertKnownRange(identifiedAt(new Greataxe(), 3), 8 + 21, 63 + 21);
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

		// a roll of 5 plus 5+round(1.5*3) at +3; the earlier 4+lvl made it 12
		assertEquals(15, rat.HT - rat.HP);
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

		// a roll of 10 plus 15 at +0; 11a410f07's 12+2*lvl made it 22
		assertEquals(25, rat.HT - rat.HP);
	}

	@Test
	void spikeAndLungeWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		assertTypicalRange(new Spear(), 2 + 9, 20 + 9);
		assertTypicalRange(new Glaive(), 5 + 12, 40 + 12);
		assertTypicalRange(new Rapier(), 1 + 5, 8 + 5);
		assertTypicalRange(new Katana(), 4 + 8, 20 + 8);

		// 2*3, round(2.5*3), round(1.5*3) and 2*3 on top of the base boosts
		assertKnownRange(identifiedAt(new Spear(), 3), 5 + 15, 32 + 15);
		assertKnownRange(identifiedAt(new Glaive(), 3), 8 + 20, 64 + 20);
		assertKnownRange(identifiedAt(new Rapier(), 3), 4 + 10, 14 + 10);
		assertKnownRange(identifiedAt(new Katana(), 3), 7 + 14, 35 + 14);
	}

	@Test
	void spikeAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSpear spear = new FixedRollSpear();
		hero.belongings.weapon = spear;
		int cell = 4 * W + 6; // in reach, not adjacent
		SturdyRat rat = ratAt(cell);

		spear.duelistAbility(hero, cell);

		// a roll of 5 plus 9 at +0; 11a410f07's 7+1.5*lvl made it 12
		assertEquals(14, rat.HT - rat.HP);
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
		// a roll of 5 plus 8 at +0; 11a410f07's 6+1.5*lvl made it 11
		assertEquals(13, rat.HT - rat.HP);
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

		// a roll of 5 plus 5+round(1.5*3); the unbuffed +4 would make it 16
		assertEquals(15, rat.HT - rat.HP);
	}

	@Test
	void cleaveWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		// ea3d9db28 left the worn shortsword at 3+lvl
		assertTypicalRange(new Sword(), 3 + 5, 20 + 5);
		assertTypicalRange(new Shortsword(), 2 + 4, 15 + 4);
		assertTypicalRange(new WornShortsword(), 1 + 3, 10 + 3);
		assertTypicalRange(new Longsword(), 4 + 6, 25 + 6);
		assertTypicalRange(new Greatsword(), 5 + 7, 30 + 7);

		assertKnownRange(identifiedAt(new Sword(), 3), 6 + 8, 32 + 8);
		assertKnownRange(identifiedAt(new Shortsword(), 3), 5 + 7, 24 + 7);
		assertKnownRange(identifiedAt(new WornShortsword(), 3), 4 + 6, 16 + 6);
		assertKnownRange(identifiedAt(new Longsword(), 3), 7 + 9, 40 + 9);
		assertKnownRange(identifiedAt(new Greatsword(), 3), 8 + 10, 48 + 10);
	}

	@Test
	void flailDescribesItsBoostPerSpin() {
		heroOnFixture();
		String typical = new Flail().abilityInfo();
		assertTrue(typical.contains("typically deal _+8 damage_"), typical);
		String known = identifiedAt(new Flail(), 3).abilityInfo();
		assertTrue(known.contains("deal _+14 damage_"), known);
	}

	@Test
	void cleaveAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSword sword = new FixedRollSword();
		hero.belongings.weapon = sword;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		sword.duelistAbility(hero, cell);

		// a roll of 5 plus 5 at +0; 11a410f07's 4+lvl made it 9
		assertEquals(10, rat.HT - rat.HP);
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

		// 10 plus 8 per spin at +0; 11a410f07's 6+2*lvl made it 22
		assertEquals(26, rat.HT - rat.HP);
	}

	@Test
	void comboStrikeWeaponsDescribeTheirBoostPerHit() {
		heroOnFixture();
		assertTrue(new Sai().abilityInfo().contains("typically deals _+3 damage_"), new Sai().abilityInfo());
		assertTrue(new Gloves().abilityInfo().contains("typically deals _+2 damage_"), new Gloves().abilityInfo());
		assertTrue(new Gauntlet().abilityInfo().contains("typically deals _+4 damage_"), new Gauntlet().abilityInfo());

		String sai = identifiedAt(new Sai(), 3).abilityInfo();
		assertTrue(sai.contains("deals _+5 damage_"), sai);
		String gloves = identifiedAt(new Gloves(), 3).abilityInfo();
		assertTrue(gloves.contains("deals _+4 damage_"), gloves);
		String gauntlet = identifiedAt(new Gauntlet(), 3).abilityInfo();
		assertTrue(gauntlet.contains("deals _+6 damage_"), gauntlet);
	}

	@Test
	void harvestWeaponsDescribeTheirBleedAmount() {
		heroOnFixture();
		Sickle sickle = new Sickle();
		sickle.level(3);
		assertTrue(sickle.abilityInfo().contains("typically deals _15 damage._"), sickle.abilityInfo());
		WarScythe scythe = new WarScythe();
		scythe.level(3);
		assertTrue(scythe.abilityInfo().contains("typically deals _30 damage._"), scythe.abilityInfo());

		// 15+2.5*3 and 30+4.5*3, rounded
		String sickleKnown = identifiedAt(new Sickle(), 3).abilityInfo();
		assertTrue(sickleKnown.contains("deals _23 damage._"), sickleKnown);
		String scytheKnown = identifiedAt(new WarScythe(), 3).abilityInfo();
		assertTrue(scytheKnown.contains("deals _44 damage._"), scytheKnown);
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
		// 15+2.5*3 rounded at +3 in place of the roll of 5; 11a410f07's 12+2*lvl made it 18
		assertEquals(23f, rat.buff(Bleeding.class).level(), 0.0001f);
	}

	@Test
	void warScytheHarvestBleedTakesAFlatAmount() {
		Hero hero = heroOnFixture();
		FixedRollWarScythe scythe = new FixedRollWarScythe();
		hero.belongings.weapon = scythe;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		scythe.duelistAbility(hero, cell);

		// 30 at +0 in place of the roll of 10; 11a410f07's 22.5+3.5*lvl rounded made it 23
		assertEquals(30f, rat.buff(Bleeding.class).level(), 0.0001f);
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
		expect(wrong, "hand axe", 5 + 9, damageAtPlusThree(FixedRollHandAxe::new, adjacent, none));
		expect(wrong, "battle axe", 5 + 10, damageAtPlusThree(FixedRollBattleAxe::new, adjacent, none));
		expect(wrong, "war hammer", 5 + 11, damageAtPlusThree(FixedRollWarHammer::new, adjacent, none));
		// retribution: a roll of 10 plus 15+2*3
		expect(wrong, "greataxe", 10 + 21, damageAtPlusThree(FixedRollGreataxe::new, adjacent, belowHalfHealth));

		// spike and lunge reach a rat two tiles away
		expect(wrong, "spear", 5 + 15, damageAtPlusThree(FixedRollSpear::new, twoAway, none));
		expect(wrong, "glaive", 5 + 20, damageAtPlusThree(FixedRollGlaive::new, twoAway, none));
		expect(wrong, "katana", 5 + 14, damageAtPlusThree(FixedRollKatana::new, twoAway, none));

		// cleave
		expect(wrong, "sword", 5 + 8, damageAtPlusThree(FixedRollSword::new, adjacent, none));
		expect(wrong, "shortsword", 5 + 7, damageAtPlusThree(FixedRollShortsword::new, adjacent, none));
		expect(wrong, "worn shortsword", 5 + 6, damageAtPlusThree(FixedRollWornShortsword::new, adjacent, none));
		expect(wrong, "longsword", 5 + 9, damageAtPlusThree(FixedRollLongsword::new, adjacent, none));
		expect(wrong, "greatsword", 5 + 10, damageAtPlusThree(FixedRollGreatsword::new, adjacent, none));

		// combo strike after two recent hits: the boost counts once per hit
		expect(wrong, "sai", 5 + 2 * 5, damageAtPlusThree(FixedRollSai::new, adjacent, twoRecentHits));
		expect(wrong, "studded gloves", 5 + 2 * 4, damageAtPlusThree(FixedRollGloves::new, adjacent, twoRecentHits));
		expect(wrong, "stone gauntlet", 5 + 2 * 6, damageAtPlusThree(FixedRollGauntlet::new, adjacent, twoRecentHits));

		assertTrue(wrong.isEmpty(), String.join("\n", wrong));
	}

	@Test
	void heavyBlowBoostFollowsTheAugment() {
		heroOnFixture();
		HandAxe info = identifiedAt(new HandAxe(), 3);
		info.augment = Weapon.Augment.DAMAGE;
		// 5-21 at +3, plus 9, times 1.5
		assertKnownRange(info, 21, 45);

		Consumer<Hero> none = hero -> { };
		int dealt = damageAtPlusThree(() -> {
			FixedRollHandAxe axe = new FixedRollHandAxe();
			axe.augment = Weapon.Augment.DAMAGE;
			return axe;
		}, 4 * W + 5, none);
		// a roll of 5 plus 9*1.5 rounded; without the augment the boost would be 9
		assertEquals(5 + 14, dealt);
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

		// 10 plus 14 per spin at +3
		assertEquals(10 + 2 * 14, dealt[0], "no augment");
		// the roll and the boost both take the 1.5x damage augment: 15 plus 21 per spin
		assertEquals(15 + 2 * 21, dealt[1], "damage augment");
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
	void lashHitsEveryEnemyInReachWithNoBoost() {
		Hero hero = heroOnFixture();
		FixedRollWhip whip = new FixedRollWhip();
		hero.belongings.weapon = whip;
		SturdyRat closest = ratAt(4 * W + 5);
		ElusiveRat far = HeadlessLevel.at(new ElusiveRat(), 7 * W + 4); // three tiles away, nothing between
		StubSprite.attach(far);
		Dungeon.level.heroFOV[far.pos] = true;

		Random.pushGenerator(4242L);
		try {
			whip.duelistAbility(hero, null);
		} finally {
			Random.popGenerator();
		}

		// just the roll of 5 on every target; 11a410f07's 2+0.5*lvl boost made it 7
		assertEquals(5, closest.HT - closest.HP, "the closest enemy");
		assertEquals(5, far.HT - far.HP, "11a410f07: the others are guaranteed hits too, not just the closest");
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
