package com.qsr.customspd.actors.hero.spheregrid;

import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.windows.WndSphereGrid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sphere grid window shows each node's display name from the message bundle, not its enum name. */
class SphereNodeTitleTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void everyNodeHasADisplayName() {
		assertEquals("Attunement", SphereNode.ATTUNEMENT.title());
		assertEquals("Ember I", SphereNode.EMBER_I.title());
		assertEquals("Might III", SphereNode.MIGHT_III.title());
		assertEquals("Conflagration", SphereNode.CONFLAGRATION.title());
		assertEquals("Elemental Conflux", SphereNode.ELEMENTAL_CONFLUX.title());
		for (SphereNode node : SphereNode.values()) {
			String title = node.title();
			assertFalse(title.isEmpty() || title.contains("_") || title.contains("."), node + " title: " + title);
		}
	}

	private static String label(SphereNode node, SphereGrid grid, boolean activated, boolean unlocked) throws Exception {
		Method m = WndSphereGrid.class.getDeclaredMethod("nodeLabel",
				SphereNode.class, SphereGrid.class, boolean.class, boolean.class);
		m.setAccessible(true);
		return (String) m.invoke(null, node, grid, activated, unlocked);
	}

	@Test
	void gridWindowRowsUseDisplayNames() throws Exception {
		SphereGrid grid = new SphereGrid();
		for (SphereNode node : SphereNode.values()) {
			for (int state = 0; state < 3; state++) {
				String row = label(node, grid, state == 2, state > 0);
				assertTrue(row.contains(node.title()), row);
				for (SphereNode any : SphereNode.values()) {
					assertFalse(row.contains(any.name()), row);
				}
			}
		}
		assertTrue(label(SphereNode.ELEMENTAL_CONFLUX, grid, false, true).contains("Ember II, Frost II, Storm II"),
				label(SphereNode.ELEMENTAL_CONFLUX, grid, false, true));
	}
}
