package com.qsr.customspd;

import com.badlogic.gdx.Input;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Port regression for upstream c03acdc30: when loading saved key bindings, a custom key
 * removed itself from the defaults instead of the action's own default key. That default
 * was then merged back in as the action's next binding, pushing its real next default out.
 * Covers the keyboard and the controller blocks of SPDAction.loadBindings.
 */
class SPDActionBindingsPortTest {

	private static final String BINDINGS_FILE = "keybinds.dat";

	private LinkedHashMap<Integer, GameAction> oldBindings;
	private LinkedHashMap<Integer, GameAction> oldControllerBindings;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		oldBindings = KeyBindings.getAllBindings();
		oldControllerBindings = KeyBindings.getAllControllerBindings();
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		KeyBindings.setAllControllerBindings(SPDAction.getControllerDefaults());
	}

	@AfterEach
	void tearDown() {
		FileUtils.deleteFile(BINDINGS_FILE);
		KeyBindings.setAllBindings(oldBindings);
		KeyBindings.setAllControllerBindings(oldControllerBindings);
	}

	/** The bindings with one key swapped for another, keeping each action's key order. */
	private static LinkedHashMap<Integer, GameAction> rebound(LinkedHashMap<Integer, GameAction> bindings, int oldKey, int newKey) {
		LinkedHashMap<Integer, GameAction> result = new LinkedHashMap<>();
		for (Map.Entry<Integer, GameAction> e : bindings.entrySet()) {
			result.put(e.getKey() == oldKey ? newKey : e.getKey(), e.getValue());
		}
		return result;
	}

	/** Saves the current bindings, then loads them back the way a game start does. */
	private static void saveAndReload() {
		SPDAction.saveBindings();
		KeyBindings.setAllBindings(new LinkedHashMap<Integer, GameAction>());
		KeyBindings.setAllControllerBindings(new LinkedHashMap<Integer, GameAction>());
		SPDAction.loadBindings();
	}

	@Test
	void rebindingTheFirstKeyKeepsTheOldDefaultOut() {
		KeyBindings.setAllBindings(rebound(SPDAction.getDefaults(), Input.Keys.W, Input.Keys.K));
		saveAndReload();

		assertEquals(Arrays.asList(Input.Keys.K, Input.Keys.UP, Input.Keys.NUMPAD_8),
				KeyBindings.getKeyboardKeysForAction(SPDAction.N));
		assertFalse(KeyBindings.getAllBindings().containsKey(Input.Keys.W),
				"the replaced default key stays unbound");
	}

	@Test
	void rebindingTheSecondKeyKeepsTheOldDefaultOut() {
		KeyBindings.setAllBindings(rebound(SPDAction.getDefaults(), Input.Keys.UP, Input.Keys.L));
		saveAndReload();

		assertEquals(Arrays.asList(Input.Keys.W, Input.Keys.L, Input.Keys.NUMPAD_8),
				KeyBindings.getKeyboardKeysForAction(SPDAction.N));
		assertFalse(KeyBindings.getAllBindings().containsKey(Input.Keys.UP),
				"the replaced default key stays unbound");
	}

	@Test
	void untouchedBindingsLoadAsTheDefaults() {
		saveAndReload();

		assertEquals(SPDAction.getDefaults(), KeyBindings.getAllBindings());
		assertEquals(SPDAction.getControllerDefaults(), KeyBindings.getAllControllerBindings());
	}

	@Test
	void rebindingAControllerButtonKeepsTheOldDefaultOut() {
		KeyBindings.setAllControllerBindings(rebound(SPDAction.getControllerDefaults(), Input.Keys.BUTTON_B, Input.Keys.BUTTON_C));
		saveAndReload();

		assertEquals(Arrays.asList(Input.Keys.BUTTON_C),
				KeyBindings.getControllerKeysForAction(SPDAction.EXAMINE));
		assertFalse(KeyBindings.getAllControllerBindings().containsKey(Input.Keys.BUTTON_B),
				"the replaced default button stays unbound");
	}
}
