import express from "express";
import cors from "cors";
import quizRouter from "./routes/quizRoutes.js";

const createApp = () => {
  const app = express();

  app.use(cors());
  app.use(express.json({ limit: "1mb" }));

  app.get("/health", (_req, res) => {
    res.json({ status: "ok" });
  });

  app.use("/api/quiz", quizRouter);

  app.use((err, _req, res, _next) => {
    // eslint-disable-next-line no-console
    console.error(err);
    const status = err.status || 500;
    res.status(status).json({
      error: err.message || "Internal server error"
    });
  });

  return app;
};

export default createApp;

