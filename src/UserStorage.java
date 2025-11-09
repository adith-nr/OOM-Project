import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extremely small helper that stores usernames and hashed passwords in a flat file.
 * Designed to keep dependencies minimal while still avoiding plain-text passwords.
 */
final class UserStorage {

    private static final String DATA_DIRECTORY = "user-data";
    private static final String USERS_FILE = "users.txt";

    private final Path usersPath;
    private final Map<String, String> credentials = new HashMap<>();

    UserStorage() {
        Path dir = Paths.get(DATA_DIRECTORY);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // If creating the directory fails we still attempt to work with in-memory users.
        }
        this.usersPath = dir.resolve(USERS_FILE);
        loadExistingUsers();
    }

    synchronized boolean authenticate(String username, char[] password) {
        String normalizedUser = normalize(username);
        if (normalizedUser.isEmpty()) {
            return false;
        }
        String storedHash = credentials.get(normalizedUser);
        if (storedHash == null) {
            return false;
        }
        return storedHash.equals(hashPassword(password));
    }

    synchronized void register(String username, char[] password) throws IOException {
        String normalizedUser = normalize(username);
        if (normalizedUser.length() < 3) {
            throw new IllegalArgumentException("Username must contain at least 3 characters.");
        }
        if (credentials.containsKey(normalizedUser)) {
            throw new IllegalArgumentException("Username already exists.");
        }
        String passwordHash = hashPassword(password);
        if (passwordHash.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        credentials.put(normalizedUser, passwordHash);
        persistUsers();
    }

    private void loadExistingUsers() {
        if (!Files.exists(usersPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(usersPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException ignored) {
            // Fallback to in-memory map only.
        }
    }

    private void persistUsers() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            builder.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue())
                    .append(System.lineSeparator());
        }
        Files.writeString(
                usersPath,
                builder.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(new String(password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing SHA-256 algorithm", ex);
        }
    }
}
