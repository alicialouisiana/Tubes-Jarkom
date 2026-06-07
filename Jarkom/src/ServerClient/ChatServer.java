package ServerClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe maps
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 23181;

    private static final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("Chat Server Started on port " + PORT);

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

    public static void broadcastMessage(Message message) {
        if (message.getRoomName() != null) {
            ChatRoom room = chatRooms.get(message.getRoomName());
            if (room != null) {
                room.broadcastMessage(message);
            } else {
                System.err.println("Attempted to broadcast to non-existent room: " + message.getRoomName());
            }
        } else {
            ChatRoom general = chatRooms.get("General");
            if (general != null) {
                general.broadcastMessage(message);
            } else {
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

    public static void sendMessageToClient(String clientName, Message message) {
        ClientHandler handler = connectedClients.get(clientName);
        if (handler != null && handler.getOutStream() != null) {
            try {
                handler.getOutStream().writeObject(message);
                handler.getOutStream().flush();
            } catch (IOException e) {
                System.err.println("Error to client " + clientName + ": " + e.getMessage());
            }
        }
    }

    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;
        private String currentRoomName = "General"; 

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

                out.writeObject(new Message("SERVER", "SUBMITNAME", Message.MessageType.SYSTEM));
                out.flush();

                Object nameObj = in.readObject();

                clientName = (String) nameObj.toString().substring(19);
                if (clientName == null || clientName.trim().isEmpty()) {
                    System.out.println("Client did not provide a name. Disconnecting.");
                    return;
                }

                if (connectedClients.containsKey(clientName)) {
                    sendMessageToClient(clientName, new Message("SERVER", "Name '" + clientName + "' is already taken. Please choose another.", Message.MessageType.SYSTEM));
                    System.out.println("Duplicate name attempt: " + clientName);
                    return; // Disconnect this client
                }

                connectedClients.put(clientName, this); 

                System.out.println(clientName + " has connected.");

                ChatRoom general = chatRooms.get("General");
                if (general != null) {
                    general.addClient(clientName, out);
                } else {
                    System.err.println("General room not found! Cannot add client " + clientName);
                    return;
                }

                sendRoomListToClient();

                Object receivedObject;
                while ((receivedObject = in.readObject()) != null) {
                    if (receivedObject instanceof Message) {
                        Message incomingMessage = (Message) receivedObject;
                        System.out.println("Server received from " + incomingMessage.getSenderName() + ": " + incomingMessage.getType() + " - " + incomingMessage.getContent() + (incomingMessage.getRoomName() != null ? " (Room: " + incomingMessage.getRoomName() + ")" : ""));

                        switch (incomingMessage.getType()) {
                            case TEXT:
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

                            case IMAGE:
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoomName)) {
                                    ChatRoom currentChatRoom = chatRooms.get(currentRoomName);
                                    if (currentChatRoom != null) {
                                        currentChatRoom.broadcastMessage(incomingMessage);
                                    }
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

                                    handleLeaveRoom(clientName, currentRoomName, out, "Left"); // Leave current room first
                                    currentRoomName = newRoomName; 
                                    newRoom.addClient(clientName, out); 
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.CREATE_ROOM_RESPONSE, newRoomName));
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, newRoomName));
                                    broadcastRoomListToAll(); 
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
                                    handleLeaveRoom(clientName, currentRoomName, out, "Left");

                                    targetRoom.addClient(clientName, out);
                                    currentRoomName = roomToJoin;
                                    System.out.println(clientName + " joined room: " + roomToJoin);
                                    sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.JOIN_ROOM_RESPONSE, roomToJoin));

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
                                        generalRoomAfterLeave.addClient(clientName, out); 
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
                                    ClientHandler targetHandler = connectedClients.get(userToKick);
                                    if (targetHandler != null) {
                                        handleLeaveRoom(userToKick, roomToKickFrom, targetHandler.getOutStream(), "Kick");
                                        kickRoom.broadcastMessage(new Message("SERVER", userToKick + " has been kicked by " + clientName + ".", Message.MessageType.USER_KICKED, roomToKickFrom));
                                        sendMessageToClient(clientName, new Message("SERVER", "SUCCESS", Message.MessageType.KICK_USER_RESPONSE, roomToKickFrom, userToKick, null));

                                        sendMessageToClient(userToKick, new Message("SERVER", "You have been kicked from room '" + roomToKickFrom + "' by " + clientName + ".", Message.MessageType.USER_KICKED, roomToKickFrom)); // Using ROOM_CLOSED for simple message, or add new type

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

            String owner = room.getOwnerName();
            List<String> members = new java.util.ArrayList<>(room.getActiveUsers());

            if (message.getContent().endsWith("Group List")) {
                sendMessageToClient(clientName, new Message("SERVER", "Info for room '" + requestedRoomName + "' for Group List",
                        Message.MessageType.ROOM_INFO_RESPONSE,
                        requestedRoomName, 
                        owner, 
                        members));         
            } else {
                if (message.getContent().startsWith("Silent")) {
                    sendMessageToClient(clientName, new Message("SERVER", "Silent" + requestedRoomName + "'",
                            Message.MessageType.ROOM_INFO_RESPONSE,
                            requestedRoomName, 
                            owner, 
                            members));         
                } else {
                    sendMessageToClient(clientName, new Message("SERVER", "Info for room '" + requestedRoomName + "'",
                            Message.MessageType.ROOM_INFO_RESPONSE,
                            requestedRoomName, 
                            owner, 
                            members));         
                }
            }

            System.out.println(clientName + " successfully sent info for room " + requestedRoomName);
        }

        private void handleLeaveRoom(String clientName, String roomName, ObjectOutputStream outStream, String type) {
            ChatRoom room = chatRooms.get(roomName);
            if (room != null) {
                room.removeClient(clientName, outStream, type);
            }
        }

        private void sendRoomListToClient() {
            List<String> roomNames = new ArrayList<>(chatRooms.keySet());
            Collections.sort(roomNames);
            sendMessageToClient(clientName, new Message("SERVER", "Available Rooms", Message.MessageType.LIST_ROOMS_RESPONSE, roomNames));
            System.out.println("Sent room list to " + clientName + ": " + roomNames);
        }
    }

    public static void broadcastRoomListToAll() {
        List<String> roomNames = new ArrayList<>(chatRooms.keySet());
        Collections.sort(roomNames);
        Message roomListMessage = new Message("SERVER", "Updated Room List", Message.MessageType.LIST_ROOMS_RESPONSE, roomNames);

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
