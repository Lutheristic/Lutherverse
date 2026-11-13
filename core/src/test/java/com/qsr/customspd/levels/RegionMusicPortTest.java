package com.qsr.customspd.levels;

import com.qsr.customspd.Assets;
import com.qsr.customspd.actors.mobs.Goo;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.modding.CustomLevelLayout;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.audio.Music;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Upstream db782a5a9: each region's music list has six entries and adds the region's third
 * segment, on its regular floors and on its boss floor once the boss is beaten. Levels a pack
 * lays out itself keep playing the music the pack's layout names.
 */
class RegionMusicPortTest {

	private static final float[] CHANCES = {1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

	@BeforeEach
	void silence() {
		HeadlessGdx.boot();
		Music.INSTANCE.end();
		Music.INSTANCE.enable(false);
	}

	@AfterEach
	void restore() {
		Music.INSTANCE.end();
		Music.INSTANCE.enable(true);
	}

	@Test
	void regularFloorsPlayTheSixEntryRegionLists() {
		assertPlays(new SewerLevel(), Assets.Music.SEWERS_1, Assets.Music.SEWERS_2, Assets.Music.SEWERS_3);
		assertPlays(new PrisonLevel(), Assets.Music.PRISON_1, Assets.Music.PRISON_2, Assets.Music.PRISON_3);
		assertPlays(new CavesLevel(), Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_3);
		assertPlays(new CityLevel(), Assets.Music.CITY_1, Assets.Music.CITY_2, Assets.Music.CITY_3);
		assertPlays(new HallsLevel(), Assets.Music.HALLS_1, Assets.Music.HALLS_2, Assets.Music.HALLS_3);
	}

	@Test
	void bossFloorsPlayTheRegionListOnceTheBossIsBeaten() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20230903L)) {
			Level sewers = run.generate("5");
			for (Mob m : new ArrayList<>(sewers.mobs)) {
				if (m instanceof Goo) sewers.mobs.remove(m);
			}
			assertPlays(sewers, Assets.Music.SEWERS_1, Assets.Music.SEWERS_2, Assets.Music.SEWERS_3);

			Level prison = run.generate("10");
			Field state = PrisonBossLevel.class.getDeclaredField("state");
			state.setAccessible(true);
			state.set(prison, PrisonBossLevel.State.WON);
			assertPlays(prison, Assets.Music.PRISON_1, Assets.Music.PRISON_2, Assets.Music.PRISON_3);

			Level caves = run.generate("15");
			caves.map[14 + 13 * caves.width()] = Terrain.EMPTY;
			assertPlays(caves, Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_3);

			Level city = run.generate("20");
			city.map[7 + 25 * city.width()] = Terrain.DOOR;
			assertPlays(city, Assets.Music.CITY_1, Assets.Music.CITY_2, Assets.Music.CITY_3);

			Level halls = run.generate("25");
			halls.map[halls.exit()] = Terrain.EXIT;
			assertPlays(halls, Assets.Music.HALLS_1, Assets.Music.HALLS_2, Assets.Music.HALLS_3);
		}
	}

	@Test
	void aPackLaidOutLevelStillPlaysItsOwnMusic() throws Exception {
		CustomLevelLayout layout = Json.Default.decodeFromString(CustomLevelLayout.Companion.serializer(),
				"{\"width\": 3, \"height\": 3, \"region\": 1,"
						+ " \"music\": {\"tracks\": [\"music/theme_2.ogg\", \"music/caves_boss.ogg\"], \"probs\": [1.0, 0.5]},"
						+ " \"map\": [4, 4, 4, 4, 1, 4, 4, 4, 4]}");
		new CustomLevel(layout, "c1").playLevelMusic();

		assertArrayEquals(new String[]{"music/theme_2.ogg", "music/caves_boss.ogg"}, playingList());
		assertArrayEquals(new float[]{1f, 0.5f}, playingChances());
	}

	private static void assertPlays(Level level, String first, String second, String third) {
		Music.INSTANCE.end();
		level.playLevelMusic();
		String name = level.getClass().getSimpleName();
		assertNotNull(playingList(), name + " started its music");
		assertArrayEquals(new String[]{first, second, second, first, third, third}, playingList(), name + " music");
		assertArrayEquals(CHANCES, playingChances(), name + " chances");
		assertEquals(6, playingChances().length);
	}

	private static String[] playingList() {
		return (String[]) read("trackList");
	}

	private static float[] playingChances() {
		return (float[]) read("trackChances");
	}

	private static Object read(String field) {
		try {
			Field f = Music.class.getDeclaredField(field);
			f.setAccessible(true);
			return f.get(Music.INSTANCE);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
