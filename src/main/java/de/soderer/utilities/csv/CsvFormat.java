package de.soderer.utilities.csv;

public class CsvFormat {
	/** The Constant DEFAULT_SEPARATOR. */
	public static final char DEFAULT_SEPARATOR = ',';

	/** The Constant DEFAULT_STRING_QUOTE. */
	public static final char DEFAULT_STRING_QUOTE = '"';

	/** Default output linebreak. */
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

	/** Use "\\n" to escape linebreaks on csv output */
	private boolean escapeLineBreaks = true;

	/**
	 * The Enum QuoteMode.
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

		public static QuoteMode getFromString(final String quoteModeString) throws Exception {
			for (final QuoteMode quoteMode : QuoteMode.values()) {
				if (quoteMode.toString().equalsIgnoreCase(quoteModeString)) {
					return quoteMode;
				}
			}
			throw new Exception("Invalid quote mode: " + quoteModeString);
		}
	}

	public CsvFormat() {
	}

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

	public char getSeparator() {
		return separator;
	}

	public void setSeparator(final char separator) {
		if (separator == '\r' || separator == '\n') {
			throw new IllegalArgumentException("Separator '" + separator + "' is invalid");
		} else if (quoteMode != QuoteMode.NO_QUOTE && separator == stringQuote) {
			throw new IllegalArgumentException("Separator '" + separator + "' is invalid");
		} else {
			this.separator = separator;
		}
	}

	public CsvFormat withSeparator(final char newSeparator) {
		setSeparator(newSeparator);
		return this;
	}

	public char getStringQuote() {
		return stringQuote;
	}

	/**
	 * Setter for stringQuote character.
	 * Also sets stringQuoteEscape character.
	 *
	 * @param stringQuote
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

	public CsvFormat withStringQuote(final Character newStringQuote) {
		setStringQuote(newStringQuote);
		return this;
	}

	public char getStringQuoteEscapeCharacter() {
		return stringQuoteEscapeCharacter;
	}

	public void setStringQuoteEscapeCharacter(final char stringQuoteEscapeCharacter) {
		if (stringQuoteEscapeCharacter == separator || stringQuoteEscapeCharacter == '\r' || stringQuoteEscapeCharacter == '\n') {
			throw new IllegalArgumentException("Stringquote escape character '" + stringQuoteEscapeCharacter + "' is invalid");
		} else {
			this.stringQuoteEscapeCharacter = stringQuoteEscapeCharacter;
		}
	}

	public CsvFormat withStringQuoteEscapeCharacter(final char newStringQuoteEscapeCharacter) {
		setStringQuoteEscapeCharacter(newStringQuoteEscapeCharacter);
		return this;
	}

	public boolean isLineBreakInDataAllowed() {
		return lineBreakInDataAllowed;
	}

	public void setLineBreakInDataAllowed(final boolean lineBreakInDataAllowed) {
		this.lineBreakInDataAllowed = lineBreakInDataAllowed;
	}

	public CsvFormat withLineBreakInDataAllowed(final boolean newLineBreakInDataAllowed) {
		setLineBreakInDataAllowed(newLineBreakInDataAllowed);
		return this;
	}

	public boolean isEscapedStringQuoteInDataAllowed() {
		return escapedStringQuoteInDataAllowed;
	}

	public void setEscapedStringQuoteInDataAllowed(final boolean escapedStringQuoteInDataAllowed) {
		this.escapedStringQuoteInDataAllowed = escapedStringQuoteInDataAllowed;
	}

	public CsvFormat withEscapedStringQuoteInDataAllowed(final boolean newEscapedStringQuoteInDataAllowed) {
		setEscapedStringQuoteInDataAllowed(newEscapedStringQuoteInDataAllowed);
		return this;
	}

	public boolean isFillMissingTrailingColumnsWithNull() {
		return fillMissingTrailingColumnsWithNull;
	}

	public void setFillMissingTrailingColumnsWithNull(final boolean fillMissingTrailingColumnsWithNull) {
		this.fillMissingTrailingColumnsWithNull = fillMissingTrailingColumnsWithNull;
	}

	public CsvFormat withFillMissingTrailingColumnsWithNull(final boolean newFillMissingTrailingColumnsWithNull) {
		setFillMissingTrailingColumnsWithNull(newFillMissingTrailingColumnsWithNull);
		return this;
	}

	public boolean isRemoveSurplusEmptyTrailingColumns() {
		return removeSurplusEmptyTrailingColumns;
	}

	public void setRemoveSurplusEmptyTrailingColumns(final boolean removeSurplusEmptyTrailingColumns) {
		this.removeSurplusEmptyTrailingColumns = removeSurplusEmptyTrailingColumns;
	}

	public CsvFormat withRemoveSurplusEmptyTrailingColumns(final boolean newRemoveSurplusEmptyTrailingColumns) {
		setRemoveSurplusEmptyTrailingColumns(newRemoveSurplusEmptyTrailingColumns);
		return this;
	}

	public boolean isAlwaysTrim() {
		return alwaysTrim;
	}

	public void setAlwaysTrim(final boolean alwaysTrim) {
		this.alwaysTrim = alwaysTrim;
	}

	public CsvFormat withAlwaysTrim(final boolean newAlwaysTrim) {
		setAlwaysTrim(newAlwaysTrim);
		return this;
	}

	public boolean isIgnoreEmptyLines() {
		return ignoreEmptyLines;
	}

	public void setIgnoreEmptyLines(final boolean ignoreEmptyLines) {
		this.ignoreEmptyLines = ignoreEmptyLines;
	}

	public CsvFormat withIgnoreEmptyLines(final boolean newIgnoreEmptyLines) {
		setIgnoreEmptyLines(newIgnoreEmptyLines);
		return this;
	}

	public QuoteMode getQuoteMode() {
		return quoteMode;
	}

	public void setQuoteMode(final QuoteMode quoteMode) {
		if (quoteMode == null) {
			throw new IllegalArgumentException("Given quoteMode is invalid");
		} else if (quoteMode != QuoteMode.NO_QUOTE && separator == stringQuote) {
			throw new IllegalArgumentException("StringQuote '" + stringQuote + "' is invalid");
		} else {
			this.quoteMode = quoteMode;
		}
	}

	public CsvFormat withQuoteMode(final QuoteMode newQuoteMode) {
		setQuoteMode(newQuoteMode);
		return this;
	}

	public String getLineBreak() {
		return lineBreak;
	}

	public void setLineBreak(final String lineBreak) {
		if (!"\r".equals(lineBreak) && !"\n".equals(lineBreak) && !"\r\n".equals(lineBreak)) {
			throw new IllegalArgumentException("Given linebreak is invalid");
		} else {
			this.lineBreak = lineBreak;
		}
	}

	public CsvFormat withLineBreak(final String newLineBreak) {
		setLineBreak(newLineBreak);
		return this;
	}

	public boolean isHeaderInFirstLine() {
		return headerInFirstLine;
	}

	public void setHeaderInFirstLine(final boolean headerInFirstLine) {
		this.headerInFirstLine = headerInFirstLine;
	}

	public CsvFormat withHeaderInFirstLine(final boolean newHeaderInFirstLine) {
		setHeaderInFirstLine(newHeaderInFirstLine);
		return this;
	}

	public boolean isEscapeLineBreaks() {
		return escapeLineBreaks;
	}

	public void setEscapeLineBreaks(final boolean escapeLineBreaks) {
		this.escapeLineBreaks = escapeLineBreaks;
	}

	public CsvFormat withEscapeLineBreaks(final boolean newEscapeLineBreaks) {
		setEscapeLineBreaks(newEscapeLineBreaks);
		return this;
	}
}
