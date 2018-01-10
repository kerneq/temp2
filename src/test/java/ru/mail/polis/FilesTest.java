package ru.mail.polis;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Files} facilities
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public class FilesTest {
    @Test
    public void createRemove() throws IOException {
        final File dir = Files.createTempDirectory();
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());

        final File data = new File(dir, "data");
        assertFalse(data.exists());
        assertTrue(data.createNewFile());
        assertTrue(data.isFile());

        Files.recursiveDelete(dir);
        assertFalse(dir.exists());
    }
}
