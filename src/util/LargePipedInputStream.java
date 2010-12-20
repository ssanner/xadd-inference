package util;

import java.io.*;

public class LargePipedInputStream 
	extends PipedInputStream {
	
	protected static int PIPE_SIZE = 65536;
	protected byte[] buffer = new byte[PIPE_SIZE];

	/** Do not call **/
	private LargePipedInputStream() { }
	
	public LargePipedInputStream(PipedOutputStream src) 
		throws IOException {
		connect(src);
	}
	
	public int getBufferSize() {
		return buffer.length;
	}
	
	public static void main(String[] args) {
		try {
			LargePipedInputStream lpis = new LargePipedInputStream();
			System.out.println("Bytes available = " + lpis.available());
			System.out.println("Buffer size =     " + lpis.getBufferSize());
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}
