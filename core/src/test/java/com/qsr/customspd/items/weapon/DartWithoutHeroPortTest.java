package com.qsr.customspd.items.weapon;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.melee.Crossbow;
import com.qsr.customspd.items.weapon.missiles.darts.Dart;
import com.qsr.customspd.items.weapon.missiles.darts.PoisonDart;
import com.qsr.customspd.sprites.GooSprite;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the null-guards of upstream 3cc21b55a (v2.5.0, "fixed crash errors in
 * catalogs"): item and sprite code that is reached with no hero / no scene must not crash.
 *
 * Dart keeps the hero's crossbow in a static, refreshed by updateCrossbow(); with no hero it
 * used to dereference Dungeon.hero, and (with the guard but without the reset) would keep a
 * previous run's crossbow alive. GooSprite's spray emitter comes from centerEmitter(), which is
 * null when there is no GameScene; update() used to dereference it.
 *
 * Upstream's TippedDart hunk of the same commit guards the charged-shot branch of
 * durabilityPerUse(), which since 11a410f07 also reads the crossbow: that static can outlive the
 * hero it came from.
 */
class DartWithoutHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void aDartCanBeThrownWithNoHeroAndForgetsThePreviousCrossbow() {
		Hero hero = new Hero();
		hero.belongings.weapon = new Crossbow();
		Dungeon.hero = hero;

		Dart dart = new Dart();
		dart.throwSound(); // refreshes the static crossbow from the hero
		assertEquals(4, dart.min(0) /* crossbow level 0: 4 base */, "fixture: the hero's crossbow is picked up");

		Dungeon.hero = null;
		assertDoesNotThrow(dart::throwSound, "upstream 3cc21b55a: no hero must not crash updateCrossbow");
		assertEquals(1, dart.min(0), "with no hero there is no crossbow: bare dart damage");
	}

	@Test
	void aTippedDartsDurabilityNeedsNoHeroEvenWithAStaleCrossbow() {
		Hero hero = new Hero();
		hero.belongings.weapon = new Crossbow();
		Dungeon.hero = hero;
		new Dart().throwSound(); // the static crossbow now points at this hero's bow

		Dungeon.hero = null;
		PoisonDart tipped = new PoisonDart();
		float perUse = assertDoesNotThrow(tipped::durabilityPerUse, "the charged-shot check must not need a hero");
		assertEquals(100f + 0.001f, perUse, 0.0005f, "no hero, no charged shot: a single use");
	}

	@Test
	void aGooSpriteWithoutASceneCanStillUpdate() {
		GooSprite sprite = new GooSprite();
		assertDoesNotThrow(sprite::update, "upstream 3cc21b55a: spray is null outside a GameScene");
	}
}
