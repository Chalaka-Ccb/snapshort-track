import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Core Snapshot Engine - Part 1 of File System Snapshot and Diff Tool
 * Responsible for capturing directory snapshots using TreeMap (BST-based structure)
 */
public class SnapshotEngine {

    /**
     * Represents metadata for a file
     */
    public static class FileMetadata {
        private final String filePath;
        private final long fileSize;
        private final long lastModified;

        public FileMetadata(String filePath, long fileSize, long lastModified) {
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
        }

        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public long getLastModified() { return lastModified; }

        @Override
        public String toString() {
            return String.format("File[path=%s, size=%d, modified=%d]",
                    filePath, fileSize, lastModified);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FileMetadata that = (FileMetadata) obj;
            return fileSize == that.fileSize &&
                    lastModified == that.lastModified &&
                    Objects.equals(filePath, that.filePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filePath, fileSize, lastModified);
        }
    }

    // TreeMap (BST-based structure) to store file snapshots
    private TreeMap<String, FileMetadata> currentSnapshot;

    public SnapshotEngine() {
        this.currentSnapshot = new TreeMap<>();
    }

    /**
     * Captures a snapshot of the specified directory
     * @param directoryPath The path of the directory to snapshot
     * @return A TreeMap representing the snapshot
     * @throws IOException If directory cannot be read
     */
    public TreeMap<String, FileMetadata> captureSnapshot(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        // Clear previous snapshot
        currentSnapshot = new TreeMap<>();

        // Walk the file tree and capture metadata
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String absolutePath = file.toAbsolutePath().toString();
                long size = Files.size(file);
                long lastModified = Files.getLastModifiedTime(file).toMillis();

                // Store file metadata in the TreeMap (BST-based structure)
                currentSnapshot.put(absolutePath, new FileMetadata(absolutePath, size, lastModified));

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // Skip files that can't be accessed but continue traversal
                System.err.println("Warning: Cannot access file: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        return currentSnapshot;
    }

    /**
     * Gets the current snapshot
     * @return The current TreeMap snapshot
     */
    public TreeMap<String, FileMetadata> getCurrentSnapshot() {
        return new TreeMap<>(currentSnapshot); // Return a copy for safety
    }

    /**
     * Prints the current snapshot in sorted order (by file path)
     */
    public void printSnapshot() {
        if (currentSnapshot.isEmpty()) {
            System.out.println("No snapshot available. Capture a snapshot first.");
            return;
        }

        System.out.println("=== SNAPSHOT CONTENTS ===");
        System.out.printf("%-60s %-12s %-20s%n", "FILE PATH", "SIZE (bytes)", "LAST MODIFIED");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Map.Entry<String, FileMetadata> entry : currentSnapshot.entrySet()) {
            FileMetadata metadata = entry.getValue();
            System.out.printf("%-60s %-12d %-20d%n",
                    truncatePath(metadata.getFilePath(), 55),
                    metadata.getFileSize(),
                    metadata.getLastModified());
        }

        System.out.println("Total files: " + currentSnapshot.size());
    }

    /**
     * Helper method to truncate long paths for display
     */
    private String truncatePath(String path, int maxLength) {
        if (path.length() <= maxLength) return path;

        int start = path.length() - maxLength + 3; // +3 for the ellipsis
        return "..." + path.substring(start);
    }

    /**
     * Test method to demonstrate the functionality
     */
    public static void main(String[] args) {
        SnapshotEngine engine = new SnapshotEngine();

        try {
            // Capture snapshot of current directory
            String testDir = "."; // Current directory
            engine.captureSnapshot(testDir);

            // Display the snapshot
            engine.printSnapshot();

        } catch (IOException e) {
            System.err.println("Error capturing snapshot: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}