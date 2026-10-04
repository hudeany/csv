package de.soderer.utilities.csv;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import de.soderer.utilities.csv.CsvFormat.QuoteMode;
import de.soderer.utilities.csv.utilities.Utilities;

/**
 * Writer for CSV data to an output stream.
 * <p>
 * The data format (separator, string quote, escaping, quote mode, line break) is defined by a
 * {@link CsvFormat}. The format is evaluated when the writer is created, later changes of the
 * format object are not supported. The number of values of the first line defines the expected
 * number of values of all following lines.
 * </p>
 */
public class CsvWriter implements Closeable {
	/** CSV data format definition */
	private final CsvFormat csvFormat;

	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Current output separator as string for internal use. */
	private final String separatorString;

	/** Current output string quote as string for internal use. */
	private final String stringQuoteString;

	/** Current output string quote escape character as string for internal use. */
	private final String stringQuoteEscapeString;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	/** Lines written until now. */
	private int writtenLines = 0;

	/** Number of columns to write, set by first line written. */
	private int numberOfColumns = -1;

	/** Output writer. */
	private BufferedWriter outputWriter = null;

	/** Minimum sizes of columns for beautification */
	private int[] minimumColumnSizes = null;

	/** Padding locations of columns for beautification (true = right padding = left aligned) */
	private boolean[] columnPaddings = null;

	/** Use backslash escape sequences (e.g. "\\n") in values */
	private final boolean escapeLineBreaks;

	/**
	 * Creates a new CSV writer using UTF-8 encoding and the default {@link CsvFormat}.
	 *
	 * @param outputStream
	 *            the output stream to write to
	 * @throws IllegalArgumentException
	 *             if the output stream is null
	 */
	public CsvWriter(final OutputStream outputStream) {
		this(outputStream, DEFAULT_ENCODING);
	}

	/**
	 * Creates a new CSV writer using the given encoding and the default {@link CsvFormat}.
	 *
	 * @param outputStream
	 *            the output stream to write to
	 * @param encoding
	 *            the encoding of the output data
	 * @throws IllegalArgumentException
	 *             if the output stream or the encoding is null
	 */
	public CsvWriter(final OutputStream outputStream, final Charset encoding) {
		this(outputStream, encoding, new CsvFormat());
	}

	/**
	 * Creates a new CSV writer using UTF-8 encoding and the given CSV format.
	 *
	 * @param outputStream
	 *            the output stream to write to
	 * @param csvFormat
	 *            the CSV format of the output data
	 * @throws IllegalArgumentException
	 *             if the output stream or the CSV format is null
	 */
	public CsvWriter(final OutputStream outputStream, final CsvFormat csvFormat) {
		this(outputStream, DEFAULT_ENCODING, csvFormat);
	}

