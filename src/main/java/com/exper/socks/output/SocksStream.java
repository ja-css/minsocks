package com.exper.socks.output;

import java.io.InputStream;
import java.io.OutputStream;

public interface SocksStream {
	/**
	 * Close this stream.
	 */
	void close();

	/**
	 * Returns an {@link InputStream} for sending data on this stream.
	 * 
	 * @return An {@link InputStream} for transferring data on this stream.
	 */
	InputStream getInputStream();

	/**
	 * Returns an {@link OutputStream} for receiving data from this stream.
	 * 
	 * @return An {@link OutputStream} for receiving data from this stream.
	 */
	OutputStream getOutputStream();
}
