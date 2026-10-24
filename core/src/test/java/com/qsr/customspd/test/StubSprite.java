package com.qsr.customspd.test;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * F5 test foundation: a no-op {@link CharSprite} for headless tests. Game logic calls its char's
 * sprite unguarded in places (Mob.act's showAlert/hideAlert, Char.attack's miss-branch
 * showStatus, Mob.notice, Char.die...), which NPEs when a test char has no sprite; real sprites
 * need a scene, textures and a GL context. This one draws nothing, touches no scene, and records
 * the calls a test may want to assert on (alerts, status texts, attacks).
 *
 * Animations complete instantly, the way CharSprite.onComplete(Animation) would at the end of
 * them: attack/operate run their callback, or with none ch.onAttackComplete()/onOperateComplete();
 * a jump runs its callback. Motion is instant and fires no onMotionComplete (Char's default does
 * nothing; Actor.process's sprite-motion wait is what that callback serves, and TurnPump has no
 * such wait). {@code visible} starts false, as for a char outside the hero's view, so
 * Mob.doAttack resolves the attack immediately; Char.move overwrites it from heroFOV like a
 * real sprite.
 *
 * Limits: this is a plain CharSprite, so code that casts its char's sprite to a specific class
 * (HeroSprite.updateArmor, a boss's own sprite) still fails and needs its own stub.
 */
public class StubSprite extends CharSprite {

	/** One showStatus / showStatusWithIcon call. */
	public static final class Status {
		public final int color;
		public final String text;

		Status(int color, String text) {
			this.color = color;
			this.text = text;
		}

		@Override
		public String toString() {
			return Integer.toHexString(color) + ":" + text;
		}
	}

	public final List<Status> statuses = new ArrayList<>();
	public final List<Integer> attacks = new ArrayList<>();
	public int alertsShown = 0;
	public int lostShown = 0;

	public StubSprite() {
		super();
		visible = false;
	}

	/** Gives the char a fresh stub sprite (replacing any it has) and returns it. */
	public static StubSprite attach(Char ch) {
		StubSprite sprite = new StubSprite();
		sprite.ch = ch;
		ch.sprite = sprite;
		return sprite;
	}

	/** Stubs the hero and every mob on the current level that has no sprite yet. */
	public static void attachAll() {
		if (Dungeon.hero != null && Dungeon.hero.sprite == null) attach(Dungeon.hero);
		if (Dungeon.level != null) {
			for (Mob m : Dungeon.level.mobs) {
				if (m.sprite == null) attach(m);
			}
		}
	}

	/** The stub on a char, failing loudly if it has none or a real sprite. */
	public static StubSprite of(Char ch) {
		if (!(ch.sprite instanceof StubSprite)) {
			throw new IllegalStateException(ch + " has no StubSprite: " + ch.sprite);
		}
		return (StubSprite) ch.sprite;
	}

	// ------------------------------------------------------------------ recorded calls

	@Override
	public void showStatus(int color, String text, Object... args) {
		showStatusWithIcon(color, text, -1 /* FloatingText.NO_ICON */, args);
	}

	@Override
	public void showStatusWithIcon(int color, String text, int icon, Object... args) {
		statuses.add(new Status(color, args.length > 0 ? Messages.format(text, args) : text));
	}

	@Override
	public void showAlert() {
		alertsShown++;
	}

	@Override
	public void showLost() {
		lostShown++;
	}

	@Override
	public synchronized void attack(int cell, Callback callback) {
		attacks.add(cell);
		if (callback != null) {
			callback.call();
		} else if (ch != null) {
			ch.onAttackComplete();
		}
	}

	@Override
	public synchronized void operate(int cell, Callback callback) {
		if (callback != null) {
			callback.call();
		} else if (ch != null) {
			ch.onOperateComplete();
		}
	}

	@Override
	public synchronized void zap(int cell, Callback callback) {
		if (callback != null) callback.call();
	}

	@Override
	public void jump(int from, int to, float height, float duration, Callback callback) {
		if (callback != null) callback.call();
	}

	// ------------------------------------------------------------------ rendering: no-ops

	@Override public void play(Animation anim) {}
	@Override public void link(Char ch) { this.ch = ch; ch.sprite = this; }
	@Override public void place(int cell) {}
	@Override public void idle() {}
	@Override public void move(int from, int to) {}
	@Override public void interruptMotion() {}
	@Override public void turnTo(int from, int to) {}
	@Override public void die() {}
	@Override public void burst(int color, int n) {}
	@Override public void bloodBurstA(PointF from, int damage) {}
	@Override public void flash() {}
	@Override public void add(State state) {}
	@Override public void remove(State state) {}
	@Override public void aura(int color) {}
	@Override public void clearAura() {}
	@Override public void update() {}
	@Override public void draw() {}
	@Override public void kill() {}
	@Override public void showSleep() {}
	@Override public void hideSleep() {}
	@Override public void hideAlert() {}
	@Override public void hideLost() {}
	@Override public void hideEmo() {}

	/** A detached emitter: callers can pour/burst into it (as with GameScene's) and nothing renders. */
	@Override public Emitter emitter() { return new Emitter(); }
	@Override public Emitter centerEmitter() { return new Emitter(); }
	@Override public Emitter bottomEmitter() { return new Emitter(); }
}
