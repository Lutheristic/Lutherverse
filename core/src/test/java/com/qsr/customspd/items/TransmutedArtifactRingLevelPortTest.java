package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.artifacts.Artifact;
import com.qsr.customspd.items.artifacts.HornOfPlenty;
import com.qsr.customspd.items.rings.Ring;
import com.qsr.customspd.items.scrolls.ScrollOfTransmutation;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD d3ecf68a6 (v2.5.0): "rings transmuted from artifacts can
 * now be up to +2".
 *
 * When every artifact has been generated, transmuting one yields a random ring instead. The ring
 * used to be forced to +0, throwing away the artifact's upgrades. It now gets +1 for an artifact
 * at 5-9 visible upgrades and +2 for a fully upgraded one (10). Visible upgrades are the artifact's
 * level scaled to a 0-10 range, so HornOfPlenty (level cap 10) maps level 1:1.
 *
 * The scenario empties the artifact deck so Generator.randomArtifact() returns null.
 */
class TransmutedArtifactRingLevelPortTest {

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
		Dungeon.hero = new Hero();
		Generator.fullReset();
		java.util.Arrays.fill(Generator.Category.ARTIFACT.probs, 0f);
	}

	@AfterEach
	void tearDown() {
		Generator.fullReset();
		Dungeon.hero = prevHero;
	}

	private Item transmuteArtifactAtLevel(int level) {
		HornOfPlenty horn = new HornOfPlenty();
		horn.level(level);
		horn.levelKnown = true;
		assertEquals(level, horn.visiblyUpgraded(), "fixture: visible upgrades match the level");
		return ScrollOfTransmutation.changeItem(horn);
	}

	@Test
	void aFullyUpgradedArtifactBecomesAPlusTwoRing() {
		Item result = transmuteArtifactAtLevel(10);

		assertTrue(result instanceof Ring, "with no artifacts left the result is a ring");
		assertEquals(2, result.level());
	}

	@Test
	void aHalfUpgradedArtifactBecomesAPlusOneRing() {
		Item result = transmuteArtifactAtLevel(5);

		assertTrue(result instanceof Ring);
		assertEquals(1, result.level());
	}

	@Test
	void controlALightlyUpgradedArtifactStillBecomesAPlusZeroRing() {
		Item result = transmuteArtifactAtLevel(4);

		assertTrue(result instanceof Ring);
		assertEquals(0, result.level());
	}
}
