import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class QuizQuestion {
    private final String prompt;
    private final List<String> options;
    private final int correctIndex;

    QuizQuestion(String prompt, List<String> options, int correctIndex) {
        this.prompt = prompt;
        this.options = new ArrayList<>(options);
        this.correctIndex = correctIndex;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return new ArrayList<>(options);
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    @SuppressWarnings("unchecked")
    static QuizQuestion fromJsonMap(Map<String, Object> map) {
        Object promptValue = map.getOrDefault("question", map.get("prompt"));
        if (!(promptValue instanceof String)) {
            throw new IllegalArgumentException("Question prompt missing or not a string");
        }
        Object optionsValue = map.get("options");
        if (!(optionsValue instanceof List)) {
            throw new IllegalArgumentException("Options missing or not an array");
        }
        List<Object> rawOptions = (List<Object>) optionsValue;
        List<String> options = new ArrayList<>();
        for (Object entry : rawOptions) {
            if (!(entry instanceof String)) {
                throw new IllegalArgumentException("Option must be a string");
            }
            options.add((String) entry);
        }

        Object answerValue = map.containsKey("answerIndex") ? map.get("answerIndex") : map.get("correctIndex");
        if (!(answerValue instanceof Number)) {
            throw new IllegalArgumentException("answerIndex missing or not a number");
        }
        int correctIndex = ((Number) answerValue).intValue();
        if (correctIndex < 0 || correctIndex >= options.size()) {
            throw new IllegalArgumentException("answerIndex out of bounds for options");
        }

        return new QuizQuestion((String) promptValue, options, correctIndex);
    }
}

