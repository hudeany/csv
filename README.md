# Java CSV-Reader and CSV-Writer

CsvReader can read .csv files and streams in any characterset encoding (also with BOM) and with configurable separator and optional stringquote (delimiter).

CsvWriter can write .csv files and streams and supports the same features as CsvReader.

General features:

- Using data streams instead of files only
- Handles linebreaks within quoted data
- Handles string quote character within quoted data
- Configurable separator character
- Configurable optional string quote character
- Configurable allows too short csvdata lines (trailing empty columns)
- Read line per line or all data at once

Format features:

- Separating character can be configured
- Character for string quotes can be configured
- Character to escape the string quote character within quoted strings can be configured. By default this is the stringquote character itself, so it is doubled in quoted strings, but may also be configured to a backslash '\'
- Allow linebreaks in data texts without the effect of a new data set line or use "\\n" to escape linebreaks on csv output
- Allow escaped stringquotes to use them as a character in data text. May be turned off for data consistency checks
- Allow lines with less than the expected number of data entries per line can be configured
- Allow lines with more than the expected number of data entries per line, if those are empty
- Trim all data values
- Quote data entries: NO_QUOTE, QUOTE_IF_NEEDED, QUOTE_STRINGS, QUOTE_ALL_DATA
- Linebreak character for output can be configured
- Ignore empty lines can be configured
- Use headers in first csv line can be configured

Example:

		CsvReader reader = null;
		try {
			String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\r\n;\"\"56\"";

			CsvFormat csvFormat = new CsvFormat()
				.setSeparator(';');
			
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), csvFormat);
			
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|", dataLines.get(1).get(0));
			Assert.assertEquals("jkl", dataLines.get(1).get(1));
			Assert.assertEquals("4\n\n;\"56", dataLines.get(1).get(2));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		CsvWriter writer = null;
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer = new CsvWriter(byteArrayOutputStream, new CsvFormat().setSeparator(';').setStringQuote('\"'));
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
			String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\n;\"\"56\"\n";
			writer.close();
			writer = null;
			Assert.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
