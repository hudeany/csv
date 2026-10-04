package de.soderer.utilities.csv.utilities;

/**
 * Text helper methods for the CSV library.
 */
public class Utilities {
	/**
	 * Utility class, not to be instantiated.
	 */
	private Utilities() {
	}

	/**
	 * Normalizes all line breaks (CRLF, CR, LF) to unix style (LF).
	 *
	 * @param value
	 *            the text to normalize, may be null
	 * @return the normalized text, or null if the value was null
	 */
	public static String normalizeLinebreaks(final String value) {
		if (value == null) {
			return null;
		}
		return value.replace("\r\n", "\n").replace("\r", "\n");
	}

	/**
	 * Escapes a text for CSV output using backslash escape sequences.
	 * <p>
	 * Backslash, LF, CR, tab, backspace and form feed are escaped as \\, \n, \r, \t, \b and \f.
	 * Other control characters (below 32 and 127) are escaped as unicode sequences like u0007.
	 * Quote characters are not escaped. {@link #unescapeCSV(String)} reverses this escaping.
	 * </p>
	 *
	 * @param text
	 *            the text to escape, may be null
	 * @return the escaped text, or null if the text was null
	 */
	public static String escapeCSV(final String text) {
		if (text == null) {
			return null;
		}

		final StringBuilder escapedTextBuilder = new StringBuilder();
		for (final char nextChar : text.toCharArray()) {
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\b':
					escapedTextBuilder.append("\\b");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				default:
					if (nextChar < 32 || nextChar == 127) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}
		return escapedTextBuilder.toString();
	}

	/**
	 * Resolves backslash escape sequences in a text, reversing {@link #escapeCSV(String)}.
	 * <p>
	 * Supported sequences are \n, \r, \t, \b, \f, escaped space, \\, \', \", hexadecimal
	 * sequences with 2 digits (x41), 16 bit unicode sequences with 4 digits (u0041) and unicode
	 * code points with 8 digits (U0001F600).
	 * </p>
	 *
	 * @param javaEscapedText
	 *            the text to unescape, may be null
	 * @return the unescaped text, or null if the text was null
	 * @throws Exception
	 *             if the text contains an unknown escape sequence, an invalid hexadecimal or
	 *             unicode sequence, or ends with a single unescaped backslash
	 */
	public static String unescapeCSV(final String javaEscapedText) throws Exception {
		if (javaEscapedText == null) {
			return null;
		}

		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = javaEscapedText.length();
		for (int i = 0; i < length; i++) {
			final char nextChar = javaEscapedText.charAt(i);

			if (nextChar == '\\') {
				if (i + 1 >= length) {
					throw new Exception("Invalid escape sequence at character index " + i + " (unescaped backslash at end of text)");
				}
				final char oneMoreChar = javaEscapedText.charAt(i + 1);
				switch (oneMoreChar) {
					case 'n':
						unescapedTextBuilder.append('\n');
						i++;
						break;
					case 'r':
						unescapedTextBuilder.append('\r');
						i++;
						break;
					case 't':
						unescapedTextBuilder.append('\t');
						i++;
						break;
					case 'b':
						unescapedTextBuilder.append('\b');
						i++;
						break;
					case 'f':
						unescapedTextBuilder.append('\f');
						i++;
						break;
					case ' ':
						unescapedTextBuilder.append(' ');
						i++;
						break;
					case '\\':
						unescapedTextBuilder.append('\\');
						i++;
						break;
					case '\'':
						unescapedTextBuilder.append('\'');
						i++;
						break;
					case '\"':
						unescapedTextBuilder.append('\"');
						i++;
						break;
					case 'x':
						// Hexadecimal escapes: 8 bit size
						unescapedTextBuilder.append((char) parseHexDigits(javaEscapedText, i, 2, "hex"));
						i += 3;
						break;
					case 'u':
						// Java escapes: 16 bit size
						unescapedTextBuilder.append((char) parseHexDigits(javaEscapedText, i, 4, "unicode"));
						i += 5;
						break;
					case 'U': {
						// Unicode escapes: 32 bit size
						final int codePoint = parseHexDigits(javaEscapedText, i, 8, "unicode");
						if (!Character.isValidCodePoint(codePoint)) {
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + javaEscapedText.substring(i, i + 10) + "')");
						}
						unescapedTextBuilder.append(Character.toChars(codePoint));
						i += 9;
						break;
					}
					default:
						throw new Exception("Invalid escape sequence at character index " + i + " ('" + "\\" + oneMoreChar + "')");
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}
		return unescapedTextBuilder.toString();
	}

	/**
	 * Parses the hexadecimal digits of an escape sequence like \x41, \u0041 or \U0001F600.
	 *
	 * @param text
	 *            the text containing the escape sequence
	 * @param sequenceStartIndex
	 *            the index of the backslash starting the escape sequence
	 * @param digitCount
	 *            the number of hexadecimal digits expected after the backslash and type character
	 * @param sequenceType
	 *            the type of the sequence for error messages ("hex" or "unicode")
	 * @return the parsed value
	 * @throws Exception
	 *             if there are not enough characters left or a character is not a hexadecimal
	 *             digit
	 */
	private static int parseHexDigits(final String text, final int sequenceStartIndex, final int digitCount, final String sequenceType) throws Exception {
		final int digitsStartIndex = sequenceStartIndex + 2;
		final int digitsEndIndex = Math.min(digitsStartIndex + digitCount, text.length());
		int value = 0;
		for (int i = digitsStartIndex; i < digitsStartIndex + digitCount; i++) {
			final int digit = i < text.length() ? hexDigitValue(text.charAt(i)) : -1;
			if (digit == -1) {
				throw new Exception("Invalid " + sequenceType + " sequence at character index " + sequenceStartIndex + " ('" + text.substring(sequenceStartIndex, digitsEndIndex) + "')");
			}
			value = (value << 4) | digit;
		}
		return value;
	}

	/**
	 * Returns the value of an ASCII hexadecimal digit (0-9, a-f, A-F). Unlike
	 * {@link Character#digit(char, int)} this does not accept non ASCII digits like fullwidth or
	 * Arabic-Indic digits.
	 *
	 * @param character
	 *            the character to convert
	 * @return the value 0 to 15, or -1 if the character is no ASCII hexadecimal digit
	 */
	private static int hexDigitValue(final char character) {
		if (character >= '0' && character <= '9') {
			return character - '0';
		} else if (character >= 'a' && character <= 'f') {
			return character - 'a' + 10;
		} else if (character >= 'A' && character <= 'F') {
			return character - 'A' + 10;
		} else {
			return -1;
		}
	}
}
