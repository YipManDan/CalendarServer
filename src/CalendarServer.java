import java.awt.GridLayout;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class CalendarServer {
    private JFrame mainFrame;
    private JTextArea event;
    private static ServerSocket serverSocket;
    private static int port = 9001; //default port number
    private boolean continueServer;
    private ArrayList<ClientThread> list;
    private ArrayList<String> usernameList;
    private ArrayList<DateEvent> events;
    private static int uniqueID;
    private static int eventID;
    
	public CalendarServer(int port) {
		mainFrame = new JFrame("Calendar Server");
    	mainFrame.setSize(800,600);
    	mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        event = new JTextArea(80, 80);
        event.setEditable(false);
        JPanel center = new JPanel(new GridLayout(1,1));
        center.setBorder(new EmptyBorder(10,10,10,10));
        center.add(new JScrollPane(event));
        mainFrame.add(center);
        mainFrame.setVisible(true);
        continueServer = true;
        list = new ArrayList<ClientThread>();
        usernameList = new ArrayList<String>();
        events = new ArrayList<DateEvent>();
        uniqueID = 0;
        eventID = 0;
	}
	
	void start() {
		try {
            serverSocket = new ServerSocket(port); //Start listening on port
            appendEvent("Web Server running on Inet Address " + serverSocket.getInetAddress()
            + " port " + serverSocket.getLocalPort());
            
            
            //Server infinite loop and wait for clients to connect
            while (continueServer) {
                
                Socket socket = serverSocket.accept(); //accept client connection
                appendEvent("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
                
                //Create a new thread and handle the connection
                ClientThread ct = new ClientThread(socket);
                if(ct.checkUsername()) {
                	list.add(ct); //Save client to ArrayList
                	ct.start();
                }
            }
            //when server stops
            try
            {
                serverSocket.close();
                //close all threads
                for(int i=0; i<list.size(); i++)
                {
                    ClientThread tempThread = list.get(i);
                    try{
                        tempThread.socket.close();
                        tempThread.in.close();
                        tempThread.out.close();
                    }
                    catch (IOException ioE){

                    }
                }
            }
            catch (Exception e)
            {
                appendEvent("Exception in closing server and clients: " + e);
            }
            
        } catch (Exception e) {
        	appendEvent(new Date() + " Exception in starting server: " + e);
        }			
	}
	
    public void appendEvent(String str) {
        event.append(str + "\n");
        //event.setCaretPosition(chat.getText().length() - 1);
    }
	
    private synchronized void removeThread(int index) {
        list.remove(index);
        for(int i = list.size()-1; i >= 0; --i) {
            ClientThread ct = list.get(i);
        }
    }   
    
    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for(int i = 0; i < list.size(); ++i) {
            ClientThread ct = list.get(i);
            if(ct.id == id) {
                removeThread(i);
                appendEvent("Disconnected Client" + i + " : " + ct.username + " removed from list");
                return;
            }
        }
    }
    
    //Send a message to all clients in recipients list
    private synchronized void multicast(DateEvent event) {

    	for(int i = 0; i < list.size(); i++) {
    		if(event.getMembers().containsKey(list.get(i).username)) {
                ClientThread ct = list.get(i);
                event.setTimestamp(new Date());
                if (!ct.writeMsg(event)) {
                    removeThread(i);
                    appendEvent("Disconnected Client" + i + " : " + ct.username + " removed from list");
                } else {
                	appendEvent("Event information sent to user " + ct.username);
                }
    		}
    	}
    }   
    
	public static void main(String[] args) {
		CalendarServer cs = new CalendarServer(9001);
		cs.start();
	}
	
	//Class to handle the clients
	class ClientThread extends Thread {
	    Socket socket;
	    ObjectInputStream in;
	    ObjectOutputStream out;
	    
	    int id; //Unique ID (easier for deconnection)
	    String username; //Client username
	    DateEvent incomingEvent;
	    String date;

	    // Constructor
	    ClientThread(Socket socket) {
	        // a unique id
	        id = ++uniqueID;
	        this.socket = socket;

	        //Create both Data Stream
	        try {
	            out = new ObjectOutputStream(socket.getOutputStream());
	            in  = new ObjectInputStream(socket.getInputStream());	            
	            
	        } catch (IOException e) {
	        	appendEvent("Exception creating new Input/output Streams: " + e);
	            return;
	        } //catch (ClassNotFoundException e) {}

	        date = new Date().toString() + "\n";
	    }

	    public boolean checkUsername() throws ClassNotFoundException, IOException {
            // read the username
            username = (String) in.readObject();
            
            for(ClientThread cthread : list) {
            	if(cthread.username.equals(username)) {
            		out.writeObject("Username is already in use, please select another username");
            		close();
            		return false;
            	}
            }
            
            appendEvent(username + " has connected.");	
            
            if(!usernameList.contains(username)) {
            	usernameList.add(username);
            	appendEvent("Detected new user. Adding " + username + " to user list.");
            }
            
            for(int i = 0; i < events.size(); i++) {
            	if(events.get(i).getMembers().containsKey(username)) {
            		events.get(i).setTimestamp(new Date());
                    if (!writeMsg(events.get(i))) {
                        removeThread(i);
                        appendEvent("Disconnected Client " + i + " : " + username + " removed from list");
                    } else {
                    	appendEvent("Event information sent to user " + username);
                    }
            	}
            }            
			return true;
		}

		@Override
	    public void run() {
			appendEvent("Now listening for the user " + username);
	        boolean loggedIn = true;
	        //Keep running until LOGOUT
	        while(loggedIn) {
	            // read a DateEvent object
	            try {
	            	Object received = in.readObject();

                	if(received instanceof DateEvent) {	            	
		            	incomingEvent = (DateEvent) received;
		            	appendEvent("Receiving event information from " + username);
		            	
			            //Event does not exist on the server because id is null, add it
			            if(incomingEvent.getID() == null) {
			            	incomingEvent.setID(++eventID);
			            	events.add(incomingEvent);	 
			            	appendEvent("No eventID found, changing it to " + eventID);
			            	multicast(incomingEvent);
			            } else {
			            	boolean existingEvent = false;
		            		for(int i = 0; i < events.size(); i++) {
		            			if(incomingEvent.getID().equals(events.get(i).getID())) {
		        	            	appendEvent("Found match for eventID" + incomingEvent.getID());
		            				existingEvent = true;
		            				events.set(i, incomingEvent);
		            				break;
		            			}
		            		}
		            		
		            		//This should never happen, but just in case
		            		if(!existingEvent) {
		    	            	events.add(incomingEvent);	           			
		            		}
		            		multicast(incomingEvent);
			            }
		            	appendEvent("Total number of events stored on server: " + events.size());		            	
                	} else if(received instanceof String) {
                		writeMembers();
                	} else {
                		//appendEvent("Error: Received object of type " + received.getClass().getName());
                	}
	            } catch (IOException e) {
	            	appendEvent(username + " Exception reading Streams: " + e);
	                break;			
	            } catch(ClassNotFoundException e2) {
	                break;
	            }
	            

	        }
	        
	        //Remove self from the arrayList containing the list of connected Clients
	        remove(id);
	        close();
	    }

	    // try to close everything
	    private void close() {
	        // try to close the connection
	        try {
	            if(out != null) out.close();
	        } catch(Exception e) {}
	        
	        try {
	            if(in != null) in.close();
	        } catch(Exception e) {}
	        
	        try {
	            if(socket != null) socket.close();
	        } catch (Exception e) {}
	    }

	    //Write message to the Client output stream
	    private boolean writeMsg(DateEvent eventToSend) {
	        // if Client is still connected send the message to it
	        if(!socket.isConnected()) {
	            close();
	            return false;
	        }
	        //ChatMessage cMsg = new ChatMessage(type, msg, recipients, sender);
	        appendEvent("Updating event info for " + username);

	        // write the message to the stream
	        try {
	            out.writeObject(eventToSend);
	        } catch(IOException e) {
	            // if an error occurs, do not abort just inform the user
	        	appendEvent("Error sending message to " + username);
	        	appendEvent(e.toString());
	        }
	        
	        return true;
	    }

	    private boolean writeMembers() {
	        // if Client is still connected send the message to it
	        if(!socket.isConnected()) {
	            close();
	            return false;
	        }	    	
	        
        	appendEvent("Member list request received. Sending to " + username);		            	               		
	        
        	// write the message to the stream
	        try {
	        	//ArrayList<String> usernameList = new ArrayList<>();
	        	//for(ClientThread clients : list) {
	        	//	usernameList.add(clients.username);
	        	//}
	            out.writeObject(usernameList);
	        } catch(IOException e) {
	            // if an error occurs, do not abort just inform the user
	        	appendEvent("Error sending message to " + username);
	        	appendEvent(e.toString());
	        }
	        
	        return true;
	    }
	}
}
