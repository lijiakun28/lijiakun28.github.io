package com.example.gomokugame1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Optional;

/**
 * Main Gomoku Game Class
 * Features:
 * 1. Creates 20x20 board, black moves first
 * 2. Implements JavaFX graphical interface
 * 3. Supports mouse click placement
 * 4. 30-second time limit per turn
 * 5. Automatically checks win/draw conditions
 */
public class GomokuGame extends Application {

    /**
     * Game Logic Inner Class
     * Handles all game rules and state management
     */
    private static class GameLogic {
        public static final int BOARD_SIZE = 20;  // Board size: 20 rows × 20 columns
        public static final int WIN_COUNT = 5;    // Win condition: 5 consecutive stones

        private final char[][] board;             // Board array: ' '=empty, 'B'=black, 'W'=white
        private char currentPlayer;               // Current player: 'B'=black, 'W'=white
        private boolean gameOver;                 // Whether game is over
        private char winner;                      // Winner: 'B', 'W', or 'D' (draw)
        private int movesCount;                   // Total number of moves
        private int blackMovesCount;              // Black player moves count
        private int whiteMovesCount;              // White player moves count
        private final int maxMoves = BOARD_SIZE * BOARD_SIZE;  // Maximum moves (board cells)

        public GameLogic() {
            board = new char[BOARD_SIZE][BOARD_SIZE];
            resetGame();
        }

