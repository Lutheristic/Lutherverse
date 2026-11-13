package com.qsr.customspd.windows;

import com.qsr.customspd.items.EnergyCrystal;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.food.Food;
import com.qsr.customspd.items.food.Pasty;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The journal's item popup ends with how often the item was upgraded or used (upstream f6299e07c,
 * shown in CPDU's list popup rather than upstream's grid).
 */
class WndJournalUseCountTest {

	/** What the popup shows for a seen item that is not class armor. */
	private static String desc(Class<?> cls) throws Exception {
		Method m = Class.forName("com.qsr.customspd.windows.WndJournal$CatalogTab")
				.getDeclaredMethod("useCountText", Item.class, Class.class);
		m.setAccessible(true);
		Item item = (Item) Reflection.newInstance(cls);
		return item.info() + m.invoke(null, item, cls);
	}

	private static String plainDesc(Class<?> cls) {
		return ((Item) Reflection.newInstance(cls)).info();
	}

	private static void setUses(Class<?> cls, int uses) {
		Catalog.countUses(cls, uses - Catalog.useCount(cls));
	}

	@Test
	void artifactCountsReadAsUpgrades() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			run.enterFirstLevel();
			Class<?> artifact = Catalog.ARTIFACTS.items().iterator().next();
			int before = Catalog.useCount(artifact);
			try {
				setUses(artifact, 4);
				assertEquals(plainDesc(artifact) + "\n\nYou have upgraded this item a total of _4_ times.",
						desc(artifact));
			} finally {
				setUses(artifact, before);
			}
		}
	}

	@Test
	void theItemPopupEndsWithTheUseCount() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			run.enterFirstLevel();
			Class<?> weapon = Catalog.MELEE_WEAPONS.items().iterator().next();

			Map<Class<?>, Integer> before = new LinkedHashMap<>();
			for (Class<?> cls : Arrays.asList(weapon, Gold.class, EnergyCrystal.class, Food.class, Pasty.class)) {
				before.put(cls, Catalog.useCount(cls));
			}
			try {
				setUses(weapon, 3);
				setUses(Gold.class, 1234);
				setUses(EnergyCrystal.class, 7);
				setUses(Food.class, 2);
				setUses(Pasty.class, 1);

				assertEquals(plainDesc(weapon) + "\n\nYou have upgraded this item a total of _3_ times.",
						desc(weapon));
				assertEquals(plainDesc(Gold.class) + "\n\nYou have spent a total of _1,234_ gold.",
						desc(Gold.class));
				assertEquals(plainDesc(EnergyCrystal.class) + "\n\nYou have used a total of _7_ energy crystals.",
						desc(EnergyCrystal.class));
				assertEquals(plainDesc(Food.class) + "\n\nYou have used this item a total of _2_ times.",
						desc(Food.class));
				assertEquals(plainDesc(Pasty.class), desc(Pasty.class), "a single use shows no count");
			} finally {
				for (Map.Entry<Class<?>, Integer> e : before.entrySet()) setUses(e.getKey(), e.getValue());
			}
		}
	}
}
