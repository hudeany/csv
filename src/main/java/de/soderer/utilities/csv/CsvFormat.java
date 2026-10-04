package de.soderer.utilities.csv;

/**
 * Definition of a CSV data format, used by {@link CsvReader} and {@link CsvWriter}.
 * <p>
 * The defaults follow RFC 4180: comma as separator, double quote as string quote, doubled string
 * quotes within quoted values, line breaks allowed within quoted values and no backslash escaping.
 * All setters validate their values against the current settings, so the order of configuration
 * may matter (e.g. change the string quote before using its old character as separator).
 * </p>
 * <p>
 * Setters return nothing (bean conform), the corresponding withX() methods return this format
 * for method chaining.
 * </p>
 */
public class CsvFormat {
	/** Default separator character (comma). */
	public static final char DEFAULT_SEPARATOR = ',';

	/** Default string quote character (double quote). */
	public static final char DEFAULT_STRING_QUOTE = '"';

	/** Default output line break (LF). */
	public static final String DEFAULT_LINEBREAK = "\n";

	/** Mandatory separating character */
	private char separator = DEFAULT_SEPARATOR;

	/** Character for stringquotes */
	private char stringQuote = DEFAULT_STRING_QUOTE;

	/**
	 * Character to escape the stringquote character within quoted strings.
	 * By default this is the stringquote character itself, so it is doubled in quoted strings,
	 * but may also be configured to a backslash '\'.
	 */
	private char stringQuoteEscapeCharacter = DEFAULT_STRING_QUOTE;

	/** Allow linebreaks in data texts without the effect of a new data set line. */
	private boolean lineBreakInDataAllowed = true;

	/** Allow escaped stringquotes to use them as a character in data text.
	 * May be turned off for data consistency checks. */
	private boolean escapedStringQuoteInDataAllowed = true;

	/** Allow lines with less than the expected number of data entries per line. */
	private boolean fillMissingTrailingColumnsWithNull = false;

	/** Allow lines with more than the expected number of data entries per line, if those are empty. */
	private boolean removeSurplusEmptyTrailingColumns = false;

	/** Trim all data values */
	private boolean alwaysTrim = false;

	/** Quote data entries. */
	private QuoteMode quoteMode = QuoteMode.QUOTE_IF_NEEDED;

	/** Linebreak for output only. */
	private String lineBreak = DEFAULT_LINEBREAK;

	/** Ignore empty lines */
	private boolean ignoreEmptyLines = false;

	/** Use headers in first csv line */
	private boolean headerInFirstLine = true;

	/**
	 * Use backslash escape sequences in values (e.g. "\n" for a linebreak, "\\" for a backslash).
	 * Applies to csv output (escaping) and csv input (unescaping).
	 * Disabled by default, because RFC 4180 does not know any backslash escaping,
	 * so backslashes (e.g. in Windows paths) are plain characters and linebreaks
	 * are written as real linebreaks within quoted values.
	 */
	private boolean escapeLineBreaks = false;

	/**
	 * Modes for quoting values when writing CSV data.
	 */
	public enum QuoteMode {
		/** Throw an error, when any quotation is needed */
		NO_QUOTE,

		/** Do only quote, when a quotation is needed */
		QUOTE_IF_NEEDED,

		/** Quote all strings, quote other data, when a quotation is needed */
		QUOTE_STRINGS,

		/** Quote all data */
		QUOTE_ALL_DATA;

		/**
		 * Returns the quote mode with the given name, ignoring case.
		 *
		 * @param quoteModeString
		 *            the name of the quote mode, e.g. "quote_if_needed"
		 * @return the quote mode
		 * @throws Exception
		 *             if no quote mode with the given name exists
		 */
		public static QuoteMode getFromString(final String quoteModeString) throws Exception {
			for (final QuoteMode quoteMode : QuoteMode.values()) {
				if (quoteMode.toString().equalsIgnoreCase(quoteModeString)) {
					return quoteMode;
				}
			}
			throw new Exception("Invalid quote mode: " + quoteModeString);
		}
	}

	/**
	 * Creates a new CSV format with default settings (RFC 4180).
	 */
	public CsvFormat() {
	}

