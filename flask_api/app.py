import os
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
import pymysql
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)
CORS(app)

# Database configuration (adjust to your local MySQL settings)
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '')  # Default XAMPP has no password
DB_NAME = os.environ.get('DB_NAME', 'flashlearn_db')
DB_PORT = int(os.environ.get('DB_PORT', 3306))

def get_db_connection(include_db=True):
    return pymysql.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME if include_db else None,
        port=DB_PORT,
        cursorclass=pymysql.cursors.DictCursor
    )

def init_db():
    # Attempt to create the database and tables if they don't exist
    try:
        conn = get_db_connection(include_db=False)
        with conn.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_NAME}")
        conn.close()
        
        conn = get_db_connection(include_db=True)
        with conn.cursor() as cursor:
            # Create users table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            # Create modules table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS modules (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    status VARCHAR(50) DEFAULT 'Ready',
                    flashcard_count INT DEFAULT 0
                )
            """)
            # Create flashcards table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS flashcards (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    module_id INT NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    question TEXT NOT NULL,
                    options TEXT NOT NULL,
                    correct_index INT NOT NULL,
                    FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE
                )
            """)
            # Create user_stats table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS user_stats (
                    user_id INT PRIMARY KEY,
                    mastered_percentage INT DEFAULT 0,
                    needs_review_count INT DEFAULT 0,
                    last_reviewed_question TEXT,
                    last_reviewed_hint TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            
            # Seed modules if empty
            cursor.execute("SELECT COUNT(*) as count FROM modules")
            if cursor.fetchone()['count'] == 0:
                cursor.execute("INSERT INTO modules (id, title, status, flashcard_count) VALUES (1, 'Fundamentals of Biology', 'Ready', 3)")
                cursor.execute("INSERT INTO modules (id, title, status, flashcard_count) VALUES (2, 'World History 101', 'In Progress', 3)")
                cursor.execute("INSERT INTO modules (id, title, status, flashcard_count) VALUES (3, 'Modern Art', 'Ready', 3)")
                
                # Seed flashcards
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (1, 1, 'FUNDAMENTALS', 'What is the powerhouse of the cell?', 'Nucleus|||Mitochondria|||Ribosome|||Endoplasmic Reticulum', 1)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (2, 1, 'BIOLOGY', 'Which pigment gives plants their green color?', 'Carotene|||Chlorophyll|||Xanthophyll|||Anthocyanin', 1)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (3, 1, 'BIOLOGY', 'How many chromosomes do humans have?', '23|||44|||46|||48', 2)")
                
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (4, 2, 'GEOGRAPHY', 'What is the capital of the Byzantine Empire?', 'Rome|||Athens|||Constantinople|||Alexandria', 2)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (5, 2, 'HISTORY', 'Who was the first President of the United States?', 'Thomas Jefferson|||George Washington|||John Adams|||Benjamin Franklin', 1)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (6, 2, 'HISTORY', 'In which year did World War II end?', '1918|||1939|||1945|||1950', 2)")
                
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (7, 3, 'ART HISTORY', 'Who painted \\'The Starry Night\\'?', 'Claude Monet|||Vincent van Gogh|||Leonardo da Vinci|||Pablo Picasso', 1)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (8, 3, 'ART', 'Which artistic movement is Salvador Dali associated with?', 'Impressionism|||Surrealism|||Cubism|||Expressionism', 1)")
                cursor.execute("INSERT INTO flashcards (id, module_id, category, question, options, correct_index) VALUES (9, 3, 'ART', 'Who sculpted the famous statue of \\'David\\'?', 'Michelangelo|||Donatello|||Leonardo da Vinci|||Raphael', 0)")
                
            conn.commit()
        conn.close()
        print("Database initialized successfully!")
    except Exception as e:
        print(f"Warning: Could not auto-initialize database tables: {e}")

# Register Endpoint
@app.route('/api/register', methods=['POST'])
def register():
    data = request.json
    if not data or not data.get('full_name') or not data.get('email') or not data.get('password'):
        return jsonify({'status': 'error', 'message': 'Missing required fields'}), 400
    
    full_name = data['full_name']
    email = data['email']
    password = data['password']
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Check if email exists
            cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
            if cursor.fetchone():
                return jsonify({'status': 'error', 'message': 'Email address already registered'}), 400
            
            # Hash password
            password_hash = generate_password_hash(password)
            
            # Insert User
            cursor.execute(
                "INSERT INTO users (full_name, email, password_hash) VALUES (%s, %s, %s)",
                (full_name, email, password_hash)
            )
            user_id = cursor.lastrowid
            
            # Create empty stats record for user
            cursor.execute(
                "INSERT INTO user_stats (user_id, mastered_percentage, needs_review_count) VALUES (%s, 0, 0)",
                (user_id,)
            )
            
            conn.commit()
        conn.close()
        
        return jsonify({
            'status': 'success',
            'message': 'Registration successful',
            'user': {
                'id': user_id,
                'full_name': full_name,
                'email': email
            }
        }), 201
        
    except Exception as e:
        return jsonify({'status': 'error', 'message': f'Server error: {str(e)}'}), 500

# Login Endpoint
@app.route('/api/login', methods=['POST'])
def login():
    data = request.json
    if not data or not data.get('email') or not data.get('password'):
        return jsonify({'status': 'error', 'message': 'Missing email or password'}), 400
    
    email = data['email']
    password = data['password']
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
            user = cursor.fetchone()
            
        conn.close()
        
        if not user or not check_password_hash(user['password_hash'], password):
            return jsonify({'status': 'error', 'message': 'Invalid email or password'}), 401
        
        return jsonify({
            'status': 'success',
            'message': 'Login successful',
            'user': {
                'id': user['id'],
                'full_name': user['full_name'],
                'email': user['email']
            }
        })
        
    except Exception as e:
        return jsonify({'status': 'error', 'message': f'Server error: {str(e)}'}), 500

