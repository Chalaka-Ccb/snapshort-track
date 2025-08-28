import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Command-Line Interface - Part 3 of File System Snapshot and Diff Tool
 * Provides user interaction and integrates all components
 */
public class FileSnapshotCLI {
    private SnapshotEngine snapshotEngine;
    private HistoryManager historyManager;
    private Scanner scanner;

    public FileSnapshotCLI() {
        this.snapshotEngine = new SnapshotEngine();
        this.historyManager = new HistoryManager();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Starts the interactive command-line interface
     */
    public void start() {
        System.out.println("=== File System Snapshot and Diff Tool ===");
        System.out.println("Type 'help' for available commands");

        boolean running = true;
        while (running) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : "";

            try {
                switch (command) {
                    case "snapshot":
                        handleSnapshot(argument);
                        break;
                    case "diff":
                        handleDiff(argument);
                        break;
                    case "list":
                        handleList(argument);
                        break;
                    case "show":
                        handleShow(argument);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    case "":
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * Handles the snapshot command
     */
    private void handleSnapshot(String path) throws IOException {
        if (path.isEmpty()) {
            System.out.print("Enter directory path: ");
            path = scanner.nextLine().trim();
        }

        if (path.isEmpty()) {
            System.out.println("No directory path provided");
            return;
        }

        System.out.println("Capturing snapshot of: " + path);
        Map<String, SnapshotEngine.FileMetadata> snapshot = snapshotEngine.captureSnapshot(path);
        historyManager.addSnapshot(snapshot);
        System.out.println("Snapshot captured successfully! (" + snapshot.size() + " files)");
    }

    /**
     * Handles the diff command
     */
    private void handleDiff(String argument) {
        if (historyManager.getHistorySize() < 2) {
            System.out.println("Need at least 2 snapshots to compare");
            return;
        }

        HistoryManager.DiffResult result;
        if (argument.isEmpty()) {
            // Compare two most recent snapshots
            result = historyManager.diffLatest();
        } else {
            // Parse indices for comparison
            String[] indices = argument.split(" ");
            if (indices.length != 2) {
                System.out.println("Usage: diff [older_index] [newer_index]");
                return;
            }

            try {
                int olderIndex = Integer.parseInt(indices[0]);
                int newerIndex = Integer.parseInt(indices[1]);
                result = historyManager.diff(olderIndex, newerIndex);
            } catch (NumberFormatException e) {
                System.out.println("Invalid indices. Please use numbers.");
                return;
            }
        }

        historyManager.printDiffResult(result);
    }

    /**
     * Handles the list command
     */
    private void handleList(String argument) {
        if (argument.equals("history")) {
            historyManager.printHistory();
        } else {
            System.out.println("Usage: list history");
        }
    }

    /**
     * Handles the show command
     */
    private void handleShow(String argument) {
        if (argument.equals("current")) {
            snapshotEngine.printSnapshot();
        } else {
            System.out.println("Usage: show current");
        }
    }

    /**
     * Prints help information
     */
    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  snapshot [path]    - Capture snapshot of directory (prompts if path not provided)");
        System.out.println("  diff               - Compare two most recent snapshots");
        System.out.println("  diff [old] [new]   - Compare specific snapshots by index");
        System.out.println("  list history       - Show snapshot history");
        System.out.println("  show current       - Show current snapshot contents");
        System.out.println("  help               - Show this help message");
        System.out.println("  exit               - Exit the program");
    }

    /**
     * Main method to start the application
     */
    public static void main(String[] args) {
        FileSnapshotCLI cli = new FileSnapshotCLI();
        cli.start();
    }
}