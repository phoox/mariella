package org.mariella.oxygen.remoting.http.common;

public class AuthenticationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

public AuthenticationException(String message) {
	super(message);
}

public AuthenticationException(String message, Throwable cause) {
	super(message, cause);
}

}
