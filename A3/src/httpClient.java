import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
public class httpClient {
	
	BufferedWriter bw = null;
	BufferedReader br = null;
	BufferedWriter bwF = null;
	boolean dflag = false;
	boolean fflag = false;
	BufferedReader brFile = null;
	String data ="";
	int PORT = 8007;
    String USER_AGENT = "Concordia-HTTP/1.0";
    String httpVersion = "HTTP/1.0";
	

	public String getRequest(String url, ArrayList<String> headers,boolean displayHeader, int redirectionAttempt) {
	
		String responseString ="";
		String redirectResponseString ="";
		String host ="";
		String path ="";
		CharSequence status301 = "301";
		try {
			//Set host and path
			if (url.isEmpty()) {
	            System.out.println("URL is missing");
	            return "ERROR";
	        } else {
	            int separator = url.indexOf("/", 7);
	            if (separator == -1) {
	                host = url.substring(7, url.length());
	            } else {
	                host = url.substring(7, separator);
	                if(host.contains("localhost"))
	                	host ="localhost";
	                path = url.substring(separator);
	            }
	        }
			
			//System.out.println("host"+host);
			//System.out.println("path"+path);
			if(path.equals(""))
				path="/";
			
			//Connection TCP
			//Socket socket = new Socket(InetAddress.getByName(host), PORT);
			UDPClientSocket clientSocket = new UDPClientSocket(3000, PORT);

			// Prepare request
            String request = "";
            request += "GET" + " " +path + " " + httpVersion +"\r\n";
            request += "Host: " + host+"\r\n";
            request += "User-Agent: " + USER_AGENT +"\r\n";
            if (fflag) {
                request += "Content-Type: multipart/form-data; boundary=" + "***" + "\r\n";
            }
            // Attach headers
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    request += header + "\r\n";
                }
            }
            if (!data.isEmpty()) {
                request += "Content-Length: " + data.length() + "\r\n";
            }
            request += "\r\n";
            request += data;
            clientSocket.sendData(request);

			//Response
            br = new BufferedReader(new StringReader(clientSocket.receiveData()));

