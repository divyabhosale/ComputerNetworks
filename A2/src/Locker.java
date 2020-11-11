
public class Locker {

    private String filename;
    private int read;
    private int write;

    public Locker(String filename, String rw) {
        this.filename = filename;
        switch (rw) {
            case "READ":
                read = 1;
                write = 0;
                break;
            case "WRITE":
                read = 0;
                write = 1;
                break;
            default:
                System.out.println("lock is invalid " + rw);
                break;
        }
    }
    public void addReads() { 
    	++this.read; 
    	}

    public void removeReads() { 
    	--this.read; 
    	}
    
    public String getFilename() { 
    	return filename; 
    	}

    public int getReads() { 
    	return read; 
    	}

    public int getWrites() { 
    	return write; 
    	}

    
}