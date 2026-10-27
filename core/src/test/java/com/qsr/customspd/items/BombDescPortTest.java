package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.bombs.Bomb;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bomb.desc() already passes the explosion's damage range (upstream 853b719c6); the English text
 * now shows it. With no hero the range is the depth 1 one, 5-15.
 */
class BombDescPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void theBombDescriptionShowsItsDamage() {
		Hero prior = Dungeon.hero;
		Dungeon.hero = null;
		try {
			String desc = new Bomb().desc();
			assertTrue(desc.contains("deals _5-15 damage_ to anything adjacent to it"), desc);
		} finally {
			Dungeon.hero = prior;
		}
	}
}
