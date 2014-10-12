package org.exfio.weavedroid;

public class AccountNotFoundException extends Exception {
	private static final long serialVersionUID = 8925373748071527486L;
	
	public AccountNotFoundException(String message) {
		super(message);
	}

	public AccountNotFoundException(Throwable throwable) {
		super(throwable);
	}

	public AccountNotFoundException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
