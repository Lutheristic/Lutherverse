package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.effects.FloatingText;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Converting an enemy to an ally (AllyBuff.affectAndLoot, used by Corruption and Doom-style
 * conversions) awards the enemy's exp, and the floating text must carry the experience icon like
 * every other exp gain (upstream 18311e138).
 */
class AllyBuffExpIconPortTest {

	/** Records every status shown instead of drawing it. */
	private static final class RecordingSprite extends CharSprite {
		final List<Integer> icons = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			icons.add(icon);
			texts.add(text);
		}

		@Override
		public void add(State state) {}

		@Override
		public void remove(State state) {}

		@Override
		public Emitter emitter() {
			return new Emitter();
		}
	}

	private HeadlessRun run;
	private Hero hero;
	private RecordingSprite heroSprite;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231208L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		heroSprite = new RecordingSprite();
		hero.sprite = heroSprite;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private int freeCellNextToHero() {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) return cell;
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	@Test
	void corruptingAnEnemyShowsTheExperienceIcon() {
		Rat rat = new Rat();
		rat.pos = freeCellNextToHero();
		rat.sprite = new RecordingSprite();
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		assertTrue(hero.lvl <= rat.maxLvl, "test setup: the rat must still award exp");
		assertTrue(rat.EXP > 0, "test setup: the rat must carry exp");

		AllyBuff.affectAndLoot(rat, hero, Corruption.class);

		assertTrue(heroSprite.icons.contains(FloatingText.EXPERIENCE),
				"the exp gain must use the experience icon, got icons " + heroSprite.icons);
		assertTrue(heroSprite.texts.contains(Integer.toString(rat.EXP)),
				"and show the bare exp amount, got " + heroSprite.texts);
		assertEquals(1, heroSprite.icons.size(), "exactly one status for the exp gain");
	}
}
