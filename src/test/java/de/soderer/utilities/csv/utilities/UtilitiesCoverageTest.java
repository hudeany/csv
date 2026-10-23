package de.soderer.utilities.csv.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link Utilities} and {@link CountingInputStream}.
 */
@SuppressWarnings("static-method")
class UtilitiesCoverageTest {
	@Test
	void normalizeLinebreaks() {
		assertNull(Utilities.normalizeLinebreaks(null));
		assertEquals("a\nb\nc\nd", Utilities.normalizeLinebreaks("a\r\nb\rc\nd"));
	}

	@Test
	void escapeCSV() {
		assertNull(Utilities.escapeCSV(null));
		assertEquals("\\\\\\n\\r\\t\\b\\f\\u0007\\u007Fa\"'", Utilities.escapeCSV("\\\n\r\t\b\f\u0007\u007Fa\"'"));
	}

	@Test
	void unescapeCSV() throws Exception {
		assertNull(Utilities.unescapeCSV(null));
		assertEquals("\n\r\t\b\f \\'\"x", Utilities.unescapeCSV("\\n\\r\\t\\b\\f\\ \\\\\\'\\\"x"));
		assertEquals("A\u00e4\uD83D\uDE00", Utilities.unescapeCSV("\\x41\\u00E4\\U0001F600"));
		final String original = "tab\there\nnew line \\ back\u0001slash";
		assertEquals(original, Utilities.unescapeCSV(Utilities.escapeCSV(original)));

		assertThrows(Exception.class, () -> Utilities.unescapeCSV("end\\"));
		assertThrows(Exception.class, () -> Utilities.unescapeCSV("\\q"));
		assertThrows(Exception.class, () -> Utilities.unescapeCSV("\\xZ1"));
		assertThrows(Exception.class, () -> Utilities.unescapeCSV("\\u00"));
		assertThrows(Exception.class, () -> Utilities.unescapeCSV("\\U00110000"));
		assertThrows(Exception.class, () -> Utilities.unescapeCSV("\\U0000"));
	}

	@Test
	void countingInputStream() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new CountingInputStream(null));
		try (CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8)))) {
			assertEquals('0', stream.read());
			assertEquals(1, stream.getCount());
			final byte[] buffer = new byte[2];
			assertEquals(2, stream.read(buffer));
			assertEquals(2, stream.read(buffer, 0, 2));
			assertEquals(5, stream.getByteCount());
			stream.mark(100);
			assertEquals(2, stream.skip(2));
			assertEquals(7, stream.getCount());
			stream.reset();
			assertEquals(5, stream.getCount());
			assertEquals(5, stream.resetCount());
			assertEquals(0, stream.getCount());
			assertEquals(2, stream.skip(2));
			stream.reset();
			// The mark stays at the same stream position relative to the new counting start
			assertEquals(0, stream.getByteCount());
			assertEquals(0, stream.skip(0));
			final byte[] rest = new byte[10];
			assertEquals(5, stream.read(rest));
			assertEquals(-1, stream.read());
			assertEquals(-1, stream.read(rest));
			assertEquals(5, stream.resetByteCount());
		}
		try (CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))) {
			stream.read();
			// Reset without mark starts counting from zero
			stream.reset();
			assertEquals(0, stream.getCount());
		}
	}
}