	/**
	 * Creates a new CSV format with the given settings. All other settings keep their defaults.
	 *
	 * @param separator
	 *            the separator character
	 * @param stringQuote
	 *            the string quote character
	 * @param stringQuoteEscapeCharacter
	 *            the character escaping a string quote within quoted values
	 * @param lineBreakInDataAllowed
	 *            true to allow line breaks within quoted values
	 * @param escapedStringQuoteInDataAllowed
	 *            true to allow escaped string quotes within values
	 * @param fillMissingTrailingColumnsWithNull
	 *            true to fill lines with too few values with null values
	 * @param removeSurplusEmptyTrailingColumns
	 *            true to remove surplus trailing values of a line, if they are empty
	 * @param alwaysTrim
	 *            true to trim all values
	 * @param quoteMode
	 *            the quote mode for writing
	 * @param lineBreak
	 *            the line break for writing ("\n", "\r\n" or "\r")
	 * @throws IllegalArgumentException
	 *             if any of the settings is invalid or conflicts with another one
	 */
	public CsvFormat(final char separator, final char stringQuote, final char stringQuoteEscapeCharacter, final boolean lineBreakInDataAllowed, final boolean escapedStringQuoteInDataAllowed, final boolean fillMissingTrailingColumnsWithNull, final boolean removeSurplusEmptyTrailingColumns, final boolean alwaysTrim, final QuoteMode quoteMode, final String lineBreak) {
		this.separator = separator;
		this.stringQuote = stringQuote;
		this.stringQuoteEscapeCharacter = stringQuoteEscapeCharacter;
		this.lineBreakInDataAllowed = lineBreakInDataAllowed;
		this.escapedStringQuoteInDataAllowed = escapedStringQuoteInDataAllowed;
		this.fillMissingTrailingColumnsWithNull = fillMissingTrailingColumnsWithNull;
		this.removeSurplusEmptyTrailingColumns = removeSurplusEmptyTrailingColumns;
		this.alwaysTrim = alwaysTrim;
		this.quoteMode = quoteMode;
		this.lineBreak = lineBreak;

		// Use setters to validate parameters
		setSeparator(separator);
		setStringQuote(stringQuote);
		setStringQuoteEscapeCharacter(stringQuoteEscapeCharacter);
		setLineBreakInDataAllowed(lineBreakInDataAllowed);
		setEscapedStringQuoteInDataAllowed(escapedStringQuoteInDataAllowed);
		setFillMissingTrailingColumnsWithNull(fillMissingTrailingColumnsWithNull);
		setAlwaysTrim(alwaysTrim);
		setQuoteMode(quoteMode);
		setLineBreak(lineBreak);
	}

	/**
	 * Returns the separator character between values.
	 *
	 * @return the separator character
	 */
	public char getSeparator() {
		return separator;
	}

	/**
	 * Sets the separator character between values.
	 *
	 * @param separator
	 *            the separator character
	 * @throws IllegalArgumentException
	 *             if the separator is a line break character or equals the string quote while
	 *             quoting is active
	 */
	public void setSeparator(final char separator) {
		if (separator == '\r' || separator == '\n') {
			throw new IllegalArgumentException("Separator '" + separator + "' is invalid");
		} else if (quoteMode != QuoteMode.NO_QUOTE && separator == stringQuote) {
			throw new IllegalArgumentException("Separator '" + separator + "' is invalid");
		} else {
			this.separator = separator;
		}
	}

	/**
	 * Sets the separator character between values.
	 *
	 * @param newSeparator
	 *            the separator character
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the separator is invalid, see {@link #setSeparator(char)}
	 */
	public CsvFormat withSeparator(final char newSeparator) {
		setSeparator(newSeparator);
		return this;
	}

	/**
	 * Returns the string quote character. It is only used, if the quote mode is not
	 * {@link QuoteMode#NO_QUOTE}.
	 *
	 * @return the string quote character
	 */
	public char getStringQuote() {
		return stringQuote;
	}

	/**
	 * Sets the string quote character.
	 * <p>
	 * Also sets the string quote escape character to the same character (doubled string quotes)
	 * and the quote mode to {@link QuoteMode#QUOTE_IF_NEEDED}. Null deactivates quoting by
	 * setting the quote mode to {@link QuoteMode#NO_QUOTE}.
	 * </p>
	 *
	 * @param stringQuote
	 *            the string quote character, or null to deactivate quoting
	 * @throws IllegalArgumentException
	 *             if the string quote is a line break character or equals the separator
	 */
	public void setStringQuote(final Character stringQuote) {
		if (stringQuote != null) {
			if (stringQuote == '\r' || stringQuote == '\n' || separator == stringQuote) {
				throw new IllegalArgumentException("StringQuote '" + stringQuote + "' is invalid");
			} else {
				this.stringQuote = stringQuote;
				stringQuoteEscapeCharacter = stringQuote;
				quoteMode = QuoteMode.QUOTE_IF_NEEDED;
			}
		} else {
			quoteMode = QuoteMode.NO_QUOTE;
		}
	}

