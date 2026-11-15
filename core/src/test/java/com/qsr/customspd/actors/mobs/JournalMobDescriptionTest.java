package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.levels.traps.GuardianTrap;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal bestiary builds a fresh instance of each mob and asks it for its sprite and
 * description, with no weapon or armor rolled. Upstream 52aa38780 (v2.5.0) splits the statue
 * descriptions so the base text stands alone and the gear sentence is added only when the gear
 * exists.
 */
class JournalMobDescriptionTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20241115L);
		run.enterFirstLevel();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void statueWithNoWeaponDescribesAsItsBaseText() {
		String desc = new Statue().description();
		assertEquals(Messages.get(Statue.class, "desc"), desc);
		assertFalse(desc.contains("%"), desc);
	}

	@Test
	void armoredStatueWithNoGearDescribesAndDrawsWithoutThrowing() {
		ArmoredStatue statue = new ArmoredStatue();
		String desc = statue.description();
		assertEquals(Messages.get(ArmoredStatue.class, "desc"), desc);
		assertFalse(desc.contains("%"), desc);
		assertNotNull(assertDoesNotThrow(statue::sprite));
	}

	@Test
	void guardianWithNoWeaponDescribesAsItsBaseText() {
		String desc = new GuardianTrap.Guardian().description();
		assertEquals(Messages.get(GuardianTrap.Guardian.class, "desc"), desc);
		assertFalse(desc.contains("%"), desc);
	}

	@Test
	void statueWithAWeaponAddsTheWeaponSentence() {
		Statue statue = new Statue();
		statue.createWeapon(false);
		String desc = statue.description();
		String tail = "\n\n" + Messages.get(Statue.class, "desc_weapon", statue.weapon.name());
		assertTrue(desc.endsWith(tail), desc);
		assertEquals(Messages.get(Statue.class, "desc") + tail, desc);
	}

	@Test
	void armoredStatueWithGearAddsTheGearSentence() {
		ArmoredStatue statue = new ArmoredStatue();
		statue.createWeapon(false);
		String desc = statue.description();
		String tail = "\n\n" + Messages.get(ArmoredStatue.class, "desc_arm_wep",
				statue.weapon.name(), statue.armor.name());
		assertEquals(Messages.get(ArmoredStatue.class, "desc") + tail, desc);
		assertFalse(desc.contains("%"), desc);
	}

	@Test
	void guardianWithAWeaponAddsTheWeaponSentence() {
		GuardianTrap.Guardian guardian = new GuardianTrap.Guardian();
		guardian.createWeapon(false);
		String desc = guardian.description();
		String tail = "\n\n" + Messages.get(GuardianTrap.Guardian.class, "desc_weapon",
				guardian.weapon.name());
		assertEquals(Messages.get(GuardianTrap.Guardian.class, "desc") + tail, desc);
		assertFalse(desc.contains("%"), desc);
	}
}
