package com.qsr.customspd;

import com.badlogic.gdx.files.FileHandle;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 46e30809c and fc6964c15: the startup temp file pass deletes zero length files left
 * by an interrupted save, and leaves files with content alone.
 */
class TempFileCleanupPortTest {

	private static final String DIR = "temp-cleanup-test";

	@BeforeEach
	void makeDir() {
		HeadlessGdx.boot();
		FileUtils.getFileHandle(DIR).deleteDirectory();
		FileUtils.getFileHandle(DIR).mkdirs();
	}

	@AfterEach
	void removeDir() {
		FileUtils.getFileHandle(DIR).deleteDirectory();
	}

	@Test
	void aZeroLengthFileIsDeletedAndOthersAreKept() {
		FileHandle empty = FileUtils.getFileHandle(DIR + "/game.dat");
		empty.writeBytes(new byte[0], false);
		FileHandle kept = FileUtils.getFileHandle(DIR + "/depth1.dat");
		kept.writeString("not a save, but not empty", false);
		assertTrue(empty.exists());
		assertEquals(0, empty.length(), "test setup: the leftover is empty");

		FileUtils.cleanTempFiles(DIR);

		assertFalse(empty.exists(), "the zero length leftover is gone");
		assertTrue(kept.exists(), "a file with content stays");
		assertEquals("not a save, but not empty", kept.readString());
	}
}
