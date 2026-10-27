package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.weapon.melee.Dagger;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Swift equip's second use at +2 no longer runs out 5 turns after the first one (upstream
 * b7051a219): it lasts for the whole 20 turn cooldown.
 */
class SwiftEquipSecondUsePortTest {

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

	private static Hero duelist(int swiftEquipPoints) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		hero.talents.get(1).put(Talent.SWIFT_EQUIP, swiftEquipPoints);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	private static float equipFromPack(Hero hero, KindOfWeapon weapon) {
		weapon.collect(hero.belongings.backpack);
		float before = hero.cooldown();
		assertTrue(weapon.doEquip(hero), "fixture: equip succeeds");
		return hero.cooldown() - before;
	}

	@Test
	void theSecondUseIsStillFreeLateInTheCooldown() {
		Hero hero = duelist(2);
		// the first swift equip starts a 19 turn cooldown; ten turns later, 9 are left
		Buff.affect(hero, Talent.SwiftEquipCooldown.class, 9f).secondUse = true;

		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f, "second use, free");
		assertFalse(hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse(), "and spent");
		assertEquals(2f, equipFromPack(hero, new Dagger()), 0.0001f, "a third swap is paid in full");
	}

	@Test
	void aCooldownFromAnOldSaveGetsItsSecondUseBack() throws Exception {
		// saved with the second use unspent but past the old 5 turn window
		Hero hero = duelist(2);
		Talent.SwiftEquipCooldown saved = Buff.affect(hero, Talent.SwiftEquipCooldown.class, 9f);
		saved.secondUse = true;
		saved.detach();

		Talent.SwiftEquipCooldown restored = SaveRoundtrip.of(saved);
		restored.attachTo(hero);
		assertEquals(9f, restored.cooldown(), 0.001f, "fixture: the remaining cooldown survives the save");

		assertTrue(restored.hasSecondUse());
		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f);
	}

	@Test
	void theTalentTextMatches() {
		String desc = Talent.SWIFT_EQUIP.desc();
		assertTrue(desc.contains("instantly _one time_ every 20 turns."), desc);
		assertTrue(desc.contains("instantly _two times_ every 20 turns."), desc);
	}
}
