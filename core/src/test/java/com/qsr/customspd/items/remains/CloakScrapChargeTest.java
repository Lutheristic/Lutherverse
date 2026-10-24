package com.qsr.customspd.items.remains;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.artifacts.Artifact;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Rogue's CloakScrap remains item gives every uncursed artifact the hero has active 4 turns of
 * charge (upstream 3694bc666 routed this through ArtifactRecharge.chargeArtifacts). A cursed
 * artifact must not be charged.
 */
class CloakScrapChargeTest {

	/** Records the charge it is given instead of applying an artifact-specific rule. */
	static class RecordingArtifact extends Artifact {
		float charged;

		@Override
		protected ArtifactBuff passiveBuff() {
			return new ArtifactBuff();
		}

		@Override
		public void charge(Hero target, float amount) {
			charged += amount;
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
	void chargesUncursedArtifactsByFourTurnsAndSkipsCursedOnes() {
		Hero hero = new Hero();
		Dungeon.hero = hero;

		RecordingArtifact clean = new RecordingArtifact();
		clean.activate(hero);
		RecordingArtifact cursed = new RecordingArtifact();
		cursed.cursed = true;
		cursed.activate(hero);

		new CloakScrap().doEffect(hero);

		assertEquals(4f, clean.charged, 0.0001f, "an uncursed artifact gets 4 turns of charge");
		assertEquals(0f, cursed.charged, 0.0001f, "a cursed artifact must not be charged");
	}
}
