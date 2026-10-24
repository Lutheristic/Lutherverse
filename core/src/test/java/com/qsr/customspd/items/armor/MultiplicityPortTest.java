package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.PinCushion;
import com.qsr.customspd.actors.mobs.DwarfKing;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.curses.Multiplicity;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression for upstream 06d3e6981 (v2.2.0, "fixed exploits where multiplicity could skip
 * DK's 2nd phase").
 *
 * Multiplicity duplicates its attacker by round-tripping it through a Bundle, which copies the
 * attacker's buffs. A {@link DwarfKing.KingDamager} on a minion is a pending hit on the king
 * that fires when the buff detaches, so a duplicated one was a second pending hit and let the
 * glyph skip the king's second phase. The fix strips it (and, as before, PinCushion) from the
 * copy.
 *
 * The same upstream diff also changed the proc chance from 1/20 to 1/2; v2.5.4 has 1/20 again,
 * so that hunk is not ported and the test loops a seeded generator until the 1/20 roll fires.
 */
class MultiplicityPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	@Test
	void theDuplicateDoesNotCopyKingDamagerOrPinCushion() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int center = 4 * level.width() + 4;

		// a non-hero defender keeps Multiplicity off its MirrorImage branch (hero-only)
		Rat defender = HeadlessLevel.at(new Rat(), center);
		StubSprite.attach(defender);
		Rat attacker = HeadlessLevel.at(new Rat(), center + 1);
		StubSprite.attach(attacker);
		Buff.affect(attacker, DwarfKing.KingDamager.class);
		Buff.affect(attacker, PinCushion.class);
		assertNotNull(attacker.buff(DwarfKing.KingDamager.class), "fixture: the attacker carries the link");
		assertNotNull(attacker.buff(PinCushion.class), "fixture: the attacker carries the cushion");

		Multiplicity glyph = new Multiplicity();
		Mob duplicate = null;
		Random.pushGenerator(7);
		try {
			for (int i = 0; i < 2000 && duplicate == null; i++) {
				int before = level.mobs.size();
				try {
					glyph.proc(new ClothArmor(), attacker, defender, 0);
				} catch (NullPointerException expected) {
					// After GameScene.add(m) the glyph plays ScrollOfTeleportation.appear on the new
					// mob, whose sprite only exists in a real scene. The copy is already built and
					// registered with the level by then, which is all this test inspects.
				}
				if (level.mobs.size() > before) {
					List<Mob> added = new ArrayList<>(level.mobs);
					duplicate = added.get(added.size() - 1);
				}
			}
		} finally {
			Random.popGenerator();
		}

		assertNotNull(duplicate, "Multiplicity must proc within 2000 seeded rolls at 1/20");
		assertEquals(Rat.class, duplicate.getClass(), "fixture: it duplicated the attacker");
		assertNull(duplicate.buff(DwarfKing.KingDamager.class),
				"upstream 06d3e6981: the copy must not carry the original's pending damage to the king");
		assertNull(duplicate.buff(PinCushion.class), "stuck projectiles are not duplicated");
		assertNotNull(attacker.buff(DwarfKing.KingDamager.class), "the original keeps its link");
	}
}
