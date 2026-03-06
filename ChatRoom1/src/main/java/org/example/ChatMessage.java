package org.example;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Chat message entity class
 * Responsible for encapsulating and representing various types of messages in the chat room
 * Including: user join/leave messages, text messages, user list messages, etc.
 *
 * Functions:
 * 1. Store message metadata (user ID, username, content, timestamp, type)
 * 2. Provide methods for formatting time strings
 * 3. Generate different display formats based on message type
 */
public class ChatMessage implements Serializable {
    /** Serialization version ID to ensure serialization compatibility */
    private static final long serialVersionUID = 1L;

    // ============ Message Property Fields ============
    /** Unique user identifier, 5-digit ID automatically assigned by server */
    private String userId;

    /** User-defined display name */
    private String username;

    /** Message body content */
    private String content;

    /** Message sending timestamp (milliseconds) */
    private long timestamp;

    /** Message type, distinguishing different message purposes */
    private MessageType type;

    /**
     * Message type enumeration
     * USER_JOIN - User joined the chat room
     * USER_LEAVE - User left the chat room
     * TEXT_MESSAGE - Regular text message
     * USER_LIST - Online user list message
     * HISTORY_REQUEST - Historical message request (reserved function)
     */
    public enum MessageType {
        USER_JOIN,      // User joined
        USER_LEAVE,     // User left
        TEXT_MESSAGE,   // Text message
        USER_LIST,      // User list
        HISTORY_REQUEST // Historical message request
    }

    // ============ Constructor ============
    /**
     * Constructor for creating message object
     * @param userId User ID
     * @param username Username
     * @param content Message content
     * @param type Message type
     */
    public ChatMessage(String userId, String username, String content, MessageType type) {
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis(); // Record current time
    }

    // ============ Getter Methods ============
    /**
     * Get user ID
     * @return User ID string
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get username
     * @return Username string
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get message content
     * @return Message body
     */
    public String getContent() {
        return content;
    }

    /**
     * Get timestamp
     * @return Message sending timestamp (milliseconds)
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get message type
     * @return MessageType enumeration value
     */
    public MessageType getType() {
        return type;
    }

    // ============ Helper Methods ============
    /**
     * Format timestamp into readable string
     * Format: HH:mm:ss (24-hour clock)
     * @return Formatted time string
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    /**
     * Generate formatted display string based on message type
     * Format description:
     * - User joined: [Time] System: User username (ID) joined the chat room
     * - User left: [Time] System: User username (ID) left the chat room
     * - Text message: [Time] username (ID): message content
     * @return Formatted complete message string
     */
    @Override
    public String toString() {
        String timeStr = getFormattedTime();

        // Choose different display format based on message type
        switch (type) {
            case USER_JOIN:
                return String.format("[%s] System: User %s (%s) joined the chat room", timeStr, username, userId);

            case USER_LEAVE:
                return String.format("[%s] System: User %s (%s) left the chat room", timeStr, username, userId);

            case TEXT_MESSAGE:
                return String.format("[%s] %s (%s): %s", timeStr, username, userId, content);

            default:
                return content;
        }
    }
}