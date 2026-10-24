package com.qsr.customspd.items.stones;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Terror;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression test for upstream SPD 951a4c5fb (v2.4.0): "fixed stone of fear applying to
 * hero & allies".
 *
 * StoneOfFear.activate terrified whatever char stood on the target cell, including the hero's
 * allies (and the hero). It now skips allies. An enemy on the cell is still terrified (control).
 */
class StoneOfFearAllyPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;

	/** Item.curUser is protected; a subclass is the way in. */
	private static final class CurUser extends StoneOfFear {
		static void set(Hero hero) {
			curUser = hero;
		}
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		CurUser.set(null);
	}

	private Hero setUpHero(HeadlessLevel level) {
		prevHero = Dungeon.hero;
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 1 * level.width() + 1);
		StubSprite sprite = StubSprite.attach(hero);
		sprite.parent = new Group(); // the stone shows a Flare on the hero sprite's parent
		CurUser.set(hero);
		return hero;
	}

	private Rat ratAt(int cell, Char.Alignment alignment) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.alignment = alignment;
		StubSprite.attach(rat);
		return rat;
	}

	@Test
	void anAllyOnTheTargetCellIsNotTerrified() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 4 * level.width() + 4;
		Rat ally = ratAt(cell, Char.Alignment.ALLY);

		new StoneOfFear().activate(cell);

		assertNull(ally.buff(Terror.class), "allies must not be terrified by a stone of fear");
	}

	@Test
	void controlAnEnemyOnTheTargetCellIsStillTerrified() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 4 * level.width() + 4;
		Rat enemy = ratAt(cell, Char.Alignment.ENEMY);

		new StoneOfFear().activate(cell);

		assertNotNull(enemy.buff(Terror.class), "enemies are still terrified");
	}
}
