package org.grouplens.lenskit.webapp;

public class BadRequestException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super();
	}
	
	public BadRequestException(String message) {
		super(message);
	}
	
	public BadRequestException(Throwable cause) {
		super(cause);
	}
	
	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}
