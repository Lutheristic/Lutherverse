package com.qsr.customspd.scenes;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Hunger;
import com.qsr.customspd.actors.buffs.WellFed;
import com.qsr.customspd.items.journal.Guidebook;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 75532abf0: the guidebook hint is logged by GameScene.flashForDocument, with a
 * mobile and a desktop line and a status popup, instead of at each call site.
 */
class GuidebookHintPortTest {

	private HeadlessRun run;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261115L);
		run.enterFirstLevel();
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		try {
			GLog.update.remove(logListener);
		} finally {
			run.close();
		}
	}

	@Test
	void hintKeysAreV254() {
		assertEquals("Your guidebook has advice for you! Tap the blinking journal button to read it.",
				Messages.get(Guidebook.class, "hint_mobile"));
		assertEquals("Your guidebook has advice for you! Select the blinking journal button (J) to read it.",
				Messages.get(Guidebook.class, "hint_desktop", "J"));
		assertEquals("Guidebook", Messages.get(Guidebook.class, "hint_status"));
		assertEquals("java.lang.object.hint", Messages.get(Guidebook.class, "hint"),
				"the old single hint key must be gone (Messages falls back to the bare key)");
		assertNotEquals("Your guidebook has advice for you!", Messages.get(Guidebook.class, "hint"),
				"the old single hint text must not resolve");
	}

	@Test
	void hungerDoesNotLogTheHintItself() {
		assertNull(GameScene.scene, "test setup: no game scene");
		assertFalse(Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_FOOD), "test setup: food page unread");

		Hunger hunger = Dungeon.hero.buff(Hunger.class);
		assertNotNull(hunger, "test setup: the hero has a Hunger buff");
		WellFed wellFed = Dungeon.hero.buff(WellFed.class);
		if (wellFed != null) wellFed.detach();

		// one step short of hungry, so the next act() crosses HUNGRY
		Bundle b = new Bundle();
		hunger.storeInBundle(b);
		b.put("level", Hunger.HUNGRY - 5f);
		hunger.restoreFromBundle(b);
		assertTrue(hunger.hunger() < Hunger.HUNGRY, "test setup: not yet hungry");

		log.clear();
		hunger.act();

		assertTrue(hunger.hunger() >= Hunger.HUNGRY, "test setup: act() crossed HUNGRY; log was " + log);
		for (String line : log) {
			assertFalse(line.contains("guidebook has advice"),
					"Hunger must leave the hint to flashForDocument, logged: " + line);
		}
	}
}
