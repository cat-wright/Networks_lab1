import javax.swing.*;  
import java.awt.*;  
import java.awt.event.*;  

/** 
 * ClientGui.java
 * ClientGui class is a child class of Client.
 * Sets up a server socket through the parent class, and displays a gui for the 
 *  user to interact with
 */
public class ClientGui extends Client {  

	History history = new History();
	private String clientUsername;
	private String newUsername;
	private JFrame f;
	private JLabel welcomeText;
	private JTextField send_to;
	private JList hist;
	private JScrollPane scroll;
	private JTextField message;
	private JLabel feedback;

	/**
   	* Client Gui Constructor.
    * Initializes the parent class, creates the gui, and starts the server
    * @param address : IP address of server to connect to
    * @param port : Port of server to connect to
    * @param username : username of the client
    */	
	public ClientGui(String address, int port, String username) {
		super( address,  port,  username);
		clientUsername = username;
		display();
		super.startServer();
	}

	/**
   	* Method to update username
    * Calls the updateUsername on the parent class
	* Sets a temporary variable which will be stored permanetly if the update goes through    
    * @param username : the new username that client would like to switch to
    */	
	public void updateUsername(String username) {
		super.updateUsername(username);
		newUsername = username;
	}

	/**
   	* Method to update user's History
    * Adds a message to the client history, and updates history display
    * @param username : the new username of the client we are communicating with
    * @param incoming : Indicates whether the message in incoming (true) or outgoing (false)
    * @param m : the message content
    */	
	@Override
	public void addMessageToHistory(String username, Boolean incoming, String m) {
		// display green message content if username update went through. 
		// update username officially in Gui 
		if (username.equals("warning") && m.equals("Successfully changed username.")) {
			System.out.println("change username success");
			feedback.setText(m);
			feedback.setForeground(Color.green);
			clientUsername = newUsername;
			super.updateLocalUsername(clientUsername);
			updateUsernameDisplay();
		// Else display warning with red label
		} else if(username.equals("warning")) {
			feedback.setText(m);
			feedback.setForeground(Color.red);

		// Message from user. Add message to history and update GUI
		} else {
			history.addMessage(username, incoming, m);
			updateHistoryDisplay();
		}
	}

	/**
   	* Method sendMessage
    * Grabs and resets message content from the GUI, and sends to server
    */	
	private void sendMessage() {
		String to = send_to.getText();
		String content = message.getText();
		message.setText("");

		// error or success reporting
		if (to.length() == 0) {
			feedback.setText("Error! The username " + to + " does not exist.");
			feedback.setForeground(Color.red);
		} else {
			super.sendMessage(to, content);
			addMessageToHistory(to, false, content);
			feedback.setText("Success! Your message was sent to " + to);
			feedback.setForeground(Color.green);
			updateHistoryDisplay();
		}
	}

	/**
   	* Method updateHistoryDisplay
    * Updates the history text on the page with the latest information
    */	
	private void updateHistoryDisplay() {
		String[] histText = history.getMessages();
		hist.setListData(histText);
	}

	/**
   	* Method updateUsernameDisplay
    * Update the welcome text to display the new username
    */	
	private void updateUsernameDisplay() {
		welcomeText.setText("Welcome " + clientUsername);
	}
 
	/**
   	* Method display
    * Initializes a JFrame, and renders the various components of the GUI
    */	
	private void display() {
		f = new JFrame();
		f.setLayout(null);

		renderMenu();
		renderWelcomeText();
		renderSendToInput();
		renderHistory();
		renderUserMessageBox();
		renderUserFeedback();

		f.setSize(500,500); 
		f.setLayout(null); 
		f.setVisible(true); 

		// Window listener to stop the server when the window closes
		f.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {

				ClientGui.super.stopServer();				
				System.exit(0);
			}

		});
	}

	/**
   	* Method renderMenu
    * Renders the Settings menu with option to update username
    */	
	private void renderMenu() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("Settings");
		menu.setMnemonic(KeyEvent.VK_A);
		menuBar.add(menu);

		// Menu item to update username
		JMenuItem menuItem1 = new JMenuItem("Change Username", KeyEvent.VK_T);
		menu.add(menuItem1);

		// Action listener to update username
		menuItem1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String username = (String)JOptionPane.showInputDialog(
                    f,
                    "Enter New Username:",
                    "Customized Dialog",
                    JOptionPane.PLAIN_MESSAGE);

				System.out.println("new username: " + username);
				updateUsername(username);
			}});

		// Menu item that doesn't do anything
		JMenuItem menuItem2 = new JMenuItem("Change Background Color");
		menuItem2.setMnemonic(KeyEvent.VK_B);
		menu.add(menuItem2);

		f.setJMenuBar(menuBar);

	}

	/**
   	* Method renderWelcomeText
    * Displays welcome text with the username of the client.
    */	
	private void renderWelcomeText() {
		welcomeText = new JLabel("Welcome " + clientUsername );
		welcomeText.setBounds(10,20, 400, 40);
		welcomeText.setFont(new Font("Verdana", Font.BOLD, 30));
		welcomeText.setForeground(Color.blue);
		f.add(welcomeText);
	}

	/**
   	* Method renderSendToInput
    * User can specify who to send the message to
    */	
	private void renderSendToInput() {
		JLabel to_label = new JLabel("To: "); 
		to_label.setBounds(10, 70, 30, 40); 

		send_to = new JTextField();  
        send_to.setBounds(50,70, 440, 40); 
        f.add(to_label);
		f.add(send_to);	 
	}

	/**
   	* Method renderUserMessageBox
    * Textbox and "send" button, where user can input and send a message.
    */	
	private void renderUserMessageBox() {
		message = new JTextField();  
        message.setBounds(10,340, 420, 40); 
		JButton b = new JButton("Send");
		b.setBounds(430,340, 60, 40);
    	b.setForeground(Color.green);
    	// b.setBackground(Color.green);
    	b.setOpaque(true);
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}});

		f.add(message);
		f.add(b); 
	}

	/**
   	* Method renderUserFeedback
    * A label to display a response when the user sends a message.
    * Can display "Success" or "Error", etc.
    */	
	private void renderUserFeedback() {
		feedback = new JLabel(""); 
		feedback.setBounds(10, 390, 480, 40); 
		f.add(feedback);
	}

	/**
   	* Method renderHistory
    * Displays a textarea that holds the chat history between this user and another
    */	
	private void renderHistory() {
		hist = new JList();
        scroll = new JScrollPane();
        scroll.setViewportView(hist);
  		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setBounds(10,120,480,200);
  		f.add(scroll);
	}

	// 
	/**
   	* Method Main
    * Main function that initialized Client Gui
    */	
	public static void main(String[] args) {  
		ClientGui gui = new ClientGui(args[0], Integer.parseInt(args[1]), args[2]);
	} 

}  