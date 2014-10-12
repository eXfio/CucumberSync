package org.exfio.weavedroid;

public class WeaveDroidReturnValue {
	private Exception exception = null;
	private String message      = null;
	
	public void setException(Exception e) {
		this.exception = e;
		this.message   = e.getMessage();
	}

	public Exception getException() {
		return exception;
	}

	public void setMessage(String message) {
		this.exception = null;
		this.message   = message;
	}

	public String getMessage() {
		if ( exception != null ) {
			return String.format("%s - %s", exception.getClass().getName(), message);
		} else {
			return message;
		}
	}
}
