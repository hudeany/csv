package de.soderer.utilities.csv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.soderer.utilities.csv.CsvFormat.QuoteMode;
import de.soderer.utilities.csv.utilities.BasicReader;
import de.soderer.utilities.csv.utilities.Utilities;

/**
 * The Class CsvReader.
 */
public class CsvReader extends BasicReader {
	/** CSV data format definition */
	private CsvFormat csvFormat = new CsvFormat();

	/** If a single read was done, it is impossible to make a full read at once with readAll(). */
	private boolean singleReadStarted = false;

	/** Number of columns expected (set by first read line). */
	private int numberOfColumns = -1;

	/** Number of lines read until now. */
	private int readCsvLines = 0;

	/**
	 * CSV Reader derived constructor.
	 *
	 * @param inputStream
	 *            the input stream
	 */
	public CsvReader(final InputStream inputStream) throws Exception {
		this(inputStream, DEFAULT_ENCODING);
	}

	/**
	 * CSV Reader derived constructor.
	 *
	 * @param inputStream
	 *            the input stream
	 * @param encoding
	 *            the encoding
	 */
	public CsvReader(final InputStream inputStream, final Charset encoding) throws Exception {
		super(inputStream, encoding);
	}

	/**
	 * CSV Reader derived constructor.
	 *
	 * @param inputStream
	 *            the input stream
	 * @param separator
	 *            the separator
	 */
	public CsvReader(final InputStream inputStream, final CsvFormat csvFormat) throws Exception {
		this(inputStream, DEFAULT_ENCODING, csvFormat);
	}

	/**
	 * CSV Reader derived constructor.
	 *
	 * @param inputStream
	 *            the input stream
	 * @param encoding
	 *            the encoding
	 * @param separator
	 *            the separator
	 */
	public CsvReader(final InputStream inputStream, final Charset encoding, final CsvFormat csvFormat) throws Exception {
		this(inputStream, encoding);

		if (csvFormat == null) {
			throw new Exception("Invalid empty csvFormat parameter");
		} else {
			this.csvFormat = csvFormat;
		}
	}

	/**
	 * Get configured csv format
	 *
	 * @return
	 */
	public CsvFormat getCsvFormat() {
		return csvFormat;
	}

	/**
	 * Configured csv format
	 *
	 * @param csvFormat
	 * @throws Exception
	 */
	public void setCsvFormat(final CsvFormat csvFormat) throws Exception {
		if (csvFormat == null) {
			throw new Exception("Invalid empty csvFormat parameter");
		} else {
			this.csvFormat = csvFormat;
		}
	}

	/**
	 * Configured csv format
	 *
	 * @param csvFormat
	 * @throws Exception
	 */
	public CsvReader withCsvFormat(final CsvFormat newCsvFormat) throws Exception {
		setCsvFormat(newCsvFormat);
		return this;
	}

	/**
	 * Get lines read until now.
	 *
	 * @return the read lines
	 */
	public int getReadCsvLines() {
		return readCsvLines;
	}

