package de.soderer.utilities.csv;

/**
 * Exception signaling invalid CSV data, carrying the number of the line in which the error was
 * detected.
 */
public class CsvDataException extends Exception {
	/** Serial version UID for serialization. */
	private static final long serialVersionUID = 5128483227215628395L;

	/** Number of the CSV line in which the error was detected, or -1 if unknown. */
	private final int errorLineNumber;

	/**
	 * Creates a new exception without message.
	 *
	 * @param errorLineNumber
	 *            the number of the CSV line in which the error was detected, or -1 if unknown
	 */
	public CsvDataException(final int errorLineNumber) {
		super();
		this.errorLineNumber = errorLineNumber;
	}

	/**
	 * Creates a new exception with message and cause.
	 *
	 * @param message
	 *            the detail message
	 * @param errorLineNumber
	 *            the number of the CSV line in which the error was detected, or -1 if unknown
	 * @param cause
	 *            the cause of this exception
	 */
	public CsvDataException(final String message, final int errorLineNumber, final Throwable cause) {
		super(message, cause);
		this.errorLineNumber = errorLineNumber;
	}

	/**
	 * Creates a new exception with message.
	 *
	 * @param message
	 *            the detail message
	 * @param errorLineNumber
	 *            the number of the CSV line in which the error was detected, or -1 if unknown
	 */
	public CsvDataException(final String message, final int errorLineNumber) {
		super(message);
		this.errorLineNumber = errorLineNumber;
	}

	/**
	 * Creates a new exception with cause.
	 *
	 * @param errorLineNumber
	 *            the number of the CSV line in which the error was detected, or -1 if unknown
	 * @param cause
	 *            the cause of this exception
	 */
	public CsvDataException(final int errorLineNumber, final Throwable cause) {
		super(cause);
		this.errorLineNumber = errorLineNumber;
	}

	/**
	 * Returns the number of the CSV line in which the error was detected.
	 *
	 * @return the line number, or -1 if unknown
	 */
	public int getErrorLineNumber() {
		return errorLineNumber;
	}
}
