import mongoose from "mongoose";

const questionSchema = new mongoose.Schema(
  {
    question: {
      type: String,
      required: true,
      trim: true
    },
    options: {
      type: [String],
      validate: {
        validator: (arr) => Array.isArray(arr) && arr.length >= 2,
        message: "Each question must provide at least two options"
      },
      required: true
    },
    correctAnswer: {
      type: String,
      required: true,
      trim: true
    }
  },
  { _id: false }
);

const quizSchema = new mongoose.Schema(
  {
    topic: {
      type: String,
      required: true,
      trim: true
    },
    difficulty: {
      type: String,
      enum: ["easy", "medium", "hard"],
      default: "medium"
    },
    questionCount: {
      type: Number,
      min: 1,
      required: true
    },
    questions: {
      type: [questionSchema],
      validate: {
        validator: (arr) => Array.isArray(arr) && arr.length >= 1,
        message: "Quiz must contain at least one question"
      },
      required: true
    }
  },
  {
    timestamps: { createdAt: "createdAt", updatedAt: false }
  }
);

const Quiz = mongoose.model("Quiz", quizSchema);

export default Quiz;
