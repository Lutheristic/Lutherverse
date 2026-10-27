package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.ArcaneResin;
import com.qsr.customspd.items.wands.WandOfFrost;
import com.qsr.customspd.items.wands.WandOfLightning;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wand preservation as upstream b7051a219 left it: the old wand always comes back at +0, there is
 * no arcane resin on a miss, and it works once at +1 and once per hero level at +2. It used to be
 * a 67%/100% roll, up to five times, with resin whenever the roll failed.
 */
class WandPreservationPortTest {

	private static final int SEEDS = 30;

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

	private static Hero mage(int points) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		hero.talents.get(1).put(Talent.WAND_PRESERVATION, points);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	private static MagesStaff staffWithMagicMissile() {
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.level(2);
		return new MagesStaff(wand);
	}

	@Test
	void atOnePointTheFirstImbueAlwaysReturnsTheOldWand() {
		for (int seed = 1; seed <= SEEDS; seed++) {
			Random.pushGenerator(seed);
			try {
				Hero hero = mage(1);
				staffWithMagicMissile().imbueWand(new WandOfFrost(), hero);

				WandOfMagicMissile returned = hero.belongings.getItem(WandOfMagicMissile.class);
				assertNotNull(returned, "seed " + seed + ": the old wand comes back every time");
				assertEquals(0, returned.trueLevel(), "returned at +0");
				assertNull(hero.belongings.getItem(ArcaneResin.class), "seed " + seed + ": no resin");
			} finally {
				Random.popGenerator();
				tearDown();
			}
		}
	}

	@Test
	void atOnePointTheSecondImbueLosesTheWandAndGivesNoResin() {
		for (int seed = 1; seed <= SEEDS; seed++) {
			Random.pushGenerator(seed);
			try {
				Hero hero = mage(1);
				MagesStaff staff = staffWithMagicMissile();
				staff.imbueWand(new WandOfFrost(), hero);
				staff.imbueWand(new WandOfLightning(), hero);

				assertNull(hero.belongings.getItem(WandOfFrost.class), "seed " + seed + ": only one preservation at +1");
				assertNull(hero.belongings.getItem(ArcaneResin.class), "seed " + seed + ": no resin when a wand is lost");
			} finally {
				Random.popGenerator();
				tearDown();
			}
		}
	}

	@Test
	void atTwoPointsItWorksOncePerHeroLevel() {
		Hero hero = mage(2);
		MagesStaff staff = staffWithMagicMissile();

		staff.imbueWand(new WandOfFrost(), hero);
		assertNotNull(hero.belongings.getItem(WandOfMagicMissile.class), "first imbue at this level is preserved");

		WandOfLightning lightning = new WandOfLightning();
		staff.imbueWand(lightning, hero);
		assertNull(hero.belongings.getItem(WandOfFrost.class), "a second imbue at the same level loses the wand");
		assertNull(hero.belongings.getItem(ArcaneResin.class));

		hero.earnExp(hero.maxExp(), null);
		assertEquals(2, hero.lvl, "fixture: one level gained");
		assertNull(hero.buff(Talent.WandPreservationCounter.class), "a level up gives the use back at +2");

		staff.imbueWand(new WandOfFrost(), hero);
		assertTrue(hero.belongings.backpack.items.contains(lightning), "preserved again after the level up");
	}

	@Test
	void atOnePointALevelUpDoesNotGiveTheUseBack() {
		Hero hero = mage(1);
		MagesStaff staff = staffWithMagicMissile();
		staff.imbueWand(new WandOfFrost(), hero);

		hero.earnExp(hero.maxExp(), null);
		assertNotNull(hero.buff(Talent.WandPreservationCounter.class));

		staff.imbueWand(new WandOfLightning(), hero);
		assertNull(hero.belongings.getItem(WandOfFrost.class));
	}

	@Test
	void anOldSavesUsedCounterStillLoadsAndCountsAsSpent() throws Exception {
		// a counter saved before this change counted preservations, up to five
		Talent.WandPreservationCounter saved = new Talent.WandPreservationCounter();
		saved.countUp(2);
		Talent.WandPreservationCounter restored = SaveRoundtrip.of(saved);
		assertEquals(2f, restored.count(), 0.0001f, "the counter round-trips under its own name");

		for (int seed = 1; seed <= SEEDS; seed++) {
			Random.pushGenerator(seed);
			try {
				Hero hero = mage(1);
				SaveRoundtrip.of(saved).attachTo(hero);
				staffWithMagicMissile().imbueWand(new WandOfFrost(), hero);

				assertNull(hero.belongings.getItem(WandOfMagicMissile.class), "seed " + seed + ": the one use is spent");
				assertNull(hero.belongings.getItem(ArcaneResin.class), "seed " + seed + ": and no resin");
			} finally {
				Random.popGenerator();
				tearDown();
			}
		}

		Hero hero = mage(2);
		SaveRoundtrip.of(saved).attachTo(hero);
		hero.earnExp(hero.maxExp(), null);
		staffWithMagicMissile().imbueWand(new WandOfFrost(), hero);
		assertNotNull(hero.belongings.getItem(WandOfMagicMissile.class), "at +2 the next level up frees it again");
	}

	@Test
	void theImbueWindowPromisesTheWandBackOnlyWhileAUseIsLeft() {
		String returned = Messages.get(MagesStaff.class, "imbue_talent");
		String lost = Messages.get(MagesStaff.class, "imbue_lost");

		mage(0);
		assertEquals(lost, MagesStaff.oldWandText(), "without the talent");
		tearDown();

		Hero hero = mage(1);
		assertEquals(returned, MagesStaff.oldWandText(), "+1, use not spent");
		staffWithMagicMissile().imbueWand(new WandOfFrost(), hero);
		assertNotNull(hero.buff(Talent.WandPreservationCounter.class), "fixture: the use is spent");
		assertEquals(lost, MagesStaff.oldWandText(), "+1, use spent");
		tearDown();

		hero = mage(2);
		staffWithMagicMissile().imbueWand(new WandOfFrost(), hero);
		assertEquals(lost, MagesStaff.oldWandText(), "+2, use spent at this level");
		hero.earnExp(hero.maxExp(), null);
		assertEquals(returned, MagesStaff.oldWandText(), "+2, after a level up");
	}

	@Test
	void theTalentAndImbueTextMatch() {
		String desc = Talent.WAND_PRESERVATION.desc();
		assertTrue(desc.contains("the old wand is returned at +0, _but this can only be done once._"), desc);
		assertTrue(desc.contains("_this can be done once per hero level._"), desc);
		assertFalse(desc.contains("resin"), desc);

		String talent = Messages.get(MagesStaff.class, "imbue_talent");
		assertEquals("The wand currently imbued in your staff _will be returned at +0_.", talent);
		assertEquals("The wand currently imbued in your staff _will be lost_.", Messages.get(MagesStaff.class, "imbue_lost"));
	}
}
