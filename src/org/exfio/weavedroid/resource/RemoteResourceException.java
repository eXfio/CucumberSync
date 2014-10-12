package org.exfio.weavedroid.resource;

public class RemoteResourceException extends Exception {
	private static final long serialVersionUID = 1593264832655590320L;
	
	public RemoteResourceException(String message) {
		super(message);
	}

	public RemoteResourceException(Throwable throwable) {
		super(throwable);
	}

	public RemoteResourceException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
