import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 
 * Server.java
 * Server class handles the server that connects all clients.
 * Maintains threads used for communication, and a directory to lookup available client threads.
 */
public class Server {
    //Server directory stores list of connected Clients.
    // Key: username
    // Value: Connection class (Thread)
    private Map<String, Connection> server_directory = Collections.synchronizedMap(new HashMap<>());

    //delayFile stores the time delay of messages sent between clients in milliseconds.  Writes all client time delays to a single file.
    private File delayFile = new File("delay.txt");

    //Used in testing to see when all sockets have been closed
    private int closed = 0;
    /**
     * Constructor for Server.java continuously listens for client sockets trying to connect.  If one connects and has an
     * allowable username, the Server starts a new Connections thread for the socket.  The Server also adds the username and
     * Connection thread to the server_directory.
     * @param ss ServerSocket the server will listen to for potential new connections
     * @throws IOException If the socket is not connected correctly
     */
    private Server(ServerSocket ss) throws IOException {
        if(delayFile.delete()) System.out.println("Delay file deleted");
        while(true){
            //accept packet and extract name
            Socket s = ss.accept();
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            String username = dis.readUTF();

            //check if the desired client username is allowable.  "username", "warning", and "robotuser"
            // are used in communication, therefore are not allowed as usernames.
            if(username.equals("username") || username.equals("warning") || username.equals("robotuser")){
                dos.writeUTF("warning:::0:::Invalid username. Try again.");
                s.close();
                continue;
            }

            //see if this name is associated with a connection already, call this connection c
            Connection c;
            if((c = server_directory.get(username)) != null){
                //In this case, the user does exist and wishes to reconnect.
                if(!c.awake){
                    c.wake(s);
                }
                else{
                    dos.writeUTF("warning:::0:::Username is taken! Try again.");
                    s.close();
                }
            }
            //establish new user if name not recognized
            else {
                c = new Connection(s, username, dis, dos);
                c.start();
                server_directory.put(username, c);
            }
        }

    }

