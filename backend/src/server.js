import "dotenv/config";
import mongoose from "mongoose";
import createApp from "./app.js";

const {
  PORT = 3000,
  MONGODB_URI,
  NODE_ENV = "development"
} = process.env;

if (!MONGODB_URI) {
  throw new Error("MONGODB_URI environment variable is required");
}

mongoose.set("strictQuery", true);

const app = createApp();

const startServer = async () => {
  try {
    await mongoose.connect(MONGODB_URI);
    // eslint-disable-next-line no-console
    console.log("Connected to MongoDB");

    app.listen(PORT, () => {
      // eslint-disable-next-line no-console
      console.log(`AI QuizMaster backend running on port ${PORT} [${NODE_ENV}]`);
    });
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error("Failed to start server:", error);
    process.exit(1);
  }
};

startServer();

