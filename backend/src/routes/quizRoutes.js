import { Router } from "express";
import Quiz from "../models/Quiz.js";
import Result from "../models/Result.js";
import { generateQuizQuestions } from "../services/geminiService.js";

const MIN_QUESTIONS = Number(process.env.QUIZ_MIN_QUESTIONS ?? 5);
const MAX_QUESTIONS = Number(process.env.QUIZ_MAX_QUESTIONS ?? 10);
const DIFFICULTY_LEVELS = ["easy", "medium", "hard"];

const router = Router();

router.post("/generate", async (req, res, next) => {
  try {
    const { topic, questionCount, difficulty } = req.body || {};
    if (!topic || typeof topic !== "string" || !topic.trim()) {
      return res.status(400).json({ error: "Topic is required" });
    }

    let finalCount;
    if (questionCount === undefined || questionCount === null || questionCount === "") {
      finalCount = MIN_QUESTIONS;
    } else {
      const numericCount = Number(questionCount);
      if (!Number.isInteger(numericCount)) {
        return res.status(400).json({ error: "questionCount must be an integer" });
      }
      finalCount = numericCount;
    }
    if (finalCount < MIN_QUESTIONS || finalCount > MAX_QUESTIONS) {
      return res.status(400).json({ error: `questionCount must be between ${MIN_QUESTIONS} and ${MAX_QUESTIONS}` });
    }

    const normalizedDifficulty = typeof difficulty === "string" ? difficulty.toLowerCase() : "medium";
    if (!DIFFICULTY_LEVELS.includes(normalizedDifficulty)) {
      return res.status(400).json({ error: `difficulty must be one of: ${DIFFICULTY_LEVELS.join(", ")}` });
    }

    const generatedQuestions = await generateQuizQuestions(topic.trim(), finalCount, normalizedDifficulty);

    const quiz = await Quiz.create({
      topic: topic.trim(),
      difficulty: normalizedDifficulty,
      questionCount: finalCount,
      questions: generatedQuestions.map((q) => ({
        question: q.question,
        options: q.options,
        correctAnswer: q.correctAnswer
      }))
    });

    const questionsForClient = quiz.questions.map((question) => {
      const answerIndex = question.options.findIndex((option) => option === question.correctAnswer);
      if (answerIndex === -1) {
        throw new Error("Stored quiz question is missing the correct answer in options");
      }
      return {
        question: question.question,
        options: question.options,
        answerIndex
      };
    });

    res.status(201).json({
      quizId: quiz._id,
      topic: quiz.topic,
      difficulty: quiz.difficulty,
      questionCount: quiz.questionCount,
      questions: questionsForClient,
      createdAt: quiz.createdAt
    });
  } catch (error) {
    next(error);
  }
});

router.post("/submit", async (req, res, next) => {
  try {
    const { quizId, answers, user } = req.body || {};
    if (!quizId) {
      return res.status(400).json({ error: "quizId is required" });
    }
    if (!Array.isArray(answers)) {
      return res.status(400).json({ error: "answers must be an array of selected option indices" });
    }

    const quiz = await Quiz.findById(quizId);
    if (!quiz) {
      return res.status(404).json({ error: "Quiz not found" });
    }

    const total = quiz.questions.length;
    if (answers.length !== total) {
      return res.status(400).json({ error: `answers array must contain ${total} items` });
    }

    const correctAnswerIndices = quiz.questions.map((question) =>
      question.options.findIndex((option) => option === question.correctAnswer)
    );

    let correctCount = 0;
    quiz.questions.forEach((question, index) => {
      const selectedIndex = answers[index];
      if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= question.options.length) {
        return;
      }
      const selectedOption = question.options[selectedIndex];
      if (selectedOption === question.correctAnswer) {
        correctCount += 1;
      }
    });

    const score = Math.round((correctCount / total) * 100);

    const resultDocument = await Result.create({
      user: user || undefined,
      quizId,
      topic: quiz.topic,
      difficulty: quiz.difficulty,
      score,
      correctCount,
      total,
      answers
    });

    res.status(201).json({
      score,
      correctCount,
      total,
      resultId: resultDocument._id,
      correctAnswers: correctAnswerIndices
    });
  } catch (error) {
    next(error);
  }
});

export default router;
