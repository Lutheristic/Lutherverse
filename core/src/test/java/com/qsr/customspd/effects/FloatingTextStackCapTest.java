package com.qsr.customspd.effects;

import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Floating texts stacked on one tile are capped at 5 at a time (upstream 888477378): each push
 * shortens the remaining life of the texts it nudges upward by numBelow/5 of the lifespan, so the
 * sixth text up has none left, and update() now kills a text whose time is exactly 0 instead of
 * leaving it on screen forever. Drives FloatingText's private push() directly with text-less
 * instances (no font headless); they all sit at the same spot, so every push nudges the whole
 * stack, which is the spam case the cap exists for.
 */
class FloatingTextStackCapTest {

	private static final int KEY = 987654321; // a cell no level has, so no real stack is touched

	private final List<FloatingText> made = new ArrayList<>();
	private float prevElapsed;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveElapsed() {
		prevElapsed = Game.elapsed;
	}

	@AfterEach
	void cleanUp() throws ReflectiveOperationException {
		for (FloatingText txt : made) txt.kill();
		stacks().remove(KEY);
		Game.elapsed = prevElapsed;
	}

	@SuppressWarnings("unchecked")
	private static SparseArray<ArrayList<FloatingText>> stacks() throws ReflectiveOperationException {
		Field f = FloatingText.class.getDeclaredField("stacks");
		f.setAccessible(true);
		return (SparseArray<ArrayList<FloatingText>>) f.get(null);
	}

	private static float timeLeft(FloatingText txt) throws ReflectiveOperationException {
		Field f = FloatingText.class.getDeclaredField("timeLeft");
		f.setAccessible(true);
		return f.getFloat(txt);
	}

	/** What reset() does minus the text and icon: a fresh text with a full lifespan, then push(). */
	private FloatingText pushNew() throws ReflectiveOperationException {
		FloatingText txt = new FloatingText();
		made.add(txt);
		txt.setPos(0, 0);
		Field f = FloatingText.class.getDeclaredField("timeLeft");
		f.setAccessible(true);
		f.setFloat(txt, 1f);
		Method push = FloatingText.class.getDeclaredMethod("push", FloatingText.class, int.class);
		push.setAccessible(true);
		push.invoke(null, txt, KEY);
		return txt;
	}

	@Test
	void textsNudgedUpLoseLifeSoAtMostFiveShowPerTile() throws ReflectiveOperationException {
		for (int i = 0; i < 6; i++) pushNew();

		// oldest first: 5, 4, 3, 2, 1 and 0 texts below each one
		assertEquals(0f,   timeLeft(made.get(0)), 1e-6f, "the 6th text up has no life left");
		assertEquals(0.2f, timeLeft(made.get(1)), 1e-6f);
		assertEquals(0.4f, timeLeft(made.get(2)), 1e-6f);
		assertEquals(0.6f, timeLeft(made.get(3)), 1e-6f);
		assertEquals(0.8f, timeLeft(made.get(4)), 1e-6f);
		assertEquals(1f,   timeLeft(made.get(5)), 1e-6f, "the newest text keeps its full lifespan");
	}

	@Test
	void aTextWithNoLifeLeftIsRemovedOnItsNextUpdate() throws ReflectiveOperationException {
		for (int i = 0; i < 6; i++) pushNew();
		FloatingText oldest = made.get(0);
		assertTrue(oldest.alive);

		Game.elapsed = 0.016f;
		oldest.update();

		assertFalse(oldest.alive, "a text at exactly 0 time left must be killed, not left on screen");
		assertFalse(stacks().get(KEY).contains(oldest), "and it leaves the tile's stack");
	}
}
