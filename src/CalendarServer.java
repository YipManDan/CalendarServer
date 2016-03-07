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
    private static int port = 8080; //default port number is 8080
    private boolean continueServer;
    private ArrayList<ClientThread> list;
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
                list.add(ct); //Save client to ArrayList
                ct.start();
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
            whoIsIn(ct);
        }
    }
    
    private synchronized void whoIsIn(ClientThread thread) {
        /*thread.writeMsg(ChatMessage.MESSAGE, "List of the users connected at " + sdf.format(new Date()) + "\n", null, new UserId(0, "Server"));
        // scan all the users connected
        for(int i = 0; i < list.size(); ++i) {
            ClientThread ct = list.get(i);
            //writeMsg(ChatMessage.WHOISIN, (i + 1) + ") " + ct.username + " since " + ct.date);
            if(ct.id == thread.id) {
                thread.writeUser(ct.username, ct.id, true);
            }
            else
                thread.writeUser(ct.username, ct.id, false);

        }
        thread.writeMsg(ChatMessage.MESSAGE, "", null, new UserId(0, "Server"));*/

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
		CalendarServer cs = new CalendarServer(8080);
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
	            
	            // read the username
	            username = (String) in.readObject();
	            appendEvent(username + " has connected");
	            
	        } catch (IOException e) {
	        	appendEvent("Exception creating new Input/output Streams: " + e);
	            return;
	        } catch (ClassNotFoundException e) {}

	        date = new Date().toString() + "\n";
	    }

	    @Override
	    public void run() {
	        boolean loggedIn = true;
	        //Keep running until LOGOUT
	        while(loggedIn) {
	            // read a DateEvent object
	            try {
	            	incomingEvent = (DateEvent) in.readObject();
	            	appendEvent("Receiving event information from " + username);
	            } catch (IOException e) {
	            	appendEvent(username + " Exception reading Streams: " + e);
	                break;			
	            } catch(ClassNotFoundException e2) {
	                break;
	            }
	            
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
	            // the messaage part of the ChatMessage
	            ///String message = cm.getMessage();

	            // Switch on the type of message receive
	            //switch(cm.getType()) {

	           /* case ChatMessage.MESSAGE:
	                System.out.println("Message received from " + username);
	                if(cm.getRecipients().size()==0)
	                    broadcast(username + ": " + message);
	                else
	                    multicast(cm, username, id);
	                break;
	            case ChatMessage.LOGOUT:
	                event(username + " disconnected with a LOGOUT message.");
	                loggedIn = false;
	                break;
	            case ChatMessage.WHOISIN:
	                System.out.println("WHOISIN received from " + username);
	                whoIsIn(this);
	                break;
	            }*/
            	appendEvent("Total number of events stored on server: " + events.size());
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
	    private boolean writeUser(String msg, int userID, boolean isReceiver) {
	        // if Client is still connected send the message to it
	        if (!socket.isConnected()) {
	            close();
	            return false;
	        }
	        //ChatMessage cMsg = new ChatMessage(ChatMessage.WHOISIN, msg, new UserId(0, "Server"), isReceiver);
	        //cMsg.setUserID(userID);

	        // write the message to the stream
	       /*try {
	            //out.writeObject(cMsg);
	        } catch (IOException e) {
	            // if an error occurs, do not abort just inform the user
	        	appendEvent("Error sending message to " + username);
	        	appendEvent(e.toString());
	        }*/

	        return true;
	    }
	}
}
