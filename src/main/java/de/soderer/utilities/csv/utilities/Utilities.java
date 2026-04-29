package de.soderer.utilities.csv.utilities;

public class Utilities {
	public static String normalizeLinebreaks(final String value) {
		return value.replace("\r\n", "\n").replace("\r", "\n");
	}

	public static String escapeCSV(final String text) {
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

	public static String unescapeCSV(final String javaEscapedText) throws Exception {
		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = javaEscapedText.length();

		for (int i = 0; i < length; i++) {
			final char nextChar = javaEscapedText.charAt(i);

			if (nextChar == '\\' && i + 1 < length) {
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
					case 'x': // hexadecimal escapes: 8-bit size
						if (i + 3 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 4);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 3;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid hex sequence at character index " + i + " ('" + "\\x" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\x" + invalidHex + "')");
						}
						break;
					case 'u': // Java escapes: 16-bit size
						if (i + 5 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 6);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 5;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + invalidHex + "')");
						}
						break;
					case 'U': // Unicode escapes: 32-bit size
						if (i + 9 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 10);
							try {
								final int code = Integer.parseInt(hex, 32);
								unescapedTextBuilder.append((char) code);
								i += 9;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + invalidHex + "')");
						}
						break;
					default:
						throw new Exception("Invalid escape sequence at character index " + i + " ('" + "\\" + oneMoreChar + "')");
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}

		return unescapedTextBuilder.toString();
	}
}
