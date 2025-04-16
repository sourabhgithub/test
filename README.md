import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AssertEventually {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void assertEventually(final Callback callback) {
        AtomicInteger numberOfTries = new AtomicInteger(0);
        long waitTimeInMilliseconds = 750L;
        AssertionError error = new AssertionError();

        // Create a retry task
        Runnable retryTask = new Runnable() {
            @Override
            public void run() {
                if (numberOfTries.get() < 5) {
                    try {
                        callback.apply(); // Try executing the callback
                        return; // Success, exit the method
                    } catch (AssertionError assertionError) {
                        error.setMessage(assertionError.getMessage());
                        numberOfTries.incrementAndGet(); // Increment retry count
                    }

                    // Schedule next retry with an exponential backoff
                    scheduler.schedule(this, waitTimeInMilliseconds, TimeUnit.MILLISECONDS);
                    waitTimeInMilliseconds *= 2; // Double the wait time for the next retry
                } else {
                    // If retries exhausted, throw the last captured error
                    throw new AssertionError("Max retries reached: " + error.getMessage());
                }
            }
        };

        // Initial call to start the retry process
        retryTask.run();
    }

    public static void modularAssertEventually(final Callback callback) {
        AtomicInteger numberOfTries = new AtomicInteger(0);
        long waitTimeInMilliseconds = 750L;
        AssertionError error = new AssertionError();

        // Create a retry task
        Runnable retryTask = new Runnable() {
            @Override
            public void run() {
                if (numberOfTries.get() < 5) {
                    try {
                        callback.apply(); // Try executing the callback
                        return; // Success, exit the method
                    } catch (AssertionError assertionError) {
                        error.setMessage(assertionError.getMessage());
                        numberOfTries.incrementAndGet(); // Increment retry count
                    }

                    // Schedule next retry with an exponential backoff
                    scheduler.schedule(this, waitTimeInMilliseconds, TimeUnit.MILLISECONDS);
                    waitTimeInMilliseconds *= 2; // Double the wait time for the next retry
                } else {
                    // If retries exhausted, throw the last captured error
                    throw new AssertionError("Max retries reached: " + error.getMessage());
                }
            }
        };

        // Initial call to start the retry process
        retryTask.run();
    }

    // Example Callback functional interface
    @FunctionalInterface
    public interface Callback {
        void apply() throws AssertionError;
    }

    public static void main(String[] args) {
        // Example usage
        assertEventually(() -> {
            // Your callback code here (throw an exception to simulate failure)
            System.out.println("Attempting operation...");
            throw new AssertionError("Operation failed");
        });
    }
}