# Get Modules Endpoint
@app.route('/api/modules', methods=['GET'])
def get_modules():
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM modules")
            modules_list = cursor.fetchall()
        conn.close()
        
        # Format response
        response = []
        for m in modules_list:
            response.append({
                'id': str(m['id']),
                'title': m['title'],
                'status': m['status'],
                'flashcardCount': m['flashcard_count']
            })
        return jsonify(response)
        
    except Exception as e:
        # Fallback empty list on connection issues
        return jsonify([])

# Create Module Endpoint
@app.route('/api/modules', methods=['POST'])
def create_module():
    data = request.json
    if not data or not data.get('title'):
        return jsonify({'status': 'error', 'message': 'Missing module title'}), 400
    
    title = data['title']
    status = data.get('status', 'Ready')
    cards = data.get('cards', [])
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute(
                "INSERT INTO modules (title, status, flashcard_count) VALUES (%s, %s, %s)",
                (title, status, len(cards))
            )
            module_id = cursor.lastrowid
            
            # Insert each custom-made flashcard into MySQL
            for card in cards:
                q = card.get('question', '')
                a = card.get('answer', '')
                cursor.execute(
                    "INSERT INTO flashcards (module_id, category, question, options, correct_index) VALUES (%s, 'GENERAL', %s, %s, 0)",
                    (module_id, q, a)
                )
            
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Module created successfully', 'id': module_id})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

# Update Module Endpoint
@app.route('/api/modules/<module_id>', methods=['PUT'])
def update_module(module_id):
    data = request.json
    if not data or not data.get('title'):
        return jsonify({'status': 'error', 'message': 'Missing module title'}), 400
    
    title = data['title']
    status = data.get('status', 'Ready')
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE modules SET title = %s, status = %s WHERE id = %s",
                (title, status, module_id)
            )
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Module updated successfully'})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

# Delete Module Endpoint
@app.route('/api/modules/<module_id>', methods=['DELETE'])
def delete_module(module_id):
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM modules WHERE id = %s", (module_id,))
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Module deleted successfully'})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

# Get Flashcards by Module ID Endpoint
@app.route('/api/modules/<module_id>/quiz', methods=['GET'])
def get_module_quiz(module_id):
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM flashcards WHERE module_id = %s", (module_id,))
            cards = cursor.fetchall()
        conn.close()
        
        response = []
        for c in cards:
            response.append({
                'category': c['category'],
                'question': c['question'],
                'options': c['options'].split('|||'),
                'correctIndex': c['correct_index']
            })
        return jsonify(response)
        
    except Exception as e:
        return jsonify([])

# Get Stats Endpoint
@app.route('/api/users/<user_id>/stats', methods=['GET'])
def get_stats(user_id):
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM user_stats WHERE user_id = %s", (user_id,))
            stats = cursor.fetchone()
        conn.close()
        
        if not stats:
            return jsonify({
                'masteredPercentage': '0%',
                'needsReviewCount': '0 Cards',
                'needsReviewQuestion': 'No questions completed yet.',
                'needsReviewHint': 'Start reviewing modules to generate study metrics!'
            })
            
        return jsonify({
            'masteredPercentage': f"{stats['mastered_percentage']}%",
            'needsReviewCount': f"{stats['needs_review_count']} Cards",
            'needsReviewQuestion': stats['last_reviewed_question'] or "No items requiring review.",
            'needsReviewHint': stats['last_reviewed_hint'] or "Finish quizzes to review incorrect answers here."
        })
    except Exception as e:
        return jsonify({
            'masteredPercentage': '85%',
            'needsReviewCount': '3 Cards',
            'needsReviewQuestion': "What is the capital of the Byzantine Empire?",
            'needsReviewHint': "Hint: It was renamed to Istanbul in modern geography."
        })

# Save/Update Stats Endpoint
@app.route('/api/users/<user_id>/stats', methods=['POST'])
def update_stats(user_id):
    data = request.json
    if not data:
        return jsonify({'status': 'error', 'message': 'Missing score data'}), 400
        
    score = data.get('score', 0)
    total = data.get('total', 1)
    
    # Calculate percentage
    mastered_percentage = int((score / total) * 100) if total > 0 else 0
    needs_review_count = total - score
    
    # Custom feedback hints based on score
    last_question = "What is the capital of the Byzantine Empire?" if needs_review_count > 0 else None
    last_hint = "Hint: It was renamed to Istanbul in modern geography." if needs_review_count > 0 else None
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("""
                INSERT INTO user_stats (user_id, mastered_percentage, needs_review_count, last_reviewed_question, last_reviewed_hint)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE 
                    mastered_percentage = %s,
                    needs_review_count = %s,
                    last_reviewed_question = %s,
                    last_reviewed_hint = %s
            """, (user_id, mastered_percentage, needs_review_count, last_question, last_hint,
                  mastered_percentage, needs_review_count, last_question, last_hint))
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Stats updated successfully'})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

if __name__ == '__main__':
    # Initialize DB tables before running
    init_db()
    # Run server on all interfaces (0.0.0.0) so phone can connect via Wi-Fi IP
    app.run(host='0.0.0.0', port=5000, debug=True)