	/**
	 * Sets the string quote character, see {@link #setStringQuote(Character)}.
	 *
	 * @param newStringQuote
	 *            the string quote character, or null to deactivate quoting
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the string quote is invalid
	 */
	public CsvFormat withStringQuote(final Character newStringQuote) {
		setStringQuote(newStringQuote);
		return this;
	}

	/**
	 * Returns the character escaping a string quote within quoted values. If it equals the string
	 * quote, string quotes are doubled (RFC 4180).
	 *
	 * @return the string quote escape character
	 */
	public char getStringQuoteEscapeCharacter() {
		return stringQuoteEscapeCharacter;
	}

	/**
	 * Sets the character escaping a string quote within quoted values, e.g. a backslash. Must be
	 * set after {@link #setStringQuote(Character)}, which resets it.
	 *
	 * @param stringQuoteEscapeCharacter
	 *            the string quote escape character
	 * @throws IllegalArgumentException
	 *             if the character is a line break character or equals the separator
	 */
	public void setStringQuoteEscapeCharacter(final char stringQuoteEscapeCharacter) {
		if (stringQuoteEscapeCharacter == separator || stringQuoteEscapeCharacter == '\r' || stringQuoteEscapeCharacter == '\n') {
			throw new IllegalArgumentException("Stringquote escape character '" + stringQuoteEscapeCharacter + "' is invalid");
		} else {
			this.stringQuoteEscapeCharacter = stringQuoteEscapeCharacter;
		}
	}

	/**
	 * Sets the character escaping a string quote within quoted values, see
	 * {@link #setStringQuoteEscapeCharacter(char)}.
	 *
	 * @param newStringQuoteEscapeCharacter
	 *            the string quote escape character
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the character is invalid
	 */
	public CsvFormat withStringQuoteEscapeCharacter(final char newStringQuoteEscapeCharacter) {
		setStringQuoteEscapeCharacter(newStringQuoteEscapeCharacter);
		return this;
	}

	/**
	 * Returns whether line breaks are allowed within quoted values when reading.
	 *
	 * @return true, if line breaks in data are allowed
	 */
	public boolean isLineBreakInDataAllowed() {
		return lineBreakInDataAllowed;
	}

	/**
	 * Sets whether line breaks are allowed within quoted values when reading.
	 *
	 * @param lineBreakInDataAllowed
	 *            true to allow line breaks in data
	 */
	public void setLineBreakInDataAllowed(final boolean lineBreakInDataAllowed) {
		this.lineBreakInDataAllowed = lineBreakInDataAllowed;
	}

	/**
	 * Sets whether line breaks are allowed within quoted values when reading.
	 *
	 * @param newLineBreakInDataAllowed
	 *            true to allow line breaks in data
	 * @return this format for chaining
	 */
	public CsvFormat withLineBreakInDataAllowed(final boolean newLineBreakInDataAllowed) {
		setLineBreakInDataAllowed(newLineBreakInDataAllowed);
		return this;
	}

	/**
	 * Returns whether escaped string quotes are allowed as part of values when reading. May be
	 * turned off for data consistency checks.
	 *
	 * @return true, if escaped string quotes in data are allowed
	 */
	public boolean isEscapedStringQuoteInDataAllowed() {
		return escapedStringQuoteInDataAllowed;
	}

	/**
	 * Sets whether escaped string quotes are allowed as part of values when reading.
	 *
	 * @param escapedStringQuoteInDataAllowed
	 *            true to allow escaped string quotes in data
	 */
	public void setEscapedStringQuoteInDataAllowed(final boolean escapedStringQuoteInDataAllowed) {
		this.escapedStringQuoteInDataAllowed = escapedStringQuoteInDataAllowed;
	}

	/**
	 * Sets whether escaped string quotes are allowed as part of values when reading.
	 *
	 * @param newEscapedStringQuoteInDataAllowed
	 *            true to allow escaped string quotes in data
	 * @return this format for chaining
	 */
	public CsvFormat withEscapedStringQuoteInDataAllowed(final boolean newEscapedStringQuoteInDataAllowed) {
		setEscapedStringQuoteInDataAllowed(newEscapedStringQuoteInDataAllowed);
		return this;
	}

