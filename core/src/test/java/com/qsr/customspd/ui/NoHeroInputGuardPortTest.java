package com.qsr.customspd.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.QuickSlot;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.sprites.NecromancerSprite;
import com.qsr.customspd.test.GameClasses;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StaticSnapshot;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream caaa82003: the toolbar's buttons and key actions read
 * Dungeon.hero.ready with no null check, so input arriving while there is no hero crashed,
 * and NecromancerSprite.finishSummoning crashed when no summon had been started.
 */
class NoHeroInputGuardPortTest {

	private StaticSnapshot saved;
	private Toolbar toolbar;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		saved = StaticSnapshot.capture(GameClasses.all());
		// empty quickslots and no pending aim, whatever earlier tests left behind: a filled
		// slot lays out text (no fonts headless) and an aimed slot skips the hero check
		Dungeon.quickslot = new QuickSlot();
		QuickSlotButton.targetingSlot = -1;

		LinkedHashMap<Integer, GameAction> keys = SPDAction.getDefaults();
		// the wait button's action has no default key
		keys.put(Input.Keys.K, SPDAction.WAIT);
		KeyBindings.setAllBindings(keys);
		KeyBindings.setAllControllerBindings(SPDAction.getControllerDefaults());

		Dungeon.hero = null;
		toolbar = new Toolbar();
	}

	@AfterEach
	void tearDown() {
		if (toolbar != null) toolbar.destroy();
		saved.restore();
	}

	private static int keyFor(GameAction action) {
		int key = KeyBindings.getFirstKeyForAction(action, false);
		if (key == 0) key = KeyBindings.getFirstKeyForAction(action, true);
		assertTrue(key != 0, "precondition: a key is bound to " + action.name());
		return key;
	}

	private static void send(KeyEvent... events) {
		for (KeyEvent event : events) KeyEvent.addKeyEvent(event);
		try {
			KeyEvent.processKeyEvents();
		} finally {
			// a throwing listener leaves the queue full; drop it so the next key starts clean
			clearPendingKeyEvents();
		}
	}

	private static void pressAndRelease(GameAction action) {
		int key = keyFor(action);
		send(new KeyEvent(key, true), new KeyEvent(key, false));
	}

	/** Holds the action's key down long enough for its button's long press to fire, then lets go. */
	private static void holdAndRelease(GameAction action) {
		int key = keyFor(action);
		Button.pressedButton = null;
		send(new KeyEvent(key, true));
		Button held = Button.pressedButton;
		try {
			assertNotNull(held, "precondition: a toolbar button took the " + action.name() + " key");
			assertSame(action, held.keyAction(), "precondition: the held button is the " + action.name() + " button");
			Game.elapsed = Button.longClick;
			held.update();
		} finally {
			send(new KeyEvent(key, false));
		}
	}

	private static void clearPendingKeyEvents() {
		try {
			Field f = KeyEvent.class.getDeclaredField("keyEvents");
			f.setAccessible(true);
			((List<?>) f.get(null)).clear();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void toolbarKeysDoNothingWithoutAHero() {
		GameAction[] actions = {
				SPDAction.WAIT,
				SPDAction.REST,
				SPDAction.WAIT_OR_PICKUP,
				SPDAction.EXAMINE,
				SPDAction.INVENTORY,
				SPDAction.QUICKSLOT_SELECTOR,
				SPDAction.INVENTORY_SELECTOR
		};
		List<Executable> presses = new ArrayList<>();
		for (GameAction action : actions) {
			presses.add(() -> assertDoesNotThrow(() -> pressAndRelease(action), action.name() + " with no hero"));
		}
		assertAll(presses);
		assertNull(Dungeon.hero);
	}

	@Test
	void holdingTheWaitKeysDoesNothingWithoutAHero() {
		// a finished long press vibrates, which asks the platform
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		GameAction[] actions = {
				SPDAction.WAIT,
				SPDAction.WAIT_OR_PICKUP
		};
		List<Executable> holds = new ArrayList<>();
		for (GameAction action : actions) {
			holds.add(() -> assertDoesNotThrow(() -> holdAndRelease(action), "holding " + action.name() + " with no hero"));
		}
		assertAll(holds);
		assertNull(Dungeon.hero);
	}

	@Test
	void finishingASummonThatNeverStartedDoesNotCrash() {
		NecromancerSprite sprite = new NecromancerSprite();
		assertDoesNotThrow(sprite::finishSummoning);
	}
}
