import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class ProcessClientRequest implements Runnable{
	private boolean printDebug;
    private Socket client;
    private String directory;
    
    private BufferedReader br;
    private String contentType = "";
    
    private boolean listOfFiles = false;
    private String filePath = "";
    private String method = "";
    
    private int statusCode = 200;
    private StringBuilder response = new StringBuilder();
    private StringBuilder responseBody = new StringBuilder();
    
    ProcessClientRequest(Socket client, boolean printDebug, String directory) {
        this.client = client;
        this.printDebug = printDebug;
        this.directory = directory;
        new Thread(this).start();
    }
    
    public void run() {
	    try {
	            if (printDebug) 
	            	System.out.println("\n**Received Request**\n");
	            processRequest();
	            
	            if(listOfFiles) {
	            	System.out.println("Sending list of all files in current directory");
	            	sendAllFiles(directory, responseBody);
	            }
	            else if(method.equals("GET")) {
	            	
	            }else if(method.equals("POST")) {
	            	
	            }
	            
	            generateResponse();
	            if (printDebug) 
	            	System.out.println("\n**Sending Response**\n");
	            System.out.println("Sent Response"+response);
	            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
	            bw.write(response.toString());
	            bw.flush();
	            br.close();
	            bw.close();
	            if (printDebug) 
	            	System.out.println("\n**Complete**\n");
	            Thread.sleep(1000);
	            
	            
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
            if (printDebug) 
            	System.out.println(line);
            
            if (method.isEmpty()) {
                if (line.substring(0, 3).equals("GET")) {
                    method = "GET";
                } else if (line.substring(0, 4).equals("POST")) {
                    method = "POST";
                }
                int pathBeginAt = line.indexOf("/");
                filePath = line.substring(pathBeginAt, line.indexOf(" ", pathBeginAt+1));
                if (filePath.equals("/")) {
                    listOfFiles = true;
                    return;
                }
            }
        }
        
        
    }
    
    private void sendAllFiles(String path, StringBuilder responseBody) {
        File curDir = new File(path);
        File[] filesList = curDir.listFiles();
        if (null != filesList) {
            for (File file : filesList) {
                if(file.isFile()) {
                    responseBody.append(".").append(file.getPath().substring(directory.length())).append("\r\n");
                } else if (file.isDirectory()) {
                	sendAllFiles(path + "/" + file.getName(), responseBody);
                }
            }
        }
    }
    
    private void generateResponse() throws Exception {
        if (statusCode == 404) {
            response.append("HTTP/1.1 404 NOT FOUND\r\n");
            responseBody.append("The requested URL was not found on the server.\r\n");
            responseBody.append("If you entered the URL manually, please check you spelling and try again.\r\n");
        } else if (statusCode == 403) {
            response.append("HTTP/1.1 403 Forbidden\r\n");
        } else if (statusCode == 400) {
            response.append("HTTP/1.1 400 Bad Request\r\n");
        } else {
            Thread.sleep(1000);
            response.append("HTTP/1.1 200 OK\r\n");
            if (method.equals("POST"))
            responseBody.append("Post file successfully.");
        }
        response.append("Connection: close\r\n");
        response.append("Server: httpfs\n");
        response.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
        response.append("Content-Type: ").append(contentType).append("\r\n");
        response.append("Content-Length: ").append(responseBody.length()).append("\r\n");
        response.append("\r\n");
        response.append(responseBody.toString());
    }

}
