package com.qsr.customspd.test;

import com.qsr.customspd.scenes.CellSelector;
import com.qsr.customspd.scenes.GameScene;

import java.lang.reflect.Field;

/**
 * Puts an idle {@link CellSelector} (no listener) into GameScene, so code that calls
 * {@code GameScene.cancel()} or {@code GameScene.cancelCellSelector()} runs headless. A real
 * selector needs the scene's tilemap, so this one is allocated without running its constructor.
 *
 * Usage:
 *   IdleCellSelector.install();     // in @BeforeAll, after HeadlessGdx.boot()
 *   IdleCellSelector.uninstall();   // in @AfterAll
 */
public final class IdleCellSelector {

	private static Object previous;
	private static boolean installed = false;

	public static synchronized void install() {
		if (installed) return;
		try {
			Field field = selectorField();
			previous = field.get(null);
			field.set(null, allocate());
		} catch (Exception e) {
			throw setupFailed(e);
		}
		installed = true;
	}

	public static synchronized void uninstall() {
		if (!installed) return;
		try {
			selectorField().set(null, previous);
		} catch (Exception e) {
			throw setupFailed(e);
		}
		previous = null;
		installed = false;
	}

	private static Field selectorField() throws NoSuchFieldException {
		Field field = GameScene.class.getDeclaredField("cellSelector");
		field.setAccessible(true);
		return field;
	}

	private static CellSelector allocate() throws Exception {
		Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
		Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
		theUnsafe.setAccessible(true);
		return (CellSelector) unsafeClass.getMethod("allocateInstance", Class.class)
				.invoke(theUnsafe.get(null), CellSelector.class);
	}

	private static IllegalStateException setupFailed(Exception e) {
		return new IllegalStateException("could not swap GameScene.cellSelector for an idle one;"
				+ " if GameScene's field or the JDK changed, update IdleCellSelector", e);
	}

	private IdleCellSelector() {}
}
