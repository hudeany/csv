package de.soderer.utilities.csv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.utilities.csv.CsvFormat.QuoteMode;

public class CsvReaderWriterTest {

	// =========================================================================
	// Helper methods
	// =========================================================================

	/** Write values with default CsvFormat and return the resulting CSV string. */
	private static String write(final CsvFormat format, final List<?>... rows) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (CsvWriter writer = new CsvWriter(out, StandardCharsets.UTF_8, format)) {
			for (final List<?> row : rows) {
				writer.writeValues(row);
			}
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	/** Read all rows from a CSV string using the given format. */
	private static List<List<String>> read(final CsvFormat format, final String csv) throws Exception {
		final InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
		try (CsvReader reader = new CsvReader(in, StandardCharsets.UTF_8, format)) {
			return reader.readAll();
		}
	}

	/** Round-trip: write rows, then read them back. */
	private static List<List<String>> roundTrip(final CsvFormat format, final List<?>... rows) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (CsvWriter writer = new CsvWriter(out, StandardCharsets.UTF_8, format)) {
			for (final List<?> row : rows) {
				writer.writeValues(row);
			}
		}
		final InputStream in = new ByteArrayInputStream(out.toByteArray());
		try (CsvReader reader = new CsvReader(in, StandardCharsets.UTF_8, format)) {
			return reader.readAll();
		}
	}

	// =========================================================================
	// CsvWriter – Basic output
	// =========================================================================

	@Nested
	@DisplayName("CsvWriter – Basic output")
	class WriterBasicTests {

