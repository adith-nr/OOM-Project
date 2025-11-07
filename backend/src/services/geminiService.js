import { GoogleGenerativeAI } from "@google/generative-ai";
import { extractJsonFromResponse, ensureArrayLength } from "../utils/jsonUtils.js";

const { GEMINI_API_KEY, GEMINI_MODEL = "gemini-2.5-flash" } = process.env;

const DIFFICULTY_LEVELS = ["easy", "medium", "hard"];
const MIN_QUESTIONS = Number(process.env.QUIZ_MIN_QUESTIONS ?? 5);
const MAX_QUESTIONS = Number(process.env.QUIZ_MAX_QUESTIONS ?? 10);

let generativeModel = null;

if (GEMINI_API_KEY) {
  const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
  generativeModel = genAI.getGenerativeModel({ model: GEMINI_MODEL });
}

const quizPrompt = (topic, questionCount, difficulty) => `
Generate a multiple-choice quiz about "${topic}".
- The quiz must contain exactly ${questionCount} questions.
- Overall difficulty should be ${difficulty}.
- Each question must include exactly 4 answer options.
- Provide the index (0-based) of the correct option as "correctAnswerIndex".
- Respond strictly as minified JSON: { "questions": [ { "question": "...", "options": ["...","...","...","..."], "correctAnswerIndex": 1 }, ... ] }
`;

export const generateQuizQuestions = async (topic, questionCount = MIN_QUESTIONS, difficulty = "medium") => {
  if (!topic || !topic.trim()) {
    throw new Error("Topic is required to generate a quiz");
  }
  if (!generativeModel) {
    throw new Error("GEMINI_API_KEY is not configured");
  }

  const sanitizedTopic = topic.trim();
  const desiredCount = Number(questionCount);
  if (!Number.isInteger(desiredCount) || desiredCount < MIN_QUESTIONS || desiredCount > MAX_QUESTIONS) {
    throw new Error(`questionCount must be an integer between ${MIN_QUESTIONS} and ${MAX_QUESTIONS}`);
  }

  const normalizedDifficulty = String(difficulty || "medium").toLowerCase();
  if (!DIFFICULTY_LEVELS.includes(normalizedDifficulty)) {
    throw new Error(`difficulty must be one of: ${DIFFICULTY_LEVELS.join(", ")}`);
  }

  const response = await generativeModel.generateContent(
    quizPrompt(sanitizedTopic, desiredCount, normalizedDifficulty)
  );
  const text = response.response.text();
  const jsonPayload = extractJsonFromResponse(text);

  let parsed;
  try {
    parsed = JSON.parse(jsonPayload);
  } catch (error) {
    throw new Error("Gemini response was not valid JSON");
  }

  const questions = ensureArrayLength(parsed.questions, desiredCount, desiredCount);

  return questions.map((question, index) => {
    if (
      !question ||
      typeof question.question !== "string" ||
      !Array.isArray(question.options) ||
      question.options.length !== 4
    ) {
      throw new Error(`Question ${index + 1} is missing required fields`);
    }

    const correctAnswerIndex = Number(question.correctAnswerIndex);
    if (!Number.isInteger(correctAnswerIndex) || correctAnswerIndex < 0 || correctAnswerIndex >= question.options.length) {
      throw new Error(`Question ${index + 1} has an invalid correctAnswerIndex`);
    }

    const options = question.options.map((option) => String(option).trim());
    const correctAnswer = options[correctAnswerIndex];

    return {
      question: question.question.trim(),
      options,
      correctAnswer,
      correctAnswerIndex
    };
  });
};
