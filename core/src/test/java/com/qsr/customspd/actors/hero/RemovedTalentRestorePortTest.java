package com.qsr.customspd.actors.hero;

import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A save that names a talent the game no longer has must restore, not crash: metamorphosis
 * replacements were resolved with an unguarded Talent.valueOf (upstream 327b19986).
 */
class RemovedTalentRestorePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private static Hero warrior() {
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		hero.subClass = HeroSubClass.NONE;
		Talent.initClassTalents(hero);
		return hero;
	}

	@Test
	void restoringASaveWithARemovedTalentIsSkippedNotFatal() throws Exception {
		Hero original = warrior();
		original.upgradeTalent(Talent.HEARTY_MEAL);
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);

		// a pre-1.4.0 save: a metamorphosis involving the removed BERSERKING_STAMINA, and points in it
		Bundle replacements = new Bundle();
		replacements.put("BERSERKING_STAMINA", "HEARTY_MEAL");
		replacements.put("HEARTY_MEAL", "BERSERKING_STAMINA");
		saved.put("replacements", replacements);
		Bundle tier1 = saved.getBundle("talents_tier_1");
		tier1.put("BERSERKING_STAMINA", 2);
		saved.put("talents_tier_1", tier1);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.WARRIOR;
		fresh.subClass = HeroSubClass.NONE;
		Talent.restoreTalentsFromBundle(loaded, fresh);

		assertEquals(1, fresh.pointsInTalent(Talent.HEARTY_MEAL), "real talents still restore around the removed one");
		assertTrue(fresh.metamorphedTalents.isEmpty(), "a metamorphosis involving a removed talent is dropped");
	}

	@Test
	void unrelatedMetamorphosisStillRestores() throws Exception {
		Hero original = warrior();
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);
		Bundle replacements = new Bundle();
		replacements.put("HEARTY_MEAL", "IRON_WILL");
		saved.put("replacements", replacements);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.WARRIOR;
		fresh.subClass = HeroSubClass.NONE;
		Talent.restoreTalentsFromBundle(loaded, fresh);

		assertEquals(Talent.IRON_WILL, fresh.metamorphedTalents.get(Talent.HEARTY_MEAL), "control: valid replacements are kept");
	}
}
