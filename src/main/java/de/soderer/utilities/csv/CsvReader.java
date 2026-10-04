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
 * Reader for CSV data from an input stream.
 * <p>
 * The data format (separator, string quote, escaping etc.) is defined by a {@link CsvFormat}.
 * Lines are read one by one by {@link #readNextCsvLine()} or all at once by {@link #readAll()}.
 * The number of values of the first line defines the expected number of values of all following
 * lines. Line breaks may be CR, LF or CRLF, a leading UTF-8 BOM is skipped.
 * </p>
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
	 * Creates a new CSV reader using UTF-8 encoding and the default {@link CsvFormat}.
	 *
	 * @param inputStream
	 *            the input stream to read from
	 * @throws Exception
	 *             if the input stream is null
	 */
	public CsvReader(final InputStream inputStream) throws Exception {
		this(inputStream, DEFAULT_ENCODING);
	}

	/**
	 * Creates a new CSV reader using the given encoding and the default {@link CsvFormat}.
	 *
	 * @param inputStream
	 *            the input stream to read from
	 * @param encoding
	 *            the encoding of the input data, or null for UTF-8
	 * @throws Exception
	 *             if the input stream is null
	 */
	public CsvReader(final InputStream inputStream, final Charset encoding) throws Exception {
		super(inputStream, encoding);
	}

	/**
	 * Creates a new CSV reader using UTF-8 encoding and the given CSV format.
	 *
	 * @param inputStream
	 *            the input stream to read from
	 * @param csvFormat
	 *            the CSV format of the input data
	 * @throws Exception
	 *             if the input stream or the CSV format is null
	 */
	public CsvReader(final InputStream inputStream, final CsvFormat csvFormat) throws Exception {
		this(inputStream, DEFAULT_ENCODING, csvFormat);
	}

	/**
	 * Creates a new CSV reader using the given encoding and CSV format.
	 *
	 * @param inputStream
	 *            the input stream to read from
	 * @param encoding
	 *            the encoding of the input data, or null for UTF-8
	 * @param csvFormat
	 *            the CSV format of the input data
	 * @throws Exception
	 *             if the input stream or the CSV format is null
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
	 * Returns the configured CSV format.
	 *
	 * @return the CSV format
	 */
	public CsvFormat getCsvFormat() {
		return csvFormat;
	}

	/**
	 * Sets the CSV format. Should be set before reading starts.
	 *
	 * @param csvFormat
	 *            the CSV format of the input data
	 * @throws Exception
	 *             if the CSV format is null
	 */
	public void setCsvFormat(final CsvFormat csvFormat) throws Exception {
		if (csvFormat == null) {
			throw new Exception("Invalid empty csvFormat parameter");
		} else {
			this.csvFormat = csvFormat;
		}
	}

	/**
	 * Sets the CSV format. Should be set before reading starts.
	 *
	 * @param newCsvFormat
	 *            the CSV format of the input data
	 * @return this reader for chaining
	 * @throws Exception
	 *             if the CSV format is null
	 */
	public CsvReader withCsvFormat(final CsvFormat newCsvFormat) throws Exception {
		setCsvFormat(newCsvFormat);
		return this;
	}

	/**
	 * Returns the number of CSV lines read so far. A CSV line may span several physical lines, if
	 * quoted values contain line breaks.
	 *
	 * @return the number of CSV lines read
	 */
	public int getReadCsvLines() {
		return readCsvLines;
	}

	/**
	 * Reads the next CSV line. The reader is closed automatically, when the end of data is
	 * reached.
	 *
	 * @return the values of the next line, or null if the end of data has been reached
	 * @throws IOException
	 *             if reading fails or the data ends within a quoted value
	 * @throws CsvDataException
	 *             if the data does not match the CSV format, e.g. an inconsistent number of values
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
				close();
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
	 * Reads all CSV lines at once and closes the reader. This is only possible before
	 * {@link #readNextCsvLine()} was called for the first time.
	 *
	 * @return the values of all lines
	 * @throws IOException
	 *             if reading fails or the data ends within a quoted value
	 * @throws CsvDataException
	 *             if the data does not match the CSV format
	 * @throws IllegalStateException
	 *             if {@link #readNextCsvLine()} was called before
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
	 * Parse a single value to apply quoting and escaping rules.
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

	/**
	 * Checks if a list contains only null, empty or blank values.
	 *
	 * @param list
	 *            the list to check, may be null
	 * @return true, if the list contains no value with non whitespace characters
	 */
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
	 * Reads the data to the end, counts all CSV lines and closes the reader. The count may be less
	 * than the number of physical lines because of line breaks in quoted values. It includes the
	 * first line, which may contain column headers. This is only possible before
	 * {@link #readNextCsvLine()} was called for the first time.
	 *
	 * @return the number of CSV lines
	 * @throws IOException
	 *             if reading fails or the data ends within a quoted value
	 * @throws CsvDataException
	 *             if the data does not match the CSV format
	 * @throws IllegalStateException
	 *             if {@link #readNextCsvLine()} was called before
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
	 * Parses a single CSV line using the default {@link CsvFormat}.
	 *
	 * @param csvLine
	 *            the CSV line, without or with trailing line break
	 * @return the values of the line
	 * @throws Exception
	 *             if the text contains no or more than one CSV line, or does not match the CSV
	 *             format
	 */
	public static List<String> parseCsvLine(final String csvLine) throws Exception {
		return parseCsvLine(new CsvFormat(), csvLine);
	}

	/**
	 * Parses a single CSV line using the given CSV format. Counterpart of
	 * {@link CsvWriter#getCsvLine(CsvFormat, List)}.
	 *
	 * @param csvFormat
	 *            the CSV format
	 * @param csvLine
	 *            the CSV line, without or with trailing line break
	 * @return the values of the line
	 * @throws Exception
	 *             if the text contains no or more than one CSV line, or does not match the CSV
	 *             format
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
		}
	}

	/**
	 * Returns the first duplicate CSV file header. Leading and trailing whitespaces are ignored,
	 * empty headers are skipped and headers are case-sensitive.
	 *
	 * @param csvFileHeaders
	 *            the headers to check
	 * @return the first duplicate header, or null if there is no duplicate
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
