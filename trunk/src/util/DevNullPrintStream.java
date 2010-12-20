package util;

import java.io.PrintStream;

public class DevNullPrintStream extends PrintStream {
	public DevNullPrintStream() { super(new DevNullOutputStream()); }
	public void write(byte[] buf, int off, int len) {}
	public void write(int b) {}
	public void write(byte [] b) {}
}
