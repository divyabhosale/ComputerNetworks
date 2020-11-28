import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;

public class ProcessClientRequest implements Runnable{
	private static HashMap<String, Locker> locksMap = new HashMap<>();
	private boolean printDebug;
    private Socket client;
    private String directory;
    
    private BufferedReader br;
    private String contentType = "";
    private String contentDisposition = "";
    
    private boolean listOfFiles = false;
    private String filePath = "";
    private String method = "";
    private String data = "";
    
    private int statusCode = 200;
    private StringBuilder response = new StringBuilder();
    private StringBuilder responseBody = new StringBuilder();
    
    Writer fileWriter;
    
    Boolean errorFlag = false;
    Boolean overwrite = true;
    
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
	           if(!errorFlag) {
	            if(listOfFiles) {
	            	System.out.println("Sending list of all files in current directory");
	            	sendAllFiles(directory);
	            }
	            else if(method.equals("GET")) {
	            	System.out.println("filePath"+filePath);
	            	fetchFileDetails(directory);
	            }else if(method.equals("POST")) {
	            	System.out.println("filePath"+filePath);
	            	updateFile(directory,data);
	            }
	           }
	            generateResponse();
	            if (printDebug) 
	            	System.out.println("\n**Sending Response**\n");
	            System.out.println("Sent Response\n"+response);
	            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
	            bw.write(response.toString());
	            bw.flush();
	            br.close();
	            bw.close();
	            if (printDebug) 
	            	System.out.println("\n**Complete**\n");
	            Thread.sleep(3000);
	            
	            if (!listOfFiles && !errorFlag) {
	                if (method.equals("GET")) {
	                    Locker lock = locksMap.get(filePath);
	                    if(lock!= null) {
		                    if (lock.getReads() == 1) {
		                        locksMap.remove(filePath);
		                       // if (printDebug) System.out.println("Lock released:        " + filePath);
		                    } else {
		                        lock.removeReads();
		                    }
	                    }
	                } else if (method.equals("POST") && statusCode == 200) {
	                    locksMap.remove(filePath);
	                    //if (printDebug) System.out.println("Lock released:        " + filePath);
	                }
	            }
	           
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
    
    
    private void updateFile(String directory, String data) throws Exception {
		// TODO Auto-generated method stub
    	if (locksMap.containsKey(filePath)) {
            statusCode = 403;
            String readOrWrite = 1 == locksMap.get(filePath).getWrites() ? "written." : "read.";
            responseBody.append("File ").append(filePath).append(" is being ").append(readOrWrite).append("\r\n");
            return;
        } else {
            Locker lock = new Locker(filePath, "WRITE");
            locksMap.put(filePath, lock);
        }
        String postFilePath = directory + filePath;
        try {
        if(overwrite)
        	fileWriter = new FileWriter(postFilePath, false);
        else
        	fileWriter = new FileWriter(postFilePath, true);
        //System.out.println(fileWriter);
        //System.out.println("data in function "+data);
        File getFile = new File(postFilePath);
   	 	System.out.println(getFile);
        String fileType = getFile.toURI().toURL().openConnection().getContentType();
        contentType = fileType;
        if (contentType.equals("text/plain")) {
            contentDisposition = "inline";
        } else {
            contentDisposition = "attachment; filename=" + directory + filePath + ";";
        }
        fileWriter.write(data);
        fileWriter.close();
        }catch(FileNotFoundException e) {
        	e.printStackTrace();
        	File newDirectory = new File(postFilePath.substring(0,postFilePath.lastIndexOf("/")));
            //newDirectory.mkdirs();
            //fileWriter = new FileWriter(postFilePath, false);
        	statusCode = 403;
            responseBody.append("Cannot access subdirectories, access denied.\r\n");
            
        }
       

		
	}
   

