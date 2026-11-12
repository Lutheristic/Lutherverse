package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.wands.WandOfLivingEarth;
import com.qsr.customspd.plants.Earthroot;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 0ac18035d: the skeleton's bone explosion skipped rock armor
 * (Wand of Living Earth) and Earthroot armor, which only apply in defenseProc. Both now apply
 * at double effect, without being used up twice, before the usual double armor roll.
 */
class SkeletonBoneExplosionArmorPortTest {

	private static final int SIZE = 10;
	private static final int CENTER = 5 * SIZE + 5;

	/** An adjacent bystander with no armor of its own, recording every hit it takes. */
	static class Bystander extends Rat {
		final List<Integer> hits = new ArrayList<>();

		{
			HP = HT = 1000;
		}

		@Override
		public int drRoll() {
			return 0;
		}

		@Override
		public void damage(int dmg, Object src) {
			hits.add(dmg);
			super.damage(dmg, src);
		}
	}

	private int prevDepth;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevDepth = Dungeon.depth;
		Dungeon.depth = 1;
		HeadlessLevel.install(SIZE, SIZE);
		Hero hero = new Hero();
		hero.lvl = 100; // far past the skeleton's loot level, so its death drops nothing
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		Dungeon.depth = prevDepth;
	}

	private static Field field(Class<?> owner, String name) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	/**
	 * Kills a skeleton next to a fresh bystander under a fixed seed; returns the bystander.
	 * The previous bystander is cleared first (removing a char detaches its buffs).
	 */
	private static Bystander explode(long seed, Consumer<Bystander> armorUp) {
		for (Char ch : Actor.chars().toArray(new Char[0])) {
			if (ch != Dungeon.hero) Actor.remove(ch);
		}
		Skeleton skeleton = HeadlessLevel.at(new Skeleton(), CENTER);
		StubSprite.attach(skeleton);
		Bystander bystander = HeadlessLevel.at(new Bystander(), CENTER + 1);
		StubSprite.attach(bystander);
		armorUp.accept(bystander);

		Random.pushGenerator(seed);
		try {
			skeleton.die(null);
		} finally {
			Random.popGenerator();
		}
		assertEquals(1, bystander.hits.size(), "the explosion hits the bystander once");
		return bystander;
	}

	@Test
	void earthrootArmorBlocksTwiceButIsUsedOnce() throws Exception {
		for (long seed = 1; seed <= 6; seed++) {
			int raw = explode(seed, b -> { }).hits.get(0);

			Bystander armored = explode(seed, b -> Buff.affect(b, Earthroot.Armor.class).level(100));
			int taken = armored.hits.get(0);

			// at depth 1 earthroot blocks 3 per hit
			assertEquals(Math.max(0, raw - 6), taken, "earthroot's flat block applies twice (seed " + seed + ")");
			Earthroot.Armor armor = armored.buff(Earthroot.Armor.class);
			assertEquals(97, field(Earthroot.Armor.class, "level").getInt(armor), "but only one block is used up");
		}
	}

	@Test
	void rockArmorAppliesToTheExplosion() throws Exception {
		for (long seed = 1; seed <= 6; seed++) {
			int raw = explode(seed, b -> { }).hits.get(0);

			Bystander armored = explode(seed, b -> {
				try {
					WandOfLivingEarth.RockArmor rock = Buff.affect(b, WandOfLivingEarth.RockArmor.class);
					field(WandOfLivingEarth.RockArmor.class, "armor").setInt(rock, 100);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			});
			int taken = armored.hits.get(0);

			// rock armor blocks half of the hit, rounded up; upstream then scales the rest by that
			// fraction rounded to a whole number, so an even roll is halved and an odd one is blocked
			int expected = raw % 2 == 0 ? raw / 2 : 0;
			assertEquals(expected, taken, "rock armor applies to the explosion (seed " + seed + ", roll " + raw + ")");
			WandOfLivingEarth.RockArmor rock = armored.buff(WandOfLivingEarth.RockArmor.class);
			assertEquals(100 - (raw - raw / 2), field(WandOfLivingEarth.RockArmor.class, "armor").getInt(rock),
					"and is only used up once");
		}
	}
}
