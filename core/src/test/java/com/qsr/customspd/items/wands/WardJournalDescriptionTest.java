package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.journal.Bestiary;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal bestiary shows a ward and a sentry built only for display, with no wand behind
 * them. Upstream 52aa38780 (v2.5.0) gives such an instance generic text, and keeps the tiered
 * text with the live damage numbers for a ward that is actually on the level.
 */
class WardJournalDescriptionTest {

	private HeadlessRun run;
	private Map<Bestiary, Map<Class<?>, Boolean>> savedSeen;

	@BeforeEach
	void startRun() throws ReflectiveOperationException {
		run = HeadlessRun.start(20241115L);
		run.enterFirstLevel();
		savedSeen = snapshotSeen();
		clearSeen();
	}

	@AfterEach
	void closeRun() throws ReflectiveOperationException {
		try {
			restoreSeen(savedSeen);
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<Class<?>, Boolean> seenOf(Bestiary cat) throws ReflectiveOperationException {
		Field seen = Bestiary.class.getDeclaredField("seen");
		seen.setAccessible(true);
		return (Map<Class<?>, Boolean>) seen.get(cat);
	}

	private static Map<Bestiary, Map<Class<?>, Boolean>> snapshotSeen() throws ReflectiveOperationException {
		Map<Bestiary, Map<Class<?>, Boolean>> copy = new LinkedHashMap<>();
		for (Bestiary cat : Bestiary.values()) {
			copy.put(cat, new LinkedHashMap<>(seenOf(cat)));
		}
		return copy;
	}

	private static void restoreSeen(Map<Bestiary, Map<Class<?>, Boolean>> copy) throws ReflectiveOperationException {
		for (Bestiary cat : Bestiary.values()) {
			seenOf(cat).putAll(copy.get(cat));
		}
	}

	private static void clearSeen() throws ReflectiveOperationException {
		for (Bestiary cat : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : seenOf(cat).entrySet()) {
				e.setValue(false);
			}
		}
	}

	@Test
	void wardOffTheLevelDescribesAsAGenericWard() {
		WandOfWarding.Ward ward = new WandOfWarding.Ward();
		ward.upgrade(0);
		assertEquals(2, ward.tier);
		assertEquals(Messages.get(WandOfWarding.Ward.class, "desc_generic_ward"), ward.description());
	}

	@Test
	void sentryBuiltForDisplayDescribesAsAGenericSentryAndStaysUnseen() {
		assertFalse(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class));
		WandOfWarding.Ward.WardSentry sentry = new WandOfWarding.Ward.WardSentry();
		for (int i = 0; i < 4; i++) {
			sentry.upgrade(3);
		}
		assertEquals(5, sentry.tier);
		assertEquals(Messages.get(WandOfWarding.Ward.class, "desc_generic_sentry"), sentry.description());
		assertFalse(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class));
	}

	@Test
	void wardOnTheLevelDescribesWithItsDamageNumbers() {
		WandOfWarding.Ward ward = new WandOfWarding.Ward();
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = Dungeon.hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				ward.pos = cell;
				break;
			}
		}
		Actor.add(ward);
		try {
			String desc = ward.description();
			assertEquals(Messages.get(WandOfWarding.Ward.class, "desc_1", 3, 12, 1), desc);
			assertTrue(desc.contains("3-12"), desc);
		} finally {
			Actor.remove(ward);
		}
	}
}