            String responseLine;
            String responseHeader = "";
            String responseBody = "";
            Boolean redirectTo = false;
            Boolean redirectStatus = false;
            boolean headerCheck = false;
            while ((responseLine = br.readLine()) != null) {
            	if (!redirectStatus && responseLine.contains(status301)) {
            		redirectStatus = true;
                    redirectTo = true;
                }
            	if (!headerCheck && responseLine.equals("")) {
            		headerCheck = true;
            	}
            	if (!headerCheck) {
                    responseHeader += responseLine + "\n";
                } else {
                    responseBody += responseLine + "\n";
                }
            	
            	//Redirect Code
                if (redirectTo && responseLine.length() > 10 && responseLine.substring(0, 10).equals("Location: ")) {
                    String redirectLocation = responseLine.substring(10, responseLine.length());
                    url = redirectLocation;                     
                    System.out.println("Redirecting to:" + url );
                    if(redirectionAttempt <=5)
                    redirectResponseString = getRequest(url, headers,displayHeader,++redirectionAttempt);
                    break;
                }
            }
			
                        
            if(displayHeader)
            	responseString += responseHeader;
            responseString += responseBody;
            
			}catch(Exception e) {
				e.printStackTrace();
				System.out.println("ERROR in command");
		}
		
		if(!redirectResponseString.equals(""))
			return redirectResponseString;
		else
			return responseString;
	}

	public String postRequest(String url, ArrayList<String> headers, Boolean displayHeader ,String data) {
	
		String responseString ="";
		String host ="";
		String path ="";
		try {
			if (url.isEmpty()) {
	            System.out.println("URL is missing");
	            return "ERROR";
	        } else {
	            int separator = url.indexOf("/", 7);
	            if (separator == -1) {
	                host = url.substring(7, url.length());
	            } else {
	                host = url.substring(7, separator);
	                if(host.contains("localhost"))
	                	host ="localhost";
	                path = url.substring(separator);
	            }
	        }
			
			//System.out.println("host"+host);
			//System.out.println("path"+path);
	
		//Connection TCP
		//Socket socket = new Socket(InetAddress.getByName(host), PORT);
			UDPClientSocket clientSocket = new UDPClientSocket(3000, PORT);

			// Prepare request
            String request = "";
            request += "POST" + " " +path + " " + httpVersion +"\r\n";
            request += "Host: " + host+"\r\n";
            request += "User-Agent: " + USER_AGENT +"\r\n";
            if (fflag) {
                request += "Content-Type: multipart/form-data; boundary=" + "***" + "\r\n";
            }
            // Attach headers
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    request += header + "\r\n";
                }
            }
            if (!data.isEmpty()) {
            	if(data.contains(":") && data.startsWith("{") && data.endsWith("}")) 
            		data = convertToJson(data);
                request += "Content-Length: " + data.length() + "\r\n";
            }
            if(!host.equals("localhost")) {
            	request += "\r\n";
            	request += data+"\r\n";
            }
            else {
            	request += "\n";
            	request += "Data:"+data+"\r\n";
            	
            }
          
            clientSocket.sendData(request);
        //Response
        br = new BufferedReader(new StringReader(clientSocket.receiveData()));

        String responseLine;
        String responseHeader = "";
        String responseBody = "";
        boolean headerCheck = false;
        while ((responseLine = br.readLine()) != null) {
        	if (!headerCheck && responseLine.equals("")) {
        		headerCheck = true;
        	}
        	if (!headerCheck) {
                responseHeader += responseLine + "\n";
            } else {
                responseBody += responseLine + "\n";
            }
        }
		
        
        //Display Result
        if(displayHeader)
        	responseString += responseHeader;
        responseString += responseBody;
        
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("ERROR in command");
	}

		return responseString;
	}


	public String processRequest(String args[]) {
		
		String response ="";
		String url ="";
		ArrayList<String> headers = new ArrayList<String>();
		String method = args[0].equals("get") ? "GET" : "POST";
		int argLength = args.length;
		boolean writeInFile = false;
		boolean displayHeader = false;
		
		try {
			for (int i = 1; i < argLength; ++i) {
	            String token = args[i];
	            switch (token) {
	            case "-o":
                    if (argLength > i++ && !args[i].startsWith("http://" )) {
                        String oFilename = args[i];
                        File oFile = new File(oFilename);
                        oFile.createNewFile();
                        bwF = new BufferedWriter(new FileWriter(oFile));
                        writeInFile = true;
                        
                    } else {
                        System.out.println("ERROR : -o: missing a output file name");
                        return "ERROR";
                    }
                    break;
	            case "-d":
                    if (method.equals("GET")) {
                        System.out.println("ERROR : -d: GET does not allow inline data");
                        return "ERROR";
                    } else {
                        if (dflag) {
                            System.out.println("ERROR :-d: duplicate option");
                            return "ERROR";
                        } else if (fflag) {
                            System.out.println("ERROR: -d: -d and -f are not allowed together");
                            return "ERROR";
                        } else if (argLength > i++ && !args[i].startsWith("http://" ) ) {
                            data = args[i] ;
                            dflag = true;
                        } else {
                            System.out.println("ERROR :-d: missing inline data");
                            return "ERROR";
                        }
                    }
                    break;
	            case "-f":
                    if (method.equals("GET")) {
                        System.out.println("ERROR: -f: GET does not allow file data");
                        return "ERROR";
                    } else {
                        if (fflag) {
                            System.out.println("ERROR : -f: duplicate option");
                            return "ERROR";
                        } else if (dflag) {
                            System.out.println("ERROR : -f: -d and -f are not allowed together");
                            return "ERROR";
                        } else if (argLength > i++ && !args[i].startsWith("http://" ) ) {
                            String filename = args[i];
                            File file = new File(filename);
                            String content = "";
                            if (file.exists()) {
                            	brFile = new BufferedReader(new FileReader(file));
                                String line;
                                while ((line = brFile.readLine()) != null) {
                                    content += line + "\r\n";
                                }
                                data += "--" + "***" + "\r\n";
                                data += "Content-Disposition: form-data; name=\"file\"; filename=" + filename + "\r\n";
                                data += "Content-Type: text/plain" + "\r\n";
                                data += "Content-Length:" + content.length() + "\r\n";
                                data += "\r\n";                                                             
                                data += content + "\r\n";
                                data += "--"  +"***" + "--" +"\r\n";
                                fflag = true;
                            } else {
                                System.out.println(filename + "File does not exist");
                                return "ERROR";
                            }
                        } else {
                            System.out.println("ERROR : -f: missing file name");
                            return "ERROR";
                        }
                    }
                    break;
	            case "-h":
                    if (argLength > i++ && args[i].contains(":") && !args[i].startsWith("http://" ) ) {
                        headers.add(args[i]);
                    } else {
                        System.out.println("ERROR : -h: missing or incorrect key:value pair(s)");
                        return "ERROR";
                    }
                    break;
	            case "-v":
	            	displayHeader = true;
	            	break;
	             default :
	            	 if (url.isEmpty()) {
	                     if (token.length() > 6 && token.startsWith("http://" )) {
	                         url = token;
	                         if(token.contains("localhost")) {
	                        	 String tempPort = token.substring(token.lastIndexOf(":"),token.length());
	                        	 PORT = Integer.parseInt(tempPort.substring(1,tempPort.indexOf("/")));
	                        	 //System.out.println("port number :"+PORT);
	                         }
	                     } else {
	                         System.out.println(token + ": invalid command");
	                         return "ERROR";
	                     }
	                 } else {
	                     System.out.println(token + ": invalid command");
	                     return "ERROR";
	                 }
	                 break;
	            }
			}
		
		if(method == "GET") {
			
			response = getRequest(url, headers,displayHeader,1);
		}
		else
			response = postRequest(url,headers,displayHeader,data);
		
		if(writeInFile) {
			try {
				bwF.write(response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			response ="";
		}
		
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("ERROR in command");
		}
	 finally {
        try {
            if (null != bw) {
                bw.close();
            }
            if (null != br) {
                br.close();
            }
            if (null != brFile) {
            	brFile.close();
            }
            if (null != bwF) {
                bwF.close();
            }
        } catch (IOException ioe) {
        	System.out.println("ERROR in command");
        }
    }
		return response;
	}
	

	private String convertToJson(String data) throws Exception {
		// TODO Auto-generated method stub
		String formattedJSON = "{";
		data = data.replace("{", "");
		data = data.replace("}", "");
		String[] dataArray = data.split(",");
		
		for(String d : dataArray) {
			String[] dataKeyValue = d.split(":");
			
			Boolean numeric = true;
			try {
	           Double.parseDouble(dataKeyValue[1]);
	        } catch (NumberFormatException e) {
	            numeric = false;
	        }
			
			if(numeric)
				formattedJSON += "\""+ dataKeyValue[0].trim() + "\":" +  dataKeyValue[1].trim() + ",";
			else
				formattedJSON += "\""+ dataKeyValue[0].trim() + "\":" + "\""+ dataKeyValue[1].trim() + "\",";

		}
		formattedJSON = formattedJSON.substring(0, formattedJSON.length() - 1) +"}";
		return formattedJSON;
	}
	
}
