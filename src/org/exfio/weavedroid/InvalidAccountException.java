package org.exfio.weavedroid;

public class InvalidAccountException extends Exception {
	private static final long serialVersionUID = 7225373793851520256L;
	
	public InvalidAccountException(String message) {
		super(message);
	}

	public InvalidAccountException(Throwable throwable) {
		super(throwable);
	}

	public InvalidAccountException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
