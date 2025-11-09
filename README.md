# AI QuizMaster

AI QuizMaster is a small quiz generator that pairs a desktop Swing client with a Node.js backend. Users can register or sign in locally, request Gemini-powered quizzes on any topic, take them inside the app, and review their personal history.

## Features
- Topic-based quiz generation with selectable difficulty and question count (5–10).
- Gemini API creates fresh multiple-choice questions stored in MongoDB.
- Java Swing desktop client with login, quiz, results, and history views.
- Local flat-file storage (`user-data/`) keeps hashed credentials and a per-user score log.

## Project Structure
- `backend/` – Express + MongoDB API that talks to Gemini and persists quizzes/results.
- `src/` – Java desktop client (AIQuizMaster) that consumes the backend.
- `user-data/` – Runtime directory the client uses for credentials (`users.txt`) and history (`history.txt`).

## Prerequisites
- Node.js 18+ and npm.
- Java 17+ (the client uses the built-in `java.net.http` client).
- MongoDB instance (local or Atlas connection string).
- Google Gemini API key with access to `gemini-2.5-flash` (configurable).

## Backend Setup
1. `cd backend`
2. Install dependencies: `npm install`
3. Create `.env` (or set env vars) with at least:
   ```
   PORT=3000
   MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>/quiz
   GEMINI_API_KEY=your_google_api_key
   GEMINI_MODEL=gemini-2.5-flash        # optional override
   QUIZ_MIN_QUESTIONS=5                 # optional
   QUIZ_MAX_QUESTIONS=10                # optional
   ```
4. Start the server: `npm run dev` (nodemon) or `npm start`.
5. Verify health check at `http://localhost:3000/health`.

## Desktop Client Setup
1. From the repo root, compile the sources:
   ```
   mkdir -p out
   javac -d out src/*.java
   ```
2. Run the app (expects the backend on `http://localhost:3000`):
   ```
   java -cp out AIQuizMaster
   ```
3. Register a user inside the app (stored locally in `user-data/users.txt`), generate quizzes, and view results/history.

> Tip: keep the backend running while you use the client; quiz generation fails if the API or Gemini key is missing.

## Troubleshooting
- **Quiz generation fails immediately** – Confirm the backend is running, `MONGODB_URI` is reachable, and `GEMINI_API_KEY` is valid.
- **Login not persisted** – Ensure the client process can write to `user-data/`. Delete `user-data/users.txt` only if you want a clean slate.
- **Connection refused** – Adjust `QuizService.BASE_URL` in `src/QuizService.java` if you run the backend on another host/port.
