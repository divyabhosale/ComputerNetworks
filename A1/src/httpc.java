
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
                case "get": 
                	httpClient httpGet = new httpClient();
                	String result= "";
                	result = httpGet.processRequest(args);
                	break;
                
                case "post": 
                	httpClient httpPost = new httpClient();
                	String resultPost= "";
                	resultPost = httpPost.processRequest(args);
                    break;
                
                default:
                    System.out.println(args[0] + ": invalid command");
                    break;
            }
        }


	}
}