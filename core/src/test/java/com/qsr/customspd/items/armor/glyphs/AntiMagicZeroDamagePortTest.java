package com.qsr.customspd.items.armor.glyphs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Frost;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.hero.abilities.rogue.ShadowClone;
import com.qsr.customspd.actors.mobs.ArmoredStatue;
import com.qsr.customspd.actors.mobs.npcs.PrismaticImage;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.armor.PlateArmor;
import com.qsr.customspd.items.artifacts.DriedRose;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 38eb17421: anti-magic on the armor of the shadow clone, the
 * armored statue, the prismatic image and the rose ghost could take magic damage below 0, and
 * Char.damage ignores a negative hit outright. The reduction is now floored at 0, so a fully
 * absorbed hit still lands as a 0 damage hit: it shows its number and breaks frost.
 */
class AntiMagicZeroDamagePortTest {

	private static final int SIZE = 8;

	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel.install(SIZE, SIZE);
		hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** +10 anti-magic: its reduction rolls at least 10, far more than the 1 damage hit below. */
	private static Armor antiMagicArmor() {
		Armor armor = new PlateArmor();
		armor.inscribe(new AntiMagic());
		armor.level(10);
		return armor;
	}

	private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	/** Frosts the char, hits it for 1 magic damage, and returns the damage numbers it showed. */
	private static List<String> frostAndZap(Char ch) {
		StubSprite sprite = StubSprite.attach(ch);
		Buff.affect(ch, Frost.class, 10f);
		sprite.statuses.clear();
		ch.damage(1, new WandOfMagicMissile());
		List<String> shown = new ArrayList<>();
		for (StubSprite.Status s : sprite.statuses) shown.add(s.text);
		return shown;
	}

	private static void assertAbsorbedHitStillLands(Char ch) {
		int hp = ch.HP;
		List<String> shown = frostAndZap(ch);
		assertEquals(hp, ch.HP, "anti-magic absorbs the whole hit");
		assertEquals("[0]", shown.toString(), "the absorbed hit still shows as 0 damage");
		assertNull(ch.buff(Frost.class), "the absorbed hit still breaks frost");
	}

	@Test
	void theArmoredStatueTakesAnAbsorbedHitAsZero() throws Exception {
		ArmoredStatue statue = HeadlessLevel.at(new ArmoredStatue(), 3 * SIZE + 3);
		set(ArmoredStatue.class, statue, "armor", antiMagicArmor());

		assertAbsorbedHitStillLands(statue);
	}

	@Test
	void thePrismaticImageTakesAnAbsorbedHitAsZero() {
		hero.belongings.armor = antiMagicArmor();
		PrismaticImage image = HeadlessLevel.at(new PrismaticImage(), 3 * SIZE + 3);
		image.duplicate(hero, 20);

		assertAbsorbedHitStillLands(image);
	}

	@Test
	void theRoseGhostTakesAnAbsorbedHitAsZero() throws Exception {
		DriedRose rose = new DriedRose();
		set(DriedRose.class, rose, "armor", antiMagicArmor());
		DriedRose.GhostHero ghost = HeadlessLevel.at(new DriedRose.GhostHero(), 3 * SIZE + 3);
		ghost.HP = ghost.HT = 20;
		set(DriedRose.GhostHero.class, ghost, "rose", rose);

		assertAbsorbedHitStillLands(ghost);
	}

	@Test
	void theShadowCloneTakesAnAbsorbedHitAsZero() {
		// with cloned armor at 3 points the clone uses the hero's glyph on 3 hits in 4
		hero.belongings.armor = antiMagicArmor();
		Talent.initClassTalents(hero);
		hero.talents.get(0).put(Talent.CLONED_ARMOR, 3);

		int absorbed = 0;
		for (long seed = 1; seed <= 12; seed++) {
			ShadowClone.ShadowAlly clone = HeadlessLevel.at(new ShadowClone.ShadowAlly(), 3 * SIZE + 3);
			Random.pushGenerator(seed);
			List<String> shown;
			try {
				shown = frostAndZap(clone);
			} finally {
				Random.popGenerator();
			}
			assertEquals(1, shown.size(), "every hit lands, absorbed or not (seed " + seed + "): " + shown);
			assertNull(clone.buff(Frost.class), "every hit breaks frost (seed " + seed + ")");
			if (shown.get(0).equals("0")) absorbed++;
			clone.destroy();
		}
		assertTrue(absorbed > 0, "some hits were absorbed by the cloned glyph");
	}
}
