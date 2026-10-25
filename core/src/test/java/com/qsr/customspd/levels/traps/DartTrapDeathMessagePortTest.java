package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Rankings;
import com.qsr.customspd.actors.buffs.Poison;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import com.qsr.customspd.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Port regression for upstream 6d2aa45a2 (v2.5.0): a dart trap that kills the hero logs its own
 * death message.
 *
 * The hero sees the trap, so the dart flies as a MissileSprite and does its damage when the
 * flight ends. A bare scene holds the sprite, and updating it lands the dart.
 */
class DartTrapDeathMessagePortTest {

	private static final int W = 8;
	private static final int TRAP = 4 * W + 4;

	/**
	 * Any hit leaves this hero at 0 HP, since the real damage and death paths need a game scene.
	 * Poison would not attach for the same reason: its splash needs the scene's emitters.
	 */
	static class FragileHero extends Hero {
		{
			immunities.add(Poison.class);
		}

		@Override
		public void damage(int dmg, Object src) {
			HP = 0;
		}
	}

	private HeadlessRun run;
	private Game priorGame;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = new Signal.Listener<String>() {
		@Override
		public boolean onSignal(String text) {
			log.add(text);
			return false;
		}
	};

	@BeforeEach
	void start() {
		// a real run, since Dungeon.fail ranks the death
		run = HeadlessRun.start(20261025L);
		FileUtils.deleteFile(Rankings.RANKINGS_FILE);
		priorGame = Game.instance;
		new Game(null, null) {{
			scene = new Scene();
		}};
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		try {
			GLog.update.remove(logListener);
			HeadlessLevel.uninstall();
			FileUtils.deleteFile(Rankings.RANKINGS_FILE);
			Game.instance = priorGame;
		} finally {
			run.close();
		}
	}

	/** The hero stands on the trap and sees it; the trap fires and its dart lands. */
	private static FragileHero springUnderTheHero(Trap trap) {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		FragileHero hero = new FragileHero();
		HeroClass.WARRIOR.initHero(hero);
		HeadlessLevel.at(hero, TRAP);
		StubSprite.attach(hero);
		Dungeon.hero = hero;
		level.heroFOV[TRAP] = true;

		trap.set(TRAP);
		trap.activate();
		new TurnPump().step();

		Game.elapsed = 1f;
		for (int i = 0; i < 10 && hero.isAlive(); i++) {
			Game.scene().update();
		}
		return hero;
	}

	private String lastLine() {
		return log.isEmpty() ? null : log.get(log.size() - 1);
	}

	@Test
	void aWornDartTrapThatKillsTheHeroSaysSo() {
		FragileHero hero = springUnderTheHero(new WornDartTrap());

		assertFalse(hero.isAlive(), "fixture: the dart kills the hero");
		assertEquals(GLog.NEGATIVE + Messages.get(WornDartTrap.class, "ondeath"), lastLine());
	}

	@Test
	void aPoisonDartTrapThatKillsTheHeroSaysSo() {
		FragileHero hero = springUnderTheHero(new PoisonDartTrap());

		assertFalse(hero.isAlive(), "fixture: the dart kills the hero");
		assertEquals(GLog.NEGATIVE + Messages.get(PoisonDartTrap.class, "ondeath"), lastLine());
	}
}
