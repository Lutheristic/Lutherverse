package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Challenges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.abilities.huntress.SpiritHawk;
import com.qsr.customspd.actors.hero.abilities.rogue.ShadowClone;
import com.qsr.customspd.actors.mobs.npcs.MirrorImage;
import com.qsr.customspd.actors.mobs.npcs.PrismaticImage;
import com.qsr.customspd.items.wands.WandOfLivingEarth;
import com.qsr.customspd.items.wands.WandOfRegrowth;
import com.qsr.customspd.levels.traps.GuardianTrap;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.sprites.PrismaticSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.gltextures.TextureCache;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal bestiary builds a fresh instance of each mob and asks it for its sprite and
 * description, with no weapon or armor rolled. Upstream 52aa38780 (v2.5.0) splits the statue
 * descriptions so the base text stands alone and the gear sentence is added only when the gear
 * exists. The same commit splits the hawk, earthen guardian and golden lotus descriptions so the
 * live numbers appear only for an instance that is actually on the level.
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

	private static Char placeNearHero(Char ch) {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = Dungeon.hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				ch.pos = cell;
				Actor.add(ch);
				return ch;
			}
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	@Test
	void alliesOffTheLevelDescribeAsTheirBaseText() {
		SpiritHawk.HawkAlly hawk = new SpiritHawk.HawkAlly();
		assertEquals(Messages.get(SpiritHawk.HawkAlly.class, "desc"), hawk.description());
		assertFalse(hawk.description().contains("%"));

		WandOfLivingEarth.EarthGuardian guardian = new WandOfLivingEarth.EarthGuardian();
		assertEquals(Messages.get(WandOfLivingEarth.EarthGuardian.class, "desc"), guardian.description());
		assertFalse(guardian.description().contains("%"));

		WandOfRegrowth.Lotus lotus = new WandOfRegrowth.Lotus();
		assertEquals(Messages.get(WandOfRegrowth.Lotus.class, "desc"), lotus.description());
		assertFalse(lotus.description().contains("%"));
	}

	@Test
	void alliesOnTheLevelAddTheirLiveNumbers() {
		SpiritHawk.HawkAlly hawk = (SpiritHawk.HawkAlly) placeNearHero(new SpiritHawk.HawkAlly());
		assertTrue(hawk.description().startsWith(Messages.get(SpiritHawk.HawkAlly.class, "desc")
				+ "\n\n" + Messages.get(SpiritHawk.HawkAlly.class, "desc_remaining", 100)),
				hawk.description());
		Actor.remove(hawk);

		WandOfLivingEarth.EarthGuardian guardian =
				(WandOfLivingEarth.EarthGuardian) placeNearHero(new WandOfLivingEarth.EarthGuardian());
		guardian.setInfo(Dungeon.hero, 2, 0);
		int blockMax = Dungeon.isChallenged(Challenges.NO_ARMOR) ? 4 : 9;
		assertEquals(Messages.get(WandOfLivingEarth.EarthGuardian.class, "desc") + "\n\n"
				+ Messages.get(WandOfLivingEarth.EarthGuardian.class, "wand_info", 2, blockMax),
				guardian.description());
		Actor.remove(guardian);

		WandOfRegrowth.Lotus lotus = (WandOfRegrowth.Lotus) placeNearHero(new WandOfRegrowth.Lotus());
		assertEquals(Messages.get(WandOfRegrowth.Lotus.class, "desc") + "\n\n"
				+ Messages.get(WandOfRegrowth.Lotus.class, "wand_info", 0, 40, 40),
				lotus.description());
		Actor.remove(lotus);
	}

	@Test
	void hawkDescribesWithNoHero() {
		Dungeon.hero = null;
		String desc = assertDoesNotThrow(() -> new SpiritHawk.HawkAlly().description());
		assertEquals(Messages.get(SpiritHawk.HawkAlly.class, "desc"), desc);
	}

	@Test
	void imagesWithNoHeroFallBackToClothArmor() {
		MirrorImage mirror = new MirrorImage();
		assertNotNull(mirror.sprite());
		assertEquals(1, mirror.armTier);

		PrismaticImage prism = new PrismaticImage();
		assertNotNull(prism.sprite());
		assertEquals(1, prism.armTier);
	}

	@Test
	void shadowCloneDrawsAsTheRogueWhateverTheHero() {
		assertEquals(HeroClass.WARRIOR, Dungeon.hero.heroClass);
		assertNotSame(TextureCache.get(HeroClass.WARRIOR.spritesheet()),
				TextureCache.get(HeroClass.ROGUE.spritesheet()));
		ShadowClone.ShadowSprite sprite = new ShadowClone.ShadowSprite();
		assertSame(TextureCache.get(HeroClass.ROGUE.spritesheet()), sprite.texture);
	}

	@Test
	void prismaticSpriteIsTintedWhenBuilt() {
		PrismaticSprite sprite = new PrismaticSprite();
		assertEquals(0.5f, sprite.rm);
		assertEquals(0.5f, sprite.gm);
		assertEquals(0.5f, sprite.bm);
	}
}
