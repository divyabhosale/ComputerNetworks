
public class httpfs {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HttpServer httpfs = new HttpServer();
        int argsLength = args.length;

        for (int i = 0; i < argsLength; ++i) {
            String option = args[i];
            switch(option) {
                case "help":
                	 String outputString = "\n";
                     outputString += "httpfs is a simple file server.\n";
                     outputString += "usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n";
                     outputString += "     -v Prints debugging messages.\n";
                     outputString += "    -p Specifies the port number that the server will listen and serve at.\n";
                     outputString += "     Default is 8080.\n";
                     outputString += "    -d Specifies the directory that the server will use to read/write\n";
                     outputString += "    requested files. Default is the current directory when launching the\n";
                     outputString += "    application.\n";
                     System.out.println(outputString);                	
                    break;
                case "-v":
                    httpfs.setPrintDebug(true);
                    break;
                case "-p":
                    if (++i < argsLength) {
                        String tempPortNumber = args[i];
                        try {
                            httpfs.setPortNumber(Integer.parseInt(tempPortNumber));
                        } catch (Exception exception) {
                            System.out.println(tempPortNumber + ": port number has to be an integer");
                        }
                    } else {
                        System.out.println("-p: port number is missing");
                    }
                    break;
                case "-d":
                    if (++i < argsLength) {
                        httpfs.setDirectory(args[i]);
                    } else {
                        System.out.println("-d: directory path is missing");
                    }
                    break;
                default:
                    System.out.println(option + ":invalid command");
                    return;
            }
        }

        httpfs.startServer();

	}

}
