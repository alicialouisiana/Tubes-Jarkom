package ServerClient;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatRoom {
    private final String roomName;
    private final String ownerName; 
    private final Set<ObjectOutputStream> clientStreams; 
    private final Set<String> activeUsers; 

    public ChatRoom(String roomName, String ownerName) {
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.clientStreams = Collections.synchronizedSet(new HashSet<>()); 
        this.activeUsers = Collections.synchronizedSet(new HashSet<>()); 
    }

    public String getRoomName() {
        return roomName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Set<String> getActiveUsers() {
        return activeUsers;
    }

    public void addClient(String userName, ObjectOutputStream outStream) {
        synchronized (clientStreams) {
            clientStreams.add(outStream);
        }
        synchronized (activeUsers) {
            activeUsers.add(userName);
        }
        broadcastMessage(new Message("SERVER", userName + " has joined the room. Say hi!", Message.MessageType.USER_JOINED_ROOM, roomName));
    }

    public void removeClient(String userName, ObjectOutputStream outStream, String type) {
        boolean removedStream;
        boolean removedUser;
        synchronized (clientStreams) {
            removedStream = clientStreams.remove(outStream);
        }
        synchronized (activeUsers) {
            removedUser = activeUsers.remove(userName);
        }
        if (removedUser) {
            if(type.equals("Left")){
                broadcastMessage(new Message("SERVER", userName + " has left the room.", Message.MessageType.USER_LEFT_ROOM, roomName));
            } else if(type.equals("Kick")){
                broadcastMessage(new Message("SERVER", userName + " has been kicked out by owner.", Message.MessageType.USER_LEFT_ROOM, roomName));
            }
        }
    }

    public void broadcastMessage(Message message) {
        synchronized (clientStreams) {
            clientStreams.forEach(outStream -> {
                try {
                    outStream.writeObject(message);
                    outStream.flush();
                } catch (IOException e) {
                    System.err.println("Error broadcasting to client in room " + roomName + ": " + e.getMessage());
                }
            });
        }
    }

    public boolean kickUser(String userToKick) {
        if (!activeUsers.contains(userToKick)) {
            return false; // User not in this room
        }
        return true; 
    }
    
    public int getClientCount() {
        return activeUsers.size();
    }
}