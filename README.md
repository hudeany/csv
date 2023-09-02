# Java CSV-Reader an CSV-Writer

CsvReader can read .csv files in any characterset encoding (also with BOM) and with configurable separator and optional stringquote (delimiter).

CsvWriter can write .csv files and supports the same features as CsvReader.

There are also some other convenient optional configuration values.

For feature documentation and examples see wiki or javadoc.

Features

    Using data streams instead of files only
    Considers linebreaks within quoted data
    Considers string quote character within quoted data
    Configurable separator character
    Configurable optional string quote character
    Configurable allows too short csvdata lines (trailing empty columns)
    Read line per line or all data at once
