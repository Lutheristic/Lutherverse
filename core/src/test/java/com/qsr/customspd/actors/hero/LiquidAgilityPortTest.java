package com.qsr.customspd.actors.hero;

import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.buffs.GreaterHaste;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Liquid agility as upstream b7051a219 left it, with d167ce29f, 9e6aeafee and cc4577413's text:
 * the duelist's T2 potion talent splits into an evasion buff and an accuracy buff, and lethal
 * haste grants greater haste. CPDU's earlier port kept the buffs under their pre-rename class
 * names (RestoredAgilityTracker, RestoredAgilACCTracker) and dropped their strings. The buffs now
 * carry upstream's names, with Bundle aliases so a save holding the old ones still loads.
 */
class LiquidAgilityPortTest {

	private static final String OLD_EVA = "com.qsr.customspd.actors.hero.Talent$RestoredAgilityTracker";
	private static final String OLD_ACC = "com.qsr.customspd.actors.hero.Talent$RestoredAgilACCTracker";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
		Messages.setup(Languages.ENGLISH);
	}

	/** A buff bundled as a save from before the rename holds it: same fields, the old class name. */
	private static Bundlable restoreUnderOldName(Bundlable buff, String oldClassName) throws Exception {
		Bundle saved = new Bundle();
		saved.put("buff", buff);
		Bundle stored = saved.getBundle("buff");
		stored.put("__className", oldClassName);
		saved.put("buff", stored);
		return SaveRoundtrip.writeRead(saved).get("buff");
	}

	@Test
	void theOldBuffNamesNoLongerResolveByThemselves() {
		assertNull(Reflection.forName(OLD_EVA));
		assertNull(Reflection.forName(OLD_ACC));
	}

	@Test
	void anOldAccuracyBuffRestoresWithItsUses() throws Exception {
		Talent.LiquidAgilACCTracker buff = new Talent.LiquidAgilACCTracker();
		buff.uses = 2;

		Bundlable restored = restoreUnderOldName(buff, OLD_ACC);

		assertTrue(restored instanceof Talent.LiquidAgilACCTracker, "restored as " + restored);
		assertEquals(2, ((Talent.LiquidAgilACCTracker) restored).uses);
	}

	@Test
	void anOldEvasionBuffRestores() throws Exception {
		Bundlable restored = restoreUnderOldName(new Talent.LiquidAgilEVATracker(), OLD_EVA);

		assertTrue(restored instanceof Talent.LiquidAgilEVATracker, "restored as " + restored);
	}

	@Test
	void theAccuracyBuffAndGreaterHasteHaveTheirText() {
		Talent.LiquidAgilACCTracker accuracy = new Talent.LiquidAgilACCTracker();
		assertEquals("liquid agility", accuracy.name());
		assertTrue(accuracy.desc().startsWith("The Duelist's next regular melee attack will gain bonus accuracy."), accuracy.desc());

		GreaterHaste haste = new GreaterHaste();
		haste.set(3);
		assertEquals("greater haste", haste.name());
		assertTrue(haste.desc().endsWith("Turns of greater haste remaining: 3."), haste.desc());
	}

	@Test
	void theTalentTextMatchesWhatTheTalentsDo() {
		String liquidAgility = Talent.LIQUID_AGILITY.desc();
		assertTrue(liquidAgility.contains("_3x evasion_, and has _3x accuracy_ on her next melee attack"), liquidAgility);

		String lethalHaste = Talent.LETHAL_HASTE.desc();
		assertTrue(lethalHaste.contains("_3 turns_ worth of instant movement"), lethalHaste);

		String preciseAssault = Talent.PRECISE_ASSAULT.desc(true);
		assertTrue(preciseAssault.contains("_5x accuracy_"), preciseAssault);
		assertTrue(preciseAssault.contains("_infinite accuracy_"), preciseAssault);
		assertTrue(preciseAssault.contains("increase melee accuracy by 10%"), preciseAssault);
	}
}
