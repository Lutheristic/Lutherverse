package com.qsr.customspd.items.bombs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.armor.PlateArmor;
import com.qsr.customspd.items.armor.glyphs.AntiMagic;
import com.qsr.customspd.items.stones.StoneOfBlast;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * upstream d3d472925 and 92d2920d1 (v2.3.0): an explosion conjured by magic deals physical
 * damage, so the anti-magic glyph does not soften it. Only the arcane bomb's blast and the holy
 * bomb's extra hit on unholy foes are magic.
 */
class ConjuredExplosionPortTest {

	private static final int W = 9;
	private static final int CENTER = 4 * W + 4;

	/** A tough rat that keeps the source of every hit it takes. */
	public static class SourceRecordingRat extends Rat {
		final List<Object> sources = new ArrayList<>();

		{
			HP = HT = 1000;
		}

		@Override
		public void damage(int dmg, Object src) {
			sources.add(src);
			super.damage(dmg, src);
		}
	}

	/** An unholy one, which the holy bomb hits a second time. */
	public static class UndeadSourceRecordingRat extends SourceRecordingRat {
		{
			properties.add(Property.UNDEAD);
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel.install(W, W);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, W + 1);
		Random.pushGenerator(0xB0B0L);
	}

	@AfterEach
	void tearDown() {
		Random.popGenerator();
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static <T extends SourceRecordingRat> T ratNextToTheBlast(T rat) {
		HeadlessLevel.at(rat, CENTER + 1);
		StubSprite.attach(rat);
		Dungeon.level.mobs.add(rat);
		return rat;
	}

	/** How much of a 10 damage hit from this source reaches a hero in +10 anti-magic armor. */
	private static int antiMagicHeroTakes(Object src) {
		Hero hero = new Hero();
		StubSprite.attach(hero);
		Armor armor = new PlateArmor();
		armor.inscribe(new AntiMagic());
		armor.level(10);
		hero.belongings.armor = armor;
		int before = hero.HP;
		hero.damage(10, src);
		return before - hero.HP;
	}

	@Test
	void aStoneOfBlastExplosionIsNotSoftenedByAntiMagic() throws Exception {
		SourceRecordingRat rat = ratNextToTheBlast(new SourceRecordingRat());
		Method activate = StoneOfBlast.class.getDeclaredMethod("activate", int.class);
		activate.setAccessible(true);
		activate.invoke(new StoneOfBlast(), CENTER);

		assertEquals(1, rat.sources.size(), "fixture: the blast hits the rat once");
		assertEquals(10, antiMagicHeroTakes(rat.sources.get(0)), "the conjured blast is physical damage");
	}

	@Test
	void anArcaneBombBlastIsSoftenedByAntiMagic() {
		SourceRecordingRat rat = ratNextToTheBlast(new SourceRecordingRat());
		new ArcaneBomb().explode(CENTER);

		assertEquals(1, rat.sources.size(), "fixture: the blast hits the rat once");
		assertEquals(0, antiMagicHeroTakes(rat.sources.get(0)), "the arcane blast is magic damage");
	}

	@Test
	void onlyTheHolyBombsExtraHitIsSoftenedByAntiMagic() {
		SourceRecordingRat rat = ratNextToTheBlast(new UndeadSourceRecordingRat());
		new HolyBomb().explode(CENTER);

		assertEquals(2, rat.sources.size(), "fixture: an undead rat takes the blast and the holy light");
		assertEquals(10, antiMagicHeroTakes(rat.sources.get(0)), "the blast itself is physical damage");
		assertEquals(0, antiMagicHeroTakes(rat.sources.get(1)), "the holy light is magic damage");
	}

	@Test
	void anOldSavesMagicalBombLoadsAsAConjuredBomb() throws Exception {
		Bundle bomb = new Bundle();
		bomb.put("__className", "com.qsr.customspd.items.bombs.Bomb$MagicalBomb");
		bomb.put("quantity", 1);
		Bundle save = new Bundle();
		save.put("bomb", bomb);
		save.put("cause", "com.qsr.customspd.items.bombs.Bomb$MagicalBomb");
		Bundle read = SaveRoundtrip.writeRead(save);

		Bundlable restored = read.get("bomb");
		assertNotNull(restored, "the old class name resolves");
		assertEquals("com.qsr.customspd.items.bombs.Bomb$ConjuredBomb", restored.getClass().getName());
		Class<?> cause = read.getClass("cause");
		assertNotNull(cause, "a ranking's cause of death under the old name resolves");
		assertEquals("com.qsr.customspd.items.bombs.Bomb$ConjuredBomb", cause.getName());
	}
}
