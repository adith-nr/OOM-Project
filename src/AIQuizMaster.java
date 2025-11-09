import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.Cursor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AIQuizMaster extends JFrame {

    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_HOME = "HOME";
    private static final String CARD_QUIZ = "QUIZ";
    private static final String CARD_RESULTS = "RESULTS";
    private static final String CARD_HISTORY = "HISTORY";
    private static final Integer[] QUESTION_COUNT_OPTIONS = {5, 6, 7, 8, 9, 10};
    private static final String[] DIFFICULTY_OPTIONS = {"Easy", "Medium", "Hard"};
    private static final Color PRIMARY_COLOR = new Color(45, 99, 179);
    private static final Color ACCENT_COLOR = new Color(96, 154, 219);
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 252);
    private static final Color SURFACE_COLOR = Color.WHITE;
    private static final Color SURFACE_BORDER = new Color(220, 227, 233);
    private static final Color ERROR_COLOR = new Color(192, 70, 70);
    private static final Color MUTED_TEXT_COLOR = new Color(102, 117, 133);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter
            .ofPattern("MMM d, HH:mm")
            .withZone(ZoneId.systemDefault());

    private final QuizService quizService = new QuizService();
    private final UserStorage userStorage = new UserStorage();
    private final QuizHistoryStore historyStore = new QuizHistoryStore();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);
    private final LoginPanel loginPanel = new LoginPanel();
    private final HistoryPanel historyPanel = new HistoryPanel();
    private final HomePanel homePanel = new HomePanel();
    private final QuizPanel quizPanel = new QuizPanel();
    private final ResultPanel resultPanel = new ResultPanel();
    private String currentUser;

    public AIQuizMaster() {
        super("AI QuizMaster");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(620, 480));
        getContentPane().setBackground(BACKGROUND_COLOR);
        cardContainer.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        cardContainer.setBackground(BACKGROUND_COLOR);
        cardContainer.setOpaque(true);

        cardContainer.add(loginPanel, CARD_LOGIN);
        cardContainer.add(homePanel, CARD_HOME);
        cardContainer.add(quizPanel, CARD_QUIZ);
        cardContainer.add(resultPanel, CARD_RESULTS);
        cardContainer.add(historyPanel, CARD_HISTORY);

        setContentPane(cardContainer);
        cardLayout.show(cardContainer, CARD_LOGIN);
    }

    private void requestQuiz(String topic, int questionCount, String difficulty) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please sign in to generate quizzes.", "Sign In Required", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(cardContainer, CARD_LOGIN);
            return;
        }
        homePanel.setLoading(true);
        new QuizFetchWorker(topic, questionCount, difficulty).execute();
    }

    private void startQuiz(QuizService.QuizData quizData) {
        quizPanel.loadQuiz(quizData);
        cardLayout.show(cardContainer, CARD_QUIZ);
    }

    private void showResults(QuizService.QuizData quizData, int[] selections, int correctCount) {
        resultPanel.updateResults(quizData, selections, correctCount);
        if (currentUser != null && quizData != null) {
            int totalQuestions = Math.max(quizData.getQuestions().size(), 1);
            int scorePercent = Math.round((correctCount / (float) totalQuestions) * 100f);
            historyStore.recordResult(
                    currentUser,
                    quizData.getTopic(),
                    quizData.getDifficulty(),
                    correctCount,
                    totalQuestions,
                    scorePercent
            );
            historyPanel.refresh();
        }
        cardLayout.show(cardContainer, CARD_RESULTS);
    }

    private void returnHome() {
        homePanel.reset();
        quizPanel.reset();
        resultPanel.reset();
        if (currentUser == null) {
            cardLayout.show(cardContainer, CARD_LOGIN);
        } else {
            cardLayout.show(cardContainer, CARD_HOME);
        }
    }

    private void handleSuccessfulLogin(String username) {
        this.currentUser = username;
        homePanel.updateUser(username);
        historyPanel.refresh();
        returnHome();
    }

    private void logoutCurrentUser() {
        this.currentUser = null;
        homePanel.updateUser(null);
        quizPanel.reset();
        resultPanel.reset();
        homePanel.reset();
        cardLayout.show(cardContainer, CARD_LOGIN);
    }

    private final class QuizFetchWorker extends SwingWorker<QuizService.QuizData, Void> {

        private final String topic;
        private final int questionCount;
        private final String difficulty;

        QuizFetchWorker(String topic, int questionCount, String difficulty) {
            this.topic = topic;
            this.questionCount = questionCount;
            this.difficulty = difficulty;
        }

        @Override
        protected QuizService.QuizData doInBackground() throws Exception {
            return quizService.requestQuiz(topic, questionCount, difficulty);
        }

        @Override
        protected void done() {
            homePanel.setLoading(false);
            try {
                QuizService.QuizData quizData = get();
                startQuiz(quizData);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                homePanel.showError("Quiz generation interrupted.");
            } catch (ExecutionException ex) {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                homePanel.showError(message != null ? message : "Failed to generate quiz.");
            }
        }
    }

    private final class LoginPanel extends JPanel {
        private final JTextField usernameField = new JTextField();
        private final JPasswordField passwordField = new JPasswordField();
        private final JPasswordField confirmPasswordField = new JPasswordField();
        private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        private final JButton loginButton = new JButton("Login");
        private final JButton registerButton = new JButton("Register");

        LoginPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            JLabel title = new JLabel("Welcome to AI QuizMaster");
            title.setFont(TITLE_FONT);
            title.setAlignmentX(CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel("Create an account or sign in to continue.");
            subtitle.setFont(SUBTITLE_FONT);
            subtitle.setAlignmentX(CENTER_ALIGNMENT);
            subtitle.setForeground(MUTED_TEXT_COLOR);

            usernameField.setAlignmentX(CENTER_ALIGNMENT);
            usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            usernameField.setFont(usernameField.getFont().deriveFont(16f));
            usernameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            passwordField.setAlignmentX(CENTER_ALIGNMENT);
            passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            passwordField.setFont(passwordField.getFont().deriveFont(16f));
            passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            confirmPasswordField.setAlignmentX(CENTER_ALIGNMENT);
            confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            confirmPasswordField.setFont(confirmPasswordField.getFont().deriveFont(16f));
            confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            statusLabel.setAlignmentX(CENTER_ALIGNMENT);
            statusLabel.setForeground(ERROR_COLOR);
            statusLabel.setFont(statusLabel.getFont().deriveFont(13f));
            statusLabel.setVisible(false);

            stylePrimaryButton(loginButton);
            loginButton.setAlignmentX(CENTER_ALIGNMENT);
            loginButton.setMaximumSize(new Dimension(220, 40));
            loginButton.addActionListener(this::onLogin);

            styleSecondaryButton(registerButton);
            registerButton.setAlignmentX(CENTER_ALIGNMENT);
            registerButton.setMaximumSize(new Dimension(220, 40));
            registerButton.addActionListener(this::onRegister);

            card.add(title);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(subtitle);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(new JLabel("Username"));
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            card.add(usernameField);
            card.add(Box.createRigidArea(new Dimension(0, 16)));
            card.add(new JLabel("Password"));
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            card.add(passwordField);
            card.add(Box.createRigidArea(new Dimension(0, 16)));
            card.add(new JLabel("Confirm Password (for registration)"));
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            card.add(confirmPasswordField);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(loginButton);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(registerButton);
            card.add(Box.createRigidArea(new Dimension(0, 16)));
            card.add(statusLabel);

            add(Box.createVerticalGlue());
            add(card);
            add(Box.createVerticalGlue());
        }

        private void onLogin(ActionEvent event) {
            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();
            if (username.isEmpty() || password.length == 0) {
                showStatus("Enter both username and password.", ERROR_COLOR);
                return;
            }
            boolean authenticated = userStorage.authenticate(username, password);
            Arrays.fill(password, '\0');
            passwordField.setText("");
            if (authenticated) {
                showStatus("Login successful.", PRIMARY_COLOR.darker());
                SwingUtilities.invokeLater(() -> handleSuccessfulLogin(username));
            } else {
                showStatus("Incorrect username or password.", ERROR_COLOR);
            }
        }

        private void onRegister(ActionEvent event) {
            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();
            char[] confirm = confirmPasswordField.getPassword();
            if (username.isEmpty() || password.length == 0) {
                showStatus("Username and password are required.", ERROR_COLOR);
                return;
            }
            if (!Arrays.equals(password, confirm)) {
                showStatus("Passwords do not match.", ERROR_COLOR);
                return;
            }
            try {
                userStorage.register(username, password);
                showStatus("Registration complete. You can sign in now.", PRIMARY_COLOR.darker());
            } catch (IllegalArgumentException ex) {
                showStatus(ex.getMessage(), ERROR_COLOR);
            } catch (Exception ex) {
                showStatus("Failed to register user.", ERROR_COLOR);
            } finally {
                Arrays.fill(password, '\0');
                Arrays.fill(confirm, '\0');
                passwordField.setText("");
                confirmPasswordField.setText("");
            }
        }

        private void showStatus(String message, Color color) {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
            statusLabel.setVisible(true);
        }
    }

    private final class HomePanel extends JPanel {
        private final JTextField topicField = new JTextField();
        private final JComboBox<Integer> questionCountCombo = new JComboBox<>(QUESTION_COUNT_OPTIONS);
        private final JComboBox<String> difficultyCombo = new JComboBox<>(DIFFICULTY_OPTIONS);
        private final JButton generateButton = new JButton("Generate Quiz");
        private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        private final JLabel userLabel = new JLabel("Not signed in.");
        private final JButton historyButton = new JButton("Past Quizzes");
        private final JButton logoutButton = new JButton("Log Out");

        HomePanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            JLabel title = new JLabel("AI QuizMaster");
            title.setFont(TITLE_FONT);
            // title.setAlignmentX(CENTER_ALIGNMENT);
            title.setBorder(BorderFactory.createEmptyBorder(0, 200, 0, 0)); 

            JLabel subtitle = new JLabel("Describe a topic and tailor the quiz to your preference.");
            subtitle.setFont(SUBTITLE_FONT);
            // subtitle.setAlignmentX(CENTER_ALIGNMENT);
            subtitle.setForeground(MUTED_TEXT_COLOR);

              // move 40 px right
            subtitle.setBorder(BorderFactory.createEmptyBorder(0, 80, 0, 0));  // move 40 px right


            topicField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            topicField.setAlignmentX(CENTER_ALIGNMENT);
            

            topicField.setFont(topicField.getFont().deriveFont(16f));
            topicField.setToolTipText("Example: Machine Learning, World War II, Football");
            topicField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            // questionCountCombo.setAlignmentX(CENTER_ALIGNMENT);
            questionCountCombo.setMaximumSize(new Dimension(200, 32));
            // questionCountCombo.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 0));  // move 40 px right


            questionCountCombo.setSelectedItem(QUESTION_COUNT_OPTIONS[0]);
            questionCountCombo.setFocusable(false);
            questionCountCombo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(6, 80, 6, 8)
            ));

            // difficultyCombo.setAlignmentX(CENTER_ALIGNMENT);
            difficultyCombo.setMaximumSize(new Dimension(200, 32));
            difficultyCombo.setSelectedItem("Medium");
            difficultyCombo.setFocusable(false);
            difficultyCombo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(6, 80, 6, 8)
            ));

            generateButton.setAlignmentX(CENTER_ALIGNMENT);
            generateButton.addActionListener(this::onGenerate);
            stylePrimaryButton(generateButton);
            generateButton.setMaximumSize(new Dimension(220, 40));

            statusLabel.setAlignmentX(CENTER_ALIGNMENT);
            statusLabel.setFont(statusLabel.getFont().deriveFont(13f));
            statusLabel.setForeground(ERROR_COLOR);
            statusLabel.setVisible(false);

            JLabel questionCountLabel = new JLabel("Number of Questions", SwingConstants.CENTER);
            // questionCountLabel.setAlignmentX(CENTER_ALIGNMENT);
            questionCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 70, 0, 0));  // move 40 px right


            questionCountLabel.setForeground(MUTED_TEXT_COLOR);
            questionCountLabel.setFont(SUBTITLE_FONT);

            JLabel difficultyLabel = new JLabel("Difficulty", SwingConstants.CENTER);
            difficultyLabel.setAlignmentX(CENTER_ALIGNMENT);
            difficultyLabel.setForeground(MUTED_TEXT_COLOR);
            difficultyLabel.setFont(SUBTITLE_FONT);

            JLabel tipsLabel = new JLabel(
                    "<html><div style='text-align:center;color:#63748a;'>"
                            + "Quizzes are generated by the Gemini API, so every request gives you fresh questions."
                            + "</div></html>",
                    SwingConstants.CENTER);
            tipsLabel.setAlignmentX(CENTER_ALIGNMENT);
            tipsLabel.setForeground(MUTED_TEXT_COLOR);
            tipsLabel.setFont(tipsLabel.getFont().deriveFont(13f));

            JPanel userRow = new JPanel();
            userRow.setLayout(new BoxLayout(userRow, BoxLayout.X_AXIS));
            userRow.setOpaque(false);
            userRow.setAlignmentX(LEFT_ALIGNMENT);

            userLabel.setAlignmentX(LEFT_ALIGNMENT);
            userLabel.setForeground(MUTED_TEXT_COLOR);
            userLabel.setFont(SUBTITLE_FONT);

            styleSecondaryButton(historyButton);
            historyButton.setAlignmentX(RIGHT_ALIGNMENT);
            historyButton.setFocusable(false);
            historyButton.setMaximumSize(new Dimension(160, 36));
            historyButton.addActionListener(e -> {
                historyPanel.refresh();
                cardLayout.show(cardContainer, CARD_HISTORY);
            });

            styleSecondaryButton(logoutButton);
            logoutButton.setAlignmentX(RIGHT_ALIGNMENT);
            logoutButton.setFocusable(false);
            logoutButton.setMaximumSize(new Dimension(140, 36));
            logoutButton.addActionListener(e -> logoutCurrentUser());

            userRow.add(userLabel);
            userRow.add(Box.createHorizontalGlue());
            userRow.add(historyButton);
            userRow.add(Box.createHorizontalStrut(8));
            userRow.add(logoutButton);

            card.add(userRow);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(title);
            card.add(Box.createRigidArea(new Dimension(0, 8)));
            card.add(subtitle);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(topicField);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
            card.add(questionCountLabel);
            card.add(Box.createRigidArea(new Dimension(0, 6)));
            card.add(questionCountCombo);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
            card.add(difficultyLabel);
            card.add(Box.createRigidArea(new Dimension(0, 6)));
            card.add(difficultyCombo);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(generateButton);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(tipsLabel);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(statusLabel);

            add(Box.createVerticalGlue());
            add(card);
            add(Box.createVerticalGlue());

            topicField.addActionListener(this::onGenerate);
            updateUser(null);
        }

        private void onGenerate(ActionEvent event) {
            statusLabel.setText(" ");
            if (currentUser == null) {
                statusLabel.setText("Please sign in to start a quiz.");
                statusLabel.setForeground(ERROR_COLOR);
                statusLabel.setVisible(true);
                cardLayout.show(cardContainer, CARD_LOGIN);
                return;
            }
            String topic = topicField.getText().trim();
            if (topic.isEmpty()) {
                statusLabel.setText("Please enter a topic to continue.");
                statusLabel.setForeground(ERROR_COLOR);
                statusLabel.setVisible(true);
                return;
            }
            Integer count = (Integer) questionCountCombo.getSelectedItem();
            String difficultySelection = (String) difficultyCombo.getSelectedItem();
            String difficulty = difficultySelection != null ? difficultySelection.toLowerCase() : "medium";
            requestQuiz(topic, count != null ? count : QUESTION_COUNT_OPTIONS[0], difficulty);
        }

        void setLoading(boolean loading) {
            generateButton.setEnabled(!loading);
            topicField.setEnabled(!loading);
            questionCountCombo.setEnabled(!loading);
            difficultyCombo.setEnabled(!loading);
            if (loading) {
                statusLabel.setForeground(ACCENT_COLOR.darker());
                statusLabel.setText("Contacting AI backend...");
                statusLabel.setVisible(true);
            } else {
                statusLabel.setVisible(false);
            }
        }

        void showError(String message) {
            statusLabel.setText(message);
            statusLabel.setForeground(ERROR_COLOR);
            statusLabel.setVisible(true);
            JOptionPane.showMessageDialog(this, message, "Quiz Error", JOptionPane.ERROR_MESSAGE);
        }

        void reset() {
            topicField.setText("");
            statusLabel.setText(" ");
            generateButton.setEnabled(true);
            topicField.setEnabled(true);
            questionCountCombo.setSelectedItem(QUESTION_COUNT_OPTIONS[0]);
            difficultyCombo.setSelectedItem("Medium");
            questionCountCombo.setEnabled(true);
            difficultyCombo.setEnabled(true);
            statusLabel.setVisible(false);
        }

        void updateUser(String username) {
            if (username == null || username.isBlank()) {
                userLabel.setText("Not signed in.");
                historyButton.setEnabled(false);
                logoutButton.setEnabled(false);
            } else {
                userLabel.setText("Signed in as " + username);
                historyButton.setEnabled(true);
                logoutButton.setEnabled(true);
            }
        }
    }

    private final class HistoryPanel extends JPanel {
        private final DefaultListModel<String> historyModel = new DefaultListModel<>();
        private final JList<String> historyList = new JList<>(historyModel);
        private final JButton backButton = new JButton("Back to Home");

        HistoryPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            JLabel title = new JLabel("Past Quizzes");
            title.setFont(TITLE_FONT);
            title.setAlignmentX(CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel("Track how you've performed over time.", SwingConstants.CENTER);
            subtitle.setFont(SUBTITLE_FONT);
            subtitle.setForeground(MUTED_TEXT_COLOR);
            subtitle.setAlignmentX(CENTER_ALIGNMENT);

            historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            historyList.setVisibleRowCount(10);
            historyList.setFont(historyList.getFont().deriveFont(14f));
            historyList.setFocusable(false);
            historyList.setFixedCellHeight(36);

            JScrollPane scrollPane = new JScrollPane(historyList);
            scrollPane.setAlignmentX(LEFT_ALIGNMENT);
            scrollPane.setBorder(BorderFactory.createLineBorder(SURFACE_BORDER));

            styleSecondaryButton(backButton);
            backButton.setAlignmentX(CENTER_ALIGNMENT);
            backButton.addActionListener(e -> cardLayout.show(cardContainer, CARD_HOME));

            card.add(title);
            card.add(Box.createRigidArea(new Dimension(0, 8)));
            card.add(subtitle);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(scrollPane);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
            card.add(backButton);

            add(Box.createVerticalGlue());
            add(card);
            add(Box.createVerticalGlue());
        }

        void refresh() {
            historyModel.clear();
            if (currentUser == null) {
                historyModel.addElement("Sign in to view your quiz history.");
                return;
            }
            List<QuizHistoryStore.QuizRecord> records = historyStore.loadForUser(currentUser);
            if (records.isEmpty()) {
                historyModel.addElement("No quizzes completed yet.");
                return;
            }
            for (QuizHistoryStore.QuizRecord record : records) {
                String topicLabel = record.topic == null || record.topic.isBlank() ? "Custom Quiz" : record.topic;
                String difficultyLabel = capitalize(record.difficulty);
                String line = String.format(
                        "%s • %s (%s) • %d/%d correct (%d%%)",
                        formatTimestamp(record.timestamp),
                        topicLabel,
                        difficultyLabel,
                        record.correctCount,
                        record.total,
                        record.scorePercent
                );
                historyModel.addElement(line);
            }
        }

        private String formatTimestamp(String timestamp) {
            try {
                Instant instant = Instant.parse(timestamp);
                return HISTORY_TIME_FORMAT.format(instant);
            } catch (Exception ex) {
                return timestamp != null ? timestamp : "";
            }
        }
    }

    private final class QuizPanel extends JPanel {
        private final JLabel quizInfoLabel = new JLabel(" ", SwingConstants.LEFT);
        private final JLabel questionLabel = new JLabel("Question", SwingConstants.LEFT);
        private final JLabel progressLabel = new JLabel(" ", SwingConstants.RIGHT);
        private final JLabel difficultyBadge = createBadgeLabel("");
        private final JProgressBar progressBar = new JProgressBar();
        private final JRadioButton[] optionButtons = new JRadioButton[4];
        private final ButtonGroup optionGroup = new ButtonGroup();
        private final JButton nextButton = new JButton("Next");
        private final JButton submitButton = new JButton("Submit Quiz");
        private List<QuizQuestion> questions = new ArrayList<>();
        private int[] selections = new int[0];
        private int currentIndex = 0;
        private QuizService.QuizData quizData;

        QuizPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            quizInfoLabel.setAlignmentX(LEFT_ALIGNMENT);
            quizInfoLabel.setFont(quizInfoLabel.getFont().deriveFont(Font.BOLD, 18f));
            quizInfoLabel.setForeground(PRIMARY_COLOR.darker());

            questionLabel.setFont(questionLabel.getFont().deriveFont(Font.BOLD, 18f));
            questionLabel.setAlignmentX(LEFT_ALIGNMENT);

            progressLabel.setAlignmentX(RIGHT_ALIGNMENT);
            progressLabel.setForeground(MUTED_TEXT_COLOR);
            progressLabel.setFont(progressLabel.getFont().deriveFont(12f));

            difficultyBadge.setVisible(false);

            JPanel metaRow = new JPanel();
            metaRow.setLayout(new BoxLayout(metaRow, BoxLayout.X_AXIS));
            metaRow.setOpaque(false);
            metaRow.setAlignmentX(LEFT_ALIGNMENT);
            metaRow.add(quizInfoLabel);
            metaRow.add(Box.createHorizontalStrut(12));
            metaRow.add(difficultyBadge);
            metaRow.add(Box.createHorizontalGlue());
            metaRow.add(progressLabel);

            progressBar.setAlignmentX(LEFT_ALIGNMENT);
            progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            progressBar.setStringPainted(true);
            progressBar.setBorder(BorderFactory.createEmptyBorder());
            progressBar.setForeground(PRIMARY_COLOR);
            progressBar.setBackground(new Color(235, 240, 248));
            progressBar.setString(" ");

            card.add(metaRow);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(progressBar);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
            card.add(questionLabel);
            card.add(Box.createRigidArea(new Dimension(0, 18)));

            for (int i = 0; i < optionButtons.length; i++) {
                JRadioButton button = new JRadioButton("Option " + (i + 1));
                button.setAlignmentX(LEFT_ALIGNMENT);
                button.setFont(button.getFont().deriveFont(15f));
                int index = i;
                button.addActionListener(e -> onOptionSelected(index));
                optionButtons[i] = button;
                optionGroup.add(button);
                button.setOpaque(false);
                card.add(button);
                card.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            JPanel actions = new JPanel();
            actions.setAlignmentX(LEFT_ALIGNMENT);
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            nextButton.addActionListener(e -> goToNextQuestion());
            submitButton.addActionListener(e -> submitQuiz());
            submitButton.setVisible(false);
            styleSecondaryButton(nextButton);
            stylePrimaryButton(submitButton);
            actions.add(nextButton);
            actions.add(submitButton);

            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(actions);

            add(Box.createVerticalGlue());
            add(card);
            add(Box.createVerticalGlue());
        }

        void loadQuiz(QuizService.QuizData quizData) {
            this.quizData = quizData;
            this.questions = new ArrayList<>(quizData.getQuestions());
            this.selections = new int[this.questions.size()];
            Arrays.fill(this.selections, -1);
            this.currentIndex = 0;
            String topicLabel = quizData.getTopic().isEmpty() ? "Custom Quiz" : quizData.getTopic();
            String difficultyLabel = capitalize(quizData.getDifficulty());
            quizInfoLabel.setText(topicLabel);
            difficultyBadge.setText(difficultyLabel);
            difficultyBadge.setVisible(true);
            progressBar.setMaximum(this.questions.size());
            progressBar.setValue(0);
            showCurrentQuestion();
        }

        private void showCurrentQuestion() {
            QuizQuestion question = questions.get(currentIndex);
            questionLabel.setText("<html><body style='width: 450px'>" + question.getPrompt() + "</body></html>");
            progressLabel.setText(String.format("Question %d of %d", currentIndex + 1, questions.size()));
            optionGroup.clearSelection();

            List<String> options = question.getOptions();
            for (int i = 0; i < optionButtons.length; i++) {
                JRadioButton button = optionButtons[i];
                if (i < options.size()) {
                    button.setText(options.get(i));
                    button.setVisible(true);
                    button.setEnabled(true);
                } else {
                    button.setVisible(false);
                }
            }
            int previousSelection = selections[currentIndex];
            if (previousSelection >= 0 && previousSelection < optionButtons.length) {
                optionButtons[previousSelection].setSelected(true);
            }
            updateProgressIndicators();
            updateControls();
        }

        private void onOptionSelected(int selectedIndex) {
            selections[currentIndex] = selectedIndex;
            updateControls();
            updateProgressIndicators();
        }

        private void goToNextQuestion() {
            if (selections[currentIndex] == -1) {
                JOptionPane.showMessageDialog(this, "Please select an option before proceeding.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                showCurrentQuestion();
            }
        }

        private void submitQuiz() {
            if (selections[currentIndex] == -1) {
                JOptionPane.showMessageDialog(this, "Please select an option before submitting.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (quizData == null) {
                JOptionPane.showMessageDialog(this, "Quiz data not available. Please generate a new quiz.", "Quiz Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int correctCount = 0;
            for (int i = 0; i < questions.size(); i++) {
                if (selections[i] == questions.get(i).getCorrectIndex()) {
                    correctCount++;
                }
            }
            int[] selectionCopy = Arrays.copyOf(selections, selections.length);
            showResults(quizData, selectionCopy, correctCount);
        }

        private void updateControls() {
            boolean isLastQuestion = currentIndex == questions.size() - 1;
            submitButton.setVisible(isLastQuestion);
            nextButton.setVisible(!isLastQuestion);

            boolean hasSelection = selections[currentIndex] != -1 || optionGroup.getSelection() != null;
            nextButton.setEnabled(!isLastQuestion && hasSelection);
            submitButton.setEnabled(isLastQuestion && hasSelection);
        }

        void reset() {
            quizData = null;
            questions = new ArrayList<>();
            selections = new int[0];
            currentIndex = 0;
            quizInfoLabel.setText(" ");
            difficultyBadge.setVisible(false);
            questionLabel.setText("Question");
            progressLabel.setText(" ");
            progressBar.setMaximum(1);
            progressBar.setValue(0);
            progressBar.setString(" ");
            optionGroup.clearSelection();
            for (JRadioButton button : optionButtons) {
                button.setText("Option");
                button.setVisible(true);
                button.setEnabled(false);
            }
            nextButton.setEnabled(false);
            submitButton.setEnabled(false);
            submitButton.setVisible(false);
            nextButton.setVisible(true);
        }

        private void updateProgressIndicators() {
            if (selections.length == 0) {
                progressBar.setMaximum(1);
                progressBar.setValue(0);
                progressBar.setString(" ");
                progressLabel.setText(" ");
                return;
            }

            int answered = 0;
            for (int selection : selections) {
                if (selection >= 0) {
                    answered++;
                }
            }

            progressBar.setMaximum(selections.length);
            progressBar.setValue(answered);
            progressBar.setString(String.format("%d of %d answered", answered, selections.length));
            progressLabel.setText(String.format("Question %d of %d", currentIndex + 1, selections.length));
        }
    }

    private final class ResultPanel extends JPanel {
        private final JLabel summaryLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel metaLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel answersPanel = new JPanel();
        private final JScrollPane answersScroll;
        private final JButton newQuizButton = new JButton("Back to Home");

        ResultPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            JLabel title = new JLabel("Quiz Completed!", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setAlignmentX(CENTER_ALIGNMENT);

            summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, 22f));
            summaryLabel.setForeground(PRIMARY_COLOR.darker());
            summaryLabel.setAlignmentX(CENTER_ALIGNMENT);

            metaLabel.setAlignmentX(CENTER_ALIGNMENT);
            metaLabel.setForeground(MUTED_TEXT_COLOR);

            answersPanel.setLayout(new BoxLayout(answersPanel, BoxLayout.Y_AXIS));
            answersPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            answersPanel.setOpaque(false);
            answersScroll = new JScrollPane(answersPanel);
            answersScroll.setBorder(BorderFactory.createEmptyBorder());
            answersScroll.setAlignmentX(CENTER_ALIGNMENT);
            answersScroll.getVerticalScrollBar().setUnitIncrement(16);
            answersScroll.setPreferredSize(new Dimension(540, 260));
            answersScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
            answersScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            answersScroll.getViewport().setBackground(SURFACE_COLOR);

            newQuizButton.setAlignmentX(CENTER_ALIGNMENT);
            newQuizButton.addActionListener(e -> returnHome());
            stylePrimaryButton(newQuizButton);
            newQuizButton.setMaximumSize(new Dimension(220, 40));

            card.add(title);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(summaryLabel);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(metaLabel);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(answersScroll);
            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(newQuizButton);

            add(Box.createVerticalGlue());
            add(card);
            add(Box.createVerticalGlue());
        }

        void updateResults(QuizService.QuizData quizData, int[] selections, int correctCount) {
            List<QuizQuestion> questions = quizData.getQuestions();
            int totalQuestions = questions.size();
            int scorePercentage = totalQuestions == 0 ? 0 : Math.round((correctCount * 100f) / totalQuestions);

            summaryLabel.setText(String.format("Score: %d%% (%d/%d correct)", scorePercentage, correctCount, totalQuestions));

            String topicLabel = quizData.getTopic().isEmpty() ? "Custom Quiz" : quizData.getTopic();
            metaLabel.setText(String.format("Topic: %s • Difficulty: %s • Questions: %d",
                    topicLabel,
                    capitalize(quizData.getDifficulty()),
                    quizData.getQuestionCount()));

            answersPanel.removeAll();

            Color correctColor = new Color(0, 128, 0);
            Color incorrectColor = new Color(178, 34, 34);
            Color neutralColor = new Color(96, 96, 96);

            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion question = questions.get(i);
                List<String> options = question.getOptions();
                int correctIndex = question.getCorrectIndex();
                int selectedIndex = i < selections.length ? selections[i] : -1;

                String correctAnswer = correctIndex >= 0 && correctIndex < options.size()
                        ? options.get(correctIndex)
                        : "Unavailable";
                String userAnswer = selectedIndex >= 0 && selectedIndex < options.size()
                        ? options.get(selectedIndex)
                        : "No answer selected";

                boolean answeredCorrectly = selectedIndex == correctIndex && selectedIndex >= 0;

                JPanel entryPanel = new JPanel();
                entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.Y_AXIS));
                entryPanel.setAlignmentX(LEFT_ALIGNMENT);
                entryPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
                entryPanel.setOpaque(false);

                JLabel questionLabel = new JLabel(String.format("<html><body style='width: 520px'><b>%d.</b> %s</body></html>", i + 1, question.getPrompt()));
                questionLabel.setAlignmentX(LEFT_ALIGNMENT);
                questionLabel.setForeground(PRIMARY_COLOR.darker());

                JLabel userLabel = new JLabel("Your answer: " + userAnswer);
                userLabel.setAlignmentX(LEFT_ALIGNMENT);
                userLabel.setForeground(answeredCorrectly ? correctColor : (selectedIndex == -1 ? neutralColor : incorrectColor));

                JLabel correctLabel = new JLabel("Correct answer: " + correctAnswer);
                correctLabel.setAlignmentX(LEFT_ALIGNMENT);
                correctLabel.setForeground(correctColor.darker());

                entryPanel.add(questionLabel);
                entryPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                entryPanel.add(userLabel);
                entryPanel.add(Box.createRigidArea(new Dimension(0, 4)));
                entryPanel.add(correctLabel);

                answersPanel.add(entryPanel);
                if (i < questions.size() - 1) {
                    JSeparator separator = new JSeparator();
                    separator.setForeground(new Color(230, 234, 240));
                    answersPanel.add(separator);
                    answersPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
            }

            answersPanel.revalidate();
            answersPanel.repaint();
            answersScroll.getVerticalScrollBar().setValue(0);
        }

        void reset() {
            summaryLabel.setText("");
            metaLabel.setText("");
            answersPanel.removeAll();
            answersPanel.revalidate();
            answersPanel.repaint();
            answersScroll.getVerticalScrollBar().setValue(0);
        }
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE_COLOR);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE_BORDER),
                BorderFactory.createEmptyBorder(32, 32, 32, 32)
        ));
        return panel;
    }

    private JLabel createBadgeLabel(String text) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(new Color(228, 237, 251));
        badge.setForeground(PRIMARY_COLOR.darker());
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 12f));
        badge.setAlignmentX(LEFT_ALIGNMENT);
        return badge;
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR.darker(), 1, true),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(SURFACE_COLOR);
        button.setForeground(PRIMARY_COLOR);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 1, true),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to default look and feel if system L&F is unavailable
            }
            AIQuizMaster frame = new AIQuizMaster();
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
