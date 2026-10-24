package com.qsr.customspd.actors.hero.abilities.duelist;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.Talent;
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

/**
 * Upstream 34b5035e7 (Challenge hunk): the Invigorating Victory heal, paid out when the duel
 * ends with the opponent dead, floats the bare amount with the healing icon instead of the old
 * "+%dHP" message borrowed from the dewdrop.
 */
class ChallengeHealIconPortTest {

	/** Records every status shown (icon and text) instead of drawing it. */
	private static final class RecordingSprite extends CharSprite {
		final List<Integer> icons = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			icons.add(icon);
			texts.add(text);
		}

		// no scene headless: buff states and heal bursts have nothing to draw on
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
		run = HeadlessRun.start(20231204L);
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
	void winningADuelWithInvigoratingVictoryFloatsTheHealWithTheHealingIcon() {
		hero.talents.get(0).put(Talent.INVIGORATING_VICTORY, 2); // pointsInTalent scans every tier
		hero.HP = 1;

		Rat rat = new Rat();
		rat.pos = freeCellNextToHero();
		rat.sprite = new RecordingSprite();
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		Challenge.DuelParticipant duel = Buff.affect(rat, Challenge.DuelParticipant.class);

		rat.HP = 0; // the opponent died: the duel ends with the hero the victor
		duel.detach();

		// no damage was taken in the duel, so the heal is the flat 5 per talent point
		assertEquals(1 + 10, hero.HP, "the talent heals the hero");
		assertEquals(1, heroSprite.icons.size(), "exactly one status for the heal; got " + heroSprite.texts);
		assertEquals(FloatingText.HEALING, (int) heroSprite.icons.get(0));
		assertEquals("10", heroSprite.texts.get(0), "the bare heal amount, no longer \"+%dHP\"");
	}
}
