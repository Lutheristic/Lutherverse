package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.npcs.Ghost;
import com.qsr.customspd.actors.mobs.npcs.Sheep;
import com.qsr.customspd.items.wands.WandOfWarding;
import com.qsr.customspd.levels.rooms.special.SentryRoom;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bestiary marks NPCs, sheep and sentries seen when they act inside the hero's field of
 * view, without counting an encounter, and marks the ward sentry seen when a placed ward of
 * tier 3 or more is upgraded.
 */
class BestiarySightRecordingTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() throws ReflectiveOperationException {
		run = HeadlessRun.start(20241115L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
		clearBestiary();
		Bestiary.skipCountingEncounters = false;
	}

	@AfterEach
	void closeRun() throws ReflectiveOperationException {
		try {
			clearBestiary();
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static void clearBestiary() throws ReflectiveOperationException {
		Field seen = Bestiary.class.getDeclaredField("seen");
		Field counts = Bestiary.class.getDeclaredField("encounterCount");
		seen.setAccessible(true);
		counts.setAccessible(true);
		for (Bestiary cat : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
			for (Map.Entry<Class<?>, Integer> e : ((Map<Class<?>, Integer>) counts.get(cat)).entrySet()) {
				e.setValue(0);
			}
		}
	}

	private <T extends Mob> T nextToHero(T mob) {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				mob.pos = cell;
				Dungeon.level.mobs.add(mob);
				Actor.add(mob);
				return mob;
			}
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	private static void act(Actor actor) throws ReflectiveOperationException {
		Method act = Actor.class.getDeclaredMethod("act");
		act.setAccessible(true);
		act.invoke(actor);
	}

	private static void assertUnrecorded(Class<?> cls) {
		assertFalse(Bestiary.isSeen(cls), "test setup: " + cls.getSimpleName() + " starts unseen");
		assertEquals(0, Bestiary.encounterCount(cls), "test setup: " + cls.getSimpleName() + " starts uncounted");
	}

	private void assertSeenOnlyInView(Mob mob) throws ReflectiveOperationException {
		Class<?> cls = mob.getClass();
		assertUnrecorded(cls);

		Dungeon.level.heroFOV[mob.pos] = false;
		act(mob);
		assertFalse(Bestiary.isSeen(cls), cls.getSimpleName() + " acting out of view is not seen");

		Dungeon.level.heroFOV[mob.pos] = true;
		act(mob);
		assertTrue(Bestiary.isSeen(cls), cls.getSimpleName() + " acting in view is seen");
		assertEquals(0, Bestiary.encounterCount(cls), "seeing " + cls.getSimpleName() + " counts no encounter");
	}

	@Test
	void anNpcInViewIsSeen() throws ReflectiveOperationException {
		Ghost ghost = nextToHero(new Ghost());
		StubSprite.attach(ghost);
		assertSeenOnlyInView(ghost);
	}

	@Test
	void aSheepInViewIsSeen() throws ReflectiveOperationException {
		Sheep sheep = nextToHero(new Sheep());
		StubSprite.attach(sheep);
		sheep.lifespan = 10;
		assertSeenOnlyInView(sheep);
	}

	@Test
	void aSentryInViewIsSeen() throws ReflectiveOperationException {
		SentryRoom.Sentry sentry = nextToHero(new SentryRoom.Sentry());
		StubSprite.attach(sentry);
		//a room the hero is never inside, so the sentry does not start charging
		Field room = SentryRoom.Sentry.class.getDeclaredField("room");
		room.setAccessible(true);
		room.set(sentry, new EmptyRoom());
		assertSeenOnlyInView(sentry);
	}

	@Test
	void upgradingAPlacedWardFromTierThreeSeesTheWardSentry() {
		assertUnrecorded(WandOfWarding.Ward.WardSentry.class);
		//no sprite: upgrade casts it to WardSprite
		WandOfWarding.Ward ward = nextToHero(new WandOfWarding.Ward());
		ward.tier = 3;

		ward.upgrade(1);

		assertEquals(4, ward.tier, "test setup: the ward went up a tier");
		assertTrue(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class), "a placed ward upgraded from tier 3 sees the ward sentry");
		assertEquals(0, Bestiary.encounterCount(WandOfWarding.Ward.WardSentry.class), "seeing the ward sentry counts no encounter");
	}

	@Test
	void upgradingAWardThatWasNeverPlacedSeesNothing() {
		assertUnrecorded(WandOfWarding.Ward.WardSentry.class);
		WandOfWarding.Ward ward = new WandOfWarding.Ward();
		ward.tier = 3;

		ward.upgrade(1);

		assertEquals(4, ward.tier, "test setup: the ward went up a tier");
		assertFalse(Bestiary.isSeen(WandOfWarding.Ward.WardSentry.class), "a ward never added to the level does not see the ward sentry");
	}
}
