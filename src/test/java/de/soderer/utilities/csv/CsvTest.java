package de.soderer.utilities.csv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class CsvTest {
	@Test
	public void test1() {
		final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\r\n;\"\"56\"";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';');

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("def", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
			Assertions.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|", dataLines.get(1).get(0));
			Assertions.assertEquals("jkl", dataLines.get(1).get(1));
			Assertions.assertEquals("4\n\n;\"56", dataLines.get(1).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		final String csvData = "abc,d\"ef,123";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(',')
				.setStringQuote(null);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("d\"ef", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test3() {
		final String csvData = "abc;def;123\nabc;def";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote(null)
				.setFillMissingTrailingColumnsWithNull(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("def", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
			Assertions.assertEquals("abc", dataLines.get(1).get(0));
			Assertions.assertEquals("def", dataLines.get(1).get(1));
			Assertions.assertEquals(null, dataLines.get(1).get(2));
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test5() {
		final String csvData = "abc;def;123\nabc;def";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote(null)
				.setFillMissingTrailingColumnsWithNull(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			reader.readAll();
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test5_Error() {
		final String csvData = "abc;def;123\nabc;def";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote(null);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			reader.readAll();
			Assertions.fail("Exception expected");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Exception expected
		}
	}

	@Test
	public void test4() {
		final String csvData = "abc;def;123\nabc;def\n";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote(null)
				.setFillMissingTrailingColumnsWithNull(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("def", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
			Assertions.assertEquals("abc", dataLines.get(1).get(0));
			Assertions.assertEquals("def", dataLines.get(1).get(1));
			Assertions.assertEquals(null, dataLines.get(1).get(2));
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test6() {
		final String csvData = "123;\"ab\\\"c\";456\n";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote('\"')
				.setStringQuoteEscapeCharacter('\\')
				.setFillMissingTrailingColumnsWithNull(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("123", dataLines.get(0).get(0));
			Assertions.assertEquals("ab\"c", dataLines.get(0).get(1));
			Assertions.assertEquals("456", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void test7() {
		final String csvData = "123; \"abc\" ;456\n";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setStringQuote('\"')
				.setStringQuoteEscapeCharacter('\\')
				.setFillMissingTrailingColumnsWithNull(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("123", dataLines.get(0).get(0));
			Assertions.assertEquals("abc", dataLines.get(0).get(1));
			Assertions.assertEquals("456", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testCsvWriter1() {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (CsvWriter writer = new CsvWriter(byteArrayOutputStream, new CsvFormat().setSeparator(';').setStringQuote('\"'))) {
				writer.writeValues(new Object[] {
						"abc",
						"def",
						"123"
				});
				writer.writeValues(new Object[] {
						"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|",
						"jkl",
						"4\n\n;\"56"
				});
			}
			final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\\n\\n;\"\"56\"\n";
			Assertions.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testCsvWriter2() {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (CsvWriter writer = new CsvWriter(byteArrayOutputStream, new CsvFormat().setSeparator(';').setStringQuote('\"').setStringQuoteEscapeCharacter('\\'))) {
				writer.writeValues(new Object[] {
						"abc",
						"def",
						"123"
				});
				writer.writeValues(new Object[] {
						"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|",
						"jkl",
						"4\n\n;\"56"
				});
			}
			final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\\\<>(){}[]'\\\"´`^°²³*#.,:=+-~_|\";jkl;\"4\\n\\n;\\\"56\"\n";
			Assertions.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testWhitespaceOnlyValue() {
		final String csvData = "abc; ;123";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';');

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals(" ", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testWhitespaceOnlyValueTrimed() {
		final String csvData = "abc; ;123";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setAlwaysTrim(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testWhitespaceOnlyValueTrimedQuoted() {
		final String csvData = "abc; \" \" ;123";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';')
				.setAlwaysTrim(true);

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testWhitespaceOnlyValueQuoted() {
		final String csvData = "abc; \" \" ;123";

		final CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';');

		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals(" ", dataLines.get(0).get(1));
			Assertions.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testSpecialQuotation() {
		try {
			final List<String> dataLine = CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "\"abc\\\"123\"");
			Assertions.assertEquals("abc\"123", dataLine.get(0));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testQuotationError1() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "abc\\\"123");
			Assertions.fail("Missing expected exception");
		} catch (final Exception e) {
			Assertions.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError2() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "\"abc\\\"\"123");
			Assertions.fail("Missing expected exception");
		} catch (final Exception e) {
			Assertions.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError3() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "abc\"");
			Assertions.fail("Missing expected exception");
		} catch (final Exception e) {
			Assertions.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError4() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('"'), "abc\\\"123");
			Assertions.fail("Missing expected exception");
		} catch (final Exception e) {
			Assertions.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testIgnoreEmptyRows() {
		final String csvData = "abc;def;1\nabc;def;2\n\n;;\nabc;def;3\n \nabc;def;4\n";
		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), new CsvFormat().setSeparator(';').setIgnoreEmptyLines(true))) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals(dataLines.size(), 4);
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("def", dataLines.get(0).get(1));
			Assertions.assertEquals("1", dataLines.get(0).get(2));
			Assertions.assertEquals("abc", dataLines.get(1).get(0));
			Assertions.assertEquals("def", dataLines.get(1).get(1));
			Assertions.assertEquals("2", dataLines.get(1).get(2));
			Assertions.assertEquals("abc", dataLines.get(2).get(0));
			Assertions.assertEquals("def", dataLines.get(2).get(1));
			Assertions.assertEquals("3", dataLines.get(2).get(2));
			Assertions.assertEquals("abc", dataLines.get(3).get(0));
			Assertions.assertEquals("def", dataLines.get(3).get(1));
			Assertions.assertEquals("4", dataLines.get(3).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testDontIgnoreEmptyRows() {
		final String csvData = "abc;def;1\nabc;def;2\n;;\nabc;def;3\nabc;def;4\n";
		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), new CsvFormat().setSeparator(';').setIgnoreEmptyLines(false))) {
			final List<List<String>> dataLines = reader.readAll();
			Assertions.assertEquals(dataLines.size(), 5);
			Assertions.assertEquals("abc", dataLines.get(0).get(0));
			Assertions.assertEquals("def", dataLines.get(0).get(1));
			Assertions.assertEquals("1", dataLines.get(0).get(2));
			Assertions.assertEquals("abc", dataLines.get(1).get(0));
			Assertions.assertEquals("def", dataLines.get(1).get(1));
			Assertions.assertEquals("2", dataLines.get(1).get(2));
			Assertions.assertEquals("", dataLines.get(2).get(0));
			Assertions.assertEquals("", dataLines.get(2).get(1));
			Assertions.assertEquals("", dataLines.get(2).get(2));
			Assertions.assertEquals("abc", dataLines.get(3).get(0));
			Assertions.assertEquals("def", dataLines.get(3).get(1));
			Assertions.assertEquals("3", dataLines.get(3).get(2));
			Assertions.assertEquals("abc", dataLines.get(4).get(0));
			Assertions.assertEquals("def", dataLines.get(4).get(1));
			Assertions.assertEquals("4", dataLines.get(4).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}
}
