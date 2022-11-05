package com.exper.socks.net;

public class SocksException extends RuntimeException {

	private static final long serialVersionUID = 2462760291055303580L;

	public SocksException() {
		super();
	}

	public SocksException(String message) {
		super(message);
	}

	public SocksException(String message, Throwable ex) {
		super(message, ex);
	}

	public SocksException(Throwable ex) {
		super(ex);
	}
}
