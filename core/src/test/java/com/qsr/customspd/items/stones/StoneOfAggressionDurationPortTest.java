package com.qsr.customspd.items.stones;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Goo;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.RotHeart;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 01a9218ad: the stone of aggression lasts its full duration on
 * regular enemies too. Only bosses and minibosses resist it down to a quarter.
 */
class StoneOfAggressionDurationPortTest {

	private static final int SIZE = 10;
	private static final int TARGET = 5 * SIZE + 5;

	private static final float FULL = StoneOfAggression.Aggression.DURATION;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel.install(SIZE, SIZE);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/**
	 * Throws a stone at the char and returns the aggression it leaves. Headless, the throw stops
	 * at its scream effect (CellEmitter.center needs the game scene), which comes after the buff.
	 */
	private static StoneOfAggression.Aggression stoned(Char ch) {
		HeadlessLevel.at(ch, TARGET);
		StubSprite.attach(ch);
		try {
			new StoneOfAggression().activate(TARGET);
		} catch (NullPointerException e) {
			boolean atTheScream = false;
			for (StackTraceElement el : e.getStackTrace()) {
				if (el.getClassName().endsWith(".CellEmitter") && el.getMethodName().equals("center")) {
					atTheScream = true;
				}
			}
			if (!atTheScream) throw e;
		}
		StoneOfAggression.Aggression aggression = ch.buff(StoneOfAggression.Aggression.class);
		assertTrue(aggression != null, "the stone marks " + ch.getClass().getSimpleName());
		return aggression;
	}

	@Test
	void aRegularEnemyIsTargetedForTheFullDuration() {
		assertEquals(FULL, stoned(new Rat()).cooldown(), 0.0001f);
	}

	@Test
	void aBossIsTargetedForAQuarterOfIt() {
		assertEquals(FULL / 4f, stoned(new Goo()).cooldown(), 0.0001f);
	}

	@Test
	void aMinibossIsTargetedForAQuarterOfIt() {
		assertEquals(FULL / 4f, stoned(new RotHeart()).cooldown(), 0.0001f);
	}

	@Test
	void anAllyIsTargetedForTheFullDuration() {
		// unchanged by the port: an ally always got the full duration
		Rat ally = new Rat();
		ally.alignment = Char.Alignment.ALLY;
		assertEquals(FULL, stoned(ally).cooldown(), 0.0001f);
	}

	@Test
	void theBuffIconFadesOverTheFullDurationOnARegularEnemy() {
		Rat rat = new Rat();
		HeadlessLevel.at(rat, TARGET);
		StoneOfAggression.Aggression aggression = Buff.affect(rat, StoneOfAggression.Aggression.class, FULL / 2f - 1f);

		assertEquals(0.5f, aggression.iconFadePercent(), 0.0001f, "half the full duration left");
	}

	@Test
	void theDescriptionSaysBossesResistIt() {
		String desc = Messages.get(StoneOfAggression.class, "desc");
		assertTrue(desc.contains("Boss enemies can resist the effect"), desc);
	}
}
