import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ProcessClientRequest implements Runnable{
	private boolean printDebugMessage;
    private Socket client;
    private String directoryPath;
    
    private BufferedReader br;
    
    ProcessClientRequest(Socket client, boolean printDebugMessage, String directoryPath) {
        this.client = client;
        this.printDebugMessage = printDebugMessage;
        this.directoryPath = directoryPath;
        new Thread(this).start();
    }
    
    public void run() {
	    try {
	    	System.out.println(printDebugMessage);
	            if (printDebugMessage) System.out.println("*Received Request*");
	            processRequest();
	    }catch (Exception e) {
	    	e.printStackTrace();
	        System.out.println("ERROR " + e.getMessage());
	    }finally {
	        if (client != null) {
	            try {
	                client.close();
	            } catch (Exception e) {
	                client = null;
	                System.out.println("ERROR closing connection" + e.getMessage());
	            }
	        }
	    }
    }
    
    private void processRequest() throws Exception {
        String line;
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while ((line = br.readLine()) != null) {
            if (printDebugMessage) System.out.println(line);
        }
    }
}
