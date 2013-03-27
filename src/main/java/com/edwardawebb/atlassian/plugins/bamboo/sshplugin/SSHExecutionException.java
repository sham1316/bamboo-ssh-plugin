/**
 * Copyright Oct 10, 2012 Edward A. Webb
 * 
 */
package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

/**
 * Represents an error whilst executing a command on the remote host.
 * @author Edward A. Webb
 */
public class SSHExecutionException extends RuntimeException {

	
	/**
	 * 
	 */
	public SSHExecutionException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public SSHExecutionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public SSHExecutionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SSHExecutionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
