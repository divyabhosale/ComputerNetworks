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
        	System.out.println("Starting server...");
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started...");
            while (true) {
                //Socket clientSocket = serverSocket.accept();
                //new handleClientRequest(client, printDebugMessage, directoryPath);
                Socket client = serverSocket.accept();
                System.out.println("New connection from " + client.getRemoteSocketAddress());
                new ProcessClientRequest(client, printDebug, directory);
                
       
            }
        } catch (IOException e) {
            System.out.println("Error starting server  " + e.getMessage());
        }
    }
}