	private void fetchFileDetails(String directory) throws Exception {
		// TODO Auto-generated method stub
		 if (locksMap.containsKey(filePath)) {
             Locker lock = locksMap.get(filePath);
             if (0 == lock.getWrites()) {
                 lock.addReads();
             } else {
                 statusCode = 403;
                 responseBody.append("File ").append(filePath).append(" is being written").append("\r\n");
                 return;
             }
         } else {
             Locker lock = new Locker(filePath, "READ");
             locksMap.put(filePath, lock);
         }
    	 File getFile = new File(directory + filePath);
    	 System.out.println(getFile);
         String fileType = getFile.toURI().toURL().openConnection().getContentType();
         contentType = fileType;
         if (contentType.equals("text/plain")) {
             contentDisposition = "inline";
         } else {
             contentDisposition = "attachment; filename=" + directory + filePath + ";";
         }
         if (printDebug) {
             System.out.println("Content Type:        " + fileType);
             System.out.println("Content Disposition: " + contentDisposition);
         }
         //if (!contentType.equals("text/plain")) {
         //    responseBody.append("File type is ").append(contentType).append(", cannot read file.");
         if (getFile.exists() && getFile.isFile()) {
             BufferedReader getFileContents = new BufferedReader(new FileReader(getFile));
             String getLine;
             while (null != (getLine = getFileContents.readLine())) {
                 responseBody.append(getLine).append("\r\n");
             }
         } else {
             statusCode = 404;
         }
		
	}

	private void processRequest() throws Exception {
        String line;
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while ((line = br.readLine()) != null) {
            //if (printDebug) 
            	//System.out.println("Line "+line);
            
            if (method.isEmpty()) {
                if (line.substring(0, 3).equalsIgnoreCase("GET")) {
                    method = "GET";                    
                } else if (line.substring(0, 4).equalsIgnoreCase("POST")) {
                    method = "POST";
                }
                int pathBeginAt = line.indexOf("/");
                filePath = line.substring(pathBeginAt, line.indexOf(" ", pathBeginAt+1));
                //System.out.println("filePathHere "+filePath);
                
                if(filePath.contains("?overwrite=false")) {
                	overwrite = false;
                	filePath = filePath.substring(0,filePath.indexOf("?overwrite"));
                }else if (filePath.contains("?overwrite=true")) {
                	filePath = filePath.substring(0,filePath.indexOf("?overwrite"));
                }
                	  
                if ((filePath.length() > 3 && filePath.contains("/../"))|| filePath.chars().filter(ch -> ch == '/').count() >= 3) {
                    statusCode = 403;
                    responseBody.append("Cannot leave the working directory, access denied.\r\n");
                    errorFlag = true;
                    return;
                }
                if(filePath.chars().filter(ch -> ch == '/').count() == 2 ) {
                	if(!(filePath.startsWith("/GET") || filePath.startsWith("/POST") || filePath.startsWith("/get") || filePath.startsWith("/post"))) {
                		  statusCode = 403;
                          responseBody.append("Cannot leave the working directory, access denied.\r\n");
                          errorFlag = true;
                          return;
                	}
                }
                if (filePath.equals("/") || filePath.equals("/get/") || filePath.equals("/GET/")) {
                    listOfFiles = true;
                    //System.out.println("listoffiles");
                    return;
                }else if(filePath.startsWith("/get") || filePath.startsWith("/GET")) {
                	
                	filePath = filePath.substring(4);
                	return;
                }else if(filePath.startsWith("/post") || filePath.startsWith("/POST")) {
                	filePath = filePath.substring(5);
                	
                }else if(method.equals("GET")) {
                	return;
                }
                	
            }
            if(line.contains("Data:")) {
            	data = line.substring(line.lastIndexOf(":")+1);
            	return;
            }
        
        }
        //System.out.println("Done");
        
    }
    
    private void sendAllFiles(String path) {
        File curDir = new File(path);
        File[] filesList = curDir.listFiles();
        if (null != filesList) {
            for (File file : filesList) {
                if(file.isFile()) {
                    responseBody.append(".").append(file.getPath().substring(directory.length())).append("\r\n");
                } else if (file.isDirectory()) {
                	sendAllFiles(path + "/" + file.getName());
                }
            }
        }
    }
    
    private void generateResponse() throws Exception {
        if (statusCode == 404) {
            response.append("HTTP/1.1 404 NOT FOUND\r\n");
            responseBody.append("File not found on the server.\r\n");
        } else if (statusCode == 403) {
            response.append("HTTP/1.1 403 Forbidden\r\n");
        } else if (statusCode == 400) {
            response.append("HTTP/1.1 400 Bad Request\r\n");
        } else {
            Thread.sleep(1000);
            response.append("HTTP/1.1 200 OK\r\n");
            if (method.equals("POST"))
            responseBody.append("File post successful");
        }
        response.append("Connection: close\r\n");
        response.append("Server: httpfs\n");
        response.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
        response.append("Content-Type: ").append(contentType).append("\r\n");
        response.append("Content-Disposition: ").append(contentDisposition).append("\r\n");
        response.append("Content-Length: ").append(responseBody.length()).append("\r\n");
        response.append("\r\n");
        response.append(responseBody.toString());
    }

}
