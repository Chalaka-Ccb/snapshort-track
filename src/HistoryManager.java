import java.util.*;

/**
 * History Manager and Diff Engine - Part 2 of File System Snapshot and Diff Tool
 * Manages snapshot history and calculates differences between snapshots
 */
public class HistoryManager {

    // LinkedList to maintain chronological history of snapshots
    private LinkedList<Map<String, SnapshotEngine.FileMetadata>> snapshotHistory;

    public HistoryManager() {
        this.snapshotHistory = new LinkedList<>();
    }

    /**
     * Adds a new snapshot to the history
     * @param snapshot The snapshot to add (TreeMap from SnapshotEngine)
     */
    public void addSnapshot(Map<String, SnapshotEngine.FileMetadata> snapshot) {
        // Create a defensive copy to avoid external modifications
        Map<String, SnapshotEngine.FileMetadata> snapshotCopy = new TreeMap<>(snapshot);
        snapshotHistory.addFirst(snapshotCopy); // Most recent first
    }

    /**
     * Gets the most recent snapshot
     * @return The latest snapshot or null if history is empty
     */
    public Map<String, SnapshotEngine.FileMetadata> getLatestSnapshot() {
        return snapshotHistory.isEmpty() ? null : new TreeMap<>(snapshotHistory.getFirst());
    }

    /**
     * Gets a snapshot by its index in history (0 = most recent)
     * @param index The index of the snapshot
     * @return The requested snapshot or null if index is invalid
     */
    public Map<String, SnapshotEngine.FileMetadata> getSnapshot(int index) {
        if (index < 0 || index >= snapshotHistory.size()) {
            return null;
        }
        return new TreeMap<>(snapshotHistory.get(index));
    }

    /**
     * Gets the number of snapshots in history
     * @return The count of snapshots
     */
    public int getHistorySize() {
        return snapshotHistory.size();
    }

    /**
     * Represents the result of a diff operation
     */
    public static class DiffResult {
        public List<SnapshotEngine.FileMetadata> addedFiles = new ArrayList<>();
        public List<SnapshotEngine.FileMetadata> deletedFiles = new ArrayList<>();
        public List<ModifiedFile> modifiedFiles = new ArrayList<>();

        public boolean hasChanges() {
            return !addedFiles.isEmpty() || !deletedFiles.isEmpty() || !modifiedFiles.isEmpty();
        }
    }

    /**
     * Represents a modified file with both old and new metadata
     */
    public static class ModifiedFile {
        public final SnapshotEngine.FileMetadata oldVersion;
        public final SnapshotEngine.FileMetadata newVersion;

        public ModifiedFile(SnapshotEngine.FileMetadata oldVersion, SnapshotEngine.FileMetadata newVersion) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        @Override
        public String toString() {
            return String.format("Modified[%s -> %s]", oldVersion, newVersion);
        }
    }

    /**
     * Compares two snapshots and returns the differences
     * Leverages the sorted nature of TreeMap for efficient O(n) comparison
     * @param olderSnapshot The older snapshot
     * @param newerSnapshot The newer snapshot
     * @return DiffResult containing added, deleted, and modified files
     */
    public DiffResult diff(Map<String, SnapshotEngine.FileMetadata> olderSnapshot,
                           Map<String, SnapshotEngine.FileMetadata> newerSnapshot) {

        DiffResult result = new DiffResult();

        // If either snapshot is null, return empty result
        if (olderSnapshot == null || newerSnapshot == null) {
            return result;
        }

        // Use iterators to traverse both sorted maps simultaneously
        Iterator<Map.Entry<String, SnapshotEngine.FileMetadata>> oldIter = olderSnapshot.entrySet().iterator();
        Iterator<Map.Entry<String, SnapshotEngine.FileMetadata>> newIter = newerSnapshot.entrySet().iterator();

        Map.Entry<String, SnapshotEngine.FileMetadata> oldEntry = oldIter.hasNext() ? oldIter.next() : null;
        Map.Entry<String, SnapshotEngine.FileMetadata> newEntry = newIter.hasNext() ? newIter.next() : null;

        // Process both snapshots in sorted order
        while (oldEntry != null || newEntry != null) {
            if (oldEntry == null) {
                // No more old entries, all remaining new entries are added files
                result.addedFiles.add(newEntry.getValue());
                newEntry = newIter.hasNext() ? newIter.next() : null;
            }
            else if (newEntry == null) {
                // No more new entries, all remaining old entries are deleted files
                result.deletedFiles.add(oldEntry.getValue());
                oldEntry = oldIter.hasNext() ? oldIter.next() : null;
            }
            else {
                int comparison = oldEntry.getKey().compareTo(newEntry.getKey());

                if (comparison < 0) {
                    // Current old path comes before current new path → file was deleted
                    result.deletedFiles.add(oldEntry.getValue());
                    oldEntry = oldIter.hasNext() ? oldIter.next() : null;
                }
                else if (comparison > 0) {
                    // Current new path comes before current old path → file was added
                    result.addedFiles.add(newEntry.getValue());
                    newEntry = newIter.hasNext() ? newIter.next() : null;
                }
                else {
                    // Same file path - check if it was modified
                    if (!oldEntry.getValue().equals(newEntry.getValue())) {
                        result.modifiedFiles.add(new ModifiedFile(oldEntry.getValue(), newEntry.getValue()));
                    }
                    oldEntry = oldIter.hasNext() ? oldIter.next() : null;
                    newEntry = newIter.hasNext() ? newIter.next() : null;
                }
            }
        }

        return result;
    }

