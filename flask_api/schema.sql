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
    streak_count INT DEFAULT 1,
    created_decks_count INT DEFAULT 0,
    perfect_scores_count INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Database tables are initialized and ready to receive your custom-made study modules!