	/**
	 * Returns whether lines with fewer values than the first line are filled up with null values
	 * when reading, instead of throwing an error.
	 *
	 * @return true, if missing trailing values are filled with null
	 */
	public boolean isFillMissingTrailingColumnsWithNull() {
		return fillMissingTrailingColumnsWithNull;
	}

	/**
	 * Sets whether lines with fewer values than the first line are filled up with null values when
	 * reading, instead of throwing an error.
	 *
	 * @param fillMissingTrailingColumnsWithNull
	 *            true to fill missing trailing values with null
	 */
	public void setFillMissingTrailingColumnsWithNull(final boolean fillMissingTrailingColumnsWithNull) {
		this.fillMissingTrailingColumnsWithNull = fillMissingTrailingColumnsWithNull;
	}

	/**
	 * Sets whether lines with fewer values than the first line are filled up with null values when
	 * reading.
	 *
	 * @param newFillMissingTrailingColumnsWithNull
	 *            true to fill missing trailing values with null
	 * @return this format for chaining
	 */
	public CsvFormat withFillMissingTrailingColumnsWithNull(final boolean newFillMissingTrailingColumnsWithNull) {
		setFillMissingTrailingColumnsWithNull(newFillMissingTrailingColumnsWithNull);
		return this;
	}

	/**
	 * Returns whether surplus trailing values of a line, compared to the first line, are removed
	 * when reading, if they are empty. Non empty surplus values still cause an error.
	 *
	 * @return true, if empty surplus trailing values are removed
	 */
	public boolean isRemoveSurplusEmptyTrailingColumns() {
		return removeSurplusEmptyTrailingColumns;
	}

	/**
	 * Sets whether surplus trailing values of a line are removed when reading, if they are empty.
	 *
	 * @param removeSurplusEmptyTrailingColumns
	 *            true to remove empty surplus trailing values
	 */
	public void setRemoveSurplusEmptyTrailingColumns(final boolean removeSurplusEmptyTrailingColumns) {
		this.removeSurplusEmptyTrailingColumns = removeSurplusEmptyTrailingColumns;
	}

	/**
	 * Sets whether surplus trailing values of a line are removed when reading, if they are empty.
	 *
	 * @param newRemoveSurplusEmptyTrailingColumns
	 *            true to remove empty surplus trailing values
	 * @return this format for chaining
	 */
	public CsvFormat withRemoveSurplusEmptyTrailingColumns(final boolean newRemoveSurplusEmptyTrailingColumns) {
		setRemoveSurplusEmptyTrailingColumns(newRemoveSurplusEmptyTrailingColumns);
		return this;
	}

	/**
	 * Returns whether all values are trimmed when reading.
	 *
	 * @return true, if values are trimmed
	 */
	public boolean isAlwaysTrim() {
		return alwaysTrim;
	}

	/**
	 * Sets whether all values are trimmed when reading.
	 *
	 * @param alwaysTrim
	 *            true to trim values
	 */
	public void setAlwaysTrim(final boolean alwaysTrim) {
		this.alwaysTrim = alwaysTrim;
	}

	/**
	 * Sets whether all values are trimmed when reading.
	 *
	 * @param newAlwaysTrim
	 *            true to trim values
	 * @return this format for chaining
	 */
	public CsvFormat withAlwaysTrim(final boolean newAlwaysTrim) {
		setAlwaysTrim(newAlwaysTrim);
		return this;
	}

	/**
	 * Returns whether lines containing only empty or blank values are skipped when reading.
	 *
	 * @return true, if empty lines are ignored
	 */
	public boolean isIgnoreEmptyLines() {
		return ignoreEmptyLines;
	}

	/**
	 * Sets whether lines containing only empty or blank values are skipped when reading.
	 *
	 * @param ignoreEmptyLines
	 *            true to ignore empty lines
	 */
	public void setIgnoreEmptyLines(final boolean ignoreEmptyLines) {
		this.ignoreEmptyLines = ignoreEmptyLines;
	}

	/**
	 * Sets whether lines containing only empty or blank values are skipped when reading.
	 *
	 * @param newIgnoreEmptyLines
	 *            true to ignore empty lines
	 * @return this format for chaining
	 */
	public CsvFormat withIgnoreEmptyLines(final boolean newIgnoreEmptyLines) {
		setIgnoreEmptyLines(newIgnoreEmptyLines);
		return this;
	}

