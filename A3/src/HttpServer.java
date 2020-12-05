
public class HttpServer {
	 private int portNumber;
	 private boolean printDebug;
	 private String directory;
	 
	public HttpServer() {
		portNumber = 8007;
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
        
        	System.out.println("Starting server...");
            MyServerSocket serverSocket = new MyServerSocket(portNumber);
            System.out.println("Server started...");
            while (true) {
              
            	MyServerSocket newServerSocket = serverSocket.accept();
                new ProcessClientRequest(newServerSocket, printDebug, directory);
                
       
            }
       
    }
}
