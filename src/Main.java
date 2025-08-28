
public class Main {

    /**
     * Main entry point for the application
     * Usage: java FileSnapshotApplication [mode]
     * Modes:
     *   - cli (default): Start interactive command-line interface
     *   - test: Run comprehensive tests
     */
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "cli";

        switch (mode) {
            case "test":
                System.out.println("Running comprehensive tests...");
                ComprehensiveTester tester = new ComprehensiveTester();
                tester.runAllTests();
                break;

            case "cli":
            default:
                System.out.println("Starting CLI interface...");
                FileSnapshotCLI cli = new FileSnapshotCLI();
                cli.start();
                break;
        }
    }
}