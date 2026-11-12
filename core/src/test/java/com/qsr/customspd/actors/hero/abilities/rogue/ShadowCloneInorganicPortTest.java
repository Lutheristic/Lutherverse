package com.qsr.customspd.actors.hero.abilities.rogue;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.blobs.ToxicGas;
import com.qsr.customspd.actors.buffs.Bleeding;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Poison;
import com.qsr.customspd.items.artifacts.DriedRose;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 71856bee8: the shadow clone is inorganic, so it cannot bleed and
 * is immune to toxic gas and poison. The rose ghost was already inorganic; its own toxic gas
 * entry was dropped as a duplicate, and it must stay immune without it.
 */
class ShadowCloneInorganicPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void theShadowCloneCannotBleedOrBePoisoned() {
		ShadowClone.ShadowAlly clone = new ShadowClone.ShadowAlly();

		Buff.affect(clone, Bleeding.class).set(5);
		Buff.affect(clone, Poison.class).set(5);
		assertNull(clone.buff(Bleeding.class), "bleeding does not attach to the clone");
		assertNull(clone.buff(Poison.class), "poison does not attach to the clone");
		assertTrue(clone.isImmune(ToxicGas.class), "toxic gas does not hurt the clone");
		assertTrue(Char.hasProp(clone, Char.Property.INORGANIC));
	}

	@Test
	void theRoseGhostIsStillImmuneToToxicGas() {
		DriedRose.GhostHero ghost = new DriedRose.GhostHero();

		assertTrue(ghost.isImmune(ToxicGas.class));
		assertTrue(ghost.isImmune(Poison.class));
		assertTrue(ghost.isImmune(Bleeding.class));
	}
}