    /**
     * Compares the two most recent snapshots
     * @return DiffResult of the comparison
     */
    public DiffResult diffLatest() {
        if (snapshotHistory.size() < 2) {
            System.out.println("Need at least 2 snapshots to compare");
            return new DiffResult();
        }

        Map<String, SnapshotEngine.FileMetadata> newer = snapshotHistory.get(0);
        Map<String, SnapshotEngine.FileMetadata> older = snapshotHistory.get(1);

        return diff(older, newer);
    }

    /**
     * Compares snapshots by their indices in history
     * @param olderIndex Index of older snapshot (further from 0)
     * @param newerIndex Index of newer snapshot (closer to 0)
     * @return DiffResult of the comparison
     */
    public DiffResult diff(int olderIndex, int newerIndex) {
        if (olderIndex >= newerIndex) {
            System.out.println("Older index must be greater than newer index");
            return new DiffResult();
        }

        Map<String, SnapshotEngine.FileMetadata> older = getSnapshot(olderIndex);
        Map<String, SnapshotEngine.FileMetadata> newer = getSnapshot(newerIndex);

        if (older == null || newer == null) {
            System.out.println("Invalid snapshot indices");
            return new DiffResult();
        }

        return diff(older, newer);
    }

    /**
     * Prints the diff result in a readable format
     * @param result The diff result to display
     */
    public void printDiffResult(DiffResult result) {
        if (!result.hasChanges()) {
            System.out.println("No changes detected between snapshots.");
            return;
        }

        System.out.println("\n=== DIFF RESULTS ===");

        if (!result.addedFiles.isEmpty()) {
            System.out.println("\nADDED FILES (" + result.addedFiles.size() + "):");
            for (SnapshotEngine.FileMetadata file : result.addedFiles) {
                System.out.println("  + " + file.getFilePath());
            }
        }

        if (!result.deletedFiles.isEmpty()) {
            System.out.println("\nDELETED FILES (" + result.deletedFiles.size() + "):");
            for (SnapshotEngine.FileMetadata file : result.deletedFiles) {
                System.out.println("  - " + file.getFilePath());
            }
        }

        if (!result.modifiedFiles.isEmpty()) {
            System.out.println("\nMODIFIED FILES (" + result.modifiedFiles.size() + "):");
            for (ModifiedFile modified : result.modifiedFiles) {
                System.out.println("  * " + modified.oldVersion.getFilePath());
                System.out.println("      Old: size=" + modified.oldVersion.getFileSize() +
                        ", modified=" + new Date(modified.oldVersion.getLastModified()));
                System.out.println("      New: size=" + modified.newVersion.getFileSize() +
                        ", modified=" + new Date(modified.newVersion.getLastModified()));
            }
        }
    }

    /**
     * Prints the history of snapshots
     */
    public void printHistory() {
        if (snapshotHistory.isEmpty()) {
            System.out.println("No snapshots in history.");
            return;
        }

        System.out.println("\n=== SNAPSHOT HISTORY ===");
        for (int i = 0; i < snapshotHistory.size(); i++) {
            Map<String, SnapshotEngine.FileMetadata> snapshot = snapshotHistory.get(i);
            System.out.printf("[%d] Snapshot with %d files%n", i, snapshot.size());
        }
    }

    /**
     * Test method to demonstrate the functionality
     */
    public static void main(String[] args) {
        // Create components
        SnapshotEngine engine = new SnapshotEngine();
        HistoryManager manager = new HistoryManager();

        try {
            // Capture first snapshot
            System.out.println("Capturing first snapshot...");
            Map<String, SnapshotEngine.FileMetadata> snapshot1 = engine.captureSnapshot(".");
            manager.addSnapshot(snapshot1);

            // Simulate some file system changes by waiting
            System.out.println("Please make some file changes (add/delete/modify files) and press Enter...");
            System.in.read();

            // Capture second snapshot
            System.out.println("Capturing second snapshot...");
            Map<String, SnapshotEngine.FileMetadata> snapshot2 = engine.captureSnapshot(".");
            manager.addSnapshot(snapshot2);

            // Display history
            manager.printHistory();

            // Compare the two snapshots
            System.out.println("\nComparing snapshots...");
            DiffResult result = manager.diffLatest();
            manager.printDiffResult(result);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}