	/**
	 * Creates a new CSV writer using the given encoding and CSV format.
	 *
	 * @param outputStream
	 *            the output stream to write to
	 * @param encoding
	 *            the encoding of the output data
	 * @param csvFormat
	 *            the CSV format of the output data
	 * @throws IllegalArgumentException
	 *             if the output stream, the encoding or the CSV format is null
	 */
	public CsvWriter(final OutputStream outputStream, final Charset encoding, final CsvFormat csvFormat) {
		if (csvFormat == null) {
			throw new IllegalArgumentException("CsvFormat is null");
		}
		this.csvFormat = csvFormat;
		this.outputStream = outputStream;
		this.encoding = encoding;
		separatorString = Character.toString(csvFormat.getSeparator());
		stringQuoteString = Character.toString(csvFormat.getStringQuote());
		stringQuoteEscapeString = Character.toString(csvFormat.getStringQuoteEscapeCharacter());
		escapeLineBreaks = csvFormat.isEscapeLineBreaks();

		if (this.encoding == null) {
			throw new IllegalArgumentException("Encoding is null");
		} else if (this.outputStream == null) {
			throw new IllegalArgumentException("OutputStream is null");
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
	 * Writes a single CSV line. Null values are written as empty values.
	 *
	 * @param values
	 *            the values of the line
	 * @return this writer for chaining
	 * @throws CsvDataException
	 *             if the number of values differs from the first line, or a value needs quoting
	 *             while quoting is deactivated
	 * @throws IOException
	 *             if writing fails
	 * @throws IllegalStateException
	 *             if this writer is already closed
	 */
	public CsvWriter writeValues(final Object... values) throws CsvDataException, IOException {
		writeValues(Arrays.asList(values));
		return this;
	}

	/**
	 * Writes a single CSV line. Null values are written as empty values.
	 *
	 * @param values
	 *            the values of the line
	 * @return this writer for chaining
	 * @throws CsvDataException
	 *             if the values are null, their number differs from the first line, or a value
	 *             needs quoting while quoting is deactivated
	 * @throws IOException
	 *             if writing fails
	 * @throws IllegalStateException
	 *             if this writer is already closed
	 */
	public CsvWriter writeValues(final List<? extends Object> values) throws CsvDataException, IOException {
		if (values == null) {
			throw new CsvDataException("Invalid empty values after " + writtenLines + " written lines (expected: " + numberOfColumns + " was: null)", writtenLines);
		} else if (numberOfColumns != -1 && numberOfColumns != values.size()) {
			throw new CsvDataException("Inconsistent number of values after " + writtenLines + " written lines (expected: " + numberOfColumns + " was: " + values.size() + ")", writtenLines);
		}

		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("CsvWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}

		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				outputWriter.write(csvFormat.getSeparator());
			}

			String escapedValue = escapeValue(values.get(i));

			if (minimumColumnSizes != null && minimumColumnSizes.length > i) {
				if (columnPaddings != null && columnPaddings.length > i && columnPaddings[i]) {
					escapedValue = rightPad(escapedValue, minimumColumnSizes[i]);
				} else {
					escapedValue = leftPad(escapedValue, minimumColumnSizes[i]);
				}
			}

			outputWriter.write(escapedValue);
		}
		outputWriter.write(csvFormat.getLineBreak());

		writtenLines++;
		numberOfColumns = values.size();
		return this;
	}

	/**
	 * Writes several CSV lines.
	 *
	 * @param valueLines
	 *            the values of each line
	 * @return this writer for chaining
	 * @throws CsvDataException
	 *             if the values of a line are invalid, see {@link #writeValues(List)}
	 * @throws IOException
	 *             if writing fails
	 */
	public CsvWriter writeAll(final List<List<? extends Object>> valueLines) throws CsvDataException, IOException {
		for (final List<? extends Object> valuesOfLine : valueLines) {
			writeValues(valuesOfLine);
		}
		return this;
	}

	/**
	 * Escape a single data entry using stringquotes as configured.
	 *
	 * @param value
	 *            the value
	 * @return the string
	 * @throws CsvDataException
	 *             the csv data exception
	 */
	private String escapeValue(final Object value) throws CsvDataException {
		String valueString = "";
		if (value != null) {
			valueString = value.toString();
		}

		if (escapeLineBreaks) {
			valueString = Utilities.escapeCSV(Utilities.normalizeLinebreaks(valueString));
		}

		// A stringquote character in data needs quotation only if quoting is active, otherwise it is a plain character
		final boolean valueNeedsQuotation =
				(csvFormat.getQuoteMode() != QuoteMode.NO_QUOTE && valueString.contains(stringQuoteString))
				|| valueString.contains(separatorString)
				|| valueString.contains("\r")
				|| valueString.contains("\n");

		if (csvFormat.getQuoteMode() == QuoteMode.QUOTE_ALL_DATA
				|| (csvFormat.getQuoteMode() == QuoteMode.QUOTE_STRINGS && (value instanceof String || valueNeedsQuotation))
				|| (csvFormat.getQuoteMode() == QuoteMode.QUOTE_IF_NEEDED && valueNeedsQuotation)) {
			final StringBuilder escapedValue = new StringBuilder();
			escapedValue.append(stringQuoteString);
			escapedValue.append(escapeQuotedContent(valueString));
			escapedValue.append(stringQuoteString);
			return escapedValue.toString();
		} else if (valueNeedsQuotation) {
			throw new CsvDataException("StringQuote was deactivated but is needed for csv-value after " + writtenLines + " written lines", writtenLines);
		} else {
			return valueString;
		}
	}

	/**
	 * Escape the content of a value, which will be enclosed in stringquotes.
	 * If the stringquote escape character differs from the stringquote (e.g. backslash),
	 * the escape character itself must be escaped too. Otherwise a value ending with
	 * the escape character (e.g. the value C:\dir\) would escape the closing stringquote.
	 * Exception: With escapeLineBreaks and a backslash as escape character, backslashes
	 * were already doubled by Utilities.escapeCSV(), so they must not be doubled again.
	 *
	 * @param valueString
	 *            the value content without enclosing stringquotes
	 * @return the escaped value content
	 */
	private String escapeQuotedContent(final String valueString) {
		String returnValue = valueString;
		if (!stringQuoteEscapeString.equals(stringQuoteString) && !(escapeLineBreaks && "\\".equals(stringQuoteEscapeString))) {
			returnValue = returnValue.replace(stringQuoteEscapeString, stringQuoteEscapeString + stringQuoteEscapeString);
		}
		return returnValue.replace(stringQuoteString, stringQuoteEscapeString + stringQuoteString);
	}

	/**
	 * Calculates the output sizes of values including quoting and escaping, e.g. to determine
	 * minimum column sizes for beautified output.
	 *
	 * @param values
	 *            the values
	 * @return the output size of each value
	 * @throws CsvDataException
	 *             if a value needs quoting while quoting is deactivated
	 */
	public int[] calculateOutputSizesOfValues(final List<? extends Object> values) throws CsvDataException {
		final int[] returnArray = new int[values.size()];
		for (int i = 0; i < values.size(); i++) {
			returnArray[i] = escapeValue(values.get(i)).length();
		}
		return returnArray;
	}

	/**
	 * Calculates the output size of a value including quoting and escaping.
	 *
	 * @param value
	 *            the value
	 * @return the output size of the value
	 * @throws CsvDataException
	 *             if the value needs quoting while quoting is deactivated
	 */
	public int calculateOutputSizesOfValue(final Object value) throws CsvDataException {
		return escapeValue(value).length();
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
	}

	/**
	 * Returns the number of CSV lines written so far.
	 *
	 * @return the number of written lines
	 */
	public int getWrittenLines() {
		return writtenLines;
	}

	/**
	 * Flushes buffered data to the output stream.
	 *
	 * @return this writer for chaining
	 * @throws IOException
	 *             if writing fails
	 */
	public CsvWriter flush() throws IOException {
		if (outputWriter != null) {
			outputWriter.flush();
		}
		return this;
	}

	/**
	 * Creates a single CSV line without trailing line break. Values are quoted only if needed and
	 * string quotes are doubled. Null values are written as empty values.
	 *
	 * @param separator
	 *            the separator character
	 * @param stringQuote
	 *            the string quote character, or null to deactivate quoting
	 * @param escapeLineBreaks
	 *            true to use backslash escape sequences, see {@link CsvFormat#isEscapeLineBreaks()}
	 * @param values
	 *            the values
	 * @return the CSV line
	 * @throws IllegalArgumentException
	 *             if a value needs quoting while quoting is deactivated
	 */
	public static String getCsvLine(final char separator, final Character stringQuote, final boolean escapeLineBreaks, final List<? extends Object> values) {
		return getCsvLine(separator, stringQuote, escapeLineBreaks, values.toArray());
	}

	/**
	 * Creates a single CSV line without trailing line break. Values are quoted only if needed and
	 * string quotes are doubled. Null values are written as empty values.
	 *
	 * @param separator
	 *            the separator character
	 * @param stringQuote
	 *            the string quote character, or null to deactivate quoting
	 * @param escapeLineBreaks
	 *            true to use backslash escape sequences, see {@link CsvFormat#isEscapeLineBreaks()}
	 * @param values
	 *            the values
	 * @return the CSV line
	 * @throws IllegalArgumentException
	 *             if a value needs quoting while quoting is deactivated
	 */
	public static String getCsvLine(final char separator, final Character stringQuote, final boolean escapeLineBreaks, final Object... values) {
		final StringBuilder returnValue = new StringBuilder();
		final String separatorString = Character.toString(separator);
		final String stringQuoteString = stringQuote == null ? "" : Character.toString(stringQuote);
		final String doubleStringQuoteString = stringQuoteString + stringQuoteString;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				final Object value = values[i];
				// Use the index, because a leading empty value leaves the line empty
				if (i > 0) {
					returnValue.append(separator);
				}
				if (value != null) {
					String valueString = value.toString();
					if (escapeLineBreaks) {
						valueString = Utilities.escapeCSV(Utilities.normalizeLinebreaks(valueString));
					}

					// Check for stringquote only if one is configured, because contains("") is always true
					final boolean valueNeedsQuotation =
							(stringQuote != null && valueString.contains(stringQuoteString))
							|| valueString.contains(separatorString)
							|| valueString.contains("\r")
							|| valueString.contains("\n");

					if (valueNeedsQuotation) {
						if (stringQuote == null) {
							throw new IllegalArgumentException("StringQuote was deactivated but is needed for csv-value: " + valueString);
						}
						returnValue.append(stringQuoteString);
						returnValue.append(valueString.replace(stringQuoteString, doubleStringQuoteString));
						returnValue.append(stringQuoteString);
					} else {
						returnValue.append(valueString);
					}
				}
			}
		}
		return returnValue.toString();
	}

	/**
	 * Create a single csv line using the given csv format.
	 * Counterpart of {@link CsvReader#parseCsvLine(CsvFormat, String)}.
	 * The line is created by the same escaping and quoting logic as writeValues(),
	 * so separator, stringquote, stringquote escape character, quote mode and
	 * escapeLineBreaks of the csv format are respected.
	 * The returned line does not contain the trailing linebreak.
	 *
	 * @param csvFormat
	 *            the csv format
	 * @param values
	 *            the values
	 * @return the csv line
	 * @throws CsvDataException
	 *             if a value can not be written in the given csv format (e.g. quotation needed but deactivated)
	 */
	public static String getCsvLine(final CsvFormat csvFormat, final List<? extends Object> values) throws CsvDataException {
		if (csvFormat == null) {
			throw new IllegalArgumentException("Invalid empty csvFormat parameter");
		} else if (values == null) {
			throw new IllegalArgumentException("Invalid empty values parameter");
		}

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (CsvWriter csvWriter = new CsvWriter(outputStream, StandardCharsets.UTF_8, csvFormat)) {
			csvWriter.writeValues(values);
			csvWriter.flush();
		} catch (final IOException e) {
			// Can not happen when writing into memory
			throw new IllegalStateException("Unexpected error creating csv line: " + e.getMessage(), e);
		}

		final String csvLine = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		// Remove the trailing linebreak added by writeValues()
		if (csvLine.endsWith(csvFormat.getLineBreak())) {
			return csvLine.substring(0, csvLine.length() - csvFormat.getLineBreak().length());
		} else {
			return csvLine;
		}
	}

	/**
	 * Create a single csv line using the given csv format.
	 * See {@link #getCsvLine(CsvFormat, List)}.
	 *
	 * @param csvFormat
	 *            the csv format
	 * @param values
	 *            the values
	 * @return the csv line
	 * @throws CsvDataException
	 *             if a value can not be written in the given csv format (e.g. quotation needed but deactivated)
	 */
	public static String getCsvLine(final CsvFormat csvFormat, final Object... values) throws CsvDataException {
		if (values == null) {
			throw new IllegalArgumentException("Invalid empty values parameter");
		}
		return getCsvLine(csvFormat, Arrays.asList(values));
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Sets minimum output sizes of columns for beautified output. Shorter values are padded with
	 * blanks, see {@link #setColumnPaddings(boolean[])}.
	 *
	 * @param minimumColumnSizes
	 *            the minimum size of each column, or null for no padding
	 */
	public void setMinimumColumnSizes(final int[] minimumColumnSizes) {
		this.minimumColumnSizes = minimumColumnSizes;
	}

	/**
	 * Sets minimum output sizes of columns for beautified output, see
	 * {@link #setMinimumColumnSizes(int[])}.
	 *
	 * @param newMinimumColumnSizes
	 *            the minimum size of each column, or null for no padding
	 * @return this writer for chaining
	 */
	public CsvWriter withMinimumColumnSizes(final int[] newMinimumColumnSizes) {
		setMinimumColumnSizes(newMinimumColumnSizes);
		return this;
	}

	/**
	 * Sets the padding side of columns for beautified output.
	 *
	 * @param columnPaddings
	 *            for each column true for right padding (left aligned) or false for left padding
	 *            (right aligned, the default), or null for left padding of all columns
	 */
	public void setColumnPaddings(final boolean[] columnPaddings) {
		this.columnPaddings = columnPaddings;
	}

	/**
	 * Sets the padding side of columns for beautified output, see
	 * {@link #setColumnPaddings(boolean[])}.
	 *
	 * @param newColumnPaddings
	 *            for each column true for right padding (left aligned) or false for left padding
	 * @return this writer for chaining
	 */
	public CsvWriter withColumnPaddings(final boolean[] newColumnPaddings) {
		setColumnPaddings(newColumnPaddings);
		return this;
	}

	/**
	 * Prepends blanks to a string to make it fit the given minimum length.
	 *
	 * @param value
	 *            the value
	 * @param minimumLength
	 *            the minimum length
	 * @return the padded value
	 */
	private static String leftPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	/**
	 * Appends blanks to a string to make it fit the given minimum length.
	 *
	 * @param value
	 *            the value
	 * @param minimumLength
	 *            the minimum length
	 * @return the padded value
	 */
	private static String rightPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$-" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}
}
