package com.qsr.customspd.test;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.Callback;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds back what game code passes to Game.runOnRenderThread, so a test can read it.
 *
 * Quest windows and floating text are built on the render thread, and neither can be built
 * without a GL context. The callback game code posts still carries the values it was given (a
 * window's text, a floating text's colour) as captured locals, which {@link #captured} reads.
 * Nothing posted while this is open ever runs. Close it to put the real Gdx.app back.
 *
 * Usage:
 *   try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
 *       ... game code that posts to the render thread ...
 *       String text = (String) RenderThreadCalls.captured(calls.callbacks().get(0), "text");
 *   }
 */
public final class RenderThreadCalls implements AutoCloseable {

	private final Application realApp;
	private final List<Runnable> posted = new ArrayList<>();

	private RenderThreadCalls() {
		realApp = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
				new Class<?>[]{Application.class}, (proxy, method, args) -> {
					if (method.getName().equals("postRunnable")) {
						posted.add((Runnable) args[0]);
						return null;
					}
					try {
						return method.invoke(realApp, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}

	/** Starts holding back render thread calls. */
	public static RenderThreadCalls capture() {
		return new RenderThreadCalls();
	}

	/** The callbacks passed to Game.runOnRenderThread since {@link #capture}, oldest first. */
	public List<Callback> callbacks() {
		List<Callback> result = new ArrayList<>();
		for (Runnable r : posted) {
			Callback c = wrapped(r);
			if (c != null) result.add(c);
		}
		return result;
	}

	/**
	 * The value of local {@code name} that an anonymous callback captured. This reads javac's
	 * {@code val$name} capture field, so renaming that local in game code breaks the test using it.
	 */
	public static Object captured(Callback callback, String name) {
		try {
			Field f = callback.getClass().getDeclaredField("val$" + name);
			f.setAccessible(true);
			return f.get(callback);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(callback.getClass().getName() + " captured no local named " + name
					+ ". If the game code still passes this value, the local was most likely renamed, or the"
					+ " compiler no longer names capture fields val$<local>.", e);
		}
	}

	@Override
	public void close() {
		Gdx.app = realApp;
	}

	/**
	 * Game.runOnRenderThread posts {@code c::call}; this is that c, read from the field the JDK's
	 * method reference class keeps it in. If the JDK stops keeping it in a field, callbacks() comes
	 * back empty.
	 */
	private static Callback wrapped(Runnable r) {
		for (Field f : r.getClass().getDeclaredFields()) {
			if (Callback.class.isAssignableFrom(f.getType())) {
				try {
					f.setAccessible(true);
					return (Callback) f.get(r);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError("could not read the callback posted in " + r, e);
				}
			}
		}
		return null;
	}
}
