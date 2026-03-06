package org.example;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Chat room client class
 * Responsible for connecting to server, sending and receiving messages
 *
 * Core functions:
 * 1. Connect to chat room server
 * 2. Register username
 * 3. Receive and display server messages
 * 4. Send user input messages to server
 * 5. Disconnect and clean up resources
 */
public class ChatClient2 {
    // ============ Client Configuration Constants ============
    /** Server host address, use localhost for local testing */
    private static final String SERVER_HOST = "localhost";

    /** Server port number, must match server configuration */
    private static final int SERVER_PORT = 8888;

    // ============ Client State and Resources ============
    /** Socket connection with server */
    private Socket socket;

    /** Output stream for sending messages to server */
    private ObjectOutputStream output;

    /** Input stream for receiving messages from server */
    private ObjectInputStream input;

    /** User ID assigned by server, assigned after connection */
    private String userId;

    /** User-defined username, entered at startup */
    private String username;

    /** Connection status flag, true means connection is normal */
    private boolean connected = false;

    /** User input scanner for reading console input */
    private Scanner scanner;

    // ============ Main Program Entry ============
    /**
     * Client main entry method
     * Create ChatClient instance and start client
     * @param args Command line parameters (unused)
     */
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }

    // ============ Client Control Methods ============
    /**
     * Start chat room client
     * Execution flow:
     * 1. Connect to server
     * 2. Initialize input/output streams
     * 3. Register username
     * 4. Start message receiving thread
     * 5. Enter message sending loop
     * 6. Exception handling and resource cleanup
     */
    public void start() {
        // Initialize user input scanner
        scanner = new Scanner(System.in);

        try {
            // Connect to server
            System.out.println("Connecting to chat server...");
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // User registration process
            registerUser();

            // Start message receiving thread (running in background)
            Thread receiverThread = new Thread(new MessageReceiver());
            receiverThread.setDaemon(true); // Set as daemon thread, automatically ends when main thread exits
            receiverThread.start();

            // Display connection success information
            System.out.println("Connection successful! Enter messages to start chatting, type '/exit' to quit");
            System.out.println("==============================================");

            // Enter message sending loop
            handleUserInput();

        } catch (IOException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
        } finally {
            // Ensure disconnection and resource cleanup
            disconnect();
        }
    }

    /**
     * User registration process
     * Read username from console and send to server
     *
     * Process:
     * 1. Prompt user to enter username
     * 2. Verify username is not empty
     * 3. Send username to server
     *
     * @throws IOException If sending username fails
     */
    private void registerUser() throws IOException {
        System.out.print("Please enter your username: ");
        this.username = scanner.nextLine().trim();

        // Verify username cannot be empty
        while (username.isEmpty()) {
            System.out.print("Username cannot be empty, please re-enter: ");
            this.username = scanner.nextLine().trim();
        }

        // Send username to server
        output.writeObject(username);
        output.flush(); // Send immediately
    }

    /**
     * Process user input messages
     * Continuously read console input and send to server
     *
     * Special commands:
     * - /exit: Exit chat room
     * - /users: Display online users (reserved function)
     *
     * Execution flow:
     * 1. Read user input
     * 2. Check special commands
     * 3. Create message object
     * 4. Send to server
     */
    private void handleUserInput() {
        // Continuously read user input until disconnected
        while (connected && scanner.hasNextLine()) {
            String messageText = scanner.nextLine().trim();

            // Check exit command
            if (messageText.equalsIgnoreCase("/exit")) {
                break;
            }
            // Reserved command: display online users
            else if (messageText.equalsIgnoreCase("/users")) {
                System.out.println("Online users function not implemented yet");
                continue;
            }

            // Send non-empty messages
            if (!messageText.isEmpty()) {
                try {
                    // Create text message object
                    ChatMessage message = new ChatMessage(userId, username, messageText,
                            ChatMessage.MessageType.TEXT_MESSAGE);

                    // Send to server
                    output.writeObject(message);
                    output.flush();

                } catch (IOException e) {
                    System.out.println("Failed to send message: " + e.getMessage());
                    break; // Exit loop when sending fails
                }
            }
        }
    }

    // ============ Inner Class: Message Receiver ============
    /**
     * Message receiver inner class
     * Runs in independent thread, continuously receiving server messages
     *
     * Responsibilities:
     * 1. Receive message objects from server
     * 2. Display messages to console
     * 3. Handle connection exceptions
     */
    class MessageReceiver implements Runnable {
        /**
         * Main loop for message receiving
         * Continuously read messages from input stream and display
         *
         * Special processing:
         * 1. First non-system message used to set user ID
         * 2. EOFException indicates server normally closed connection
         * 3. Other exceptions indicate network errors
         */
        @Override
        public void run() {
            try {
                while (connected) {
                    // Block waiting for server message
                    ChatMessage message = (ChatMessage) input.readObject();

                    if (message != null) {
                        // If this is the first non-system message, get server-assigned user ID
                        if (userId == null && !message.getUserId().equals("SYSTEM")) {
                            userId = message.getUserId();
                        }

                        // Display message to console
                        System.out.println(message.toString());
                    }
                }

            } catch (EOFException e) {
                // Server closed connection
                System.out.println("Connection to server disconnected");
            } catch (IOException | ClassNotFoundException e) {
                // Network or deserialization error
                if (connected) {
                    System.out.println("Error receiving message: " + e.getMessage());
                }
            } finally {
                // Set connection status to disconnected
                connected = false;
            }
        }
    }

    // ============ Resource Cleanup Methods ============
    /**
     * Disconnect from server and clean up all resources
     * Execution order:
     * 1. Set connection status to false
     * 2. Close output stream (sender side)
     * 3. Close input stream (receiver side)
     * 4. Close Socket connection
     * 5. Close scanner
     */
    private void disconnect() {
        // Update connection status
        connected = false;

        try {
            // Close all resources (note closing order)
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();

        } catch (IOException e) {
            System.out.println("Exception when closing connection: " + e.getMessage());
        }

        // Display exit information
        System.out.println("Exited chat room");
    }
}