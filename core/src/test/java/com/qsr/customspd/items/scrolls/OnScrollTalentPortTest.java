package com.qsr.customspd.items.scrolls;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Invisibility;
import com.qsr.customspd.actors.buffs.ScrollEmpower;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.scrolls.exotic.ScrollOfEnchantment;
import com.qsr.customspd.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.qsr.customspd.sprites.HeroSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 33b065e69 (v2.2.0) reworked the T2 on-scroll talents. Reading any scroll that is not
 * anonymous now fires Talent.onScrollUsed from Scroll.readAnimation, scaled by the scroll's
 * talentFactor, which is 2 for upgrade, transmutation, enchantment and metamorphosis. Inscribed
 * power (was energizing upgrade) takes over the scroll empower buff that the mage's empowering
 * scrolls used to give, at +2 wand levels for 2/3 zaps; inscribed stealth (was mystical upgrade)
 * gives 3/5 turns of invisibility.
 */
class OnScrollTalentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private static Hero mage() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		curUser().set(null, hero);
		Scroll.initLabels();
		return hero;
	}

	/**
	 * Scroll.readAnimation casts the reader's sprite to HeroSprite, whose constructor loads GL
	 * textures. read() itself only queues an animation, so an instance allocated without running
	 * the constructor is enough to let the real readAnimation run to its end.
	 */
	private static void giveHeadlessHeroSprite(Hero hero) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		HeroSprite sprite = (HeroSprite) unsafe.getClass()
				.getMethod("allocateInstance", Class.class).invoke(unsafe, HeroSprite.class);
		sprite.ch = hero;
		hero.sprite = sprite;
	}

	private static int zapsLeft(Hero hero) throws Exception {
		ScrollEmpower empower = hero.buff(ScrollEmpower.class);
		assertNotNull(empower, "reading the scroll empowers the mage's wands");
		Field left = ScrollEmpower.class.getDeclaredField("left");
		left.setAccessible(true);
		return left.getInt(empower);
	}

	@Test
	void readingAScrollGivesInscribedPowerItsEmpoweredZaps() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_POWER, 1);
		giveHeadlessHeroSprite(hero);

		new ScrollOfIdentify().readAnimation();

		assertEquals(2, zapsLeft(hero), "2 empowered zaps at +1");
	}

	@Test
	void aScrollOfUpgradeDoublesThem() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_POWER, 2);
		giveHeadlessHeroSprite(hero);

		new ScrollOfUpgrade().readAnimation();

		assertEquals(6, zapsLeft(hero), "3 empowered zaps at +2, doubled");
	}

	@Test
	void anAnonymousScrollTriggersNothing() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_POWER, 2);
		giveHeadlessHeroSprite(hero);

		ScrollOfIdentify scroll = new ScrollOfIdentify();
		scroll.anonymize();
		scroll.readAnimation();

		assertNull(hero.buff(ScrollEmpower.class));
	}

	@Test
	void upgradeTransmutationEnchantmentAndMetamorphosisCountDouble() {
		assertEquals(2f, new ScrollOfUpgrade().talentFactor);
		assertEquals(2f, new ScrollOfTransmutation().talentFactor);
		assertEquals(2f, new ScrollOfEnchantment().talentFactor);
		assertEquals(2f, new ScrollOfMetamorphosis().talentFactor);
		assertEquals(1f, new ScrollOfIdentify().talentFactor, "control: other scrolls count once");
	}

	@Test
	void inscribedStealthGivesInvisibilityScaledByTheFactor() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 2);

		Talent.onScrollUsed(hero, hero.pos, 2f);

		Invisibility invis = hero.buff(Invisibility.class);
		assertNotNull(invis);
		assertEquals(10f, invis.cooldown(), 0.0001f, "5 turns at +2, doubled");
	}
}
