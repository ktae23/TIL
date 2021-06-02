package design;

import design.facade.FTP;
import design.facade.Reader;
import design.facade.SFtpClient;
import design.facade.Writer;

public class Main {

	public static void main(String[] args) {
		
		FTP ftpClient = new FTP("www.foo.co.kr", 22, "/home/etc");
		ftpClient.connect();
		ftpClient.movevDirectory();

		Reader reader = new Reader("text.temp");
		reader.fileConnect();
		reader.fileRead();

		
		Writer writer = new Writer("text.tmp");
		writer.fileConnect();
		writer.fileWrite();
		
		writer.fileDisconnect();		
		reader.fileDisconnect();
		ftpClient.disConnect();
	
		
		SFtpClient sftpCiClient = new SFtpClient("www.foo.co.kr", 22, "/home/etc/", "text.tmp");
		sftpCiClient.connect();
		sftpCiClient.read();
		sftpCiClient.write();
		sftpCiClient.disConnect();
		
	}
}
	
