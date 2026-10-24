package com.qsr.customspd.actors.hero.abilities;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.MagicImmune;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.abilities.mage.WildMagic;
import com.qsr.customspd.items.wands.Wand;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.items.wands.WandOfRegrowth;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream f6e55a633 (v2.2.0): wild magic routes every shot through the wand's tryToZap, so wands
 * that set up their zap there (regrowth's target, warding's ward budget) work, and a wand that
 * cannot fire fizzles instead of casting anyway. While the WildMagicTracker is on the hero,
 * tryToZap assumes charges and ignores magic immunity (heroic armor abilities work regardless).
 */
class WildMagicTryToZapPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		Field f = com.qsr.customspd.items.Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero hero() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 3;
		StubSprite.attach(hero);
		return hero;
	}

	@Test
	void anEmptyWandMayStillBeFiredByWildMagic() {
		Hero hero = hero();
		Wand wand = new WandOfMagicMissile();
		wand.curCharges = 0;

		assertFalse(wand.tryToZap(hero, 3 * 8 + 5), "an empty wand fizzles for an ordinary zap");

		Buff.affect(hero, WildMagic.WildMagicTracker.class, 0f);
		assertTrue(wand.tryToZap(hero, 3 * 8 + 5), "but wild magic assumes it has charges");
	}

	@Test
	void wildMagicWorksThroughMagicImmunityOnlyWhileTheTrackerIsOn() {
		Hero hero = hero();
		Wand wand = new WandOfMagicMissile();
		wand.curCharges = 2;
		Buff.affect(hero, MagicImmune.class, 10f);

		assertFalse(wand.tryToZap(hero, 3 * 8 + 5), "magic immunity blocks an ordinary zap");

		Buff.affect(hero, WildMagic.WildMagicTracker.class, 0f);
		assertTrue(wand.tryToZap(hero, 3 * 8 + 5), "heroic armor abilities work regardless");
	}

	@Test
	void aWandThatSetsUpInTryToZapIsSetUpByWildMagic() throws Exception {
		Hero hero = hero();
		Buff.affect(hero, WildMagic.WildMagicTracker.class, 0f);
		WandOfRegrowth wand = new WandOfRegrowth();
		wand.curCharges = 1;

		wand.tryToZap(hero, 3 * 8 + 6);

		Field target = WandOfRegrowth.class.getDeclaredField("target");
		target.setAccessible(true);
		assertEquals(3 * 8 + 6, target.getInt(wand), "regrowth records its aim in tryToZap");
	}

	@Test
	void aWildMagicShotAtAnUnfireableWandFizzlesInsteadOfCasting() throws Exception {
		Hero hero = hero();
		// no tracker (so tryToZap refuses the empty wand) and no scene: the old code went straight
		// to the wand's fx, which needs a scene; the new code logs the fizzle and carries on.
		Wand wand = new WandOfMagicMissile();
		wand.curCharges = 0;
		wand.partialCharge = 0f;
		Field cu = com.qsr.customspd.items.Item.class.getDeclaredField("curUser");
		cu.setAccessible(true);
		cu.set(null, hero);

		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = log::add;
		GLog.update.add(listener);
		try {
			Method zapWand = WildMagic.class.getDeclaredMethod("zapWand", ArrayList.class, Hero.class, int.class);
			zapWand.setAccessible(true);
			ArrayList<Wand> wands = new ArrayList<>();
			wands.add(wand);
			zapWand.invoke(new WildMagic(), wands, hero, 3 * 8 + 6);
		} finally {
			GLog.update.remove(listener);
		}

		assertTrue(log.contains(GLog.WARNING + Messages.get(Wand.class, "fizzles")),
				"the unfireable wand fizzles; log was " + log);
	}
}
