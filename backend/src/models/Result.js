import mongoose from "mongoose";

const resultSchema = new mongoose.Schema(
  {
    user: {
      type: String,
      default: () => process.env.QUIZ_DEFAULT_USER || "anonymous",
      trim: true
    },
    quizId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Quiz",
      required: true
    },
    score: {
      type: Number,
      required: true,
      min: 0
    },
    correctCount: {
      type: Number,
      required: true,
      min: 0
    },
    total: {
      type: Number,
      required: true,
      min: 1
    },
    difficulty: {
      type: String,
      enum: ["easy", "medium", "hard"],
      default: "medium"
    },
    topic: {
      type: String,
      trim: true
    },
    answers: {
      type: [Number],
      default: []
    },
    timestamp: {
      type: Date,
      default: () => new Date()
    }
  },
  {
    versionKey: false
  }
);

const Result = mongoose.model("Result", resultSchema);

export default Result;