		@Test
		@DisplayName("Simple row with default format produces comma-separated values")
		void simpleRow() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("a", "b", "c"));
			assertTrue(csv.startsWith("a,b,c"), "Expected 'a,b,c', got: " + csv);
		}

		@Test
		@DisplayName("Null value is written as empty field")
		void nullValueWrittenAsEmpty() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("x", null, "z"));
			assertTrue(csv.startsWith("x,,z"));
		}

		@Test
		@DisplayName("Integer and other non-String objects are written via toString()")
		void nonStringValues() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList(1, 2.5, true));
			assertTrue(csv.startsWith("1,2.5,true"));
		}

		@Test
		@DisplayName("Custom separator is used in output")
		void customSeparator() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setSeparator(';');
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("a", "b", "c"));
			assertTrue(csv.startsWith("a;b;c"));
		}

		@Test
		@DisplayName("Multiple rows are separated by the configured line break")
		void multipleRows() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("h1", "h2"), Arrays.asList("v1", "v2"));
			final String[] lines = csv.split("\n");
			assertEquals(2, lines.length);
			assertEquals("h1,h2", lines[0]);
			assertEquals("v1,v2", lines[1]);
		}

		@Test
		@DisplayName("getWrittenLines() returns correct count")
		void writtenLinesCount() throws Exception {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			try (CsvWriter writer = new CsvWriter(out, fmt)) {
				writer.writeValues("a", "b");
				writer.writeValues("c", "d");
				assertEquals(2, writer.getWrittenLines());
			}
		}

		@Test
		@DisplayName("Inconsistent column count throws CsvDataException")
		void inconsistentColumnCount() throws Exception {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			assertThrows(CsvDataException.class, () -> {
				try (CsvWriter writer = new CsvWriter(out, fmt)) {
					writer.writeValues("a", "b");
					writer.writeValues("a", "b", "c"); // wrong column count
				}
			});
		}

		@Test
		@DisplayName("writeValues(null) throws CsvDataException")
		void writeNullListThrows() {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final CsvFormat fmt = new CsvFormat();
			assertThrows(CsvDataException.class, () -> {
				try (CsvWriter writer = new CsvWriter(out, fmt)) {
					writer.writeValues((List<?>) null);
				}
			});
		}
	}

	// =========================================================================
	// CsvWriter – Quoting
	// =========================================================================

	@Nested
	@DisplayName("CsvWriter – Quoting")
	class WriterQuotingTests {

		@Test
		@DisplayName("QUOTE_IF_NEEDED: field containing separator is quoted")
		void quoteIfNeeded_separator() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("a,b", "c"));
			assertTrue(csv.startsWith("\"a,b\",c"));
		}

		@Test
		@DisplayName("QUOTE_IF_NEEDED: field containing quote char is escaped and quoted")
		void quoteIfNeeded_quoteChar() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("say \"hello\"", "x"));
			assertTrue(csv.startsWith("\"say \"\"hello\"\"\",x"));
		}

		@Test
		@DisplayName("QUOTE_ALL_DATA: every field is quoted")
		void quoteAllData() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setQuoteMode(QuoteMode.QUOTE_ALL_DATA);
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("a", "b"));
			assertTrue(csv.startsWith("\"a\",\"b\""));
		}

		@Test
		@DisplayName("QUOTE_STRINGS: only String instances are quoted, not numbers")
		void quoteStrings() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setQuoteMode(QuoteMode.QUOTE_STRINGS);
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("text", 42));
			assertTrue(csv.startsWith("\"text\",42"));
		}

		@Test
		@DisplayName("NO_QUOTE + field needing quotes throws CsvDataException")
		void noQuoteThrowsOnSpecialChars() {
			final CsvFormat fmt = new CsvFormat();
			fmt.setStringQuote(null); // sets NO_QUOTE mode
			fmt.setEscapeLineBreaks(false);
			assertThrows(CsvDataException.class, () -> write(fmt, Arrays.asList("a,b", "c")));
		}

		@Test
		@DisplayName("Backslash as stringQuoteEscapeCharacter is used correctly")
		void backslashEscapeCharacter() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setStringQuoteEscapeCharacter('\\');
			fmt.setEscapeLineBreaks(false);
			final String csv = write(fmt, Arrays.asList("say \"hello\""));
			// Should use \" instead of "" to escape the quote
			assertTrue(csv.contains("\\\""));
		}
	}

	// =========================================================================
	// CsvWriter – Line break escaping
	// =========================================================================

	@Nested
	@DisplayName("CsvWriter – Line break escaping")
	class WriterLineBreakEscapingTests {

		@Test
		@DisplayName("escapeLineBreaks=true: newline in value becomes \\n literal")
		void newlineEscaped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final String csv = write(fmt, Arrays.asList("line1\nline2"));
			assertTrue(csv.contains("\\n"), "Expected escaped \\n in: " + csv);
			assertFalse(csv.chars().filter(c -> c == '\n').count() > 1,
					"Should not contain a real newline within the value");
		}

		@Test
		@DisplayName("escapeLineBreaks=true: backslash in value becomes \\\\")
		void backslashEscaped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final String csv = write(fmt, Arrays.asList("C:\\Users\\test"));
			assertTrue(csv.contains("C:\\\\Users\\\\test"), "Expected doubled backslashes in: " + csv);
		}

		@Test
		@DisplayName("escapeLineBreaks=true: CRLF is normalized and escaped")
		void crlfEscaped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final String csv = write(fmt, Arrays.asList("a\r\nb"));
			assertTrue(csv.contains("\\n"), "Expected \\n escape for CRLF");
			assertFalse(csv.contains("\r\n"), "Should not contain literal CRLF within value");
		}
	}

	// =========================================================================
	// CsvWriter – getCsvLine() static helper
	// =========================================================================

	@Nested
	@DisplayName("CsvWriter – static getCsvLine()")
	class WriterGetCsvLineTests {

		@Test
		@DisplayName("getCsvLine produces correct comma-separated output")
		void basicCsvLine() {
			final String line = CsvWriter.getCsvLine(',', '"', false, "a", "b", "c");
			assertEquals("a,b,c", line);
		}

		@Test
		@DisplayName("getCsvLine quotes field containing separator")
		void csvLineWithSeparatorInValue() {
			final String line = CsvWriter.getCsvLine(',', '"', false, "a,b", "c");
			assertEquals("\"a,b\",c", line);
		}

		@Test
		@DisplayName("getCsvLine with escapeLineBreaks escapes newline")
		void csvLineEscapesNewline() {
			final String line = CsvWriter.getCsvLine(',', '"', true, "a\nb");
			assertTrue(line.contains("\\n"));
		}
	}

	// =========================================================================
	// CsvReader – Basic parsing
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – Basic parsing")
	class ReaderBasicTests {

		@Test
		@DisplayName("Simple CSV line is parsed into correct values")
		void simpleRead() throws Exception {
			final List<List<String>> result = read(new CsvFormat(), "a,b,c\n");
			assertEquals(1, result.size());
			assertEquals(Arrays.asList("a", "b", "c"), result.get(0));
		}

		@Test
		@DisplayName("Multiple lines are all returned")
		void multipleLines() throws Exception {
			final List<List<String>> result = read(new CsvFormat(), "a,b\nc,d\n");
			assertEquals(2, result.size());
		}

		@Test
		@DisplayName("Empty field is returned as empty string")
		void emptyField() throws Exception {
			final List<List<String>> result = read(new CsvFormat(), "a,,c\n");
			assertEquals("", result.get(0).get(1));
		}

		@Test
		@DisplayName("Trailing separator produces empty last field")
		void trailingSeparator() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			final List<List<String>> result = read(fmt, "a,b,\n");
			assertEquals(3, result.get(0).size());
			assertEquals("", result.get(0).get(2));
		}

		@Test
		@DisplayName("Custom separator is correctly parsed")
		void customSeparator() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setSeparator(';');
			final List<List<String>> result = read(fmt, "a;b;c\n");
			assertEquals(Arrays.asList("a", "b", "c"), result.get(0));
		}

		@Test
		@DisplayName("UTF-8 BOM is silently skipped")
		void utf8BomSkipped() throws Exception {
			// Prepend UTF-8 BOM (0xEF 0xBB 0xBF) — represented as char 65279 in Java
			final String csvWithBom = "\uFEFFa,b,c\n";
			final List<List<String>> result = read(new CsvFormat(), csvWithBom);
			assertEquals("a", result.get(0).get(0), "BOM should be stripped from first value");
		}

		@Test
		@DisplayName("Inconsistent column count throws CsvDataException")
		void inconsistentColumns() {
			assertThrows(CsvDataException.class, () -> read(new CsvFormat(), "a,b\nc,d,e\n"));
		}
	}

	// =========================================================================
	// CsvReader – Quoted values
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – Quoted values")
	class ReaderQuotingTests {

		@Test
		@DisplayName("Quoted field containing separator is read as single value")
		void quotedFieldWithSeparator() throws Exception {
			final List<List<String>> result = read(new CsvFormat(), "\"a,b\",c\n");
			assertEquals("a,b", result.get(0).get(0));
		}

		@Test
		@DisplayName("Doubled quote inside quoted field is unescaped")
		void doubledQuoteUnescaped() throws Exception {
			final List<List<String>> result = read(new CsvFormat(), "\"say \"\"hello\"\"\",x\n");
			assertEquals("say \"hello\"", result.get(0).get(0));
		}

		@Test
		@DisplayName("Quoted field containing newline is read as single multi-line value")
		void quotedFieldWithNewline() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final List<List<String>> result = read(fmt, "\"line1\nline2\",x\n");
			assertTrue(result.get(0).get(0).contains("\n"));
		}

		@Test
		@DisplayName("Newline inside quoted field is rejected when lineBreakInDataAllowed=false")
		void lineBreakInDataNotAllowed() {
			final CsvFormat fmt = new CsvFormat();
			fmt.setLineBreakInDataAllowed(false);
			fmt.setEscapeLineBreaks(false);
			assertThrows(CsvDataException.class, () -> read(fmt, "\"line1\nline2\",x\n"));
		}

		@Test
		@DisplayName("Unclosed quote at end of stream throws IOException")
		void unclosedQuoteThrows() {
			assertThrows(IOException.class, () -> read(new CsvFormat(), "\"unclosed"));
		}
	}

	// =========================================================================
	// CsvReader – escapeLineBreaks round-trip
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – escapeLineBreaks parsing")
	class ReaderEscapeLineBreaksTests {

		@Test
		@DisplayName("\\n literal in CSV is unescaped to real newline when escapeLineBreaks=true")
		void escapedNewlineUnescaped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final List<List<String>> result = read(fmt, "a\\nb,c\n");
			assertEquals("a\nb", result.get(0).get(0));
		}

		@Test
		@DisplayName("\\\\ in CSV is unescaped to single backslash when escapeLineBreaks=true")
		void escapedBackslashUnescaped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final List<List<String>> result = read(fmt, "C:\\\\Users,x\n");
			assertEquals("C:\\Users", result.get(0).get(0));
		}

		@Test
		@DisplayName("Invalid escape sequence throws CsvDataException")
		void invalidEscapeThrows() {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			assertThrows(CsvDataException.class, () -> read(fmt, "\\z,x\n"));
		}
	}

	// =========================================================================
	// CsvReader – alwaysTrim
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – alwaysTrim")
	class ReaderTrimTests {

		@Test
		@DisplayName("alwaysTrim=true strips leading and trailing whitespace")
		void trimEnabled() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setAlwaysTrim(true);
			final List<List<String>> result = read(fmt, "  hello  , world \n");
			assertEquals("hello", result.get(0).get(0));
			assertEquals("world", result.get(0).get(1));
		}

		@Test
		@DisplayName("alwaysTrim=false preserves whitespace")
		void trimDisabled() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setAlwaysTrim(false);
			final List<List<String>> result = read(fmt, " hello ,x\n");
			assertEquals(" hello ", result.get(0).get(0));
		}
	}

	// =========================================================================
	// CsvReader – fillMissingTrailingColumns / removeSurplusEmptyTrailingColumns
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – column count tolerance")
	class ReaderColumnToleranceTests {

		@Test
		@DisplayName("fillMissingTrailingColumnsWithNull pads short rows with null")
		void fillMissingColumns() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setFillMissingTrailingColumnsWithNull(true);
			final List<List<String>> result = read(fmt, "a,b,c\nx\n");
			assertEquals(3, result.get(1).size());
			assertNull(result.get(1).get(1));
			assertNull(result.get(1).get(2));
		}

		@Test
		@DisplayName("removeSurplusEmptyTrailingColumns strips empty trailing columns")
		void removeSurplusEmptyColumns() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setRemoveSurplusEmptyTrailingColumns(true);
			final List<List<String>> result = read(fmt, "a,b\nx,y,,\n");
			assertEquals(2, result.get(1).size());
		}
	}

	// =========================================================================
	// CsvReader – ignoreEmptyLines
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – ignoreEmptyLines")
	class ReaderIgnoreEmptyLinesTests {

		@Test
		@DisplayName("ignoreEmptyLines=true skips blank lines")
		void emptyLinesSkipped() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setIgnoreEmptyLines(true);
			final List<List<String>> result = read(fmt, "a,b\n\nc,d\n");
			assertEquals(2, result.size());
		}
	}

	// =========================================================================
	// CsvReader – getCsvLineCount()
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – getCsvLineCount()")
	class ReaderLineCountTests {

		@Test
		@DisplayName("getCsvLineCount() returns correct number of data lines")
		void lineCount() throws Exception {
			final String csv = "a,b\nc,d\ne,f\n";
			final InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
			try (CsvReader reader = new CsvReader(in, StandardCharsets.UTF_8, new CsvFormat())) {
				assertEquals(3, reader.getCsvLineCount());
			}
		}

		@Test
		@DisplayName("getCsvLineCount() throws if readNextCsvLine() was called first")
		void lineCountAfterSingleRead() throws Exception {
			final String csv = "a,b\nc,d\n";
			final InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
			try (CsvReader reader = new CsvReader(in, StandardCharsets.UTF_8, new CsvFormat())) {
				reader.readNextCsvLine();
				assertThrows(IllegalStateException.class, reader::getCsvLineCount);
			}
		}
	}

	// =========================================================================
	// CsvReader – parseCsvLine() static helper
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – static parseCsvLine()")
	class ReaderParseCsvLineTests {

		@Test
		@DisplayName("parseCsvLine parses a simple line correctly")
		void parseLine() throws Exception {
			final List<String> values = CsvReader.parseCsvLine("a,b,c");
			assertEquals(Arrays.asList("a", "b", "c"), values);
		}

		@Test
		@DisplayName("parseCsvLine with multiple CSV lines throws Exception")
		void parseMultipleLinesThrows() {
			assertThrows(Exception.class, () -> CsvReader.parseCsvLine("a,b\nc,d"));
		}
	}

	// =========================================================================
	// CsvReader – checkForDuplicateCsvHeader()
	// =========================================================================

	@Nested
	@DisplayName("CsvReader – checkForDuplicateCsvHeader()")
	class ReaderDuplicateHeaderTests {

		@Test
		@DisplayName("Returns null when no duplicates exist")
		void noDuplicates() {
			assertNull(CsvReader.checkForDuplicateCsvHeader(Arrays.asList("Name", "Age", "City")));
		}

		@Test
		@DisplayName("Returns the duplicate header name")
		void withDuplicate() {
			assertEquals("Name", CsvReader.checkForDuplicateCsvHeader(Arrays.asList("Name", "Age", "Name")));
		}

		@Test
		@DisplayName("Handles null entries in header list without throwing")
		void nullEntriesHandled() {
			assertNull(CsvReader.checkForDuplicateCsvHeader(Arrays.asList("Name", null, "Age")));
		}
	}

	// =========================================================================
	// Round-trip tests (Writer → Reader)
	// =========================================================================

	@Nested
	@DisplayName("Round-trip (Writer → Reader)")
	class RoundTripTests {

		@Test
		@DisplayName("Simple values survive a round-trip unchanged")
		void simpleRoundTrip() throws Exception {
			final List<List<String>> result = roundTrip(new CsvFormat(), Arrays.asList("hello", "world"),
					Arrays.asList("foo", "bar"));
			assertEquals("hello", result.get(0).get(0));
			assertEquals("world", result.get(0).get(1));
			assertEquals("foo", result.get(1).get(0));
		}

		@Test
		@DisplayName("Value with comma survives round-trip")
		void commaInValueRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("a,b", "c"));
			assertEquals("a,b", result.get(0).get(0));
		}

		@Test
		@DisplayName("Value with embedded newline survives round-trip with escapeLineBreaks=true")
		void newlineRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("line1\nline2", "x"));
			assertEquals("line1\nline2", result.get(0).get(0));
		}

		@Test
		@DisplayName("Backslash in value survives round-trip with escapeLineBreaks=true")
		void backslashRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(true);
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("C:\\Users\\test"));
			assertEquals("C:\\Users\\test", result.get(0).get(0));
		}

		@Test
		@DisplayName("Embedded quote survives round-trip")
		void quoteInValueRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setEscapeLineBreaks(false);
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("say \"hi\"", "x"));
			assertEquals("say \"hi\"", result.get(0).get(0));
		}

		@Test
		@DisplayName("Unicode (Umlauts) survive a round-trip")
		void umlautRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("äöüß", "café"));
			assertEquals("äöüß", result.get(0).get(0));
			assertEquals("café", result.get(0).get(1));
		}

		@Test
		@DisplayName("Empty string value survives round-trip")
		void emptyStringRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setQuoteMode(QuoteMode.QUOTE_ALL_DATA);
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("", "x"));
			assertEquals("", result.get(0).get(0));
		}

		@Test
		@DisplayName("Semicolon-separated round-trip")
		void semicolonSeparatorRoundTrip() throws Exception {
			final CsvFormat fmt = new CsvFormat();
			fmt.setSeparator(';');
			final List<List<String>> result = roundTrip(fmt, Arrays.asList("a", "b", "c"));
			assertEquals(Arrays.asList("a", "b", "c"), result.get(0));
		}
	}

	// =========================================================================
	// CsvFormat – validation
	// =========================================================================

	@Nested
	@DisplayName("CsvFormat – validation")
	class CsvFormatValidationTests {

		@Test
		@DisplayName("Separator equal to newline is rejected")
		void separatorNewlineRejected() {
			final CsvFormat fmt = new CsvFormat();
			assertThrows(IllegalArgumentException.class, () -> fmt.setSeparator('\n'));
		}

		@Test
		@DisplayName("StringQuote equal to separator is rejected")
		void quoteEqualsSeparatorRejected() {
			final CsvFormat fmt = new CsvFormat();
			assertThrows(IllegalArgumentException.class, () -> fmt.setStringQuote(','));
		}

		@Test
		@DisplayName("Invalid linebreak string is rejected")
		void invalidLinebreakRejected() {
			final CsvFormat fmt = new CsvFormat();
			assertThrows(IllegalArgumentException.class, () -> fmt.setLineBreak("X"));
		}

		@ParameterizedTest
		@DisplayName("Valid linebreak strings are accepted")
		@ValueSource(strings = { "\n", "\r", "\r\n" })
		void validLinebreaksAccepted(final String lb) {
			final CsvFormat fmt = new CsvFormat();
			assertDoesNotThrow(() -> fmt.setLineBreak(lb));
		}
	}
}
