package ServerClient;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe map

public class ChatRoom {
    private final String roomName;
    private final String ownerName; // Name of the user who created this room
    private final Set<ObjectOutputStream> clientStreams; // Streams of clients in THIS room
    private final Set<String> activeUsers; // Names of active users in THIS room

    public ChatRoom(String roomName, String ownerName) {
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.clientStreams = Collections.synchronizedSet(new HashSet<>()); // Thread-safe set
        this.activeUsers = Collections.synchronizedSet(new HashSet<>()); // Thread-safe set
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
        broadcastMessage(new Message("SERVER", userName + " has joined the room.", Message.MessageType.USER_JOINED_ROOM, roomName));
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

    // Broadcast message only to clients in this specific room
    public void broadcastMessage(Message message) {
        synchronized (clientStreams) {
            clientStreams.forEach(outStream -> {
                try {
                    outStream.writeObject(message);
                    outStream.flush();
                } catch (IOException e) {
                    System.err.println("Error broadcasting to client in room " + roomName + ": " + e.getMessage());
                    // Client might have disconnected; it will be cleaned up by ClientHandler's finally block
                }
            });
        }
    }

    // Kick a user from this room (only owner can call this)
    public boolean kickUser(String userToKick) {
        if (!activeUsers.contains(userToKick)) {
            return false; // User not in this room
        }
        // Signal the server to disconnect the user from this room
        // The actual disconnection and removal from the ClientHandler's map will happen in ChatServer
        return true; 
    }
    
    public int getClientCount() {
        return activeUsers.size();
    }
}