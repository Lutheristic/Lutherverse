package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.quest.Pickaxe;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 190831a88 (v2.2.0, "improved weapon quickslotting with metamorphed swift equip"): a
 * hero who is not a Duelist but has Swift Equip (through metamorphosis) quickslots a weapon to its
 * ability action, so tapping it swift equips the weapon. For such a hero that tap never warns and,
 * once the weapon is wielded, does nothing. A mage's staff always quickslots to its zap.
 *
 * The pickaxe hunk of that commit only reaches upstream's new mining quest; CPDU has the old one
 * alone, where the pickaxe quickslots to mining before and after, so that case is pinned as is.
 */
class SwiftEquipQuickslotPortTest {

	private static final int W = 10;

	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		// Item.execute starts with GameScene.cancel(), which needs a cell selector.
		IdleCellSelector.install();
	}

	@AfterAll
	static void restoreSelector() {
		IdleCellSelector.uninstall();
	}

	@BeforeEach
	void listen() {
		log.clear();
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		GLog.update.remove(logListener);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero hero(HeroClass cls, int swiftEquipPoints) {
		HeadlessLevel level = HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = cls;
		Talent.initClassTalents(hero);
		if (swiftEquipPoints > 0) {
			hero.talents.get(0).put(Talent.SWIFT_EQUIP, swiftEquipPoints);
		}
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	private static <T extends MeleeWeapon> T inPack(Hero hero, T weapon) {
		weapon.collect(hero.belongings.backpack);
		return weapon;
	}

	private static String needEquip() {
		return GLog.WARNING + Messages.get(MeleeWeapon.class, "ability_need_equip");
	}

	@Test
	void swiftEquipQuickslotsAWeaponToItsAbility() {
		hero(HeroClass.WARRIOR, 0);
		assertNull(new Sword().defaultAction(), "without the talent a warrior's weapon has no quickslot action");

		hero(HeroClass.WARRIOR, 1);
		assertEquals(MeleeWeapon.AC_ABILITY, new Sword().defaultAction());
	}

	@Test
	void aMagesStaffQuickslotsToItsZapForEveryone() {
		hero(HeroClass.DUELIST, 0);
		assertEquals(MagesStaff.AC_ZAP, new MagesStaff().defaultAction(), "not to the Duelist's ability");

		hero(HeroClass.WARRIOR, 1);
		assertEquals(MagesStaff.AC_ZAP, new MagesStaff().defaultAction());
	}

	@Test
	void aNonDuelistsTapSwiftEquipsTheWeapon() {
		Hero hero = hero(HeroClass.WARRIOR, 1);
		Sword sword = inPack(hero, new Sword());
		sword.usesTargeting = true; // as a previous targeted action could have left it

		sword.execute(hero, MeleeWeapon.AC_ABILITY);

		assertSame(sword, hero.belongings.weapon, "the tap swift equips the sword");
		assertFalse(sword.usesTargeting, "an equip is not a targeted action");
		for (String line : log) {
			assertFalse(line.startsWith(GLog.WARNING), "log was " + log);
		}
	}

	@Test
	void aNonDuelistsTapWithoutASwiftEquipLeftDoesNotWarn() {
		Hero hero = hero(HeroClass.WARRIOR, 1);
		assertTrue(inPack(hero, new Dagger()).doEquip(hero), "fixture: the one swift equip is spent");
		Sword sword = inPack(hero, new Sword());
		log.clear();

		sword.execute(hero, MeleeWeapon.AC_ABILITY);

		assertTrue(hero.belongings.backpack.contains(sword), "no swift equip is left, so the sword stays packed");
		assertTrue(log.isEmpty(), "a non-Duelist is not told to equip for an ability she lacks; log was " + log);
	}

	@Test
	void aNonDuelistsTapOnTheWieldedWeaponDoesNothing() {
		Hero hero = hero(HeroClass.WARRIOR, 1);
		Greatsword greatsword = new Greatsword(); // far more strength than a new hero has
		hero.belongings.weapon = greatsword;

		greatsword.execute(hero, MeleeWeapon.AC_ABILITY);

		assertTrue(log.isEmpty(), "no strength or charge warning for an ability she lacks; log was " + log);
		assertFalse(greatsword.usesTargeting);
	}

	@Test
	void aDuelistIsStillToldToEquipFirst() {
		Hero hero = hero(HeroClass.DUELIST, 0);
		Sword sword = inPack(hero, new Sword());

		sword.execute(hero, MeleeWeapon.AC_ABILITY);

		assertTrue(hero.belongings.backpack.contains(sword));
		assertEquals(1, log.size(), "log was " + log);
		assertEquals(needEquip(), log.get(0));
	}

	@Test
	void thePickaxeStillQuickslotsToMining() {
		hero(HeroClass.WARRIOR, 1);
		assertEquals(Pickaxe.AC_MINE, new Pickaxe().defaultAction());
	}
}
