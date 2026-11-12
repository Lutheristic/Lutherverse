package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.scenes.AmuletScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import com.watabou.noosa.Game;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 345ddffd1 (v2.2.0): Actor.addDelayed stopped taking negative delays. The amulet's
 * pickup used one (-5) to get its scene in before the hero's next turn, and once delays were
 * clamped at zero that actor waited behind the hero, so the scene only played after the
 * player's next action. Upstream gives the actor VFX priority instead.
 */
class AmuletSceneTimingPortTest {

	private Hero prevHero;
	private Game prevGame;
	private Object prevSceneClass;
	private boolean prevObtained;
	private boolean prevNoText;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		prevHero = Dungeon.hero;
		prevGame = Game.instance;
		prevSceneClass = sceneClass().get(null);
		prevObtained = Statistics.amuletObtained;
		prevNoText = AmuletScene.noText;
		// the amulet scene is requested through Game.switchScene; a bare Game records it
		new Game(null, Game.platform);
		Statistics.amuletObtained = false;
	}

	@AfterEach
	void tearDown() throws Exception {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		Game.instance = prevGame;
		sceneClass().set(null, prevSceneClass);
		Statistics.amuletObtained = prevObtained;
		AmuletScene.noText = prevNoText;
	}

	private static Field sceneClass() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return f;
	}

	@Test
	void theAmuletScenePlaysBeforeTheHerosNextTurn() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, 3 * level.width() + 3);

		assertTrue(new Amulet().doPickUp(hero, hero.pos));
		assertTrue(Statistics.amuletObtained);

		final int[] heroTurns = {0};
		TurnPump pump = new TurnPump(h -> {
			heroTurns[0]++;
			TurnPump.REST.take(h);
		});
		Actor first = pump.step();

		assertEquals(0, heroTurns[0], "the hero must not get a turn before the amulet scene");
		assertSame(AmuletScene.class, sceneClass().get(null), "the first actor to act asks for the amulet scene");
		assertTrue(first != hero);
	}
}
