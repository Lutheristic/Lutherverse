package com.qsr.customspd.actors.hero;

import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 33b065e69 (v2.2.0) renamed the five T2 potion/scroll talents. Saves store talents by
 * enum name, so a save written before the rename names RESTORED_WILLPOWER and the rest; the
 * renamedTalents table must carry those points, and any metamorphosis involving them, over to
 * the new names.
 */
class RenamedTalentRestorePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	/** A hero's talents stored as a pre-rename save holds them: tier 2 points under the old name. */
	private static Hero restoreOldTier2(HeroClass cls, String oldName, int points) throws Exception {
		Hero original = new Hero();
		original.heroClass = cls;
		original.subClass = HeroSubClass.NONE;
		Talent.initClassTalents(original);
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);
		Bundle tier2 = new Bundle();
		tier2.put(oldName, points);
		saved.put("talents_tier_2", tier2);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = cls;
		fresh.subClass = HeroSubClass.NONE;
		Talent.restoreTalentsFromBundle(loaded, fresh);
		return fresh;
	}

	@Test
	void restoredWillpowerPointsBecomeLiquidWillpower() throws Exception {
		assertEquals(2, restoreOldTier2(HeroClass.WARRIOR, "RESTORED_WILLPOWER", 2).pointsInTalent(Talent.LIQUID_WILLPOWER));
	}

	@Test
	void energizingUpgradePointsBecomeInscribedPower() throws Exception {
		assertEquals(2, restoreOldTier2(HeroClass.MAGE, "ENERGIZING_UPGRADE", 2).pointsInTalent(Talent.INSCRIBED_POWER));
	}

	@Test
	void mysticalUpgradePointsBecomeInscribedStealth() throws Exception {
		assertEquals(1, restoreOldTier2(HeroClass.ROGUE, "MYSTICAL_UPGRADE", 1).pointsInTalent(Talent.INSCRIBED_STEALTH));
	}

	@Test
	void restoredNaturePointsBecomeLiquidNature() throws Exception {
		assertEquals(2, restoreOldTier2(HeroClass.HUNTRESS, "RESTORED_NATURE", 2).pointsInTalent(Talent.LIQUID_NATURE));
	}

	@Test
	void restoredAgilityPointsBecomeLiquidAgility() throws Exception {
		assertEquals(1, restoreOldTier2(HeroClass.DUELIST, "RESTORED_AGILITY", 1).pointsInTalent(Talent.LIQUID_AGILITY));
	}

	@Test
	void aMetamorphosisNamingOldTalentsIsKept() throws Exception {
		Hero original = new Hero();
		original.heroClass = HeroClass.WARRIOR;
		original.subClass = HeroSubClass.NONE;
		Talent.initClassTalents(original);
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);
		Bundle replacements = new Bundle();
		replacements.put("RESTORED_WILLPOWER", "RESTORED_AGILITY");
		saved.put("replacements", replacements);
		Bundle tier2 = new Bundle();
		tier2.put("RESTORED_AGILITY", 2);
		saved.put("talents_tier_2", tier2);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.WARRIOR;
		fresh.subClass = HeroSubClass.NONE;
		Talent.restoreTalentsFromBundle(loaded, fresh);

		assertEquals(Talent.LIQUID_AGILITY, fresh.metamorphedTalents.get(Talent.LIQUID_WILLPOWER));
		assertEquals(2, fresh.pointsInTalent(Talent.LIQUID_AGILITY), "the metamorphosed slot keeps its points");
	}
}
