import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
	 private int portNumber;
	 private boolean printDebug;
	 private String directory;
	 
	public HttpServer() {
		portNumber = 8080;
		printDebug = false;
        directory = ".";
    }

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public boolean isPrintDebug() {
		return printDebug;
	}

	public void setPrintDebug(boolean printDebug) {
		this.printDebug = printDebug;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
	
	void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                //new handleClientRequest(client, printDebugMessage, directoryPath);
            }
        } catch (IOException e) {
            System.out.println("HttpFileServer.run():  " + e.getMessage());
        }
    }
}
