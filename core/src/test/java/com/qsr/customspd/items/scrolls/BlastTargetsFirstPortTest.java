package com.qsr.customspd.items.scrolls;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Blindness;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.qsr.customspd.sprites.HeroSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * upstream 025ba0e12 (v2.3.0): retribution and psionic blast pick every visible target before
 * hurting any of them, so a hit that changes what the hero can see does not spare the rest.
 */
class BlastTargetsFirstPortTest {

	private static final int W = 10;

	/** A tough rat whose wound hides another cell from the hero, the way a vision change would. */
	public static class VisionCuttingRat extends Rat {
		int hides = -1;

		{
			HP = HT = 1000;
			resistances.add(ScrollOfPsionicBlast.class);
		}

		@Override
		public void damage(int dmg, Object src) {
			super.damage(dmg, src);
			if (hides != -1) Dungeon.level.heroFOV[hides] = false;
		}
	}

	private Hero hero;
	private VisionCuttingRat first;
	private VisionCuttingRat second;

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

	/** Scroll.readAnimation casts to HeroSprite; read() only queues an animation, so a bare instance does. */
	private static void giveHeadlessHeroSprite(Hero hero) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		HeroSprite sprite = (HeroSprite) unsafe.getClass()
				.getMethod("allocateInstance", Class.class).invoke(unsafe, HeroSprite.class);
		sprite.ch = hero;
		hero.sprite = sprite;
	}

	private void setUp() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		level.cleanWalls(); // the blinded hero's view is read from the discoverable map
		hero = new Hero();
		Dungeon.hero = hero;
		giveHeadlessHeroSprite(hero);
		HeadlessLevel.at(hero, 4 * W + 2);
		curUser().set(null, hero);

		first = HeadlessLevel.at(new VisionCuttingRat(), 4 * W + 5);
		second = HeadlessLevel.at(new VisionCuttingRat(), 6 * W + 5);
		first.hides = second.pos;
		StubSprite.attach(first);
		StubSprite.attach(second);
		level.mobs.add(first);
		level.mobs.add(second);
		Arrays.fill(level.heroFOV, true);
	}

	@Test
	void retributionHitsEveryEnemyThatWasInView() throws Exception {
		setUp();

		new ScrollOfRetribution().doRead();

		assertTrue(first.HP < first.HT, "the first rat is hit");
		assertTrue(second.HP < second.HT, "the second rat is hit although the first hit hid it");
		assertNotNull(second.buff(Blindness.class), "and blinded");
	}

	@Test
	void psionicBlastHitsEveryEnemyThatWasInViewAndCountsThemForItsRecoil() throws Exception {
		setUp();

		new ScrollOfPsionicBlast().doRead();

		assertTrue(first.HP < first.HT, "the first rat is hit");
		assertTrue(second.HP < second.HT, "the second rat is hit although the first hit hid it");
		assertNotNull(second.buff(Blindness.class), "and blinded");
		// recoil is half max HP, times 0.9 per target: 20 * 0.5 * 0.81 rounds to 8 for two
		assertEquals(8, hero.HT - hero.HP, "both targets reduce the recoil");
	}
}
