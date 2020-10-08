import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
	int PORT = 80;
    String USER_AGENT = "Concordia-HTTP/1.0";
    String httpVersion = "HTTP/1.0";
	

	public String getRequest(String url, ArrayList<String> headers,boolean displayHeader) {
	
		String responseString ="";
		String host ="";
		String path ="";
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
	                path = url.substring(separator);
	            }
	        }
			//Connection TCP
			Socket socket = new Socket(InetAddress.getByName(host), PORT);
			
			//Build Request
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            bw.write("GET" + " " +path + " " + httpVersion +"\r\n");
            bw.write("Host: " + host+"\r\n");
            bw.write("User-Agent: " + USER_AGENT +"\r\n");
            if (fflag)
                bw.write("Content-Type: multipart/form-data; boundary=" + "***" + "\r\n");
            //Attach headers
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    bw.write(header + "\r\n");
                }
            }
            
            bw.write("\r\n");
            bw.flush();
            
            //Response
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        	System.out.println("Response");

            String responseLine;
            String responseHeader = "";
            String responseBody = "";
            boolean headerCheck = false;
            while ((responseLine = br.readLine()) != null) {
            	//System.out.println(responseLine);
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
           // System.out.println(responseString);
            
			}catch(Exception e) {
				System.out.println("ERROR in command");
		}
		
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
                path = url.substring(separator);
            }
        }
		
		System.out.println("host "+host);
		System.out.println("path "+path);
		
		//Connection TCP
		Socket socket = new Socket(InetAddress.getByName(host), PORT);
		
		//Build Request
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        bw.write("POST" + " " + path + " " + httpVersion +"\r\n");
        bw.write("Host: " + host+"\r\n");
        bw.write("User-Agent: " + USER_AGENT +"\r\n");
        if (fflag)
            bw.write("Content-Type: multipart/form-data; boundary=" + "***" + "\r\n");
        //Attach headers
        if (!headers.isEmpty()) {
            for (String header : headers) {
                bw.write(header + "\r\n");
            }
        }
        //Add data
        if (!data.isEmpty()) {
        	
        	if(data.contains(":") && data.startsWith("{") && data.endsWith("}")) 
        		data = convertToJson(data);
        	//data = "{\"name\": \"Sam Smith\", \"technology\": \"Python\"}";
        	bw.write("Content-Length: " + data.length() + "\r\n");
        }
        
        bw.write("\r\n");
        System.out.println("Data"+data);
        bw.write(data);
        
        bw.flush();
        
        //Response
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    	System.out.println("Response");

        String responseLine;
        String responseHeader = "";
        String responseBody = "";
        boolean headerCheck = false;
        while ((responseLine = br.readLine()) != null) {
        	//System.out.println(responseLine);
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
       // System.out.println(responseString);
        
		}catch(Exception e) {
			System.out.println("ERROR in command");
	}

		return responseString;
	}


	public String processRequest(String args[]) {
		
		String response ="";
		String url ="";
		String httpVersion = "HTTP/1.0";
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
                        System.out.println("-o: missing a output file name");
                        return "ERROR";
                    }
                    break;
	            case "-d":
                    if (method.equals("GET")) {
                        System.out.println("-d: GET does not allow inline data");
                        return "ERROR";
                    } else {
                        if (dflag) {
                            System.out.println("-d: duplicate option");
                            return "ERROR";
                        } else if (fflag) {
                            System.out.println("-d: -d and -f can not be used together");
                            return "ERROR";
                        } else if (argLength > i++ && !args[i].startsWith("http://" ) ) {
                            data = args[i] ;
                            dflag = true;
                        } else {
                            System.out.println("-d: missing inline data");
                            return "ERROR";
                        }
                    }
                    break;
	            case "-f":
                    if (method.equals("GET")) {
                        System.out.println("-f: GET does not allow file data");
                        return "ERROR";
                    } else {
                        if (fflag) {
                            System.out.println("-f: duplicate option");
                            return "ERROR";
                        } else if (dflag) {
                            System.out.println("-f: -d and -f can not be used together");
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
                            System.out.println("-f: missing file name");
                            return "ERROR";
                        }
                    }
                    break;
	            case "-h":
                    if (argLength > i++ && args[i].contains(":") && !args[i].startsWith("http://" ) ) {
                        headers.add(args[i]);
                    } else {
                        System.out.println("-h: missing or incorrect key:value pair(s)");
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
			System.out.println("url"+url);
			System.out.println("headers"+headers);
			response = getRequest(url, headers,displayHeader);
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
			System.out.println(d);
			String[] dataKeyValue = d.split(":");
			
			System.out.println(dataKeyValue[0]);
			System.out.println(dataKeyValue[1]);
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
		System.out.println(formattedJSON);
		return formattedJSON;
	}
	
}