	/**
	 * Returns the quote mode. For reading, only the difference between {@link QuoteMode#NO_QUOTE}
	 * and the other modes matters.
	 *
	 * @return the quote mode
	 */
	public QuoteMode getQuoteMode() {
		return quoteMode;
	}

	/**
	 * Sets the quote mode.
	 *
	 * @param quoteMode
	 *            the quote mode
	 * @throws IllegalArgumentException
	 *             if the quote mode is null, or quoting is activated while string quote and
	 *             separator are the same character
	 */
	public void setQuoteMode(final QuoteMode quoteMode) {
		if (quoteMode == null) {
			throw new IllegalArgumentException("Given quoteMode is invalid");
		} else if (quoteMode != QuoteMode.NO_QUOTE && separator == stringQuote) {
			throw new IllegalArgumentException("StringQuote '" + stringQuote + "' is invalid");
		} else {
			this.quoteMode = quoteMode;
		}
	}

	/**
	 * Sets the quote mode, see {@link #setQuoteMode(QuoteMode)}.
	 *
	 * @param newQuoteMode
	 *            the quote mode
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the quote mode is invalid
	 */
	public CsvFormat withQuoteMode(final QuoteMode newQuoteMode) {
		setQuoteMode(newQuoteMode);
		return this;
	}

	/**
	 * Returns the line break used when writing. When reading, CR, LF and CRLF are always accepted.
	 *
	 * @return the line break
	 */
	public String getLineBreak() {
		return lineBreak;
	}

	/**
	 * Sets the line break used when writing.
	 *
	 * @param lineBreak
	 *            the line break, one of "\n", "\r\n" or "\r"
	 * @throws IllegalArgumentException
	 *             if the line break is none of the allowed values
	 */
	public void setLineBreak(final String lineBreak) {
		if (!"\r".equals(lineBreak) && !"\n".equals(lineBreak) && !"\r\n".equals(lineBreak)) {
			throw new IllegalArgumentException("Given linebreak is invalid");
		} else {
			this.lineBreak = lineBreak;
		}
	}

	/**
	 * Sets the line break used when writing, see {@link #setLineBreak(String)}.
	 *
	 * @param newLineBreak
	 *            the line break
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the line break is invalid
	 */
	public CsvFormat withLineBreak(final String newLineBreak) {
		setLineBreak(newLineBreak);
		return this;
	}

	/**
	 * Returns whether the first line contains the column headers. This is an information for
	 * users of this format, the reader and writer treat the first line like any other line.
	 *
	 * @return true, if the first line contains headers
	 */
	public boolean isHeaderInFirstLine() {
		return headerInFirstLine;
	}

	/**
	 * Sets whether the first line contains the column headers.
	 *
	 * @param headerInFirstLine
	 *            true, if the first line contains headers
	 */
	public void setHeaderInFirstLine(final boolean headerInFirstLine) {
		this.headerInFirstLine = headerInFirstLine;
	}

	/**
	 * Sets whether the first line contains the column headers.
	 *
	 * @param newHeaderInFirstLine
	 *            true, if the first line contains headers
	 * @return this format for chaining
	 */
	public CsvFormat withHeaderInFirstLine(final boolean newHeaderInFirstLine) {
		setHeaderInFirstLine(newHeaderInFirstLine);
		return this;
	}

	/**
	 * Returns whether backslash escape sequences are used in values (e.g. "\n" for a line break,
	 * "\\" for a backslash). Applies to writing (escaping) and reading (unescaping). Disabled by
	 * default, because RFC 4180 does not know backslash escaping.
	 *
	 * @return true, if backslash escape sequences are used
	 */
	public boolean isEscapeLineBreaks() {
		return escapeLineBreaks;
	}

	/**
	 * Sets whether backslash escape sequences are used in values, see
	 * {@link #isEscapeLineBreaks()}.
	 *
	 * @param escapeLineBreaks
	 *            true to use backslash escape sequences
	 */
	public void setEscapeLineBreaks(final boolean escapeLineBreaks) {
		this.escapeLineBreaks = escapeLineBreaks;
	}

	/**
	 * Sets whether backslash escape sequences are used in values, see
	 * {@link #isEscapeLineBreaks()}.
	 *
	 * @param newEscapeLineBreaks
	 *            true to use backslash escape sequences
	 * @return this format for chaining
	 */
	public CsvFormat withEscapeLineBreaks(final boolean newEscapeLineBreaks) {
		setEscapeLineBreaks(newEscapeLineBreaks);
		return this;
	}
}
