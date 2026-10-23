package de.soderer.utilities.csv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.csv.CsvFormat.QuoteMode;

/**
 * Covers configuration checks of {@link CsvFormat}, the error cases and options of
 * {@link CsvReader} and the quoting, escaping and padding of {@link CsvWriter}.
 */
@SuppressWarnings("static-method")
class CsvCoverageTest {
	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static List<List<String>> readAll(final String csv, final CsvFormat csvFormat) throws Exception {
		try (CsvReader reader = new CsvReader(stream(csv), csvFormat)) {
			return reader.readAll();
		}
	}

	private static String write(final CsvFormat csvFormat, final List<List<? extends Object>> lines) throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (CsvWriter writer = new CsvWriter(output, StandardCharsets.UTF_8, csvFormat)) {
			writer.writeAll(lines);
		}
		return new String(output.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	void csvFormat_fullConstructorAndGetters() {
		final CsvFormat csvFormat = new CsvFormat(';', '\'', '\\', false, false, true, true, true, QuoteMode.QUOTE_ALL_DATA, "\r\n");
		assertEquals(';', csvFormat.getSeparator());
		assertEquals('\'', csvFormat.getStringQuote());
		assertEquals('\\', csvFormat.getStringQuoteEscapeCharacter());
		assertFalse(csvFormat.isLineBreakInDataAllowed());
		assertFalse(csvFormat.isEscapedStringQuoteInDataAllowed());
		assertTrue(csvFormat.isFillMissingTrailingColumnsWithNull());
		assertTrue(csvFormat.isRemoveSurplusEmptyTrailingColumns());
		assertTrue(csvFormat.isAlwaysTrim());
		assertEquals(QuoteMode.QUOTE_ALL_DATA, csvFormat.getQuoteMode());
		assertEquals("\r\n", csvFormat.getLineBreak());

		assertThrows(IllegalArgumentException.class, () -> new CsvFormat('\n', '"', '"', true, true, false, false, false, QuoteMode.QUOTE_IF_NEEDED, "\n"));
		assertThrows(IllegalArgumentException.class, () -> new CsvFormat(',', ',', '"', true, true, false, false, false, QuoteMode.QUOTE_IF_NEEDED, "\n"));
		assertThrows(IllegalArgumentException.class, () -> new CsvFormat(',', '"', '"', true, true, false, false, false, QuoteMode.QUOTE_IF_NEEDED, "x"));
		assertThrows(IllegalArgumentException.class, () -> new CsvFormat(',', '"', '"', true, true, false, false, false, null, "\n"));
	}

	@Test
	void csvFormat_fullConstructorWithoutQuoting() throws Exception {
		// Without quoting the stringquote character is irrelevant and may equal the separator, like with the setters
		final CsvFormat csvFormat = new CsvFormat('"', '"', '"', true, true, false, false, false, QuoteMode.NO_QUOTE, "\n");
		assertEquals('"', csvFormat.getSeparator());
		assertEquals(QuoteMode.NO_QUOTE, csvFormat.getQuoteMode());
		assertEquals(Arrays.asList("a", "b"), readAll("x\"y\na\"b\n", csvFormat).get(1));
		assertEquals("a\"b", CsvWriter.getCsvLine(csvFormat, "a", "b"));

		// Separator equal to stringquote is still invalid when quoting
		assertThrows(IllegalArgumentException.class, () -> new CsvFormat('"', '"', '"', true, true, false, false, false, QuoteMode.QUOTE_IF_NEEDED, "\n"));
		// The separator itself is always validated
		assertThrows(IllegalArgumentException.class, () -> new CsvFormat('\r', '"', '"', true, true, false, false, false, QuoteMode.NO_QUOTE, "\n"));
	}

	@Test
	void csvFormat_settersValidateAndChain() {
		final CsvFormat csvFormat = new CsvFormat();
		assertSame(csvFormat, csvFormat
				.withSeparator(';')
				.withStringQuote('\'')
				.withStringQuoteEscapeCharacter('\\')
				.withLineBreakInDataAllowed(false)
				.withEscapedStringQuoteInDataAllowed(false)
				.withFillMissingTrailingColumnsWithNull(true)
				.withRemoveSurplusEmptyTrailingColumns(true)
				.withAlwaysTrim(true)
				.withIgnoreEmptyLines(true)
				.withQuoteMode(QuoteMode.QUOTE_STRINGS)
				.withLineBreak("\r")
				.withHeaderInFirstLine(false)
				.withEscapeLineBreaks(true));
		assertTrue(csvFormat.isIgnoreEmptyLines());
		assertFalse(csvFormat.isHeaderInFirstLine());
		assertTrue(csvFormat.isEscapeLineBreaks());
		assertEquals("\r", csvFormat.getLineBreak());

		assertThrows(IllegalArgumentException.class, () -> csvFormat.setSeparator('\r'));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setSeparator('\''));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setStringQuote(';'));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setStringQuote('\n'));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setStringQuoteEscapeCharacter(';'));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setStringQuoteEscapeCharacter('\n'));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setQuoteMode(null));
		assertThrows(IllegalArgumentException.class, () -> csvFormat.setLineBreak(null));

		// Without stringquote the quote character may also be used as separator
		final CsvFormat noQuote = new CsvFormat().withStringQuote(null);
		assertEquals(QuoteMode.NO_QUOTE, noQuote.getQuoteMode());
		noQuote.setSeparator('"');
		assertEquals('"', noQuote.getSeparator());
		assertThrows(IllegalArgumentException.class, () -> noQuote.setQuoteMode(QuoteMode.QUOTE_IF_NEEDED));

		// Setting a stringquote resets the escape character and activates quoting
		final CsvFormat resetFormat = new CsvFormat().withStringQuote(null).withStringQuote('\'');
		assertEquals(QuoteMode.QUOTE_IF_NEEDED, resetFormat.getQuoteMode());
		assertEquals('\'', resetFormat.getStringQuoteEscapeCharacter());
	}

	@Test
	void quoteMode_getFromString() throws Exception {
		assertEquals(QuoteMode.QUOTE_ALL_DATA, QuoteMode.getFromString("quote_all_data"));
		assertEquals(QuoteMode.NO_QUOTE, QuoteMode.getFromString("NO_QUOTE"));
		assertThrows(Exception.class, () -> QuoteMode.getFromString("unknown"));
		assertThrows(Exception.class, () -> QuoteMode.getFromString(null));
	}

	@Test
	void csvDataException_constructors() {
		final Exception cause = new IllegalStateException("cause");
		assertEquals(3, new CsvDataException(3).getErrorLineNumber());
		assertEquals(4, new CsvDataException("message", 4).getErrorLineNumber());
		assertSame(cause, new CsvDataException("message", 5, cause).getCause());
		final CsvDataException withCause = new CsvDataException(6, cause);
		assertSame(cause, withCause.getCause());
		assertEquals(6, withCause.getErrorLineNumber());
	}

	@Test
	void reader_constructorsAndState() throws Exception {
		assertThrows(Exception.class, () -> new CsvReader(stream("a"), (CsvFormat) null));
		try (CsvReader reader = new CsvReader(stream("a,b\n1,2\n3,4\n"))) {
			assertThrows(Exception.class, () -> reader.setCsvFormat(null));
			final CsvFormat csvFormat = new CsvFormat();
			assertSame(reader, reader.withCsvFormat(csvFormat));
			assertSame(csvFormat, reader.getCsvFormat());
			assertEquals(Arrays.asList("a", "b"), reader.readNextCsvLine());
			assertEquals(1, reader.getReadCsvLines());
			assertThrows(IllegalStateException.class, reader::readAll);
			assertThrows(IllegalStateException.class, reader::getCsvLineCount);
		}
		try (CsvReader reader = new CsvReader(stream("a,b\n1,2\n3,4"), StandardCharsets.UTF_8)) {
			assertEquals(3, reader.getCsvLineCount());
		}
	}

	@Test
	void reader_parseCsvLineAndDuplicateHeaders() throws Exception {
		assertEquals(Arrays.asList("a", "b c", "d,e"), CsvReader.parseCsvLine("a,b c,\"d,e\""));
		assertThrows(Exception.class, () -> CsvReader.parseCsvLine(""));
		assertThrows(Exception.class, () -> CsvReader.parseCsvLine("a\nb"));

		assertNull(CsvReader.checkForDuplicateCsvHeader(Arrays.asList("a", "b", null, "", " ")));
		assertEquals("a", CsvReader.checkForDuplicateCsvHeader(Arrays.asList("a", "b", " a ")));
	}

	@Test
	void reader_columnCountHandling() throws Exception {
		final CsvFormat fill = new CsvFormat().withFillMissingTrailingColumnsWithNull(true);
		assertEquals(Arrays.asList("1", null, null), readAll("a,b,c\n1\n", fill).get(1));
		assertEquals(Arrays.asList("1", null, null), readAll("a,b,c\n1", fill).get(1));

		final CsvFormat removeSurplus = new CsvFormat().withRemoveSurplusEmptyTrailingColumns(true);
		assertEquals(Arrays.asList("1", "2"), readAll("a,b\n1,2,,\n", removeSurplus).get(1));
		assertEquals(Arrays.asList("1", "2"), readAll("a,b\n1,2,", removeSurplus).get(1));
		assertThrows(CsvDataException.class, () -> readAll("a,b\n1,2,x\n", removeSurplus));
		assertThrows(CsvDataException.class, () -> readAll("a,b\n1,2,x", removeSurplus));

		final CsvDataException exception = assertThrows(CsvDataException.class, () -> readAll("a,b\n1,2,3\n", new CsvFormat()));
		assertEquals(2, exception.getErrorLineNumber());
		assertThrows(CsvDataException.class, () -> readAll("a,b\n1", new CsvFormat()));
		assertThrows(CsvDataException.class, () -> readAll("a,b,c\n1\n", new CsvFormat()));
	}

	@Test
	void reader_linebreaksAndEmptyLines() throws Exception {
		assertEquals(3, readAll("a,b\r\n1,2\r3,4\n", new CsvFormat()).size());
		// An empty trailing value after a separator
		assertEquals(Arrays.asList("1", ""), readAll("a,b\n1,\n", new CsvFormat()).get(1));
		assertEquals(Arrays.asList("1", ""), readAll("a,b\n1,", new CsvFormat()).get(1));

		final CsvFormat ignoreEmpty = new CsvFormat().withIgnoreEmptyLines(true);
		assertEquals(2, readAll("a,b\n , \n1,2\n , ", ignoreEmpty).size());
		assertEquals(1, readAll("a\n\n\n", ignoreEmpty).size());
	}

	@Test
	void reader_quotedDataErrors() throws Exception {
		assertThrows(CsvDataException.class, () -> readAll("a\n\"line\nbreak\"\n", new CsvFormat().withLineBreakInDataAllowed(false)));
		assertEquals("line\nbreak", readAll("a\n\"line\r\nbreak\"\n", new CsvFormat()).get(1).get(0));
		assertThrows(CsvDataException.class, () -> readAll("a,b\n\"x\"y,z\n", new CsvFormat()));
		assertEquals(Arrays.asList("x", "z"), readAll("a,b\n  \"x\"  ,z\n", new CsvFormat()).get(1));
		assertThrows(IOException.class, () -> readAll("a\n\"never closed\n", new CsvFormat()));
		assertThrows(CsvDataException.class, () -> readAll("a\n\"say \"\"hi\"\"\"\n", new CsvFormat().withEscapedStringQuoteInDataAllowed(false)));
		assertEquals("say \"hi\"", readAll("a\n\"say \"\"hi\"\"\"\n", new CsvFormat()).get(1).get(0));
	}

	@Test
	void reader_options() throws Exception {
		assertEquals(Arrays.asList("x", "y"), readAll("a,b\n  x  , y \n", new CsvFormat().withAlwaysTrim(true)).get(1));

		// Without quoting, quote characters are plain data
		assertEquals(Arrays.asList("\"x", "y\""), readAll("a,b\n\"x,y\"\n", new CsvFormat().withQuoteMode(QuoteMode.NO_QUOTE)).get(1));

		// Separate escape character: an escaped stringquote and an escaped escape character
		final CsvFormat backslashEscape = new CsvFormat().withStringQuoteEscapeCharacter('\\');
		assertEquals(Arrays.asList("say \"hi\"", "C:\\dir\\", "a\\b"), readAll("a,b,c\n\"say \\\"hi\\\"\",\"C:\\\\dir\\\\\",\"a\\b\"\n", backslashEscape).get(1));

		// Escaped linebreaks
		final CsvFormat escapeLineBreaks = new CsvFormat().withEscapeLineBreaks(true);
		assertEquals("line\nbreak\ttab", readAll("a\nline\\nbreak\\ttab\n", escapeLineBreaks).get(1).get(0));
		assertThrows(CsvDataException.class, () -> readAll("a\ninvalid\\q\n", escapeLineBreaks));
		final CsvFormat escapeLineBreaksWithBackslash = new CsvFormat().withEscapeLineBreaks(true).withStringQuoteEscapeCharacter('\\');
		assertEquals("q\"uote\nx", readAll("a\n\"q\\\"uote\\nx\"\n", escapeLineBreaksWithBackslash).get(1).get(0));
	}

	@Test
	void writer_constructorsAndState() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new CsvWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8, null));
		assertThrows(IllegalArgumentException.class, () -> new CsvWriter(new ByteArrayOutputStream(), null, new CsvFormat()));
		assertThrows(IllegalArgumentException.class, () -> new CsvWriter(null, new CsvFormat()));

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (final CsvWriter writer = new CsvWriter(output)) {
			assertTrue(writer.getCsvFormat() != null);
			writer.flush();
			writer.writeValues("a", "b");
			writer.writeValues(Arrays.asList(1, 2));
			assertEquals(2, writer.getWrittenLines());
			assertThrows(CsvDataException.class, () -> writer.writeValues("only one"));
			assertThrows(CsvDataException.class, () -> writer.writeValues((List<Object>) null));
			writer.flush();
			assertEquals("a,b\n1,2\n", new String(output.toByteArray(), StandardCharsets.UTF_8));
			writer.close();
			assertThrows(IllegalStateException.class, () -> writer.writeValues("x", "y"));
		}

		final ByteArrayOutputStream latin1Output = new ByteArrayOutputStream();
		try (CsvWriter latin1Writer = new CsvWriter(latin1Output, StandardCharsets.ISO_8859_1)) {
			latin1Writer.writeValues("\u00e4");
		}
		assertArrayEquals(new byte[] { (byte) 0xE4, '\n' }, latin1Output.toByteArray());
	}

	@Test
	void writer_quoteModes() throws Exception {
		final List<List<? extends Object>> lines = new ArrayList<>();
		lines.add(Arrays.asList("text", Integer.valueOf(1), null, "a,b", "say \"hi\"", "line\nbreak"));

		assertEquals("text,1,,\"a,b\",\"say \"\"hi\"\"\",\"line\nbreak\"\n", write(new CsvFormat(), lines));
		assertEquals("\"text\",1,,\"a,b\",\"say \"\"hi\"\"\",\"line\nbreak\"\n", write(new CsvFormat().withQuoteMode(QuoteMode.QUOTE_STRINGS), lines));
		assertEquals("\"text\",\"1\",\"\",\"a,b\",\"say \"\"hi\"\"\",\"line\nbreak\"\n", write(new CsvFormat().withQuoteMode(QuoteMode.QUOTE_ALL_DATA), lines));

		final List<List<? extends Object>> plainLines = new ArrayList<>();
		plainLines.add(Arrays.asList("say \"hi\"", Integer.valueOf(2)));
		assertEquals("say \"hi\",2\n", write(new CsvFormat().withQuoteMode(QuoteMode.NO_QUOTE), plainLines));
		assertThrows(CsvDataException.class, () -> write(new CsvFormat().withQuoteMode(QuoteMode.NO_QUOTE), lines));

		// All written values can be read again
		assertEquals(Arrays.asList("text", "1", "", "a,b", "say \"hi\"", "line\nbreak"), readAll(write(new CsvFormat(), lines), new CsvFormat()).get(0));
	}

	@Test
	void writer_escaping() throws Exception {
		final List<List<? extends Object>> lines = new ArrayList<>();
		lines.add(Arrays.asList("say \"hi\"", "C:\\dir\\", "line\nbreak"));

		final CsvFormat backslashEscape = new CsvFormat().withStringQuoteEscapeCharacter('\\');
		final String backslashCsv = write(backslashEscape, lines);
		assertEquals("\"say \\\"hi\\\"\",C:\\dir\\,\"line\nbreak\"\n", backslashCsv);
		assertEquals(lines.get(0), readAll(backslashCsv, backslashEscape).get(0));

		final CsvFormat escapeLineBreaks = new CsvFormat().withEscapeLineBreaks(true);
		final String escapedCsv = write(escapeLineBreaks, lines);
		assertEquals("\"say \"\"hi\"\"\",C:\\\\dir\\\\,line\\nbreak\n", escapedCsv);
		assertEquals(lines.get(0), readAll(escapedCsv, escapeLineBreaks).get(0));

		final CsvFormat escapeLineBreaksWithBackslash = new CsvFormat().withEscapeLineBreaks(true).withStringQuoteEscapeCharacter('\\');
		final String escapedBackslashCsv = write(escapeLineBreaksWithBackslash, lines);
		assertEquals(lines.get(0), readAll(escapedBackslashCsv, escapeLineBreaksWithBackslash).get(0));
	}

	@Test
	void writer_paddingAndSizes() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (CsvWriter writer = new CsvWriter(output, new CsvFormat())) {
			writer.withMinimumColumnSizes(new int[] { 5, 5, 0 }).withColumnPaddings(new boolean[] { true });
			writer.writeValues("ab", "cd", "ef", "gh");
			assertArrayEquals(new int[] { 2, 5, 0 }, writer.calculateOutputSizesOfValues(Arrays.asList("ab", "a,b", null)));
			assertEquals(6, writer.calculateOutputSizesOfValue("\"\""));
		}
		assertEquals("ab   ,   cd,ef,gh\n", new String(output.toByteArray(), StandardCharsets.UTF_8));
		assertEquals(Arrays.asList("ab", "cd", "ef", "gh"), readAll("a,b,c,d\nab   ,   cd,ef,gh\n", new CsvFormat().withAlwaysTrim(true)).get(1));
	}

	@Test
	void writer_staticCsvLines() throws Exception {
		assertEquals("a,,\"b,c\",\"d\"\"e\"", CsvWriter.getCsvLine(',', '"', false, "a", null, "b,c", "d\"e"));
		assertEquals(",x", CsvWriter.getCsvLine(',', '"', false, Arrays.asList(null, "x")));
		assertEquals("a\\nb", CsvWriter.getCsvLine(';', null, true, "a\nb"));
		assertEquals("say \"hi\"", CsvWriter.getCsvLine(';', null, false, "say \"hi\""));
		assertThrows(IllegalArgumentException.class, () -> CsvWriter.getCsvLine(';', null, false, "a;b"));
		assertEquals("", CsvWriter.getCsvLine(',', '"', false, (Object[]) null));

		assertEquals("a;\"b;c\"", CsvWriter.getCsvLine(new CsvFormat().withSeparator(';').withLineBreak("\r\n"), "a", "b;c"));
		assertEquals("1,2", CsvWriter.getCsvLine(new CsvFormat(), Arrays.asList(1, 2)));
		assertThrows(IllegalArgumentException.class, () -> CsvWriter.getCsvLine((CsvFormat) null, Arrays.asList(1)));
		assertThrows(IllegalArgumentException.class, () -> CsvWriter.getCsvLine(new CsvFormat(), (List<Object>) null));
		assertThrows(IllegalArgumentException.class, () -> CsvWriter.getCsvLine(new CsvFormat(), (Object[]) null));
	}
}
