import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AIQuizMaster extends JFrame {

    private static final String CARD_HOME = "HOME";
    private static final String CARD_QUIZ = "QUIZ";
    private static final String CARD_RESULTS = "RESULTS";
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

    private final QuizService quizService = new QuizService();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);
    private final HomePanel homePanel = new HomePanel();
    private final QuizPanel quizPanel = new QuizPanel();
    private final ResultPanel resultPanel = new ResultPanel();

    public AIQuizMaster() {
        super("AI QuizMaster");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 480));
        getContentPane().setBackground(BACKGROUND_COLOR);
        cardContainer.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        cardContainer.setBackground(BACKGROUND_COLOR);
        cardContainer.setOpaque(true);

        cardContainer.add(homePanel, CARD_HOME);
        cardContainer.add(quizPanel, CARD_QUIZ);
        cardContainer.add(resultPanel, CARD_RESULTS);

        setContentPane(cardContainer);
    }

    private void requestQuiz(String topic, int questionCount, String difficulty) {
        homePanel.setLoading(true);
        new QuizFetchWorker(topic, questionCount, difficulty).execute();
    }

    private void startQuiz(QuizService.QuizData quizData) {
        quizPanel.loadQuiz(quizData);
        cardLayout.show(cardContainer, CARD_QUIZ);
    }

    private void showResults(QuizService.QuizData quizData, int[] selections, int correctCount) {
        resultPanel.updateResults(quizData, selections, correctCount);
        cardLayout.show(cardContainer, CARD_RESULTS);
    }

    private void returnHome() {
        homePanel.reset();
        quizPanel.reset();
        resultPanel.reset();
        cardLayout.show(cardContainer, CARD_HOME);
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

    private final class HomePanel extends JPanel {
        private final JTextField topicField = new JTextField();
        private final JComboBox<Integer> questionCountCombo = new JComboBox<>(QUESTION_COUNT_OPTIONS);
        private final JComboBox<String> difficultyCombo = new JComboBox<>(DIFFICULTY_OPTIONS);
        private final JButton generateButton = new JButton("Generate Quiz");
        private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

        HomePanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            setOpaque(true);
            setBackground(BACKGROUND_COLOR);

            JPanel card = createCardPanel();
            card.setAlignmentX(CENTER_ALIGNMENT);

            JLabel title = new JLabel("AI QuizMaster");
            title.setFont(TITLE_FONT);
            title.setAlignmentX(CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel("Describe a topic and tailor the quiz to your preference.");
            subtitle.setFont(SUBTITLE_FONT);
            subtitle.setAlignmentX(CENTER_ALIGNMENT);
            subtitle.setForeground(MUTED_TEXT_COLOR);

            topicField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            topicField.setAlignmentX(CENTER_ALIGNMENT);
            topicField.setFont(topicField.getFont().deriveFont(16f));
            topicField.setToolTipText("Example: Machine Learning, World War II, Football");
            topicField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            questionCountCombo.setAlignmentX(CENTER_ALIGNMENT);
            questionCountCombo.setMaximumSize(new Dimension(200, 32));
            questionCountCombo.setSelectedItem(QUESTION_COUNT_OPTIONS[0]);
            questionCountCombo.setFocusable(false);
            questionCountCombo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            difficultyCombo.setAlignmentX(CENTER_ALIGNMENT);
            difficultyCombo.setMaximumSize(new Dimension(200, 32));
            difficultyCombo.setSelectedItem("Medium");
            difficultyCombo.setFocusable(false);
            difficultyCombo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SURFACE_BORDER),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
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
            questionCountLabel.setAlignmentX(CENTER_ALIGNMENT);
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
        }

        private void onGenerate(ActionEvent event) {
            statusLabel.setText(" ");
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
