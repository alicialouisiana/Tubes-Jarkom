package ServerClient;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L; // For serialization
    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String roomName; // For messages related to specific rooms
    private String targetUser; // For private messages, kick/ban commands, or room owner in info
    private List<String> dataList; // For lists of rooms, users in room, or room members in info

    // Enum for message types
    public enum MessageType {
        SYSTEM, // System messages (e.g., server info, name request)
        TEXT, // Regular chat messages
        CREATE_ROOM_REQUEST,
        CREATE_ROOM_RESPONSE,
        JOIN_ROOM_REQUEST,
        JOIN_ROOM_RESPONSE,
        LEAVE_ROOM_REQUEST,
        LEAVE_ROOM_RESPONSE,
        LIST_ROOMS_REQUEST,
        LIST_ROOMS_RESPONSE,
        USERS_IN_ROOM_REQUEST,
        USERS_IN_ROOM_RESPONSE,
        USER_JOINED_ROOM, // Server broadcast when user joins a room
        USER_LEFT_ROOM, // Server broadcast when user leaves a room
        KICK_USER_REQUEST,
        KICK_USER_RESPONSE,
        USER_KICKED, // Server broadcast when user is kicked
        CLOSE_ROOM_REQUEST,
        CLOSE_ROOM_RESPONSE,
        ROOM_CLOSED, // Server broadcast when a room is closed

        // --- NEW MESSAGE TYPES FOR ROOM INFO ---
        ROOM_INFO_REQUEST, // Client requests details about a room
        ROOM_INFO_RESPONSE   // Server sends details about a room (owner, members)
        // --- END NEW MESSAGE TYPES ---
    }

    // Constructors (ensure they can handle all fields)
    public Message(String senderName, String content, MessageType type) {
        this(senderName, content, type, null, null, null);
    }

    public Message(String senderName, String content, MessageType type, String roomName) {
        this(senderName, content, type, roomName, null, null);
    }

    public Message(String senderName, String content, MessageType type, String roomName, String targetUser) {
        this(senderName, content, type, roomName, targetUser, null);
    }

    public Message(String senderName, String content, MessageType type, List<String> dataList) {
        this(senderName, content, type, null, null, dataList);
    }

    // Full constructor
    public Message(String senderName, String content, MessageType type, String roomName, String targetUser, List<String> dataList) {
        this.senderName = senderName;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.roomName = roomName;
        this.targetUser = targetUser;
        this.dataList = dataList;
    }

    // Getters
    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getTargetUser() {
        return targetUser;
    } // Used for owner name in ROOM_INFO_RESPONSE

    public List<String> getDataList() {
        return dataList;
    } // Used for member list in ROOM_INFO_RESPONSE

    // toString for debugging
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = timestamp.format(formatter);
        return String.format("[%s] %s: %s%s%s%s",
                formattedTime,
                type.name(),
                content,
                roomName != null ? " (Room: " + roomName + ")" : "",
                targetUser != null ? " (Target: " + targetUser + ")" : "",
                dataList != null ? " (Data: " + dataList + ")" : "");
    }
}
