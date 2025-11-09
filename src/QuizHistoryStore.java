import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Very small text based storage that keeps quiz summaries per user.
 * Lines are stored as: username|timestamp|topic|difficulty|correct|total|score
 */
final class QuizHistoryStore {

    private static final String DATA_DIRECTORY = "user-data";
    private static final String HISTORY_FILE = "history.txt";

    private final Path historyPath;

    QuizHistoryStore() {
        Path dir = Paths.get(DATA_DIRECTORY);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // Soft-fail makes the app usable even if persistence cannot be created.
        }
        this.historyPath = dir.resolve(HISTORY_FILE);
    }

    synchronized void recordResult(String username, String topic, String difficulty, int correct, int total, int scorePercent) {
        String cleanUser = sanitize(username);
        if (cleanUser.isEmpty() || total <= 0) {
            return;
        }
        String line = String.join("|",
                escape(cleanUser),
                Instant.now().toString(),
                escape(topic),
                escape(difficulty),
                Integer.toString(correct),
                Integer.toString(total),
                Integer.toString(scorePercent)
        );
        try {
            Files.writeString(
                    historyPath,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // History is a convenience feature; failure should not crash the app.
        }
    }

    synchronized List<QuizRecord> loadForUser(String username) {
        String cleanUser = sanitize(username);
        if (cleanUser.isEmpty() || !Files.exists(historyPath)) {
            return Collections.emptyList();
        }
        List<QuizRecord> records = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(historyPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                QuizRecord record = parseLine(line);
                if (record != null && cleanUser.equals(record.username)) {
                    records.add(record);
                }
            }
        } catch (IOException ignored) {
            return Collections.emptyList();
        }
        records.sort(Comparator.comparing((QuizRecord r) -> r.timestamp).reversed());
        return records;
    }

    private QuizRecord parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split("\\|", 7);
        if (parts.length < 7) {
            return null;
        }
        try {
            QuizRecord record = new QuizRecord();
            record.username = unescape(parts[0]);
            record.timestamp = parts[1];
            record.topic = unescape(parts[2]);
            record.difficulty = unescape(parts[3]);
            record.correctCount = Integer.parseInt(parts[4]);
            record.total = Integer.parseInt(parts[5]);
            record.scorePercent = Integer.parseInt(parts[6]);
            return record;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String escape(String value) {
        return sanitize(value).replace("|", "%7C");
    }

    private String unescape(String value) {
        return value == null ? "" : value.replace("%7C", "|");
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    static final class QuizRecord {
        String username;
        String timestamp;
        String topic;
        String difficulty;
        int correctCount;
        int total;
        int scorePercent;
    }
}
