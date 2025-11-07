import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser that supports the subset of JSON required by the quiz payload.
 * Produces Java primitives, Lists, and Maps.
 */
public final class SimpleJsonParser {

    private final String input;
    private int index;

    private SimpleJsonParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public static Object parse(String json) throws JsonParseException {
        SimpleJsonParser parser = new SimpleJsonParser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("Unexpected characters after JSON content");
        }
        return value;
    }

    private Object parseValue() throws JsonParseException {
        skipWhitespace();
        if (isAtEnd()) {
            throw error("Unexpected end of JSON input");
        }
        char c = peek();
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
                expectLiteral("true");
                return Boolean.TRUE;
            case 'f':
                expectLiteral("false");
                return Boolean.FALSE;
            case 'n':
                expectLiteral("null");
                return null;
            default:
                if (isNumberStart(c)) {
                    return parseNumber();
                }
                throw error("Unexpected character: " + c);
        }
    }

    private Map<String, Object> parseObject() throws JsonParseException {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (match('}')) {
            return result;
        }
        do {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
        } while (match(','));
        expect('}');
        return result;
    }

    private List<Object> parseArray() throws JsonParseException {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (match(']')) {
            return result;
        }
        do {
            Object value = parseValue();
            result.add(value);
            skipWhitespace();
        } while (match(','));
        expect(']');
        return result;
    }

    private String parseString() throws JsonParseException {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                return builder.toString();
            }
            if (c == '\\') {
                if (isAtEnd()) {
                    throw error("Unterminated escape sequence in string");
                }
                char escaped = advance();
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        builder.append(escaped);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        builder.append(parseUnicodeEscape());
                        break;
                    default:
                        throw error("Invalid escape sequence: \\" + escaped);
                }
            } else {
                builder.append(c);
            }
        }
        throw error("Unterminated string literal");
    }

    private char parseUnicodeEscape() throws JsonParseException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            if (isAtEnd()) {
                throw error("Incomplete unicode escape sequence");
            }
            char hex = advance();
            int digit = Character.digit(hex, 16);
            if (digit == -1) {
                throw error("Invalid hex digit in unicode escape: " + hex);
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }

    private Number parseNumber() throws JsonParseException {
        int start = index;
        if (peek() == '-') {
            advance();
        }
        if (peek() == '0') {
            advance();
        } else {
            consumeDigits();
        }
        boolean isFractional = false;
        if (match('.')) {
            isFractional = true;
            consumeDigits();
        }
        if (peekIfExists() == 'e' || peekIfExists() == 'E') {
            isFractional = true;
            advance();
            if (peekIfExists() == '+' || peekIfExists() == '-') {
                advance();
            }
            consumeDigits();
        }
        String number = input.substring(start, index);
        try {
            if (isFractional) {
                return Double.parseDouble(number);
            }
            long value = Long.parseLong(number);
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
            return value;
        } catch (NumberFormatException ex) {
            throw error("Invalid number: " + number);
        }
    }

    private void consumeDigits() throws JsonParseException {
        if (!Character.isDigit(peek())) {
            throw error("Expected digit");
        }
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }
    }

    private void expect(char expected) throws JsonParseException {
        if (isAtEnd() || peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        advance();
    }

    private void expectLiteral(String literal) throws JsonParseException {
        for (int i = 0; i < literal.length(); i++) {
            if (isAtEnd() || input.charAt(index) != literal.charAt(i)) {
                throw error("Expected \"" + literal + "\"");
            }
            index++;
        }
    }

    private boolean match(char expected) {
        if (isAtEnd() || peek() != expected) {
            return false;
        }
        index++;
        return true;
    }

    private char advance() {
        return input.charAt(index++);
    }

    private char peek() {
        return input.charAt(index);
    }

    private char peekIfExists() {
        if (isAtEnd()) {
            return '\0';
        }
        return input.charAt(index);
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                index++;
            } else {
                break;
            }
        }
    }

    private boolean isNumberStart(char c) {
        return (c >= '0' && c <= '9') || c == '-';
    }

    private boolean isAtEnd() {
        return index >= input.length();
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message + " at position " + index);
    }

    public static final class JsonParseException extends Exception {
        public JsonParseException(String message) {
            super(message);
        }
    }
}

