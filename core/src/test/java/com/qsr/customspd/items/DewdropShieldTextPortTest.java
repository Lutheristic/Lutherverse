package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.effects.FloatingText;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Upstream b859b7a9e (Dewdrop hunk): with the Shielding Dew talent, a dew that tops up the
 * barrier floats the SHIELD amount. The v2.3.0 code it fixed handed the heal amount to the
 * shielding text, so a dew that only shielded (hero at full HP) floated "0" and a mixed dew
 * floated the heal amount twice.
 */
class DewdropShieldTextPortTest {

	/** Records every status shown (icon and text) instead of drawing it. */
	private static final class RecordingSprite extends CharSprite {
		final List<Integer> icons = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			icons.add(icon);
			texts.add(text);
		}
	}

	private HeadlessRun run;
	private Hero hero;
	private RecordingSprite sprite;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231210L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		sprite = new RecordingSprite();
		hero.sprite = sprite;
		hero.talents.get(0).put(Talent.SHIELDING_DEW, 3); // pointsInTalent scans every tier; 3 points = 60% HT shield cap
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void aShieldOnlyDewFloatsTheShieldAmountNotZero() {
		hero.HP = hero.HT;
		int heal = Math.round(hero.HT * 0.05f);

		Dewdrop.consumeDew(1, hero, false);

		assertNotNull(hero.buff(Barrier.class), "a full-health hero turns the dew into a barrier");
		assertEquals(heal, hero.buff(Barrier.class).shielding(), "test setup: the whole dew became shield");
		assertEquals(1, sprite.icons.size(), "one status, the shield; got " + sprite.texts);
		assertEquals(FloatingText.SHIELDING, (int) sprite.icons.get(0));
		assertEquals(Integer.toString(heal), sprite.texts.get(0), "the shield amount, not the zero heal");
	}

	@Test
	void aMixedDewFloatsHealAndShieldAsTheirOwnAmounts() {
		hero.HP = hero.HT - 1;

		Dewdrop.consumeDew(3, hero, false);

		int heal = Math.round(hero.HT * 0.05f * 3);
		assertEquals(2, sprite.icons.size(), "a heal and a shield status; got " + sprite.texts);
		assertEquals(FloatingText.HEALING, (int) sprite.icons.get(0));
		assertEquals("1", sprite.texts.get(0), "only 1 HP was missing");
		assertEquals(FloatingText.SHIELDING, (int) sprite.icons.get(1));
		assertEquals(Integer.toString(heal - 1), sprite.texts.get(1), "the overflow went to the barrier");
	}
}