	/**
	 * Read the next line of csv data.
	 *
	 * @return the list
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CsvDataException
	 *             the csv data exception
	 */
	public List<String> readNextCsvLine() throws IOException, CsvDataException {
		readCsvLines++;
		singleReadStarted = true;
		List<String> returnList = new ArrayList<>();
		StringBuilder nextValue = new StringBuilder();
		boolean insideString = false;
		boolean isQuotedString = false;
		Character nextCharacter;
		char previousCharacter = (char) -1;
		final boolean separateEscapeCharacter = csvFormat.getStringQuoteEscapeCharacter() != csvFormat.getStringQuote();
		// Number of directly preceding escape characters within a quoted string
		int precedingEscapeCharacters = 0;

		while ((nextCharacter = readNextCharacter()) != null) {
			final char nextChar = nextCharacter;
			if (csvFormat.getQuoteMode() != QuoteMode.NO_QUOTE && nextChar == csvFormat.getStringQuote()
					&& (isQuotedString || nextValue.toString().trim().isEmpty())) {
				if (separateEscapeCharacter) {
					// A stringquote is only escaped by an odd number of preceding escape characters.
					// An even number means that those escape characters escape each other, e.g. the value C:\dir\ is written as "C:\\dir\\"
					if (precedingEscapeCharacters % 2 == 0) {
						insideString = !insideString;
					}
				} else {
					insideString = !insideString;
				}
				nextValue.append(nextChar);
				isQuotedString = true;
			} else if (!insideString) {
				if (nextChar == '\r' || nextChar == '\n') {
					if (nextValue.length() > 0 || previousCharacter == csvFormat.getSeparator()) {
						returnList.add(parseValue(nextValue.toString()));
						nextValue = new StringBuilder();
						isQuotedString = false;
					}

					if (csvFormat.isIgnoreEmptyLines() && isBlank(returnList)) {
						returnList = new ArrayList<>();
					} else if (returnList.size() > 0) {
						if (numberOfColumns == -1) {
							numberOfColumns = returnList.size();
							return returnList;
						} else if (numberOfColumns == returnList.size()) {
							return returnList;
						} else if (numberOfColumns > returnList.size() && csvFormat.isFillMissingTrailingColumnsWithNull()) {
							while (returnList.size() < numberOfColumns) {
								returnList.add(null);
							}
							return returnList;
						} else if (numberOfColumns < returnList.size() && csvFormat.isRemoveSurplusEmptyTrailingColumns()) {
							// Too many values found, so check if the trailing values are only empty items
							while (returnList.size() > numberOfColumns) {
								final String lastItem = returnList.remove(returnList.size() - 1);
								if (!"".equals(lastItem)) {
									throw new CsvDataException("Inconsistent number of values in line " + readCsvLines + " (expected: " + numberOfColumns + " actually: " + (returnList.size() + 1) + ")", readCsvLines);
								}
							}
							return returnList;
						} else {
							throw new CsvDataException("Inconsistent number of values in line " + readCsvLines + " (expected: " + numberOfColumns + " actually: " + returnList.size() + ")", readCsvLines);
						}
					}
				} else if (nextChar == csvFormat.getSeparator()) {
					returnList.add(parseValue(nextValue.toString()));
					nextValue = new StringBuilder();
					isQuotedString = false;
				} else if (isQuotedString) {
					if (!Character.isWhitespace(nextChar)) {
						throw new CsvDataException("Not allowed textdata '" + nextChar + "' after quoted text in data in line " + readCsvLines, readCsvLines);
					}
				} else {
					nextValue.append(nextChar);
				}
			} else { // insideString
				if ((nextChar == '\r' || nextChar == '\n') && !csvFormat.isLineBreakInDataAllowed()) {
					throw new CsvDataException("Not allowed linebreak in data in line " + readCsvLines, readCsvLines);
				} else {
					nextValue.append(nextChar);
				}
			}

			if (separateEscapeCharacter && insideString && nextChar == csvFormat.getStringQuoteEscapeCharacter()) {
				precedingEscapeCharacters++;
			} else {
				precedingEscapeCharacters = 0;
			}

			previousCharacter = nextCharacter;
		}

		if (insideString) {
			close();
			throw new IOException("Unexpected end of data after quoted csv-value was started in line " + readCsvLines);
		} else {
			if (nextValue.length() > 0 || previousCharacter == csvFormat.getSeparator()) {
				returnList.add(parseValue(nextValue.toString()));
			}

			if (csvFormat.isIgnoreEmptyLines() && isBlank(returnList)) {
				return null;
			} else if (returnList.size() > 0) {
				if (numberOfColumns == -1) {
					numberOfColumns = returnList.size();
					return returnList;
				} else if (numberOfColumns == returnList.size()) {
					return returnList;
				} else if (numberOfColumns > returnList.size() && csvFormat.isFillMissingTrailingColumnsWithNull()) {
					while (returnList.size() < numberOfColumns) {
						returnList.add(null);
					}
					return returnList;
				} else if (numberOfColumns < returnList.size() && csvFormat.isRemoveSurplusEmptyTrailingColumns()) {
					// Too many values found, so check if the trailing values are only empty items
					while (returnList.size() > numberOfColumns) {
						final String lastItem = returnList.remove(returnList.size() - 1);
						if (!"".equals(lastItem)) {
							throw new CsvDataException("Inconsistent number of values in line " + readCsvLines + " (expected: " + numberOfColumns + " actually: " + (returnList.size() + 1) + ")", readCsvLines);
						}
					}
					return returnList;
				} else {
					throw new CsvDataException("Inconsistent number of values in line " + readCsvLines + " (expected: " + numberOfColumns + " actually: " + returnList.size() + ")", readCsvLines);
				}
			} else {
				close();
				return null;
			}
		}
	}

	/**
	 * Read all csv data at once. This can only be done before readNextCsvLine() was called for the first time
	 *
	 * @return the list
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CsvDataException
	 *             the csv data exception
	 */
	public List<List<String>> readAll() throws IOException, CsvDataException {
		if (singleReadStarted) {
			throw new IllegalStateException("Single readNextCsvLine was called before readAll");
		}

		try {
			final List<List<String>> csvValues = new ArrayList<>();
			List<String> lineValues;
			while ((lineValues = readNextCsvLine()) != null) {
				csvValues.add(lineValues);
			}
			return csvValues;
		} finally {
			close();
		}
	}

