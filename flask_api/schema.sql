-- Create Database
CREATE DATABASE IF NOT EXISTS flashlearn_db;
USE flashlearn_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Modules Table
CREATE TABLE IF NOT EXISTS modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'Ready',
    flashcard_count INT DEFAULT 0
);

-- 3. Flashcards/Quiz Cards Table
CREATE TABLE IF NOT EXISTS flashcards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    module_id INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    question TEXT NOT NULL,
    options TEXT NOT NULL, -- Stored as comma-separated values or JSON
    correct_index INT NOT NULL,
    FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE
);

-- 4. User Statistics Table
CREATE TABLE IF NOT EXISTS user_stats (
    user_id INT PRIMARY KEY,
    mastered_percentage INT DEFAULT 0,
    needs_review_count INT DEFAULT 0,
    last_reviewed_question TEXT,
    last_reviewed_hint TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- --- SEED DATA ---
-- Seed Modules
INSERT INTO modules (id, title, status, flashcard_count) VALUES
(1, 'Fundamentals of Biology', 'Ready', 3),
(2, 'World History 101', 'In Progress', 3),
(3, 'Modern Art', 'Ready', 3)
ON DUPLICATE KEY UPDATE title=VALUES(title), status=VALUES(status), flashcard_count=VALUES(flashcard_count);

-- Seed Flashcards (options are separated by '|||')
INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES
(1, 1, 'FUNDAMENTALS', 'What is the powerhouse of the cell?', 'Nucleus|||Mitochondria|||Ribosome|||Endoplasmic Reticulum', 1),
(2, 1, 'BIOLOGY', 'Which pigment gives plants their green color?', 'Carotene|||Chlorophyll|||Xanthophyll|||Anthocyanin', 1),
(3, 1, 'BIOLOGY', 'How many chromosomes do humans have?', '23|||44|||46|||48', 2),

(4, 2, 'GEOGRAPHY', 'What is the capital of the Byzantine Empire?', 'Rome|||Athens|||Constantinople|||Alexandria', 2),
(5, 2, 'HISTORY', 'Who was the first President of the United States?', 'Thomas Jefferson|||George Washington|||John Adams|||Benjamin Franklin', 1),
(6, 2, 'HISTORY', 'In which year did World War II end?', '1918|||1939|||1945|||1950', 2),

(7, 3, 'ART HISTORY', 'Who painted ''The Starry Night''?', 'Claude Monet|||Vincent van Gogh|||Leonardo da Vinci|||Pablo Picasso', 1),
(8, 3, 'ART', 'Which artistic movement is Salvador Dali associated with?', 'Impressionism|||Surrealism|||Cubism|||Expressionism', 1),
(9, 3, 'ART', 'Who sculpted the famous statue of ''David''?', 'Michelangelo|||Donatello|||Leonardo da Vinci|||Raphael', 0)
ON DUPLICATE KEY UPDATE category=VALUES(category), question=VALUES(question), options=VALUES(options), correct_index=VALUES(correct_index);
