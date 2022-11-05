package com.exper.socks.net;


import com.exper.socks.net.SocksException;

public class SocksParsingException extends SocksException {
	public SocksParsingException(String string) {
		super(string);
	}

	public SocksParsingException(String string, Throwable ex) {
		super(string, ex);
	}

	private static final long serialVersionUID = -4997757416476363399L;
}
