package com.qsr.customspd.items.armor.glyphs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.effects.FloatingText;
import com.qsr.customspd.items.wands.WandOfFireblast;
import com.qsr.customspd.items.wands.WandOfLightning;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 2330217c0 (v2.4.0): the battlemage's fireblast and lightning on-hit effects are
 * magical damage, so Anti-Magic must resist them like the Blazing / Shocking enchantments they
 * extend. Their classes were private, so they could not be listed in AntiMagic.RESISTS.
 */
class AntiMagicOnHitPortTest {

	/** Records the floating-text icon Char.damage picks for the damage source. */
	private static final class RecordingSprite extends CharSprite {
		int icon = Integer.MIN_VALUE;

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			this.icon = icon;
		}
	}

	private com.qsr.customspd.actors.hero.Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveStatics() {
		prevHero = Dungeon.hero;
		Dungeon.hero = null;
	}

	@AfterEach
	void restoreStatics() {
		Dungeon.hero = prevHero;
	}

	private static int iconFor(Object src) {
		Rat rat = new Rat();
		rat.HP = rat.HT = 100000;
		RecordingSprite sprite = new RecordingSprite();
		rat.sprite = sprite;
		rat.damage(1, src);
		return sprite.icon;
	}

	@Test
	void battlemageFireAndLightningOnHitsAreResistedAsMagic() {
		assertTrue(AntiMagic.RESISTS.contains(WandOfFireblast.FireBlastOnHit.class),
				"the fireblast staff's on-hit must be in the Anti-Magic list");
		assertTrue(AntiMagic.RESISTS.contains(WandOfLightning.LightningOnHit.class),
				"the lightning staff's on-hit must be in the Anti-Magic list");
		assertEquals(FloatingText.MAGIC_DMG, iconFor(new WandOfFireblast.FireBlastOnHit()),
				"damage sourced from the fireblast on-hit is shown as magical damage");
		assertEquals(FloatingText.MAGIC_DMG, iconFor(new WandOfLightning.LightningOnHit()),
				"damage sourced from the lightning on-hit is shown as magical damage");
	}
}
