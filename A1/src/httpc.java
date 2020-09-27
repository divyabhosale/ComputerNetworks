import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class httpc {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
            String outputString = "\n";
            outputString += "httpc is a curl-like application but supports HTTP protocol only.\n";
            outputString += "Usage: \n    httpc command [arguments]\nThe commands are:\n";
            outputString += "    get     executes a HTTP GET request and prints the response.\n";
            outputString += "    post    executes a HTTP POST request and prints the response.\n";
            outputString += "    help    prints this screen.\n\n";
            outputString += "Use \"httpc help [command]\" for more information about a command.\n";
            System.out.println(outputString);
        } else if (args.length == 1) {
            if (args[0].equals("get") || args[0].equals("post")) {
                System.out.println(args[0] + ": URL required");
            } else {
                System.out.println(args[0] + ": invalid command");
            }
        } else {
            switch (args[0]) {
                case "help":
                    switch (args[1]) {
                        case "get":
                            String getString = "\n";
                            getString += "usage: httpc get [-v] [-h key:value] URL\n\n";
                            getString += "Get executes a HTTP GET request for a given URL.\n\n";
                            getString += "    -v           Prints the detail of the response such as protocol, status, and headers.\n";
                            getString += "    -h key:value Associates headers to HTTP Request with the format 'key:value'.\n";
                            System.out.println(getString);
                            break;
                        case "post":
                            String postString = "\n";
                            postString += "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n\n";
                            postString += "Post executes a HTTP POST request for a given URL with inline data or from file.\n\n";
                            postString += "    -v             Prints the detail of the response such as protocol, status, and headers.\n";
                            postString += "    -h key:value   Associates headers to HTTP Request with the format 'key:value'.\n";
                            postString += "    -d string          Associates an inline data to the body HTTP POST request.\n";
                            postString += "    -f file            Associates the content of a file to the body HTTP POST request.\n\n";
                            postString += "Either [-d] or [-f] can be used but not both.\n";
                            System.out.println(postString);
                            break;
                        default:
                            System.out.println(args[0] + ": invalid command");
                            break;
                    }
                    break;
                case "get": case "post": {
                    httpc http = new httpc();
                    http.makeCallout(args);
                    break;
                }
                default:
                    System.out.println(args[0] + ": invalid command");
                    break;
            }
        }


	}

	private void makeCallout(String[] args) {
		BufferedWriter bw = null;
		BufferedReader br = null;
		
		// TODO Auto-generated method stub
		String method = args[0].equals("get") ? "GET" : "POST";
		int argLength = args.length;
		int PORT = 80;
	    String USER_AGENT = "Concordia-HTTP/1.0";

		String url ="";
		String host ="";
		String path ="";
		String httpVersion = "HTTP/1.0";
		
		boolean displayHeader = false;
		// Extract URL
		try {
			for (int i = 1; i < argLength; ++i) {
	            String token = args[i];
	            
	            switch (token) {
	            case "-v":
	            	displayHeader = true;
	            	break;
	             default :
	            	 if (url.isEmpty()) {
	                     if (token.length() > 6 && token.substring(0, 7).equals("http://")) {
	                         url = token;
	                     } else {
	                         System.out.println(token + ": invalid command");
	                         return;
	                     }
	                 } else {
	                     System.out.println(token + ": invalid command");
	                     return;
	                 }
	                 break;
	            }
			}
			
			System.out.println("url "+url);
			
			//Set host and path
			if (url.isEmpty()) {
	            System.out.println("URL is missing");
	            return;
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
			
			//Request
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            bw.write(method + " " + path + " " + httpVersion +"\r\n");
            bw.write("Host: " + host+"\r\n");
            bw.write("User-Agent: " + USER_AGENT +"\r\n");
            
            bw.write("\r\n");
            bw.flush();
            
            //Response
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        	System.out.println("Response");

            String responseLine;
            String  responseHeader = "";
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
            String responseString ="";
            if(displayHeader)
            	responseString += responseHeader;
            responseString += responseBody;
            System.out.println(responseString);
            
			}catch(Exception e) {
			System.out.println("!!!!!!!!!!!!!!!!!Exception");
			System.out.println(e);
		}
	}
	
	
	
	

}
