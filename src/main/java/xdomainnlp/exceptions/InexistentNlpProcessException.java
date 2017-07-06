package xdomainnlp.exceptions;

import java.io.EOFException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Based on jplu's stanfordNLPRESTAPI.
 */
public class InexistentNlpProcessException extends RuntimeException {
	static final Logger LOGGER = LoggerFactory.getLogger(InexistentNlpProcessException.class);
	private static final long serialVersionUID = 8554689862256764988L;

	public InexistentNlpProcessException() {
		super();
	}

	public InexistentNlpProcessException(final String message) {
		super(message);
	}

	public InexistentNlpProcessException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public InexistentNlpProcessException(final Throwable cause) {
		super(cause);
	}

	private void writeObject(final ObjectOutputStream stream) throws NotSerializableException {
		throw new NotSerializableException("Impossible to serialize InexistentNlpProcessException");
	}

	private void readObject(final ObjectInputStream stream) throws EOFException {
		throw new EOFException("Improssible to deserialize InexistentNlpProcessException");
	}
}
