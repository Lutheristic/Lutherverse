package com.qsr.customspd.effects;

import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression test for the Surprise half of upstream SPD f804ec36a (v2.1.4): "fixed a couple
 * rare crash bugs".
 *
 * Surprise.hit(Char) dereferenced ch.sprite unconditionally, so a char with no sprite (not yet
 * added to the scene, or already removed) crashed the game. It now does nothing for such a char.
 * The other half of that commit (StoneOfDisarming's comparator) has no deterministic headless
 * repro: it only throws for large candidate lists and unlucky tie orders.
 */
class SurpriseNullSpritePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void hittingACharWithNoSpriteIsANoOp() {
		Rat rat = new Rat();
		assertNull(rat.sprite);

		assertDoesNotThrow(() -> Surprise.hit(rat));
		assertDoesNotThrow(() -> Surprise.hit(rat, 45f));
	}

	@Test
	void controlASpriteWithNoParentIsStillANoOp() {
		Rat rat = new Rat();
		StubSprite sprite = StubSprite.attach(rat);
		sprite.parent = null;

		assertDoesNotThrow(() -> Surprise.hit(rat));
	}
}
