import java.util.LinkedList;
import java.util.HashMap;
import java.util.ListIterator;
import java.lang.StringBuilder;


/** 
 * History.java
 * History class stores the message transaction history between a client
 * 		and other clients
 */
public class History {  

	// Stores the message history
	private LinkedList< Message> history = new LinkedList< Message>();

	// Creates a new Message, and adds it to the history
	public void addMessage(String username, boolean incoming, String message) {
		Message m = new Message(username, incoming, message);
		history.add(m);
	}

	// Returns an array of the messages, along with text containing who they are from
	public String[] getMessages() {

		LinkedList< String> msg = new LinkedList< String>();
		ListIterator<Message> listIterator = history.listIterator();

		while (listIterator.hasNext()) {
			Message nextMessage = listIterator.next();
			msg.add(nextMessage.getDirectionText() + nextMessage.getUsername());
			msg.add(nextMessage.getMessage());
		}
		
		return msg.toArray(new String[msg.size()]);
	}

}

/** 
 *  Message class stores message information, including whether it
 * 			is an incoming or outgoing message, the username, and 
 *			message content.
 */
class Message {

	private boolean is_incoming;
	private String message;
	private String username;

	// Message constructor, stores username, incoming, and message as local vars
	public Message(String uname, boolean incoming, String m) {
		is_incoming = incoming;
		message = m;
		username= uname;
	}	

	// Returns "To: " if message is outgoing, and "From: " if message is incoming
	public String getDirectionText() {
		if (is_incoming) {
			return "From: ";
		} else {
			return "To: ";
		}
	}

	// Returns the message body
	public String getMessage() {
		return message;
	}

	// Returns the username
	public String getUsername() {
		return username;
	}

}
