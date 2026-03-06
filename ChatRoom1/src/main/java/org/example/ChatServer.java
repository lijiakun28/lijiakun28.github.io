package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

/**
 * Main class of chat room server
 * Responsible for managing all client connections, message routing, and historical record storage
 *
 * Core functions:
 * 1. Listen on specified port, accept client connections
 * 2. Assign unique user ID to each client
 * 3. Manage online user list
 * 4. Forward messages to all clients
 * 5. Store chat history to local file
 * 6. Push historical records and online user list to new users
 */
public class ChatServer {
    // ============ Server Configuration Constants ============
    /** Server listening port number */
    private static final int PORT = 8888;

    /** Chat history record storage filename */
    private static final String HISTORY_FILE = "chat_history.txt";

    /** Starting value for user ID, ensuring allocation of 5-digit ID */
    private static int userIdCounter = 10000;

    // ============ Server Core Components ============
    /** Server Socket for listening to client connection requests */
    private ServerSocket serverSocket;

    /** Online client mapping table, thread-safe ConcurrentHashMap */
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /** Chat history record list, using synchronized wrapper to ensure thread safety */
    private final List<ChatMessage> chatHistory = Collections.synchronizedList(new ArrayList<>());

    /** Thread pool for managing client connection threads */
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    // ============ Main Program Entry ============
    /**
     * Main entry method of the program
     * Create ChatServer instance and start the server
     * @param args Command line parameters (unused)
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }

    // ============ Server Control Methods ============
    /**
     * Start the chat room server
     * 1. Create server Socket and bind port
     * 2. Load historical chat records
     * 3. Enter main loop to accept client connections
     * 4. Assign independent thread to handle each client connection
     *
     * Exception handling:
     * - IOException: Network or file IO exception
     * - All exceptions are caught to ensure server can shut down gracefully
     */
    public void start() {
        try {
            // Create server Socket and bind to specified port
            serverSocket = new ServerSocket(PORT);
            System.out.println("Chat server started, port: " + PORT);
            System.out.println("Historical message file: " + new File(HISTORY_FILE).getAbsolutePath());

            // Load historical messages
            loadChatHistory();

            // Main loop: continuously accept client connections
            while (true) {
                // Block until client connects
                Socket clientSocket = serverSocket.accept();
                // Create new thread for each client
                threadPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        } finally {
            // Ensure resources are released when server shuts down
            shutdown();
        }
    }

    /**
     * Load historical chat records from local file
     * File is stored in Java serialization format
     *
     * Execution flow:
     * 1. Check if historical file exists
     * 2. If exists, read and deserialize into List<ChatMessage>
     * 3. Add historical records to memory list
     *
     * @Note: Do nothing if file doesn't exist
     */
    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                // Suppress generic conversion warning
                @SuppressWarnings("unchecked")
                List<ChatMessage> history = (List<ChatMessage>) ois.readObject();
                chatHistory.addAll(history);
                System.out.println("Loaded historical messages: " + history.size() + " records");
            } catch (IOException e) {
                System.out.println("Failed to load historical messages (IO): " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to load historical messages (Class not found): " + e.getMessage());
            }
        }
    }

    /**
     * Save chat history records to local file
     * Use synchronized keyword to ensure thread safety
     *
     * Execution flow:
     * 1. Serialize chat records in memory to file
     * 2. Use try-with-resources to automatically close resources
     * 3. Exceptions are caught and recorded, without affecting main program operation
     */
    private synchronized void saveChatHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            // Create new ArrayList to avoid thread issues during serialization
            oos.writeObject(new ArrayList<>(chatHistory));
            System.out.println("Chat records saved to: " + new File(HISTORY_FILE).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("IO exception when saving chat records: " + e.getMessage());
        }
    }

    /**
     * Generate unique 5-digit user ID
     * Use synchronized to ensure thread safety and avoid ID duplication
     * @return New user ID string
     */
    private synchronized String generateUserId() {
        return String.valueOf(userIdCounter++);
    }

    /**
     * Broadcast message to all online clients
     * This is the core message routing method of the server
     *
     * Execution flow:
     * 1. If message type is TEXT_MESSAGE, USER_JOIN, or USER_LEAVE, save to history
     * 2. Traverse all online clients and send message
     * 3. Handle IO exceptions during sending
     *
     * @param message Chat message object to broadcast
     */
    public void broadcastMessage(ChatMessage message) {
        // Check if need to save to history
        if (message.getType() == ChatMessage.MessageType.TEXT_MESSAGE ||
                message.getType() == ChatMessage.MessageType.USER_JOIN ||
                message.getType() == ChatMessage.MessageType.USER_LEAVE) {

            // Add to history records
            chatHistory.add(message);
            // Asynchronously save to file
            saveChatHistory();
        }

        // Traverse all online clients and broadcast message
        for (ClientHandler client : clients.values()) {
            if (client.isConnected()) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message to client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Remove specified client from online client list
     * And broadcast user leave message to all users
     *
     * @param userId User ID to remove
     * @param username Username to remove
     */
    public void removeClient(String userId, String username) {
        // Remove from client mapping
        ClientHandler client = clients.remove(userId);

        if (client != null) {
            // Create and broadcast user leave message
            ChatMessage leaveMessage = new ChatMessage(userId, username, "",
                    ChatMessage.MessageType.USER_LEAVE);
            broadcastMessage(leaveMessage);

            System.out.println("User " + username + " (" + userId + ") disconnected");
        }
    }

    /**
     * Get chat history records
     * @return Copy of chat history records (avoid directly returning internal reference)
     */
    public List<ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    /**
     * Get current online user list
     * @return User ID to username mapping table
     */
    public Map<String, String> getOnlineUsers() {
        Map<String, String> onlineUsers = new HashMap<>();
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            onlineUsers.put(entry.getKey(), entry.getValue().getUsername());
        }
        return onlineUsers;
    }

    /**
     * Gracefully shut down server and release all resources
     * Execution flow:
     * 1. Close server Socket
     * 2. Close thread pool
     * 3. Wait for tasks in thread pool to complete
     * 4. Force shutdown of unfinished tasks
     */
    private void shutdown() {
        try {
            // Close server Socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Gracefully close thread pool
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }

        } catch (Exception e) {
            threadPool.shutdownNow();
        }
    }

    // ============ Inner Class: Client Handler ============
    /**
     * Client handler inner class
     * Each client connection corresponds to a ClientHandler instance
     * Runs in independent thread in thread pool, handling communication with a single client
     */
    class ClientHandler implements Runnable {
        /** Client Socket connection */
        private Socket socket;

        /** Output stream, sending messages to client */
        private ObjectOutputStream output;

        /** Input stream, receiving messages from client */
        private ObjectInputStream input;

        /** User ID assigned to this client */
        private String userId;

        /** Username provided by client */
        private String username;

        /** Connection status flag */
        private boolean connected = true;

        /**
         * Constructor, initialize client handler
         * @param socket Client Socket connection
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Main loop for client processing
         * Execution flow:
         * 1. Initialize input/output streams
         * 2. Assign user ID and get username
         * 3. Add client to online list
         * 4. Send historical messages and user list
         * 5. Broadcast user join message
         * 6. Enter message receiving loop
         * 7. Exception handling and resource cleanup
         */
        @Override
        public void run() {
            try {
                // Initialize IO streams (create output stream first then input stream to prevent blocking)
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                // Generate and assign user ID
                userId = generateUserId();

                // Read username sent by client
                username = (String) input.readObject();

                // Add client to online list
                clients.put(userId, this);

                // Print connection information
                System.out.println("New user connection: " + username + " (" + userId + ") from " +
                        socket.getInetAddress().getHostAddress());

                // Send historical messages to new client
                sendChatHistory();

                // Broadcast user join message
                ChatMessage joinMessage = new ChatMessage(userId, username, "",
                        ChatMessage.MessageType.USER_JOIN);
                broadcastMessage(joinMessage);

                // Send online user list
                sendOnlineUsers();

                // Message receiving loop
                while (connected) {
                    try {
                        ChatMessage message = (ChatMessage) input.readObject();
                        if (message != null) {
                            // Broadcast received message
                            broadcastMessage(message);
                        }
                    } catch (EOFException e) {
                        // Client normally disconnected
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client processing exception: " + e.getMessage());
            } finally {
                // Ensure resources are cleaned up
                disconnect();
            }
        }

        /**
         * Send chat history records to client
         * Get all historical records from server and send them to client one by one
         */
        private void sendChatHistory() {
            try {
                List<ChatMessage> history = getChatHistory();
                for (ChatMessage message : history) {
                    sendMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Failed to send historical messages: " + e.getMessage());
            }
        }

        /**
         * Send online user list to client
         * Format into easily readable string form
         */
        private void sendOnlineUsers() {
            try {
                Map<String, String> onlineUsers = getOnlineUsers();
                StringBuilder userList = new StringBuilder("Current online users (" + onlineUsers.size() + "):\n");

                // Build user list string
                for (Map.Entry<String, String> entry : onlineUsers.entrySet()) {
                    userList.append("  ").append(entry.getValue()).append(" (").append(entry.getKey()).append(")\n");
                }

                // Create and send user list message
                ChatMessage userListMessage = new ChatMessage("SYSTEM", "System",
                        userList.toString(), ChatMessage.MessageType.USER_LIST);
                sendMessage(userListMessage);

            } catch (IOException e) {
                System.out.println("Failed to send online user list: " + e.getMessage());
            }
        }

        /**
         * Send single message to client
         * @param message Chat message object to send
         * @throws IOException If sending fails
         */
        public void sendMessage(ChatMessage message) throws IOException {
            if (connected && output != null) {
                output.writeObject(message);
                output.flush(); // Ensure data is sent immediately
            }
        }

        /**
         * Check if client is still connected
         * @return true means connection is normal, false means disconnected
         */
        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed() && socket.isConnected();
        }

        /**
         * Get client username
         * @return Username string
         */
        public String getUsername() {
            return username;
        }

        /**
         * Disconnect client connection and clean up resources
         * 1. Set connection status to false
         * 2. Remove from server online list
         * 3. Close all IO streams and Socket
         */
        private void disconnect() {
            // Set connection status
            connected = false;

            // Remove client from server
            removeClient(userId, username);

            // Close all resources
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Exception when closing connection: " + e.getMessage());
            }
        }
    }
}