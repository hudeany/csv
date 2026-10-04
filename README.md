# Java CSV Reader and Writer

[![Maven Central](https://img.shields.io/maven-central/v/de.soderer/csv)](https://central.sonatype.com/artifact/de.soderer/csv)

A lightweight, dependency-free Java library for reading and writing CSV data.
It follows [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180) by default and can be configured for almost any CSV dialect found in the wild.

## Highlights

- **No dependencies**: plain Java, a single small jar
- **Stream based**: works with any `InputStream` / `OutputStream`, not only files
- **Any encoding**: UTF-8 by default, a leading UTF-8 BOM is detected and skipped
- **Robust parsing**: line breaks, separators and quotes inside quoted values, CR / LF / CRLF line endings
- **Flexible format**: separator, string quote, escape character, quote mode, line break and more
- **Lenient or strict**: tolerate missing or surplus columns, or fail fast with the exact line number
- **Read line by line or all at once**, write line by line or all at once

## Installation

The library is available on Maven Central. Replace `VERSION` with the version shown in the badge above.

**Maven**

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>csv</artifactId>
	<version>VERSION</version>
</dependency>
```

**Gradle**

```groovy
implementation "de.soderer:csv:VERSION"
```

**Without a build tool**, download the jar from the [GitHub releases](https://github.com/hudeany/csv/releases).

## Quick start

### Reading all lines at once

```java
final String csvData =
	"name;city;age\n"
	+ "Alice;Munich;34\n"
	+ "Bob;\"Berlin; Mitte\";29\n";

final CsvFormat csvFormat = new CsvFormat().withSeparator(';');

try (CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat)) {
	final List<List<String>> lines = reader.readAll();
	System.out.println(lines);
	// [[name, city, age], [Alice, Munich, 34], [Bob, Berlin; Mitte, 29]]
}
```

### Reading line by line

Recommended for large files, as only one line is held in memory at a time.

```java
try (CsvReader reader = new CsvReader(new FileInputStream("data.csv"), StandardCharsets.UTF_8, csvFormat)) {
	List<String> values;
	while ((values = reader.readNextCsvLine()) != null) {
		System.out.println(reader.getReadCsvLines() + ": " + values);
	}
}
```

### Writing

```java
final CsvFormat csvFormat = new CsvFormat()
	.withSeparator(';')
	.withLineBreak("\r\n");

try (CsvWriter writer = new CsvWriter(new FileOutputStream("data.csv"), csvFormat)) {
	writer.writeValues("name", "city", "age");
	writer.writeValues("Alice", "Munich", 34);
	writer.writeValues("Bob", "Berlin; Mitte", 29);
}
```

Result:

```
name;city;age
Alice;Munich;34
Bob;"Berlin; Mitte";29
```

Values are quoted only where needed, `null` is written as an empty value and any object is written by its `toString()`.

### Single lines

For a quick conversion of a single line without streams:

```java
final String line = CsvWriter.getCsvLine(new CsvFormat(), "a", "b,c", null, 42);
// a,"b,c",,42

final List<String> values = CsvReader.parseCsvLine(line);
// [a, b,c, , 42]
```

## Configuration

All settings are made on a `CsvFormat` object, which is passed to the reader or writer.
Each setting has a bean-style setter (`setX(...)`) and a chainable variant (`withX(...)`).

| Setting | Default | Description |
|---|---|---|
| `withSeparator(char)` | `,` | Character separating the values |
| `withStringQuote(Character)` | `"` | Character enclosing values; `null` disables quoting |
| `withStringQuoteEscapeCharacter(char)` | `"` | Escapes a string quote inside quoted values; by default the quote is doubled (`""`), may be set to e.g. `\` |
| `withQuoteMode(QuoteMode)` | `QUOTE_IF_NEEDED` | When values are quoted on output, see below |
| `withLineBreak(String)` | `\n` | Line break on output (`\n`, `\r\n` or `\r`); on input all three are always accepted |
| `withLineBreakInDataAllowed(boolean)` | `true` | Allow line breaks inside quoted values |
| `withEscapedStringQuoteInDataAllowed(boolean)` | `true` | Allow escaped string quotes as part of values; disable for strict data checks |
| `withFillMissingTrailingColumnsWithNull(boolean)` | `false` | Fill lines with too few values with `null` instead of failing |
| `withRemoveSurplusEmptyTrailingColumns(boolean)` | `false` | Accept lines with too many values, if the surplus values are empty |
| `withAlwaysTrim(boolean)` | `false` | Trim all values on input |
| `withIgnoreEmptyLines(boolean)` | `false` | Skip lines that contain only empty or blank values |
| `withEscapeLineBreaks(boolean)` | `false` | Use backslash escape sequences like `\n` inside values, see below |
| `withHeaderInFirstLine(boolean)` | `true` | Marks the first line as column headers (information for your application) |

The number of values in the first line defines the expected number of values for all following lines.

### Quote modes

| Mode | Output behaviour |
|---|---|
| `NO_QUOTE` | Never quote; fails if a value would need quoting |
| `QUOTE_IF_NEEDED` | Quote only values containing the separator, the string quote or a line break |
| `QUOTE_STRINGS` | Always quote `String` values, other values only if needed |
| `QUOTE_ALL_DATA` | Quote every value |

### Escaping

By default the library follows RFC 4180: a string quote inside a quoted value is doubled, backslashes are plain characters and line breaks are written as real line breaks inside quoted values.

```java
// Backslash as escape character for string quotes
new CsvFormat().withStringQuoteEscapeCharacter('\\');
// "say \"hi\""

// Keep every record on one physical line by escaping line breaks
new CsvFormat().withEscapeLineBreaks(true);
// first\nsecond
```

With `withEscapeLineBreaks(true)` the sequences `\n`, `\r`, `\t`, `\b`, `\f`, `\\` and unicode sequences like `\u00e4` are resolved on input. Unknown sequences cause an error, so only enable it for data that was written with this convention.

### Lenient reading

```java
final CsvFormat lenientFormat = new CsvFormat()
	.withFillMissingTrailingColumnsWithNull(true)
	.withRemoveSurplusEmptyTrailingColumns(true)
	.withIgnoreEmptyLines(true)
	.withAlwaysTrim(true);
```

## Error handling

Invalid data causes a `CsvDataException`, which carries the number of the CSV line where the problem was found:

```java
try (CsvReader reader = new CsvReader(inputStream)) {
	reader.readAll();
} catch (final CsvDataException e) {
	System.err.println("Line " + e.getErrorLineNumber() + ": " + e.getMessage());
	// Line 2: Inconsistent number of values in line 2 (expected: 2 actually: 3)
}
```

Data ending inside an unclosed quoted value causes an `IOException`.

## More features

**Aligned output**: pad columns to a minimum width for human-readable files.

```java
writer
	.withMinimumColumnSizes(new int[] { 8, 6 })
	.withColumnPaddings(new boolean[] { true, false }); // true = left aligned
// name    ;   age
// Alice   ;    34
```

**Counting lines**: `reader.getCsvLineCount()` counts the CSV records of a stream. Line breaks inside quoted values are handled correctly, so the result may be lower than the number of physical lines.

**Duplicate headers**: `CsvReader.checkForDuplicateCsvHeader(headers)` returns the first duplicate column header or `null`.

**Statistics**: `reader.getReadCsvLines()`, `reader.getReadDataSize()` (bytes read) and `writer.getWrittenLines()`.
