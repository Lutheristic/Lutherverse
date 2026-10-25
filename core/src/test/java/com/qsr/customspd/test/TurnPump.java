package com.qsr.customspd.test;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * F5 test foundation: drives the REAL actor schedule turn by turn, on the test thread. Each
 * {@link #step()} makes the same choice Actor.process makes: the earliest actor in Actor's
 * own (insertion-ordered) scheduling set, a higher actPriority winning a tie, the first one
 * met winning a full tie, sets Actor.now / current the way it does, and calls that actor's
 * act(). What it leaves out is only the threading: no wait for sprite motion, no
 * wait/notify hand-off to the render thread, no Game.switchingScene() check.
 *
 * The hero does not act(): Hero.act() waits for player input. On the hero's turn the pump runs
 * a {@link HeroTurn} instead; the default, {@link #REST}, updates the hero's view
 * (Dungeon.observe, as Hero.act does first) and rests one turn. Any char that comes up with no
 * sprite (one spawned mid-test) gets a {@link StubSprite} first.
 *
 * Needs a level and hero in place (HeadlessRun.enterFirstLevel, or HeadlessLevel + Actor.add);
 * all the state it touches is Actor's, which HeadlessRun's snapshot restores.
 *
 * Known headless gap: an attack on the hero reaches Hero.interrupt() (from Char.hit while
 * damageInterrupt is set, and from Hero.damage on any noticeable hit), which calls
 * GameScene.resetKeyHold(), which dereferences GameScene.cellSelector with no null guard. Until
 * that is guarded, pins should keep mob attacks off the hero (see MobAiTest).
 */
public final class TurnPump {

	/** What the hero does when the schedule reaches them. Must spend the hero's time. */
	public interface HeroTurn {
		void take(Hero hero);
	}

	public static final HeroTurn REST = hero -> {
		Dungeon.observe();
		hero.rest(false);
	};

	private static final Field CURRENT = field("current");
	private static final Field NOW = field("now");
	private static final Field TIME = field("time");
	private static final Field PRIORITY = field("actPriority");
	private static final Method ACT;
	static {
		try {
			ACT = Actor.class.getDeclaredMethod("act");
			ACT.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final HeroTurn heroTurn;
	private int steps = 0;

	public TurnPump() {
		this(REST);
	}

	public TurnPump(HeroTurn heroTurn) {
		this.heroTurn = heroTurn;
	}

	/** Actors acted so far. */
	public int steps() {
		return steps;
	}

	/** One actor's turn: the one Actor.process would pick next. Returns it (null if none). */
	public Actor step() {
		try {
			Actor current = null;
			float earliest = Float.MAX_VALUE;
			for (Actor actor : actors()) {
				float time = TIME.getFloat(actor);
				if (time < earliest
						|| time == earliest && (current == null || PRIORITY.getInt(actor) > PRIORITY.getInt(current))) {
					earliest = time;
					current = actor;
				}
			}
			CURRENT.set(null, current);
			if (current == null) return null;

			NOW.setFloat(null, earliest);
			if (current instanceof Char && ((Char) current).sprite == null) {
				StubSprite.attach((Char) current);
			}
			if (current == Dungeon.hero) {
				heroTurn.take(Dungeon.hero);
				if (TIME.getFloat(current) <= earliest) {
					throw new IllegalStateException("the HeroTurn did not spend the hero's time");
				}
			} else {
				ACT.invoke(current);
			}
			steps++;
			return current;
		} catch (InvocationTargetException e) {
			throw sneaky(e.getCause());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Steps until the condition holds (checked before each step). Fails after maxSteps. */
	public int runUntil(BooleanSupplier condition, int maxSteps) {
		int start = steps;
		while (!condition.getAsBoolean()) {
			if (steps - start >= maxSteps) {
				throw new AssertionError("condition not reached within " + maxSteps + " actor turns");
			}
			if (Dungeon.hero != null && !Dungeon.hero.isAlive()) {
				throw new AssertionError("the hero died after " + (steps - start) + " actor turns");
			}
			if (step() == null) {
				throw new AssertionError("no actor left to act");
			}
		}
		return steps - start;
	}

	/** Steps until the given actor has taken one turn (other actors act on the way). */
	public void untilActed(Actor actor, int maxSteps) {
		int start = steps;
		Actor acted;
		do {
			if (steps - start >= maxSteps) {
				throw new AssertionError(actor + " did not act within " + maxSteps + " actor turns");
			}
			acted = step();
			if (acted == null) throw new AssertionError("no actor left to act");
		} while (acted != actor);
	}

	private static Set<Actor> actors() {
		// Actor.all() copies into a LinkedHashSet, so ties still see insertion order.
		return Actor.all();
	}

	private static Field field(String name) {
		try {
			Field f = Actor.class.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> RuntimeException sneaky(Throwable t) throws T {
		throw (T) t;
	}
}
