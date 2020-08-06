# Client/Server Model using Multithreading

The motivation of this project is to create a web-based application following the Client/Server model.  In this case we wrote an instant messaging program consisting of clients that can send and receive messages and a server which keeps track of client availability and routes messages to their desired recipient.  Following is a description of the design considerations that went into each component of this product.

## 1. Server

## Server
The server has been created incrementally alongside the client.  The server has 5 primary functions:
  - Listen for clients trying to connect to it
  - Begin *Connections* for clients
  - Keep client *Connections* informed of clients currently online
  - Route messages sent from one client to its desired recipient's inbox
  - Pass inbox messages to the clients as they are available


*Connections*, which are described in detail below, are running threads that maintain information about specific clients.  
The server maintains a working directory referred to from here as the **_server directory_**.  The server directory is a mapping from client usernames to Connections, allowing the quick lookup of a user's status and inbox from their username.

To achieve the functions listed above, the Server begins by opening a *ServerSocket*, and continuously listens through this socket for any Client sockets attempting to communicate with the Server.  When one is discovered, the server listens to the connecting socket for the clients username.  When it finds a valid username message, it applies a series of cases to ensure the correct resources are allocated for the client:
  - **Case 1.**  The clients requested username is "username" or "warning".  
    - These are not allowed usernames, as they are used for book-keeping messages.  Returns to the client a warning that their chosen username was invalid, and rejects the connection.    
  - **Case 2.**  The clients requested username is currently a username in the server directory.
    - IF the Connection thread associated with the username is sleeping, we assume this is a client returning.  The client is linked with the sleeping Connection thread.
    - IF there is a Connection thread awake currently associated with the username, we assume this is an attempted duplicate username.  Returns to the client a warning that their chosen username is invalid, and rejects the connection.
  - **Case 3.**  The clients requested username does not fall into Case 1 or Case 2.
    - A new Connections thread is established and connected to the clients socket.  The server directory is appended with a mapping from the new username to the new Connection.  

### Connection
The Connection class is a private inner class of Server that extends Thread.  It stores the following fields:
  - **awake**: a Boolean value that stores whether there is currently an active socket connected to the Connection.
  - **address**, **port**, **username**, **dis**, and **dos**: the InetAddress, port number, username, data input stream, and data output stream of the connected Client socket. 
  - **msgQueue**: A locking queue to store incoming messages for a client. 

The Connection also has access to the server directory.

Inside the run method of a Connection there is an infinite loop that performs as follows:
  - Checks for incoming messages from the Client socket.
    - If so, checks if the message is a notification of shutdown, a request to change usernames or a request to send a message to a client.
      - If the message is notifying the server of a shutdown, the connection marks itself available to receive a new socket, closes its current socket and yields until a new socket has been provided (i.e., a user with the same username connects again).
      - If the message is to change usernames, the Connection attempts to make a key change in the server directory, and reports its success or failure to the client.
      - If the message is to be sent to a different client, the sending client finds the receiving clients Connection through the server directory, then adds the message to the receiving client's message queue.   If the recipient is not found, a warning is sent back to the sender notifying them of an invalid username.
  - Checks for outgoing messages to send to the Client socket.
    - If so, removes messages from the message queue and writes them to the Client socket.

This design provides several benefits, including high speed of message propagation and allowing for message saving.  By making a unique queue as a member of every Connection, we ensure that exactly one map lookup must be performed for each message.  The use of yielding threads to deal with offline users could be considered a waste of system resources (a system with 40 users, 2 of which are online, would still require 40 threads), but it seemed the best compromise to avoid continuous lookups while keeping the queues alive irrespective of socket activity.

## 2. Client
The Client-Side of this model connects and sends messages to the server using a socket connection where the server's job is to route the messages to the desired client. The Client class here is a parent class of the ClientGUI. The client uses data input and output streams to communicate over the socket connection. Here is a workflow of the functionality performed by the client -

* create a socket connection using parameters as (servers IP, port number, username)
* reading input streams from the IO
* redirecting the input read from IO and writing it as the output to the socket.
* reading the input from the socket as a message from another client.
* closing the socket connection and input and output channel in order to free the resources after usage

Apart from the main functionalities, the Client is used for sending automatic messages using `sendRobotMessage` which is used for testing out sending the desired number of messages to the other available Clients for testing between a large number of clients *(10 - 10000)* and calculate the time delay for the message to be sent and received in milliseconds using *time* variable and *clientDelay* array. 

The Client uses `startServer` for the reading messages and distinguishing between the type of message formats for the desired usage. Messages from the client are structured in such a way that the Server can parse them and process the desired results. The type of message structures are 
* Normal message  -  (destination_username):::(message)
* Change of user  -  username:::(username):::(new_username)
* Robot users     -  robotuser:::(time):::robotmessage 


## 3. Client-GUI

The ClientGui class inherits from the Client class, meaning that it gains all the functionality from Client, but while providing a user interface for the client that was generated using Swing. The general workflow for ClientGui is as follows: 

1. Initialize the parent class (Client)
2. Generate and start running the Graphical User Interface
3. Start server 
4. Listen to user interactions in the Gui
OR 
4. Respond to messages that arrive through the socket connection

