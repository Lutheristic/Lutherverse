package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.BrokenSeal;
import com.qsr.customspd.items.armor.glyphs.Obfuscation;
import com.qsr.customspd.items.armor.glyphs.Thorns;
import com.qsr.customspd.test.HeadlessGdx;
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
 * Port regression for upstream 5ea26eedf (v2.2.0, "fixed further errors with seal transferring
 * and class armor").
 *
 * - BrokenSeal.canTransferGlyph(): the Runic Transference rule (1 point moves common and
 *   uncommon glyphs, 2 points move any) lived inline in Armor's detach code; it is now a seal
 *   method so class-armor transfer can ask the same question.
 * - ClassArmor.upgrade(): moves the seal across as a field instead of re-affixing it. The first
 *   two upgrade tests PIN the observable result (level carried over exactly, seal and glyph
 *   preserved); they pass against the old affixSeal form too, because the old code set the level
 *   after affixing. The third reproduces the real bug: the old code affixed the seal before
 *   inscribing, so a glyphless seal picked up the armor's glyph (canary: fails against the
 *   pre-port ClassArmor, passes with it). The class-armor transfer branch lives in an anonymous
 *   WndBag selector and is not headlessly drivable, nor is Armor.execute's detach path
 *   (Item.execute needs GameScene).
 */
class SealTransferPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	private static Hero warriorWithTransference(int points) {
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.talents.get(1).put(Talent.RUNIC_TRANSFERENCE, points);
		Dungeon.hero = hero;
		return hero;
	}

	private static BrokenSeal sealWith(Armor.Glyph glyph) {
		BrokenSeal seal = new BrokenSeal();
		seal.setGlyph(glyph);
		return seal;
	}

	@Test
	void aSealWithNoGlyphNeverTransfers() {
		warriorWithTransference(2);
		assertFalse(sealWith(null).canTransferGlyph());
	}

	@Test
	void withoutTheTalentNothingTransfers() {
		warriorWithTransference(0);
		assertFalse(sealWith(new Obfuscation()).canTransferGlyph());
	}

	@Test
	void onePointMovesCommonAndUncommonGlyphsOnly() {
		warriorWithTransference(1);
		assertTrue(sealWith(new Obfuscation()).canTransferGlyph(), "common");
		assertFalse(sealWith(new Thorns()).canTransferGlyph(), "rare");
	}

	@Test
	void twoPointsMoveAnyGlyph() {
		warriorWithTransference(2);
		assertTrue(sealWith(new Thorns()).canTransferGlyph());
	}

	@Test
	void classArmorUpgradeMovesTheSealWithoutAFreeLevel() {
		Hero hero = warriorWithTransference(0);
		ClothArmor armor = new ClothArmor();
		BrokenSeal seal = new BrokenSeal();
		seal.level(1);
		armor.level(1); // the seal's +1 is already part of the armor's level
		armor.seal = seal;

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertSame(seal, classArmor.seal, "the seal travels to the class armor");
		assertEquals(1, classArmor.trueLevel(),
				"upstream 5ea26eedf: moving the seal must not re-apply its +1 level");
	}

	@Test
	void classArmorUpgradeKeepsAGlyphThatLivesOnTheArmor() {
		Hero hero = warriorWithTransference(0);
		ClothArmor armor = new ClothArmor();
		armor.inscribe(new Obfuscation());

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertTrue(classArmor.glyph instanceof Obfuscation);
		assertNull(classArmor.seal);
	}

	@Test
	void classArmorUpgradeDoesNotLetASealWithoutAGlyphPickUpTheArmorsGlyph() {
		Hero hero = warriorWithTransference(0);
		ClothArmor armor = new ClothArmor();
		armor.inscribe(new Obfuscation());
		armor.seal = new BrokenSeal(); // no glyph of its own

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertTrue(classArmor.glyph instanceof Obfuscation, "the armor keeps its glyph");
		assertNotNull(classArmor.seal);
		// Armor.inscribe() copies the glyph onto an attached seal. The old upgrade() affixed the
		// seal first and inscribed afterwards, so a glyphless seal picked up the armor's glyph
		// (the "changing specific glyph state" bug of daa7c3149/5ea26eedf).
		assertNull(classArmor.seal.getGlyph(), "a glyphless seal must stay glyphless");
	}
}
