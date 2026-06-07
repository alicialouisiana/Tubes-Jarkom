package ServerClient;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L; 
    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String roomName; 
    private String targetUser; 
    private List<String> dataList; 
    private byte[] imageData; 

    public enum MessageType {
        SYSTEM, 
        TEXT, 
        IMAGE, 
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
        USER_JOINED_ROOM, 
        USER_LEFT_ROOM, 
        KICK_USER_REQUEST,
        KICK_USER_RESPONSE,
        USER_KICKED, 
        CLOSE_ROOM_REQUEST,
        CLOSE_ROOM_RESPONSE,
        ROOM_CLOSED, 

        ROOM_INFO_REQUEST, 
        ROOM_INFO_RESPONSE,   
        TYPING
    }

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

    public Message(String senderName, String content, MessageType type, String roomName, String targetUser, List<String> dataList) {
        this.senderName = senderName;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.roomName = roomName;
        this.targetUser = targetUser;
        this.dataList = dataList;
    }

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
    } 

    public List<String> getDataList() {
        return dataList;
    } 
    
    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData (byte[] imageData) {
        this.imageData = imageData;
    }
    
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
