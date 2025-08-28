import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Comprehensive Testing Suite - Part 4 of File System Snapshot and Diff Tool
 * Tests all components with various scenarios
 */
public class ComprehensiveTester {
    private SnapshotEngine snapshotEngine;
    private HistoryManager historyManager;
    private Path testDir;

    public ComprehensiveTester() {
        this.snapshotEngine = new SnapshotEngine();
        this.historyManager = new HistoryManager();
    }

    /**
     * Sets up a test directory with sample files
     */
    private void setupTestEnvironment() throws IOException {
        // Create a temporary test directory
        testDir = Files.createTempDirectory("snapshot_test");
        System.out.println("Test directory: " + testDir);

        // Create initial test files
        createTestFile("file1.txt", "Hello World");
        createTestFile("file2.txt", "Another file");
        createTestFile("subdir/file3.txt", "Nested file");
    }

    /**
     * Creates a test file with content
     */
    private void createTestFile(String relativePath, String content) throws IOException {
        Path filePath = testDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes());
    }

    /**
     * Deletes a test file
     */
    private void deleteTestFile(String relativePath) throws IOException {
        Path filePath = testDir.resolve(relativePath);
        Files.deleteIfExists(filePath);
    }

    /**
     * Modifies a test file
     */
    private void modifyTestFile(String relativePath, String newContent) throws IOException {
        Path filePath = testDir.resolve(relativePath);
        Files.write(filePath, newContent.getBytes());
    }

    /**
     * Test 1: Basic snapshot functionality
     */
    private void testBasicSnapshot() throws IOException {
        System.out.println("\n=== Test 1: Basic Snapshot Functionality ===");

        Map<String, SnapshotEngine.FileMetadata> snapshot = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot);

        System.out.println("Snapshot captured: " + snapshot.size() + " files");
        assert snapshot.size() >= 3 : "Should find at least 3 files";
        System.out.println("✓ Basic snapshot test passed");
    }

    /**
     * Test 2: File addition detection
     */
    private void testFileAddition() throws IOException, InterruptedException {
        System.out.println("\n=== Test 2: File Addition Detection ===");

        // Take first snapshot
        Map<String, SnapshotEngine.FileMetadata> snapshot1 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot1);

        // Add a new file
        Thread.sleep(1000); // Ensure different timestamps
        createTestFile("new_file.txt", "This is a new file");

        // Take second snapshot
        Map<String, SnapshotEngine.FileMetadata> snapshot2 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot2);

        // Compare
        HistoryManager.DiffResult result = historyManager.diffLatest();

        System.out.println("Added files: " + result.addedFiles.size());
        assert result.addedFiles.size() == 1 : "Should detect 1 added file";
        assert result.addedFiles.get(0).getFilePath().contains("new_file.txt") : "Should detect new_file.txt";

        System.out.println("✓ File addition test passed");
    }

    /**
     * Test 3: File deletion detection
     */
    private void testFileDeletion() throws IOException, InterruptedException {
        System.out.println("\n=== Test 3: File Deletion Detection ===");

        // Take snapshot before deletion
        Map<String, SnapshotEngine.FileMetadata> snapshot1 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot1);

        // Delete a file
        Thread.sleep(1000);
        deleteTestFile("file1.txt");

        // Take snapshot after deletion
        Map<String, SnapshotEngine.FileMetadata> snapshot2 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot2);

        // Compare
        HistoryManager.DiffResult result = historyManager.diffLatest();

        System.out.println("Deleted files: " + result.deletedFiles.size());
        assert result.deletedFiles.size() == 1 : "Should detect 1 deleted file";
        assert result.deletedFiles.get(0).getFilePath().contains("file1.txt") : "Should detect file1.txt deletion";

        System.out.println("✓ File deletion test passed");
    }

    /**
     * Test 4: File modification detection
     */
    private void testFileModification() throws IOException, InterruptedException {
        System.out.println("\n=== Test 4: File Modification Detection ===");

        // Take snapshot before modification
        Map<String, SnapshotEngine.FileMetadata> snapshot1 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot1);

        // Modify a file
        Thread.sleep(1000);
        modifyTestFile("file2.txt", "Modified content");

        // Take snapshot after modification
        Map<String, SnapshotEngine.FileMetadata> snapshot2 = snapshotEngine.captureSnapshot(testDir.toString());
        historyManager.addSnapshot(snapshot2);

        // Compare
        HistoryManager.DiffResult result = historyManager.diffLatest();

        System.out.println("Modified files: " + result.modifiedFiles.size());
        assert result.modifiedFiles.size() == 1 : "Should detect 1 modified file";
        assert result.modifiedFiles.get(0).oldVersion.getFilePath().contains("file2.txt") : "Should detect file2.txt modification";

        System.out.println("✓ File modification test passed");
    }

    /**
     * Test 5: Error handling
     */
    private void testErrorHandling() {
        System.out.println("\n=== Test 5: Error Handling ===");

        try {
            // Test invalid directory
            snapshotEngine.captureSnapshot("/invalid/path/that/does/not/exist");
            assert false : "Should have thrown an exception";
        } catch (Exception e) {
            System.out.println("✓ Correctly handled invalid path: " + e.getMessage());
        }

        // Test diff with insufficient snapshots
        HistoryManager emptyManager = new HistoryManager();
        HistoryManager.DiffResult result = emptyManager.diffLatest();
        assert !result.hasChanges() : "Should return empty result for no snapshots";
        System.out.println("✓ Correctly handled empty history");

        System.out.println("✓ Error handling test passed");
    }

    /**
     * Test 6: Performance test with many files
     */
    private void testPerformance() throws IOException {
        System.out.println("\n=== Test 6: Performance Test ===");

        // Create many small files
        for (int i = 0; i < 100; i++) {
            createTestFile("perf_test/file_" + i + ".txt", "Content " + i);
        }

        long startTime = System.currentTimeMillis();
        Map<String, SnapshotEngine.FileMetadata> snapshot = snapshotEngine.captureSnapshot(testDir.toString());
        long endTime = System.currentTimeMillis();

        System.out.println("Captured " + snapshot.size() + " files in " + (endTime - startTime) + "ms");
        assert snapshot.size() >= 100 : "Should capture all test files";
        System.out.println("✓ Performance test passed");
    }

    /**
     * Run all tests
     */
    public void runAllTests() {
        try {
            setupTestEnvironment();

            testBasicSnapshot();
            testFileAddition();
            testFileDeletion();
            testFileModification();
            testErrorHandling();
            testPerformance();

            System.out.println("\n🎉 All tests passed successfully!");
            System.out.println("\nTest directory (for inspection): " + testDir);

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            if (testDir != null) {
                try {
                    deleteRecursive(testDir);
                } catch (IOException e) {
                    System.err.println("Warning: Could not clean up test directory: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively delete test directory
     */
    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    deleteRecursive(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    /**
     * Main method to run tests
     */
    public static void main(String[] args) {
        ComprehensiveTester tester = new ComprehensiveTester();
        tester.runAllTests();
    }
}