#### Listen to user interactions in the Gui

The chat window contains a "To" field, as well as a message field, and a submit button, that will send the message to the specified username when clicked. Sent and received messages will display in the message history component in the GUI. The GUI also contains a dropdown menu with an option for the user to change their username. When either of these options is selected, a message is formatted and sent through the socket to the server, by calling `sendMessage` on the parent class. 

#### Respond to messages that arrive through the socket connection

When a message is received through the socket in the parent class, Client will determine wether the message was sent from another user, or was returning feedback to the user. If the client receives a message from server with the format: `warning:::Message text here`, then ClientGui will display this message as feedback to the user. If the message was `Username update successful`, then ClientGui will additionally update the client's username. If the message does not contain the `warning` prefix, then the message is from another user, and will be added to this user's history.

### History 

The History class maintains a log of messages, which contain information about the user's communications with other users. This class is instantiated and updated by ClientGui. ClientGui uses this class to keep track of and eventually display, a log of messages that are sent between this user and other users.

### Message

The Message class contains information about a single message between this user and another user. The message will contain the username of the client this user is communicating with, the direction (incoming or outgoing), and also the message contents. Instances of this class are added to a user's history.

# How to run the project:

## Build code
1. `git clone git@lobogit.unm.edu:fall19group4/lab1.git`
2. `cd lab1`
3. `make`

## Run Server
4a. `java Server 8080`

## Run Client with Gui
4b. `java ClientGui ${IP of Server} 8080 ${Client Username}`

## GUI Usage 
Open multiple terminals and make sure to use the same directory as above in all the terminals.
* To send message to user - 
  * type username in To: textfield.
  * type a message in the textfield at the bottom.
  * press the send button.
  * message appears in the receivers textframe if it is open.
* To change username - 
  * click on settings (Top left).
  * click on change username.
  * enter the desired username in the Dialogue box and press ok.

### Optional
* `make run` to start the server at port 8080.
* `make clean` to remove all .class files.

# Testing and Results
For testing we implemented the Client to run a simulation of message-passing commands to the Server and record the time delay between when a message is sent from a Client and when it is received by another Client. This resulted in 3 major changes added to our existing Client/Server chat code.  
  1. **Find the time delay of each message.** A timestamp was added to each outgoing message.  When a message is received, the client subtracts the timestamp from the current time to find the message delay in milliseconds.  The receiving client writes this delay to an ArrayList.  When a client is closing, they pass their ArrayList of delay times to the Server.  The Server compiles all delay times into a single document, "delay.txt".
  2. **Simulate messages being sent.** Within the Clients loop that sends and receives messages, a call is made to send a *Robot Message*.  This is done a set amount of times specified during testing.  When the server receives a Robot Message, it chooses a random Client from the server directory to send a message to.  
  3. **Simulate several active Clients.** To simulate any number of Clients being active at the same time, we wrote a bash script (*shelltester.bash*) for forking any number of Client processes requested.  Testing can then be run by using `./shelltester.bash ${IP of Server} ${number of clients} ${username}` while a Server is active, and where username can be anything, and is used to ensure different machines request Clients with different usernames.   

An issue we encountered was that some Clients would finish sending their messages and close before others Clients were initialized.  The closed clients could still receive messages, however we would not get data from them regarding delay times.  To solve this, we added a delay proportional to the number of Clients that a Client would spin, receiving messages only, before closing.  

For testing we ran simulations of 1, 10, 100, 500, and 1000 Clients, with each Client sending 1 message each.  We also ran simulation of 1, 10, 100, and 500 Clients each sending 10 messages, and 1, 10, and 100 Clients each sending 100 messages.  When trying to simulate more than 1000 running Clients, the systems we were running on didn't have the Socket capacity to maintain enough open Sockets.  This resulted in errors, since a Thread would be created for a Socket that should have been initialized, however when trying to write or read from it the Thread would receive a NullPointerException.  We tried still gathering some data from these tests, however time delays were only recorded for the first few hundred messages, and did not accurately represent the workload required for that benchmark.  A plot of our simulations is shown below.  
![delay_plot](uploads/3aa57b13c0240db48a7a651418ae559b/delay_plot.PNG "Benchmark Testing of Client/Server")

# Contributions

### Carolyn
Wrote the classes related to the Gui: ClientGui, History, and Messages. Carolyn also contributed to the Client class with message formatting, updating usernames, troubleshooting, and ensuring that there was bi-directional communication and data flow between the Client and the Gui. Contributed to the wiki.
### Catherine
Worked on Server.java and helped troubleshoot concurrency issues.  Wrote the structure for simulation including measuring time delay and sending automated messages.  Produced the plot showing our test results, as well as contributed to the wiki. 
### Nitin
Wrote the initial structure of the Client.java and changes made to it along the way. Contributed to the wiki and its structuring. Developed the make file to build the project. 
### Thomas
Wrote the initial structure of Server.java and helped design the solution/split the project into components.  Wrote the testing scripts and edited the documentation.  Helped troubleshoot stubborn bugs.
