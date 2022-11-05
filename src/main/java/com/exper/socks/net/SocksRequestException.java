package com.exper.socks.net;

public class SocksRequestException extends Exception {
	
	private static final long serialVersionUID = 844055056337565049L;

	public SocksRequestException() {}
	public SocksRequestException(String msg) {
		super(msg);
	}
	public SocksRequestException(Throwable ex) {
		super(ex);
	}
}