	/**
	 * Parse a single value to applicate allowed double stringquotes.
	 *
	 * @param rawValue
	 *            the raw value
	 * @return the string
	 * @throws CsvDataException
	 *             the csv data exception
	 */
	private String parseValue(final String rawValue) throws CsvDataException {
		String returnValue = rawValue;

		if (isNotEmpty(returnValue)) {
			if (csvFormat.getQuoteMode() != QuoteMode.NO_QUOTE) {
				final String stringQuoteString = Character.toString(csvFormat.getStringQuote());
				if (returnValue.contains(stringQuoteString)) {
					returnValue = returnValue.trim();
				}
				if (returnValue.length() > 1 && returnValue.charAt(0) == csvFormat.getStringQuote() && returnValue.charAt(returnValue.length() - 1) == csvFormat.getStringQuote()) {
					returnValue = returnValue.substring(1, returnValue.length() - 1);
					returnValue = unescapeQuotedContent(returnValue);
				}
			}
			returnValue = Utilities.normalizeLinebreaks(returnValue);

			if (csvFormat.isEscapeLineBreaks()) {
				try {
					returnValue = Utilities.normalizeLinebreaks(Utilities.unescapeCSV(returnValue));
				} catch (final Exception e) {
					throw new CsvDataException("Unsupported escaped value in line " + readCsvLines + ": '" + returnValue + "'", readCsvLines, e);
				}
			}

			if (!csvFormat.isEscapedStringQuoteInDataAllowed() && returnValue.indexOf(csvFormat.getStringQuote()) >= 0) {
				throw new CsvDataException("Not allowed stringquote in data in line " + readCsvLines, readCsvLines);
			}

			if (csvFormat.isAlwaysTrim()) {
				returnValue = returnValue.trim();
			}
		}

		return returnValue;
	}

	/**
	 * Unescape the content of a quoted value (without its enclosing stringquotes).
	 * Counterpart of CsvWriter.escapeQuotedContent().
	 *
	 * @param quotedContent
	 *            the value content without enclosing stringquotes
	 * @return the unescaped value content
	 */
	private String unescapeQuotedContent(final String quotedContent) {
		final char stringQuote = csvFormat.getStringQuote();
		final char escapeCharacter = csvFormat.getStringQuoteEscapeCharacter();
		if (escapeCharacter == stringQuote || (csvFormat.isEscapeLineBreaks() && escapeCharacter == '\\')) {
			// Doubled stringquotes (RFC 4180), or backslash escaping where escaped backslashes
			// are resolved later by Utilities.unescapeCSV()
			return quotedContent.replace(escapeCharacter + Character.toString(stringQuote), Character.toString(stringQuote));
		} else {
			// Single pass, so that an escaped escape character is not combined with a following character
			final StringBuilder returnValue = new StringBuilder(quotedContent.length());
			for (int i = 0; i < quotedContent.length(); i++) {
				final char nextChar = quotedContent.charAt(i);
				if (nextChar == escapeCharacter && i + 1 < quotedContent.length()
						&& (quotedContent.charAt(i + 1) == stringQuote || quotedContent.charAt(i + 1) == escapeCharacter)) {
					returnValue.append(quotedContent.charAt(i + 1));
					i++;
				} else {
					// Escape character followed by any other character is kept as plain text
					returnValue.append(nextChar);
				}
			}
			return returnValue.toString();
		}
	}

	private static boolean isBlank(final List<String> list) {
		if (list != null) {
			for (final String item : list) {
				if (item != null && item.length() > 0 && item.trim().length() > 0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * This method reads the stream to the end and counts all csv value lines, which can be less than the absolute linebreak count of the stream for the reason of quoted linebreaks. The result also
	 * contains the first line, which may consist of columnheaders.
	 *
	 * @return the csv line count
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CsvDataException
	 *             the csv data exception
	 */
	public int getCsvLineCount() throws IOException, CsvDataException {
		if (singleReadStarted) {
			throw new IllegalStateException("Single readNextCsvLine was called before getCsvLineCount");
		}

		try {
			int csvLineCount = 0;
			while (readNextCsvLine() != null) {
				csvLineCount++;
			}
			return csvLineCount;
		} finally {
			close();
		}
	}

	/**
	 * Parse a single csv data line for data entries.
	 *
	 * @param csvLine
	 * @return
	 * @throws Exception
	 */
	public static List<String> parseCsvLine(final String csvLine) throws Exception {
		return parseCsvLine(new CsvFormat(), csvLine);
	}

	/**
	 * Parse a single csv data line for data entries.
	 *
	 * @param csvFormat
	 * @param csvLine
	 * @return
	 * @throws Exception
	 */
	public static List<String> parseCsvLine(final CsvFormat csvFormat, final String csvLine) throws Exception {
		try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvLine.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8, csvFormat)) {
			final List<List<String>> fullData = reader.readAll();
			if (fullData.size() < 1) {
				throw new Exception("No csv lines in data");
			} else if (fullData.size() > 1) {
				throw new Exception("Too many csv lines in data");
			} else {
				return fullData.get(0);
			}
		} catch (final CsvDataException e) {
			throw e;
		}
	}

	/**
	 * Returns the first duplicate csv file header or null if there is no duplicate.
	 * Leading and trailing whitespaces in csv file headers are omitted.
	 * Csv file headers are case-sensitive.
	 *
	 * @param csvFileHeaders
	 * @return
	 */
	public static String checkForDuplicateCsvHeader(final List<String> csvFileHeaders) {
		final Set<String> foundHeaders = new HashSet<>();
		for (String nextHeader : csvFileHeaders) {
			if (nextHeader != null) {
				nextHeader = nextHeader.trim();
				if (nextHeader.length() > 0) {
					if (foundHeaders.contains(nextHeader)) {
						return nextHeader;
					} else {
						foundHeaders.add(nextHeader);
					}
				}
			}
		}
		return null;
	}
}