    /**
     * Main method for Server.  Takes port number from argument and gets IP address of the local host.
     * Instantiates the ServerSocket and calls the constructor for Server.java
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        //Create Server Socket
        ServerSocket ss = new ServerSocket(Integer.parseInt(args[0]));
        InetAddress inet = InetAddress.getLocalHost();
        System.out.println("Server created with port " + args[0]+ " at IP: " + inet);

        //Starts the new server
        new Server(ss);
    }

    /** **USED FOR TESTING**
     * Takes all delay times from a client and appends them to the delayFile "delay.txt".
     * @param clientDelayTimes String array containing all delay times during clients test run
     */
    private synchronized void appendDelayTimes(String[] clientDelayTimes) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(delayFile, true));
            for(int i = 1; i < clientDelayTimes.length; i++) {
                out.write(clientDelayTimes[i]);
                out.write('\n');
            }
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Connection Class (private inner class of Server.java)
     * Running thread for a single client.  Maintains information about their message queue, socket, and state.
     * State being whether they are:
     *  awake: the client is currently connected, reading and writing to the socket
     *  not awake: the socket is closed and the thread is spinning, waiting for the client to return.  Messages can still
     *             be added to the client threads queue.
     */
    private class Connection extends Thread {
        boolean awake;
        String username;
        Socket source;
        DataInputStream dis;
        DataOutputStream dos;
        ConcurrentLinkedQueue<String[]> msgQueue = new ConcurrentLinkedQueue<>();

        /**
         * Constructor for Connection class.  Sets local variables accordingly
         * @param s : The socket connecting the client host to the client Thread in the server
         * @param username : the username (unique) associated with the client Thread
         * @param dis : the DataInputStream used for communication along s, from the client host to the client thread
         * @param dos : the DataOutputStream used for communication along s, from the client thread to the client host
         */
        Connection(Socket s, String username, DataInputStream dis, DataOutputStream dos) {
            this.source = s;
            this.username = username;
            this.dis = dis;
            this.dos = dos;
            this.awake = true;
        }

        /**
         * When a client has been "offline" and returns, they are woken up with the new socket connection.  The new socket is
         * stored in their client thread and used for communication from then on.
         * @param s : new socket to be connected to client thread
         */
        void wake(Socket s) {
            awake = true;
            try{
                this.source = s;
                this.dos = new DataOutputStream(s.getOutputStream());
                this.dis = new DataInputStream(s.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * adds an incoming message to a client threads queue
         * @param msg : the message to be added, such that msg[0] is the username of the sender client.
         */
        boolean receiveMsg(String[] msg) {
            msgQueue.add(msg);
            System.out.println(this.username + "is awake? " + this.awake);
            return this.awake;
        }

        /**
         *  **USED FOR TESTING**
         *  When the received message is for "robotuser", this method is called.  It sends the message to a random
         *  username in the server_directory.
         * @param msg : robot message to be sent.
         */
        void sendRobotMsgToSocket(String[] msg) {
            Object[] users = server_directory.keySet().toArray();
            String randomUser = String.valueOf(users[new Random().nextInt(server_directory.size())]);
            msg[0] = randomUser;
            sendMsgToSocket(msg);
        }

        /**
         * When a client thread receives a message to be sent to another client, sendMsgToSocket ensures that the desired
         * receiving client is in the server_directory, then finds the associated client Thread and adds the message to
         * their queue, appending the sending clients username to the beginning of the message.
         * @param msg : message to be sent.
         */
        void sendMsgToSocket(String[] msg) {
            String receiver = msg[0];
            if (server_directory.containsKey(receiver)) {
                Connection dest = server_directory.get(receiver);
                //SETS USERNAME TO BE SENDER USERNAME INSTEAD OF DESTINATION USERNAME
                msg[0] = this.username;
                try {
                    if (dest.receiveMsg(msg)) dos.writeUTF(String.format("warning:::0:::Message delivered to %s", receiver));
                    else dos.writeUTF(String.format("warning:::0:::%s is offline and will get your message when they wake up.", receiver));
                } catch (IOException e) {e.printStackTrace();}
            }
            else {
                try {
                    dos.writeUTF(String.format("warning:::0:::%s does not exist!", receiver));
                } catch (IOException e) {e.printStackTrace();}
            }
        }

        /**
         * Connection class run() method for the client Thread.
         * Continuously checks if a message is waiting to be received.  If so, checks the type of message.
         * Message Types:
         *     msg[0] = "robotuser": this is used in testing, and implies that the message should be sent using the
         *              sendRobotMsgToSocket() method.
         *     msg[0] = "username": this is used for a request to change usernames.  Calls update_username(), and writes a
         *              response to the client whether or not their username was successfully updated.
         *     msg[0] = "closing": this is sent by the client to signify that they are closing their socket.  This includes their
         *              list of delay times and lets the server know to set their thread to awake = "false"
         *     else: the message is intended to be sent to another client.  Calls sendMsgToSocket() with the message.
         *
         * After checking input, the thread checks if there are any messages in its msgQueue waiting to be sent to the client.
         * It writes a message to the socket, then removes it from the queue.
         */
        @Override
        public void run() {
	        System.out.println("Thread started for username " + username);
	        while(true) {
                try {
                    if (dis.available() != 0) {
                        String[] msg = dis.readUTF().split(":::");
                        if (msg[0].equals("robotuser"))
                        {
                            sendRobotMsgToSocket(msg);
                        }
                        else if (msg[0].equals("username"))
                        {
                            System.out.println("Request to change username to " + msg[2] + " from " + msg[1]);
                            //Writes a warning to the client that says whether the username was successfully changed
                            dos.writeUTF(update_username(msg[1], msg[2]));
                        }
                        else if (msg[0].equals("closing"))
                        {
			                closed += 1;
                            System.out.println("received close "+closed);
                            appendDelayTimes(msg);
                            this.awake = false;
                            System.out.println("Set " + this.username + " awake to " + this.awake);
                            while (!awake)
                                yield();
                            continue;
                        }
                        else
                            sendMsgToSocket(msg);
                    }
                }
                //If socket connections get broken, the socket is closed and the thread is set to awake = "false".  The thread
                //spins until it is woken back up.
                catch(IOException e){
                    if(e.getMessage().contains("Broken")){
                        try{
                        source.close();
                        }catch(Exception e2){
                            e2.printStackTrace();
                        }
                        this.awake = false;
                        while(!awake)
                            yield();
                        continue;
                    }
                    e.printStackTrace();
                    break;
                }
                if(!msgQueue.isEmpty()) {
                    String[] msg = msgQueue.peek();
                    StringBuilder inMessage = new StringBuilder(msg[0] + ":::" + msg[1]);
                    for (int i = 2; i < msg.length; i++) { 
			            inMessage.append(":::");
			            inMessage.append(msg[i]);
		            }
                    try {
                        dos.writeUTF(inMessage.toString());
			            dos.flush();
                        msgQueue.remove();

                    }
                    //If socket connections get broken, the socket is closed and the thread is set to awake = "false".  The thread
                    //spins until it is woken back up.
                    catch (Exception e) {
                        if(e.getMessage().contains("Broken")){
                            try{
                                source.close();
                            }catch(Exception e2){
                                e2.printStackTrace();
                            }
                            awake = false;
                            while(!awake)
                                yield();
                            continue;
                        }
                    }
                }
            }
        }

        /**
         * Method for updating the username of a client.  Ensures that it is not a reserved username, and that
         * no other users have the requested username.  If not, replaces server_directory username with new username.
         * @param old_username : the clients current username
         * @param new_username : the clients requested username
         * @return a message for the client as to whether their request was successful.
         */
        private String update_username(String old_username, String new_username) {
            String retMsg = "warning:::0:::";
            if(new_username.equals("username") || new_username.equals("warning") || new_username.equals("robotuser")) {
                return retMsg + "Invalid username. Try again.";
            }
            if(server_directory.containsKey(old_username)) {
                if(!server_directory.containsKey(new_username)){
                    Connection client_thread = server_directory.remove(old_username);
                    server_directory.put(new_username, client_thread);
                    this.username = new_username;
                    return retMsg + "Successfully changed username.";
                }
                else {
                    return retMsg + "Username is taken! Try again.";
                }
            }
            //This should never be returned, because the client should be sending a message with a valid old_username.  Just in case...
            return retMsg + "Invalid username.  Try again.";
        }
    }
}
