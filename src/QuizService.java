import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class QuizService {

    private static final String BASE_URL = "http://localhost:3000";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    QuizData requestQuiz(String topic, int questionCount, String difficulty) throws QuizServiceException {
        String sanitizedTopic = topic != null ? topic.trim() : "";
        if (sanitizedTopic.isEmpty()) {
            throw new QuizServiceException("Topic must not be empty");
        }
        int sanitizedCount = Math.max(1, questionCount);
        String normalizedDifficulty = (difficulty != null ? difficulty : "medium").toLowerCase();

        String payload = buildPayload(sanitizedTopic, sanitizedCount, normalizedDifficulty);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quiz/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new QuizServiceException("Network operation interrupted", ex);
        } catch (IOException ex) {
            throw new QuizServiceException("Network error while contacting quiz backend", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new QuizServiceException("Backend returned status " + response.statusCode());
        }

        return parseQuizData(response.body());
    }

    private String buildPayload(String topic, int questionCount, String difficulty) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"topic\":\"")
                .append(escapeJson(topic))
                .append("\",\"questionCount\":")
                .append(questionCount)
                .append(",\"difficulty\":\"")
                .append(escapeJson(difficulty))
                .append("\"}");
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private QuizData parseQuizData(String body) throws QuizServiceException {
        Object parsed;
        try {
            parsed = SimpleJsonParser.parse(body);
        } catch (SimpleJsonParser.JsonParseException ex) {
            throw new QuizServiceException("Failed to parse quiz JSON", ex);
        }

        if (!(parsed instanceof Map)) {
            throw new QuizServiceException("Quiz payload root must be a JSON object");
        }
        Map<String, Object> root = (Map<String, Object>) parsed;

        Object questionsValue = root.get("questions");
        if (!(questionsValue instanceof List)) {
            throw new QuizServiceException("Quiz payload missing \"questions\" array");
        }

        List<QuizQuestion> questions = parseQuestionsArray((List<Object>) questionsValue);
        if (questions.isEmpty()) {
            throw new QuizServiceException("Quiz payload returned zero questions");
        }

        String quizId = asOptionalString(root.get("quizId"));
        String topic = asOptionalString(root.get("topic"));
        String difficulty = asOptionalString(root.get("difficulty"));
        int questionCount = asOptionalInteger(root.get("questionCount"), questions.size());

        return new QuizData(quizId, topic, difficulty, questionCount, questions);
    }

    @SuppressWarnings("unchecked")
    private List<QuizQuestion> parseQuestionsArray(List<Object> rawQuestions) throws QuizServiceException {
        List<QuizQuestion> questions = new ArrayList<>();
        for (Object entry : rawQuestions) {
            if (!(entry instanceof Map)) {
                throw new QuizServiceException("Question entry should be an object");
            }
            try {
                questions.add(QuizQuestion.fromJsonMap((Map<String, Object>) entry));
            } catch (IllegalArgumentException ex) {
                throw new QuizServiceException("Invalid question structure: " + ex.getMessage(), ex);
            }
        }
        return questions;
    }

    private String asOptionalString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    private int asOptionalInteger(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    static class QuizData {
        private final String quizId;
        private final String topic;
        private final String difficulty;
        private final int questionCount;
        private final List<QuizQuestion> questions;

        QuizData(String quizId, String topic, String difficulty, int questionCount, List<QuizQuestion> questions) {
            this.quizId = quizId;
            this.topic = topic != null ? topic : "";
            this.difficulty = difficulty != null ? difficulty : "medium";
            this.questionCount = questionCount;
            this.questions = new ArrayList<>(questions);
        }

        public String getQuizId() {
            return quizId;
        }

        public String getTopic() {
            return topic;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public int getQuestionCount() {
            return questionCount;
        }

        public List<QuizQuestion> getQuestions() {
            return new ArrayList<>(questions);
        }
    }

    static class QuizServiceException extends Exception {
        QuizServiceException(String message) {
            super(message);
        }

        QuizServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
