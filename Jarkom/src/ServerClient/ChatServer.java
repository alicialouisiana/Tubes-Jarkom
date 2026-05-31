package ServerClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe maps
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 12345;

    // Map to store active chat rooms: roomName -> ChatRoom object
    private static final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    // Map to store client handlers, useful for direct access or managing client state
    private static final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("Chat Server Started on port " + PORT);

        // Create a default general room
        // A single "general" room where users start
        ChatRoom generalRoom = new ChatRoom("General", "SERVER");
        chatRooms.put("General", generalRoom);
        System.out.println("Default room 'General' created.");

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = listener.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    // Central method to broadcast a Message object to a specific room or all
    public static void broadcastMessage(Message message) {
        if (message.getRoomName() != null) {
            ChatRoom room = chatRooms.get(message.getRoomName());
            if (room != null) {
                room.broadcastMessage(message);
            } else {
                System.err.println("Attempted to broadcast to non-existent room: " + message.getRoomName());
            }
        } else {
            // For general system messages (e.g., server startup/shutdown)
            // or messages not specific to a room (e.g., initial join announcements before room selection)
            // This would usually be the "General" or a global announcement channel.
            // For now, let's assume global announcements also go through a room (e.g., "General")
            ChatRoom general = chatRooms.get("General");
            if (general != null) {
                general.broadcastMessage(message);
            } else {
                // Fallback if General doesn't exist (shouldn't happen)
                connectedClients.values().forEach(handler -> {
                    try {
                        handler.getOutStream().writeObject(message);
                        handler.getOutStream().flush();
                    } catch (IOException e) {
                        System.err.println("Error broadcasting to client during fallback: " + e.getMessage());
                    }
                });
            }
        }
    }

    // Helper to send a message to a specific client
    public static void sendMessageToClient(String clientName, Message message) {
        ClientHandler handler = connectedClients.get(clientName);
        if (handler != null && handler.getOutStream() != null) {
            try {
                handler.getOutStream().writeObject(message);
                handler.getOutStream().flush();
            } catch (IOException e) {
                System.err.println("Error sending message to client " + clientName + ": " + e.getMessage());
            }
        }
    }

    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;
        private String currentRoomName = "General"; // Every client starts in the General

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public ObjectOutputStream getOutStream() {
            return out;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());

                // 1. Request client's name
                out.writeObject(new Message("SERVER", "SUBMITNAME", Message.MessageType.SYSTEM));
                out.flush();

                // Wait for the name from the client
                Object nameObj = in.readObject();

                clientName = (String) nameObj.toString().substring(19);
                if (clientName == null || clientName.trim().isEmpty()) {
                    System.out.println("Client did not provide a name. Disconnecting.");
                    return;
                }

                // Check for duplicate names (simple check for now)
                if (connectedClients.containsKey(clientName)) {
                    sendMessageToClient(clientName, new Message("SERVER", "Name '" + clientName + "' is already taken. Please choose another.", Message.MessageType.SYSTEM));
                    System.out.println("Duplicate name attempt: " + clientName);
                    return; // Disconnect this client
                }

                connectedClients.put(clientName, this); // Add to connected clients map

                System.out.println(clientName + " has connected.");

                ChatRoom general = chatRooms.get("General");
                if (general != null) {
                    general.addClient(clientName, out);
                } else {
                    System.err.println("General room not found! Cannot add client " + clientName);
                    return;
                }

                // Send the initial list of rooms to the newly connected client
                sendRoomListToClient();

                // Main loop for receiving messages
                Object receivedObject;
                while ((receivedObject = in.readObject()) != null) {
                    if (receivedObject instanceof Message) {
                        Message incomingMessage = (Message) receivedObject;
                        System.out.println("Server received from " + incomingMessage.getSenderName() + ": " + incomingMessage.getType() + " - " + incomingMessage.getContent() + (incomingMessage.getRoomName() != null ? " (Room: " + incomingMessage.getRoomName() + ")" : ""));

                        switch (incomingMessage.getType()) {
                            case TEXT:
                                // Only broadcast if client is in a room and message is for that room
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoomName)) {
                                    ChatRoom currentChatRoom = chatRooms.get(currentRoomName);
                                    if (currentChatRoom != null) {
                                        currentChatRoom.broadcastMessage(incomingMessage);
                                    }
                                } else {
                                    System.err.println("Client " + clientName + " sent TEXT message for incorrect or null room: " + incomingMessage.getRoomName());
                                    sendMessageToClient(clientName, new Message("SERVER", "Error: Message not sent. You are not in the specified room.", Message.MessageType.SYSTEM));
                                }
                                break;

                            case TYPING:
                                if (incomingMessage.getRoomName() != null
                                        && incomingMessage.getRoomName().equals(currentRoomName)) {

                                    ChatRoom currentChatRoom = chatRooms.get(currentRoomName);

                                    if (currentChatRoom != null) {
                                        currentChatRoom.broadcastMessage(incomingMessage);
                                    }
                                }
                                break;

                            case CREATE_ROOM_REQUEST:
                                String newRoomName = incomingMessage.getContent().trim();
                                if (newRoomName.isEmpty()) {
                                    sendMessageToClient(clientName, new Message("SERVER", "Room name cannot be empty.", Message.MessageType.CREATE_ROOM_RESPONSE));
                                } else if (chatRooms.containsKey(newRoomName)) {
                                    sendMessageToClient(clientName, new Message("SERVER", "Room '" + newRoomName + "' already exists.", Message.MessageType.CREATE_ROOM_RESPONSE));
                                } else {
                                    ChatRoom newRoom = new ChatRoom(newRoomName, clientName);
                                    chatRooms.put(newRoomName, newRoom);
                                    System.out.println(clientName + " created room: " + newRoomName);

                                    // Make client automatically join the newly created room
                                    handleLeaveRoom(clientName, currentRoomName, out, "Left"); // Leave current room first
                                    currentRoomName = newRoomName; // Update client's current room
                                    newRoom.addClient(clientName, out); // Add to new room
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.CREATE_ROOM_RESPONSE, newRoomName));
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, newRoomName)); // Also send join success
                                    broadcastRoomListToAll(); // Notify all clients about new room
                                }
                                break;

                            case LIST_ROOMS_REQUEST:
                                sendRoomListToClient();
                                break;

                            case JOIN_ROOM_REQUEST:
                                String roomToJoin = incomingMessage.getRoomName();
                                if (roomToJoin == null || roomToJoin.trim().isEmpty()) {
                                    sendMessageToClient(clientName, new Message("SERVER", "Room name cannot be empty.", Message.MessageType.JOIN_ROOM_RESPONSE));
                                    break;
                                }
                                ChatRoom targetRoom = chatRooms.get(roomToJoin);
                                if (targetRoom == null) {
                                    sendMessageToClient(clientName, new Message("SERVER", "Room '" + roomToJoin + "' does not exist.", Message.MessageType.JOIN_ROOM_RESPONSE));
                                } else {
                                    // Remove from current room first
                                    handleLeaveRoom(clientName, currentRoomName, out, "Left");

                                    // Add to the new room
                                    targetRoom.addClient(clientName, out);
                                    currentRoomName = roomToJoin;
                                    System.out.println(clientName + " joined room: " + roomToJoin);
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, roomToJoin));

                                    // Send list of users in new room to the client who just joined
                                    sendMessageToClient(clientName, new Message("SERVER", "Users in room " + roomToJoin, Message.MessageType.USERS_IN_ROOM_RESPONSE, new ArrayList<>(targetRoom.getActiveUsers())));
                                }
                                break;

                            case LEAVE_ROOM_REQUEST:
                                String roomToLeave = incomingMessage.getRoomName();
                                if (roomToLeave != null && roomToLeave.equals(currentRoomName)) {
                                    handleLeaveRoom(clientName, roomToLeave, out, "Left");
                                    currentRoomName = "General"; 
                                    ChatRoom generalRoomAfterLeave = chatRooms.get("General");
                                    if (generalRoomAfterLeave != null) {
                                        generalRoomAfterLeave.addClient(clientName, out); // Add back to general for general announcements
                                    }
                                    System.out.println(clientName + " left room: " + roomToLeave);
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.LEAVE_ROOM_RESPONSE, roomToLeave));
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, "General")); // Re-join general
                                } else {
                                    sendMessageToClient(clientName, new Message("SERVER", "You are not in room '" + roomToLeave + "'", Message.MessageType.LEAVE_ROOM_RESPONSE));
                                }
                                break;

                            case USERS_IN_ROOM_REQUEST:
                                ChatRoom roomForUsers = chatRooms.get(currentRoomName);
                                if (roomForUsers != null) {
                                    sendMessageToClient(clientName, new Message("SERVER", "Users in room " + currentRoomName, Message.MessageType.USERS_IN_ROOM_RESPONSE, new ArrayList<>(roomForUsers.getActiveUsers())));
                                } else {
                                    sendMessageToClient(clientName, new Message("SERVER", "Not in any room.", Message.MessageType.SYSTEM));
                                }
                                break;

                            case KICK_USER_REQUEST:
                                String roomToKickFrom = incomingMessage.getRoomName();
                                String userToKick = incomingMessage.getTargetUser();
                                ChatRoom kickRoom = chatRooms.get(roomToKickFrom);
                                if (kickRoom != null && kickRoom.getOwnerName().equals(clientName)) {
                                    // Find the handler of the user to kick
                                    ClientHandler targetHandler = connectedClients.get(userToKick);
                                    if (targetHandler != null) {
                                        handleLeaveRoom(userToKick, roomToKickFrom, targetHandler.getOutStream(), "Kick");
                                        kickRoom.broadcastMessage(new Message("SERVER", userToKick + " has been kicked by " + clientName + ".", Message.MessageType.USER_KICKED, roomToKickFrom));
                                        sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.KICK_USER_RESPONSE, roomToKickFrom, userToKick, null));

                                        // Inform the kicked user directly
                                        sendMessageToClient(userToKick, new Message("SERVER", "You have been kicked from room '" + roomToKickFrom + "' by " + clientName + ".", Message.MessageType.USER_KICKED, roomToKickFrom)); // Using ROOM_CLOSED for simple message, or add new type

                                        // Move kicked user to General
                                        targetHandler.currentRoomName = "General";
                                        ChatRoom generalAfterKick = chatRooms.get("General");
                                        if (generalAfterKick != null) {
                                            generalAfterKick.addClient(userToKick, targetHandler.getOutStream());
                                            sendMessageToClient(userToKick, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, "General"));
                                        }
                                    } else {
                                        sendMessageToClient(clientName, new Message("SERVER", "User '" + userToKick + "' not found or not in this room.", Message.MessageType.KICK_USER_RESPONSE));
                                    }
                                } else {
                                    sendMessageToClient(clientName, new Message("SERVER", "You are not the owner of this room or room does not exist.", Message.MessageType.KICK_USER_RESPONSE));
                                }
                                break;

                            case CLOSE_ROOM_REQUEST:
                                String roomToClose = incomingMessage.getRoomName();
                                ChatRoom closingRoom = chatRooms.get(roomToClose);
                                if (closingRoom != null && closingRoom.getOwnerName().equals(clientName)) {
                                    // Notify all users in the closing room
                                    closingRoom.broadcastMessage(new Message("SERVER", "Room '" + roomToClose + "' is closing. You will be disconnected.", Message.MessageType.ROOM_CLOSED, roomToClose));

                                    // Disconnect all users from this room and move to general
                                    Set<String> usersToMove = new HashSet<>(closingRoom.getActiveUsers());
                                    for (String user : usersToMove) {
                                        ClientHandler handlerToMove = connectedClients.get(user);
                                        if (handlerToMove != null) {
                                            handleLeaveRoom(user, roomToClose, handlerToMove.getOutStream(), "Left");
                                            handlerToMove.currentRoomName = "General"; // Move to General
                                            ChatRoom generalAfterClose = chatRooms.get("General");
                                            if (generalAfterClose != null) {
                                                generalAfterClose.addClient(user, handlerToMove.getOutStream());
                                                sendMessageToClient(user, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, "General"));
                                            }
                                        }
                                    }

                                    for (String user : usersToMove) {
                                        sendMessageToClient(user, new Message("SERVER", "SUCCESS", Message.MessageType.CLOSE_ROOM_RESPONSE, roomToClose));
                                    }
                                    chatRooms.remove(roomToClose); // Remove room from server's list
                                    System.out.println("Room '" + roomToClose + "' closed by owner " + clientName + ".");
                                    broadcastRoomListToAll(); // Update room list for all clients
                                } else {
                                    sendMessageToClient(clientName, new Message("SERVER", "You are not the owner of this room or room does not exist.", Message.MessageType.CLOSE_ROOM_RESPONSE));
                                }
                                break;
                            case ROOM_INFO_REQUEST:
                                handleRoomInfoRequest(incomingMessage);
                                break;
                            default:
                                System.out.println("Unhandled message type: " + incomingMessage.getType());
                                break;
                        }
                    } else {
                        System.out.println("Server received non-Message object: " + receivedObject);
                    }
                }

            } catch (EOFException e) {
                System.out.println("Client disconnected (EOF): " + (clientName != null ? clientName : clientSocket.getInetAddress().getHostAddress()));
            } catch (SocketException e) {
                System.err.println("Client socket error (likely disconnected): " + (clientName != null ? clientName : clientSocket.getInetAddress().getHostAddress()) + " - " + e.getMessage());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error handling client " + (clientName != null ? clientName : clientSocket.getInetAddress().getHostAddress()) + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Clean up: remove client from its current room and from connectedClients map
                if (clientName != null) {
                    handleLeaveRoom(clientName, currentRoomName, out, "Left");
                    connectedClients.remove(clientName);
                    System.out.println(clientName + " has fully disconnected.");
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRoomInfoRequest(Message message) {
            String requestedRoomName = message.getRoomName();
            if (requestedRoomName == null || requestedRoomName.trim().isEmpty()) {
                sendMessageToClient(clientName, new Message("SERVER", "Please specify a room to get info for.", Message.MessageType.ROOM_INFO_RESPONSE));
                System.err.println(clientName + " sent empty ROOM_INFO_REQUEST.");
                return;
            }

            ChatRoom room = chatRooms.get(requestedRoomName);
            if (room == null) {
                sendMessageToClient(clientName, new Message("SERVER", "Room '" + requestedRoomName + "' does not exist.", Message.MessageType.ROOM_INFO_RESPONSE));
                System.out.println(clientName + " requested info for non-existent room: " + requestedRoomName);
                return;
            }

            // Get owner's name and list of current members
            String owner = room.getOwnerName();
            List<String> members = new java.util.ArrayList<>(room.getActiveUsers());

            if (message.getContent().endsWith("Group List")) {
                // Send ROOM_INFO_RESPONSE back to the requesting client
                sendMessageToClient(clientName, new Message("SERVER", "Info for room '" + requestedRoomName + "' for Group List",
                        Message.MessageType.ROOM_INFO_RESPONSE,
                        requestedRoomName, // roomName field
                        owner, // targetUser field will hold owner name
                        members));         // dataList field will hold member list
            } else {
                if (message.getContent().startsWith("Silent")) {
                    sendMessageToClient(clientName, new Message("SERVER", "Silent" + requestedRoomName + "'",
                            Message.MessageType.ROOM_INFO_RESPONSE,
                            requestedRoomName, // roomName field
                            owner, // targetUser field will hold owner name
                            members));         // dataList field will hold member list
                } else {
                    // Send ROOM_INFO_RESPONSE back to the requesting client
                    sendMessageToClient(clientName, new Message("SERVER", "Info for room '" + requestedRoomName + "'",
                            Message.MessageType.ROOM_INFO_RESPONSE,
                            requestedRoomName, // roomName field
                            owner, // targetUser field will hold owner name
                            members));         // dataList field will hold member list
                }
            }

            System.out.println(clientName + " successfully sent info for room " + requestedRoomName);
        }

        // Helper to remove client from a room
        private void handleLeaveRoom(String clientName, String roomName, ObjectOutputStream outStream, String type) {
            ChatRoom room = chatRooms.get(roomName);
            if (room != null) {
                room.removeClient(clientName, outStream, type);
            }
        }

        // Sends the current list of available room names to THIS client
        private void sendRoomListToClient() {
            List<String> roomNames = new ArrayList<>(chatRooms.keySet());
            // Optionally sort roomNames for consistent display
            Collections.sort(roomNames);
            sendMessageToClient(clientName, new Message("SERVER", "Available Rooms", Message.MessageType.LIST_ROOMS_RESPONSE, roomNames));
            System.out.println("Sent room list to " + clientName + ": " + roomNames);
        }
    }

    // Sends the current list of available room names to ALL connected clients
    public static void broadcastRoomListToAll() {
        List<String> roomNames = new ArrayList<>(chatRooms.keySet());
        Collections.sort(roomNames);
        Message roomListMessage = new Message("SERVER", "Updated Room List", Message.MessageType.LIST_ROOMS_RESPONSE, roomNames);

        // Iterate through all connected clients and send them the updated list
        connectedClients.values().forEach(handler -> {
            try {
                handler.getOutStream().writeObject(roomListMessage);
                handler.getOutStream().flush();
            } catch (IOException e) {
                System.err.println("Error broadcasting room list to " + handler.clientName + ": " + e.getMessage());
            }
        });
        System.out.println("Broadcasted updated room list: " + roomNames);
    }
}
