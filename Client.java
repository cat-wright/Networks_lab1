import java.io.*; 
import java.net.*; 
import java.util.*; 

public class Client{

    private Socket socket = null;
    private DataInputStream input  = null;
    private DataOutputStream out = null;
    private ArrayList<Long> clientDelay = new ArrayList<>();

    private boolean gui;
    private String address;
    private int port;
    private String username;
    private boolean run_server;
    private int count = 0;

	// constructor to put ip address and port 
    public Client(String address, int port, String username) { 
        this.address = address;
        this.port = port;
        this.username = username;
        this.gui = true;
        // establish a connection 
        try
        { 
            socket = new Socket(address, port); 
            System.out.println("Connection established to Server with port:" + port + " for " + username); 

            // takes input from terminal 
            input = new DataInputStream(socket.getInputStream()); 

            // sends output to the socket 
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(username);
        } 
        catch(UnknownHostException u) 
        { 
            System.out.println(u); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        }

   	
	this.run_server = true; 
} 

    //constructor for use in non-GUI applications
    public Client(String address, int port, String username, boolean run_server) { 
        this.address = address;
        this.port = port;
        this.username = username;
        this.gui = false;
        // establish a connection 
        try
        { 
            socket = new Socket(address, port); 
            System.out.println("Connection established to Server with port:" + port + " for " + username); 

            // takes input from terminal 
            input = new DataInputStream(socket.getInputStream()); 

            // sends output to the socket 
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(username);
        } 
        catch(UnknownHostException u) 
        { 
            System.out.println(u); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        }


        this.run_server = run_server;
        startServer();
    }

    //testing method, sends a message with timestamp
    private void sendRobotMessage() {
	String time = Long.toString(System.currentTimeMillis());
        try { out.writeUTF("robotuser:::" + time + ":::robottext"); }
        catch(IOException e) { e.printStackTrace(); }
    }

    private void readMessages() {
        try
        {
            // takes input from the server socket
            if (input.available() != 0) {
                String[] inputs = input.readUTF().split(":::");
                long received_time = System.currentTimeMillis();
                long sent_time = Long.parseLong(inputs[1]);
                long delay = Math.abs(sent_time - received_time);
                clientDelay.add(delay);
		StringBuilder msg = new StringBuilder();
                for (int i = 2; i < inputs.length; i++) {
                    msg.append(inputs[i]);
                    if(i != inputs.length-1) { msg.append(":::"); }
                }
                addMessageToHistory(inputs[0], true, msg.toString());
            }
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public void startServer() 
    {

        while (run_server) 
        {
            // reading input from server socket
            readMessages();
            if (!gui){
                sendRobotMessage();
                count += 1;
                if(count >= 1) break;
            }

        }
        // **USED IN TESTING**
        //100 messages have been sent, however we want the client to remain running for another "postponeRun" seconds, reading
        //messages and recording delay times.
//        long startTime = System.currentTimeMillis();
//        long postponeRun = 60000L; //10 seconds
//        while(System.currentTimeMillis() < startTime+postponeRun) {
//            readMessages();
//        }

        // close the connection
        try
        {
            StringBuilder delay = new StringBuilder("closing:::");
            for (Long delayTime : clientDelay) {
                delay.append(Long.toString(delayTime));
                delay.append(":::");
            }
            try{
                out.writeUTF(delay.toString());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            input.close(); 
            out.close(); 
            socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    }

    public void stopServer() {
        run_server = false;
    }

    // !!! DO NOT REMOVE THIS METHOD !!!
    // This message is necessary for the Gui to recieve the new message information
    // Overridden by the child class in order to add message to history
    public void addMessageToHistory(String username, Boolean incoming, String m) 
    {
        System.out.println("received message");
    }


    public void updateLocalUsername(String uname) {
        username = uname;
    }

    // send formatted message to server with new username
    public void updateUsername(String newUsername) {
        String msg = "username:::" + username + ":::" + newUsername;
        try {
            out.writeUTF(msg);
        } catch(IOException i) { 
            System.out.println(i); 
        }
    }


    // send message to other client (called by child class)
    public void sendMessage(String sentToUsername, String message) 
    {
        String sentText = sentToUsername + ":::" + System.currentTimeMillis() + ":::" + message;
        try 
        {
            out.writeUTF(sentText);    
        } 
        catch(IOException e) { e.printStackTrace(); }
    }

    // main class
    public static void main(String[] args) {
    	new Client (args[0], Integer.parseInt(args[1]), args[2], true);
    }
}