        public void resetGame() {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    board[i][j] = ' ';
                }
            }
            currentPlayer = 'B';  // Black moves first
            gameOver = false;     // Game not over
            winner = ' ';         // No winner
            movesCount = 0;       // Reset total move count
            blackMovesCount = 0;  // Reset black moves count
            whiteMovesCount = 0;  // Reset white moves count
        }

        public boolean placeStone(int row, int col) {
            if (gameOver || row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != ' ') {
                return false;  // Invalid move
            }

            board[row][col] = currentPlayer;  // Place current player's stone
            movesCount++;                      // Increment total move count

            // Increment individual player move count
            if (currentPlayer == 'B') {
                blackMovesCount++;
            } else {
                whiteMovesCount++;
            }

            // Check for win
            if (checkWin(row, col)) {
                gameOver = true;        // Game over
                winner = currentPlayer; // Current player wins
            }
            // Check for draw (board full)
            else if (movesCount >= maxMoves) {
                gameOver = true;  // Game over
                winner = 'D';     // 'D' indicates draw
            }
            // Game continues, switch player
            else {
                switchPlayer();  // Switch to other player
            }

            return true;  // Move successful
        }

        /**
         * Force switch player (used for timeout)
         */
        public void forceSwitchPlayer() {
            switchPlayer();
        }

        private boolean checkWin(int row, int col) {
            char stone = board[row][col];
            int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

            for (int[] direction : directions) {
                int count = 1;
                count += countDirection(stone, row, col, direction[0], direction[1]);
                count += countDirection(stone, row, col, -direction[0], -direction[1]);

                if (count >= WIN_COUNT) {
                    return true;
                }
            }
            return false;
        }

        private int countDirection(char stone, int startRow, int startCol, int dr, int dc) {
            int r = startRow + dr;
            int c = startCol + dc;
            int count = 0;
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == stone) {
                count++;
                r += dr;
                c += dc;
            }
            return count;
        }

        private void switchPlayer() {
            currentPlayer = (currentPlayer == 'B') ? 'W' : 'B';
        }

        // ========== Public Access Methods ==========

        public char getCurrentPlayer() {
            return currentPlayer;
        }

        public boolean isGameOver() {
            return gameOver;
        }

        public char getWinner() {
            return winner;
        }

        public int getMovesCount() {
            return movesCount;
        }

        public int getBlackMovesCount() {
            return blackMovesCount;
        }

        public int getWhiteMovesCount() {
            return whiteMovesCount;
        }

        public char getStone(int row, int col) {
            return board[row][col];
        }
    }

    // ========== GUI-related Member Variables ==========

    private GameLogic game = new GameLogic();

    // UI controls
    private Label statusLabel;
    private Label blackTimeLabel;
    private Label whiteTimeLabel;
    private Label movesLabel;
    private Label blackMovesLabel;
    private Label whiteMovesLabel;
    private Pane boardPane;

    // Timer-related variables
    private Timeline blackTimer;
    private Timeline whiteTimer;
    private int blackTimeLeft;
    private int whiteTimeLeft;
    private final int TIME_LIMIT = 30;

    // Board drawing variables
    private double boardStartX;   // Starting X coordinate of the board
    private double boardStartY;   // Starting Y coordinate of the board
    private double cellWidth;     // Width of each cell
    private double cellHeight;    // Height of each cell

    @Override
    public void start(Stage primaryStage) {
        // 1. Create UI Components
        statusLabel = new Label("Current Player: BLACK");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Create timer and moves display area
        HBox timerBox = new HBox(20);
        timerBox.setAlignment(Pos.CENTER);

        blackTimeLabel = new Label("Black: 30s");
        blackTimeLabel.setTextFill(Color.BLACK);
        blackTimeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        whiteTimeLabel = new Label("White: 30s");
        whiteTimeLabel.setTextFill(Color.BLACK);
        whiteTimeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        movesLabel = new Label("Total Moves: 0");
        movesLabel.setFont(Font.font("Arial", 14));

        // Create individual moves labels
        blackMovesLabel = new Label("Black Moves: 0");
        blackMovesLabel.setFont(Font.font("Arial", 12));
        blackMovesLabel.setTextFill(Color.DARKBLUE);

        whiteMovesLabel = new Label("White Moves: 0");
        whiteMovesLabel.setFont(Font.font("Arial", 12));
        whiteMovesLabel.setTextFill(Color.DARKRED);

        VBox movesBox = new VBox(5, blackMovesLabel, whiteMovesLabel);
        movesBox.setAlignment(Pos.CENTER_LEFT);

        timerBox.getChildren().addAll(blackTimeLabel, whiteTimeLabel, movesLabel, movesBox);

        // Create board panel
        boardPane = new Pane();
        boardPane.setPrefSize(600, 600);
        boardPane.setStyle("-fx-background-color: #DEB887;");  // Wood color

        // Listen for panel size changes
        boardPane.widthProperty().addListener((obs, oldVal, newVal) -> drawBoard());
        boardPane.heightProperty().addListener((obs, oldVal, newVal) -> drawBoard());

        // Create menu bar
        MenuBar menuBar = new MenuBar();
        Menu gameMenu = new Menu("Game");
        MenuItem newGameItem = new MenuItem("New Game");
        MenuItem exitItem = new MenuItem("Exit");
        gameMenu.getItems().addAll(newGameItem, new SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(gameMenu);

        // Create button panel
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        Button newGameBtn = new Button("New Game");
        Button exitBtn = new Button("Exit");
        buttonBox.getChildren().addAll(newGameBtn, exitBtn);

        // Set main layout
        BorderPane root = new BorderPane();
        VBox topBox = new VBox(10, menuBar, statusLabel, timerBox);
        topBox.setPadding(new Insets(10));
        root.setTop(topBox);
        root.setCenter(boardPane);
        root.setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(boardPane, new Insets(20));
        BorderPane.setMargin(buttonBox, new Insets(10));

        // ========== Event Handlers ==========

        // Board mouse click event
        boardPane.setOnMouseClicked(e -> {
            if (game.isGameOver()) {
                showAlert("Game Over", "The game is over. Please start a new game.");
                return;
            }

            // Calculate board coordinates from click position
            // Check if click is within board boundaries
            if (e.getX() < boardStartX || e.getX() > boardStartX + cellWidth * (GameLogic.BOARD_SIZE - 1) ||
                    e.getY() < boardStartY || e.getY() > boardStartY + cellHeight * (GameLogic.BOARD_SIZE - 1)) {
                return;  // Click outside board
            }

            // Calculate which intersection was clicked
            // Find the nearest intersection
            int col = (int) Math.round((e.getX() - boardStartX) / cellWidth);
            int row = (int) Math.round((e.getY() - boardStartY) / cellHeight);

            // Ensure within bounds
            col = Math.max(0, Math.min(col, GameLogic.BOARD_SIZE - 1));
            row = Math.max(0, Math.min(row, GameLogic.BOARD_SIZE - 1));

            // Attempt to place stone
            boolean success = game.placeStone(row, col);

            if (success) {
                drawStones();   // Redraw stones
                updateStatus(); // Update status display
                switchTimer();  // Switch timer
            } else {
                // Position already occupied
                showAlert("Invalid Move",
                        "This intersection is already occupied. Please choose an empty one.");
            }
        });

        // Menu and button events
        newGameItem.setOnAction(e -> resetGame());
        newGameBtn.setOnAction(e -> resetGame());
        exitItem.setOnAction(e -> primaryStage.close());
        exitBtn.setOnAction(e -> primaryStage.close());

        // Initialize timers
        setupTimers();

        // Create scene and stage
        Scene scene = new Scene(root, 900, 850);  // Wider window for additional info
        primaryStage.setTitle("Gomoku (Five in a Row) - JavaFX Implementation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Draw board after window is shown
        Platform.runLater(() -> drawBoard());
    }

    /**
     * Draw board grid
     * Draws lines at intersections (grid points) rather than between cells
     */
    private void drawBoard() {
        // Clear all graphics from board
        boardPane.getChildren().clear();

        // Get current dimensions of board panel
        double width = boardPane.getWidth();
        double height = boardPane.getHeight();

        // If dimensions invalid, don't draw
        if (width <= 0 || height <= 0) {
            return;
        }

        // Calculate padding to center the board
        double padding = 40;  // Padding around the board
        double boardWidth = width - 2 * padding;
        double boardHeight = height - 2 * padding;

        // Calculate cell dimensions
        // For 20 intersections, we have 19 intervals
        cellWidth = boardWidth / (GameLogic.BOARD_SIZE - 1);
        cellHeight = boardHeight / (GameLogic.BOARD_SIZE - 1);

        // Calculate starting position to center the board
        boardStartX = padding;
        boardStartY = padding;

        // Draw grid lines at intersections
        for (int i = 0; i < GameLogic.BOARD_SIZE; i++) {
            // Draw vertical lines
            Line vLine = new Line(
                    boardStartX + i * cellWidth, boardStartY,  // Start at top
                    boardStartX + i * cellWidth, boardStartY + boardHeight  // End at bottom
            );
            vLine.setStroke(Color.BLACK);
            boardPane.getChildren().add(vLine);

            // Draw horizontal lines
            Line hLine = new Line(
                    boardStartX, boardStartY + i * cellHeight,  // Start at left
                    boardStartX + boardWidth, boardStartY + i * cellHeight  // End at right
            );
            hLine.setStroke(Color.BLACK);
            boardPane.getChildren().add(hLine);
        }

        // Draw stones
        drawStones();
    }

    /**
     * Draw all stones at intersections
     */
    private void drawStones() {
        // Remove all existing stones (circles)
        boardPane.getChildren().removeIf(node -> node instanceof Circle);

        // Stone radius
        double stoneRadius = Math.min(cellWidth, cellHeight) * 0.4;

        // Iterate through each board position
        for (int row = 0; row < GameLogic.BOARD_SIZE; row++) {
            for (int col = 0; col < GameLogic.BOARD_SIZE; col++) {
                char stone = game.getStone(row, col);

                // If position has stone
                if (stone != ' ') {
                    // Create circle representing stone
                    Circle circle = new Circle();

                    // Calculate circle center at intersection
                    // This places the stone exactly at the line intersection
                    double centerX = boardStartX + col * cellWidth;
                    double centerY = boardStartY + row * cellHeight;

                    circle.setCenterX(centerX);
                    circle.setCenterY(centerY);
                    circle.setRadius(stoneRadius);

                    // Set stone color and border
                    if (stone == 'B') {
                        circle.setFill(Color.BLACK);
                        circle.setStroke(Color.DARKGRAY);
                    } else {
                        circle.setFill(Color.WHITE);
                        circle.setStroke(Color.DARKGRAY);
                    }

                    // Add to board panel
                    boardPane.getChildren().add(circle);
                }
            }
        }
    }

    /**
     * Initialize timers
     */
    private void setupTimers() {
        // Black player timer: triggers every second
        blackTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            blackTimeLeft--;  // Decrease time by 1 second
            blackTimeLabel.setText("Black: " + blackTimeLeft + "s");

            // Time runs out
            if (blackTimeLeft <= 0) {
                blackTimer.stop();  // Stop timer

                if (!game.isGameOver()) {
                    // Force switch to white player
                    game.forceSwitchPlayer();
                    switchTimer();  // Switch timer to white
                    updateStatus(); // Update status

                    // Show timeout message
                    showAlert("Time's Up!",
                            "Black player's time ran out. White player's turn now.");
                }
            }
        }));
        blackTimer.setCycleCount(Animation.INDEFINITE);

        // White player timer: triggers every second
        whiteTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            whiteTimeLeft--;
            whiteTimeLabel.setText("White: " + whiteTimeLeft + "s");

            if (whiteTimeLeft <= 0) {
                whiteTimer.stop();

                if (!game.isGameOver()) {
                    // Force switch to black player
                    game.forceSwitchPlayer();
                    switchTimer();  // Switch timer to black
                    updateStatus(); // Update status

                    // Show timeout message
                    showAlert("Time's Up!",
                            "White player's time ran out. Black player's turn now.");
                }
            }
        }));
        whiteTimer.setCycleCount(Animation.INDEFINITE);

        // Reset timers to initial state
        resetTimers();
    }

    /**
     * Reset timers
     */
    private void resetTimers() {
        blackTimeLeft = TIME_LIMIT;
        whiteTimeLeft = TIME_LIMIT;

        blackTimeLabel.setText("Black: " + blackTimeLeft + "s");
        whiteTimeLabel.setText("White: " + whiteTimeLeft + "s");

        // Black moves first, start black timer, stop white timer
        blackTimer.play();
        whiteTimer.stop();
    }

    /**
     * Switch timer
     */
    private void switchTimer() {
        if (game.isGameOver()) {
            blackTimer.stop();
            whiteTimer.stop();
            return;
        }

        // Switch timer based on current player
        if (game.getCurrentPlayer() == 'B') {
            // Reset and start black timer
            blackTimeLeft = TIME_LIMIT;
            blackTimeLabel.setText("Black: " + blackTimeLeft + "s");
            blackTimer.play();
            whiteTimer.stop();
        } else {
            // Reset and start white timer
            whiteTimeLeft = TIME_LIMIT;
            whiteTimeLabel.setText("White: " + whiteTimeLeft + "s");
            whiteTimer.play();
            blackTimer.stop();
        }
    }

    /**
     * Reset game
     */
    private void resetGame() {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("New Game");
        alert.setHeaderText("Start a new game?");
        alert.setContentText("Current game progress will be lost.");

        Optional<ButtonType> result = alert.showAndWait();

        // If user confirms, reset game
        if (result.isPresent() && result.get() == ButtonType.OK) {
            game.resetGame();   // Reset game logic
            resetTimers();      // Reset timers
            drawBoard();        // Redraw board
            updateStatus();     // Update status display
        }
    }

    /**
     * Update status display
     */
    private void updateStatus() {
        if (game.isGameOver()) {
            if (game.getWinner() == 'D') {
                statusLabel.setText("Game Over! It's a Draw!");
            } else {
                String winner = (game.getWinner() == 'B') ? "BLACK" : "WHITE";
                statusLabel.setText("Game Over! Winner: " + winner + "!");
            }
        } else {
            String player = (game.getCurrentPlayer() == 'B') ? "BLACK" : "WHITE";
            statusLabel.setText("Current Player: " + player);
        }

        // Update move count displays
        movesLabel.setText("Total Moves: " + game.getMovesCount());
        blackMovesLabel.setText("Black Moves: " + game.getBlackMovesCount());
        whiteMovesLabel.setText("White Moves: " + game.getWhiteMovesCount());
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}