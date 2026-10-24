package com.qsr.customspd.items.remains;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.effects.FloatingText;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The Warrior's SealShard remains item shields the hero for 10% of max HP and, since upstream
 * 34b5035e7 (v2.3.0 "floating text + icons for loads more heal/shield cases"), shows that amount
 * as positive floating text with the shielding icon. The sprite is a recording stub: there is no
 * GL context headless, and the call to showStatusWithIcon is the contract under test.
 */
class SealShardShieldTextTest {

	/** Records the floating text instead of drawing it. */
	private static final class RecordingSprite extends CharSprite {
		int color = Integer.MIN_VALUE;
		String text;
		int icon = Integer.MIN_VALUE;

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			this.color = color;
			this.text = text;
			this.icon = icon;
		}
	}

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveStatics() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void restoreStatics() {
		Dungeon.hero = prevHero;
	}

	@Test
	void shieldAmountIsShownWithTheShieldingIcon() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.HT = hero.HP = 120;
		RecordingSprite sprite = new RecordingSprite();
		hero.sprite = sprite;

		new SealShard().doEffect(hero);

		Barrier barrier = hero.buff(Barrier.class);
		assertNotNull(barrier, "the seal shard must shield the hero");
		assertEquals(12, barrier.shielding(), "a tenth of max HP");
		assertEquals(FloatingText.SHIELDING, sprite.icon, "the shield gain must show the shielding icon");
		assertEquals("12", sprite.text);
		assertEquals(CharSprite.POSITIVE, sprite.color);
	}
}
