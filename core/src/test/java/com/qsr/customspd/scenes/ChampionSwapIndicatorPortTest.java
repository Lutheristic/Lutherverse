package com.qsr.customspd.scenes;

import com.qsr.customspd.Statistics;
import com.qsr.customspd.items.weapon.melee.MeleeWeapon;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.ui.ActionIndicator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream bee976798 (v2.4.2): the Champion's weapon swap tag shows the weapons, so it has to be
 * redrawn whenever the item displays are. GameScene.update refreshes the action indicator along
 * with the quickslots while the swap is the current action, in place of the refresh calls in the
 * weapon equip and unequip code, which missed some ways the weapons can change.
 *
 * The scene and the indicator are allocated without their constructors, which build textures; the
 * part of update() this needs runs before update() checks for a live scene.
 */
class ChampionSwapIndicatorPortTest {

	private ActionIndicator.Action previousAction;
	private ActionIndicator previousInstance;
	private float previousDuration;
	private ActionIndicator indicator;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		previousAction = ActionIndicator.action;
		previousInstance = ActionIndicator.instance;
		previousDuration = Statistics.duration;
		//past the first turns, so refreshing the quickslots does not touch the waterskin setting
		Statistics.duration = 1000;
		indicator = allocate(ActionIndicator.class);
		ActionIndicator.instance = indicator;
	}

	@AfterEach
	void tearDown() {
		ActionIndicator.action = previousAction;
		ActionIndicator.instance = previousInstance;
		Statistics.duration = previousDuration;
		GameScene.updateItemDisplays = false;
	}

	@SuppressWarnings("unchecked")
	private static <T> T allocate(Class<T> type) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return (T) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
	}

	private boolean needsRefresh() throws Exception {
		Field f = ActionIndicator.class.getDeclaredField("needsRefresh");
		f.setAccessible(true);
		return f.getBoolean(indicator);
	}

	private static void updateWithItemDisplaysChanged() throws Exception {
		GameScene.updateItemDisplays = true;
		allocate(GameScene.class).update();
		assertFalse(GameScene.updateItemDisplays, "fixture: the item displays were updated");
	}

	@Test
	void updatingItemDisplaysRefreshesTheChampionSwapTag() throws Exception {
		ActionIndicator.action = new MeleeWeapon.Charger();

		updateWithItemDisplaysChanged();

		assertTrue(needsRefresh(), "the swap tag is redrawn with the item displays");
	}

	@Test
	void otherActionsAreLeftAlone() throws Exception {
		ActionIndicator.action = new ActionIndicator.Action() {
			@Override public String actionName() { return "probe"; }
			@Override public int indicatorColor() { return 0; }
			@Override public void doAction() {}
		};

		updateWithItemDisplaysChanged();

		assertFalse(needsRefresh());
	}
}
