import os
import json
import datetime
import jwt
from flask import Flask, request, jsonify, render_template_string
from flask_cors import CORS
from flask_mail import Mail, Message
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import pymysql
from werkzeug.security import generate_password_hash, check_password_hash
from dotenv import load_dotenv
from google import genai
from google.genai import types

# Load environment variables from .env
load_dotenv()

# Configure Gemini Client
client = None
GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY')
if GEMINI_API_KEY:
    client = genai.Client(api_key=GEMINI_API_KEY)

app = Flask(__name__)
CORS(app)

# Set app secret key for signing JWTs
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'flashlearn-secure-key-9988')

# Initialize Flask-Limiter
limiter = Limiter(
    key_func=get_remote_address,
    app=app,
    default_limits=["200 per day", "50 per hour"],
    storage_uri="memory://"
)

@app.errorhandler(429)
def ratelimit_handler(e):
    return jsonify(status="error", message=f"Too many requests. Please try again later. Detail: {e.description}"), 429

# --- SMTP Configuration for Gmail ---
app.config['MAIL_SERVER'] = 'smtp.gmail.com'
app.config['MAIL_PORT'] = 587
app.config['MAIL_USE_TLS'] = True
app.config['MAIL_USE_SSL'] = False

# Pulls credentials safely from your .env file
app.config['MAIL_USERNAME'] = os.environ.get('MAIL_USERNAME')
app.config['MAIL_PASSWORD'] = os.environ.get('MAIL_PASSWORD')
app.config['MAIL_DEFAULT_SENDER'] = os.environ.get('MAIL_USERNAME')

# Initialize the Mail extension
mail = Mail(app)

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
            
            # Migration: Add streak_count, created_decks_count, and perfect_scores_count if not present
            try:
                cursor.execute("ALTER TABLE user_stats ADD COLUMN streak_count INT DEFAULT 1")
            except Exception:
                pass
            try:
                cursor.execute("ALTER TABLE user_stats ADD COLUMN created_decks_count INT DEFAULT 0")
            except Exception:
                pass
            try:
                cursor.execute("ALTER TABLE user_stats ADD COLUMN perfect_scores_count INT DEFAULT 0")
            except Exception:
                pass

            # Migration: Add user_id and share_code to modules if not present
            try:
                cursor.execute("ALTER TABLE modules ADD COLUMN user_id INT NULL")
            except Exception:
                pass
            try:
                cursor.execute("ALTER TABLE modules ADD COLUMN share_code VARCHAR(10) UNIQUE NULL")
            except Exception:
                pass

            # Migration: Add avatar column to users if not present
            try:
                cursor.execute("ALTER TABLE users ADD COLUMN avatar LONGTEXT NULL")
            except Exception:
                pass

            
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
    user_id = request.args.get('userId')
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            if user_id:
                cursor.execute("SELECT * FROM modules WHERE user_id IS NULL OR user_id = %s", (user_id,))
            else:
                cursor.execute("SELECT * FROM modules")
            modules_list = cursor.fetchall()
        conn.close()
        
        # Format response
        response = []
        conn_update = None
        for m in modules_list:
            share_code = m.get('share_code')
            if not share_code or share_code.strip() == '':
                # Auto-generate code for legacy modules that don't have one!
                if not conn_update:
                    conn_update = get_db_connection()
                with conn_update.cursor() as cursor_up:
                    share_code = generate_unique_share_code(cursor_up)
                    cursor_up.execute("UPDATE modules SET share_code = %s WHERE id = %s", (share_code, m['id']))
                conn_update.commit()
            
            response.append({
                'id': str(m['id']),
                'title': m['title'],
                'status': m['status'],
                'flashcardCount': m['flashcard_count'],
                'shareCode': share_code
            })
        if conn_update:
            conn_update.close()
        return jsonify(response)
        
    except Exception as e:
        # Fallback empty list on connection issues
        return jsonify([])


def generate_unique_share_code(cursor):
    import random
    import string
    while True:
        code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
        cursor.execute("SELECT id FROM modules WHERE share_code = %s", (code,))
        if not cursor.fetchone():
            return code

def increment_created_decks(cursor, user_id):
    if not user_id:
        return
    cursor.execute("SELECT user_id FROM user_stats WHERE user_id = %s", (user_id,))
    if cursor.fetchone():
        cursor.execute("UPDATE user_stats SET created_decks_count = created_decks_count + 1 WHERE user_id = %s", (user_id,))
    else:
        cursor.execute("INSERT INTO user_stats (user_id, created_decks_count) VALUES (%s, 1)", (user_id,))

# Create Module Endpoint
@app.route('/api/modules', methods=['POST'])
def create_module():
    data = request.json
    if not data or not data.get('title'):
        return jsonify({'status': 'error', 'message': 'Missing module title'}), 400
    
    title = data['title']
    status = data.get('status', 'Ready')
    cards = data.get('cards', [])
    user_id = data.get('userId')
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            share_code = generate_unique_share_code(cursor)
            cursor.execute(
                "INSERT INTO modules (title, status, flashcard_count, user_id, share_code) VALUES (%s, %s, %s, %s, %s)",
                (title, status, len(cards), user_id, share_code)
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
            
            # Increment user's created decks count
            if user_id:
                increment_created_decks(cursor, user_id)
            
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
    cards = data.get('cards')
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE modules SET title = %s, status = %s WHERE id = %s",
                (title, status, module_id)
            )
            
            if cards is not None:
                # Delete existing flashcards for this module
                cursor.execute("DELETE FROM flashcards WHERE module_id = %s", (module_id,))
                
                # Insert each flashcard
                for card in cards:
                    q = card.get('question', '').strip()
                    a = card.get('answer', '').strip()
                    cat = card.get('category', 'GENERAL').strip().upper()
                    if q and a:
                        cursor.execute(
                            "INSERT INTO flashcards (module_id, category, question, options, correct_index) VALUES (%s, %s, %s, %s, 0)",
                            (module_id, cat, q, a)
                        )
                # Update module count
                cursor.execute("UPDATE modules SET flashcard_count = %s WHERE id = %s", (len(cards), module_id))
                
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
                'needsReviewHint': 'Start reviewing modules to generate study metrics!',
                'streakCount': 1,
                'createdDecksCount': 0,
                'perfectScoresCount': 0
            })
            
        return jsonify({
            'masteredPercentage': f"{stats['mastered_percentage']}%",
            'needsReviewCount': f"{stats['needs_review_count']} Cards",
            'needsReviewQuestion': stats['last_reviewed_question'] or "No items requiring review.",
            'needsReviewHint': stats['last_reviewed_hint'] or "Finish quizzes to review incorrect answers here.",
            'streakCount': stats.get('streak_count', 1) or 1,
            'createdDecksCount': stats.get('created_decks_count', 0) or 0,
            'perfectScoresCount': stats.get('perfect_scores_count', 0) or 0
        })
    except Exception as e:
        return jsonify({
            'masteredPercentage': '0%',
            'needsReviewCount': '0 Cards',
            'needsReviewQuestion': "No questions completed yet.",
            'needsReviewHint': "Start reviewing modules to generate study metrics!",
            'streakCount': 0,
            'createdDecksCount': 0,
            'perfectScoresCount': 0
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
            # Get current stats
            cursor.execute("SELECT streak_count, perfect_scores_count FROM user_stats WHERE user_id = %s", (user_id,))
            row = cursor.fetchone()
            current_streak = row['streak_count'] if (row and row.get('streak_count') is not None) else 0
            current_perfect = row['perfect_scores_count'] if (row and row.get('perfect_scores_count') is not None) else 0
            
            new_streak = current_streak + 1
            new_perfect = current_perfect + (1 if score == total else 0)

            cursor.execute("""
                INSERT INTO user_stats (user_id, mastered_percentage, needs_review_count, last_reviewed_question, last_reviewed_hint, streak_count, perfect_scores_count)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE 
                    mastered_percentage = %s,
                    needs_review_count = %s,
                    last_reviewed_question = %s,
                    last_reviewed_hint = %s,
                    streak_count = %s,
                    perfect_scores_count = %s
            """, (user_id, mastered_percentage, needs_review_count, last_question, last_hint, new_streak, new_perfect,
                  mastered_percentage, needs_review_count, last_question, last_hint, new_streak, new_perfect))
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Stats updated successfully'})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

# User Profile Avatar Endpoint (Upload / Download Base64 representation)
@app.route('/api/users/<user_id>/avatar', methods=['GET', 'POST'])
def user_avatar(user_id):
    if request.method == 'POST':
        try:
            data = request.get_json() or {}
            avatar = data.get('avatar')
            conn = get_db_connection()
            with conn.cursor() as cursor:
                cursor.execute("UPDATE users SET avatar = %s WHERE id = %s", (avatar, user_id))
                conn.commit()
            conn.close()
            return jsonify({'status': 'success', 'message': 'Avatar updated successfully'}), 200
        except Exception as e:
            return jsonify({'status': 'error', 'message': str(e)}), 500
    else:
        try:
            conn = get_db_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT avatar FROM users WHERE id = %s", (user_id,))
                row = cursor.fetchone()
            conn.close()
            avatar = row['avatar'] if row else None
            return jsonify({'avatar': avatar}), 200
        except Exception as e:
            return jsonify({'status': 'error', 'message': str(e)}), 500



def extract_text_from_pdf(stream):
    import pypdf
    try:
        reader = pypdf.PdfReader(stream)
        text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + "\n"
        return text
    except Exception as e:
        raise ValueError(f"Failed to extract text from PDF: {str(e)}")

def extract_text_from_txt(stream):
    try:
        return stream.read().decode('utf-8', errors='ignore')
    except Exception as e:
        raise ValueError(f"Failed to read TXT file: {str(e)}")

def generate_quiz_from_text(text):
    global client
    if not client:
        # Check again if env key has been populated since server startup
        GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY')
        if GEMINI_API_KEY:
            client = genai.Client(api_key=GEMINI_API_KEY)
        else:
            raise ValueError("GEMINI_API_KEY is not configured on the server. Please define GEMINI_API_KEY in your environmental variables or .env file.")
    
    prompt = f"""
You are an expert educational study assistant. Analyze the document text provided below and generate a quiz with 10 to 15 identification questions.
Each question should be an identification question based on the content (e.g. asking for definitions, key facts, dates, names, or key terms).
The answer for each question must be a short, precise fact or phrase (no more than 3-4 words) that uniquely answers the question.

Provide your output strictly in JSON format as a list of objects. Each object must have these exact keys:
- "category": A 1-2 word string indicating the sub-topic or domain (e.g. "BIOLOGY", "HISTORY", "GENERAL").
- "question": The identification question string.
- "answer": The correct answer string.

Do not include any intro, outro, markdown block quotes, backticks or explanation. Just return raw JSON.

Here is the document text:
{text}
"""
    try:
        response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        return json.loads(response.text)
    except Exception as e:
        raise RuntimeError(f"Gemini API execution error: {str(e)}")

# Upload PDF/TXT and generate Quiz module via Gemini AI
@app.route('/api/upload', methods=['POST'])
def upload_document():
    user_id = request.form.get('userId')
    if 'file' not in request.files:
        print("Upload Error: 'file' key not found in request.files")
        return jsonify({'status': 'error', 'message': 'No file part in the request'}), 400
    
    file = request.files['file']
    if file.filename == '':
        print("Upload Error: Filename is empty")
        return jsonify({'status': 'error', 'message': 'No file selected for uploading'}), 400
    
    filename = file.filename
    ext = os.path.splitext(filename)[1].lower()
    
    if ext not in ['.txt', '.pdf']:
        print(f"Upload Error: Unsupported file extension '{ext}' for file '{filename}'")
        return jsonify({'status': 'error', 'message': 'Unsupported file type. Please upload a .txt or .pdf file'}), 400
    
    try:
        if ext == '.txt':
            text = extract_text_from_txt(file.stream)
        else:
            text = extract_text_from_pdf(file.stream)
            
        if not text.strip():
            print(f"Upload Error: No readable text extracted from file '{filename}'")
            return jsonify({'status': 'error', 'message': 'The uploaded file has no readable text content'}), 400
            
        # Call Gemini AI to generate the questions
        print(f"File '{filename}' text extracted successfully (Length: {len(text)}). Calling Gemini AI...")
        questions = generate_quiz_from_text(text)
        
        if not isinstance(questions, list) or len(questions) == 0:
            print("Upload Error: Gemini returned an empty question list")
            return jsonify({'status': 'error', 'message': 'Gemini generated an empty question list'}), 500
            
        num_cards = len(questions)
        module_title = os.path.splitext(filename)[0]
        
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Create a new module
            share_code = generate_unique_share_code(cursor)
            cursor.execute(
                "INSERT INTO modules (title, status, flashcard_count, user_id, share_code) VALUES (%s, 'Ready', %s, %s, %s)",
                (module_title, num_cards, user_id, share_code)
            )
            module_id = cursor.lastrowid

            
            # Create the flashcard entries
            for q_obj in questions:
                q = q_obj.get('question', '').strip()
                a = q_obj.get('answer', '').strip()
                cat = q_obj.get('category', 'GENERAL').strip().upper()
                
                if q and a:
                    cursor.execute(
                        "INSERT INTO flashcards (module_id, category, question, options, correct_index) VALUES (%s, %s, %s, %s, 0)",
                        (module_id, cat, q, a)
                    )
            
            # Increment user's created decks count
            if user_id:
                increment_created_decks(cursor, user_id)
                
            conn.commit()
        conn.close()

        
        print(f"Upload Success: Generated module ID {module_id} with {num_cards} cards for '{filename}'")
        return jsonify({
            'status': 'success',
            'message': f'Successfully generated {num_cards} identification questions from {filename}',
            'id': str(module_id)
        }), 201
        
    except ValueError as val_err:
        print(f"Upload Error: ValueError - {str(val_err)}")
        return jsonify({'status': 'error', 'message': str(val_err)}), 400
    except Exception as e:
        print(f"Upload Error: Server exception - {str(e)}")
        return jsonify({'status': 'error', 'message': f'Server error processing document: {str(e)}'}), 500

# Import Module via Share Code Endpoint
@app.route('/api/modules/import', methods=['POST'])
def import_module():
    data = request.json
    if not data or not data.get('shareCode'):
        return jsonify({'status': 'error', 'message': 'Missing share code'}), 400
        
    share_code = data['shareCode'].strip().upper()
    user_id = data.get('userId')
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Find the original module
            cursor.execute("SELECT * FROM modules WHERE share_code = %s", (share_code,))
            module = cursor.fetchone()
            
            if not module:
                conn.close()
                return jsonify({'status': 'error', 'message': 'Deck not found. Check the code and try again.'}), 404
                
            if module.get('user_id') and user_id and int(float(module.get('user_id'))) == int(float(user_id)):
                conn.close()
                return jsonify({'status': 'error', 'message': 'You cannot import your own deck.'}), 400
                
            # Create a cloned module for importing user
            new_code = generate_unique_share_code(cursor)
            cursor.execute(
                "INSERT INTO modules (title, status, flashcard_count, user_id, share_code) VALUES (%s, 'Ready', %s, %s, %s)",
                (module['title'], module['flashcard_count'], user_id, new_code)
            )
            new_module_id = cursor.lastrowid
            
            # Retrieve flashcards of original module
            cursor.execute("SELECT * FROM flashcards WHERE module_id = %s", (module['id'],))
            cards = cursor.fetchall()
            
            # Copy each card
            for card in cards:
                cursor.execute(
                    "INSERT INTO flashcards (module_id, category, question, options, correct_index) VALUES (%s, %s, %s, %s, %s)",
                    (new_module_id, card['category'], card['question'], card['options'], card['correct_index'])
                )
                
            # Increment user's created decks count
            if user_id:
                increment_created_decks(cursor, user_id)
                
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': 'Module imported successfully', 'id': str(new_module_id)})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# Generate AI Flashcards from Topic Endpoint
@app.route('/api/modules/generate-topic', methods=['POST'])
def generate_topic_deck():
    data = request.json
    if not data or not data.get('topic'):
        return jsonify({'status': 'error', 'message': 'Missing topic description'}), 400
        
    topic = data['topic'].strip()
    user_id = data.get('userId')
    
    global client
    if not client:
        # Check again if env key has been populated since server startup
        GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY')
        if GEMINI_API_KEY:
            client = genai.Client(api_key=GEMINI_API_KEY)
        else:
            return jsonify({'status': 'error', 'message': 'Gemini API is not configured on the server.'}), 500
            
    prompt = f"""
    You are an expert study assistant. Generate exactly 10 high-quality identification flashcards on the topic: "{topic}".
    Each flashcard must have a clear identification question and a short, precise answer (no more than 3-4 words).
    Provide your output strictly in JSON format as a list of objects. Each object must have these exact keys:
    - "category": A 1-2 word string indicating the sub-topic (e.g. "{topic.upper()[:15]}").
    - "question": The question text.
    - "answer": The correct answer text.
    
    Do not include any intro, outro, markdown block quotes, backticks or explanation. Just return raw JSON.
    """
    
    try:
        response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        questions = json.loads(response.text)
        
        if not isinstance(questions, list) or len(questions) == 0:
            return jsonify({'status': 'error', 'message': 'Gemini generated empty flashcards list'}), 500
            
        num_cards = len(questions)
        module_title = f"{topic} Quiz"
        
        conn = get_db_connection()
        with conn.cursor() as cursor:
            share_code = generate_unique_share_code(cursor)
            cursor.execute(
                "INSERT INTO modules (title, status, flashcard_count, user_id, share_code) VALUES (%s, 'Ready', %s, %s, %s)",
                (module_title, num_cards, user_id, share_code)
            )
            module_id = cursor.lastrowid
            
            for q_obj in questions:
                q = q_obj.get('question', '').strip()
                a = q_obj.get('answer', '').strip()
                cat = q_obj.get('category', topic.upper()[:15]).strip().upper()
                if q and a:
                    cursor.execute(
                        "INSERT INTO flashcards (module_id, category, question, options, correct_index) VALUES (%s, %s, %s, %s, 0)",
                        (module_id, cat, q, a)
                    )
            
            if user_id:
                increment_created_decks(cursor, user_id)
                
            conn.commit()
        conn.close()
        return jsonify({'status': 'success', 'message': f'Generated {num_cards} flashcards successfully', 'id': str(module_id)})
    except Exception as e:
        return jsonify({'status': 'error', 'message': f'Server failed to generate flashcards: {str(e)}'}), 500

# --- SMTP Test Route ---
@app.route('/send-test')
def send_test_email():
    try:
        # Construct the email content
        recipient_email = os.environ.get('MAIL_USERNAME')
        if not recipient_email:
            return jsonify({"status": "Error", "message": "MAIL_USERNAME not set in .env file"}), 400
            
        msg = Message(
            subject="Hello from your FlashLearn App!",
            recipients=[recipient_email] # Sends a test to yourself
        )
        msg.body = "If you are reading this, your Gmail SMTP configuration works perfectly!"
        
        # Send it!
        mail.send(msg)
        return jsonify({"status": "Success", "message": "Email sent successfully!"}), 200

    except Exception as e:
        return jsonify({"status": "Error", "message": str(e)}), 500

# HTML Page Template for password reset form (Styled with Premium dark green FlashLearn accents)
HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <title>Reset Password - FlashLearn</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Inter', sans-serif;
            background-color: #F7FDFC;
            color: #1F2937;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
        }
        .card {
            background: white;
            padding: 40px;
            border-radius: 24px;
            box-shadow: 0 4px 20px rgba(0, 97, 86, 0.08);
            width: 100%;
            max-width: 400px;
            box-sizing: border-box;
        }
        .header {
            display: flex;
            align-items: center;
            margin-bottom: 24px;
        }
        .logo {
            background-color: #006156;
            color: white;
            width: 40px;
            height: 40px;
            border-radius: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            margin-right: 12px;
        }
        .logo-text {
            font-weight: 700;
            font-size: 22px;
            color: #006156;
        }
        h1 {
            font-size: 24px;
            font-weight: 700;
            margin: 0 0 8px 0;
            color: #1F2937;
        }
        p {
            color: #6B7280;
            font-size: 14px;
            margin: 0 0 24px 0;
            line-height: 1.5;
        }
        .form-group {
            margin-bottom: 20px;
            text-align: left;
        }
        label {
            display: block;
            font-size: 13px;
            font-weight: 500;
            margin-bottom: 6px;
            color: #4B5563;
        }
        input {
            width: 100%;
            padding: 14px 16px;
            border: 1.5px solid #E5E7EB;
            border-radius: 12px;
            font-size: 15px;
            box-sizing: border-box;
            outline: none;
            transition: border-color 0.2s;
        }
        input:focus {
            border-color: #006156;
        }
        .btn {
            background-color: #006156;
            color: white;
            border: none;
            width: 100%;
            padding: 14px;
            font-size: 15px;
            font-weight: 600;
            border-radius: 24px;
            cursor: pointer;
            transition: background-color 0.2s;
            margin-top: 8px;
        }
        .btn:hover {
            background-color: #004d44;
        }
        .alert {
            padding: 12px 16px;
            border-radius: 12px;
            font-size: 14px;
            margin-bottom: 20px;
            line-height: 1.4;
        }
        .alert-success {
            background-color: #DEF7EC;
            color: #03543F;
        }
        .alert-danger {
            background-color: #FDE8E8;
            color: #9B1C1C;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">
            <div class="logo">🎓</div>
            <span class="logo-text">FlashLearn</span>
        </div>
        {% if success %}
            <div class="alert alert-success">
                <strong>Success!</strong> Your password has been successfully reset. You can now open the app and log in using your new password.
            </div>
        {% else %}
            <h1>Reset Password</h1>
            <p>Enter your new password below to reset access to your FlashLearn account.</p>
            
            {% if error %}
                <div class="alert alert-danger">{{ error }}</div>
            {% endif %}
            
            <form method="POST">
                <div class="form-group">
                    <label for="password">New Password</label>
                    <input type="password" id="password" name="password" required minlength="6" placeholder="Enter at least 6 characters">
                </div>
                <div class="form-group">
                    <label for="confirm_password">Confirm New Password</label>
                    <input type="password" id="confirm_password" name="confirm_password" required minlength="6" placeholder="Re-enter password">
                </div>
                <button type="submit" class="btn">Reset Password</button>
            </form>
        {% endif %}
    </div>
</body>
</html>
"""

def generate_reset_token(user):
    # Retrieve last 15 characters of password hash to invalidate on change
    pwd_hash_part = user['password_hash'][-15:]
    payload = {
        'user_id': user['id'],
        'email': user['email'],
        'pwd_hash_part': pwd_hash_part,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=1)
    }
    return jwt.encode(payload, app.config['SECRET_KEY'], algorithm='HS256')

def verify_reset_token(token):
    try:
        payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
        user_id = payload['user_id']
        pwd_hash_part = payload['pwd_hash_part']
        
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM users WHERE id = %s", (user_id,))
            user = cursor.fetchone()
        conn.close()
        
        if not user:
            return None
            
        # Verify hash matches (enforcing strict single-use)
        current_pwd_hash_part = user['password_hash'][-15:]
        if pwd_hash_part != current_pwd_hash_part:
            return None
            
        return user
    except Exception:
        return None

# --- Real Forgot Password Route (with Rate-Limiting and enumeration protection) ---
@app.route('/api/forgot-password', methods=['POST'])
@limiter.limit("5 per minute")
def forgot_password():
    try:
        data = request.get_json() or {}
        email = data.get('email', '').strip()
        if not email:
            return jsonify({'status': 'error', 'message': 'Email is required'}), 400

        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
            user = cursor.fetchone()
        conn.close()

        # SUCCESS MESSAGE is identical for security (enumeration protection)
        success_message = "If that email exists, a reset link has been sent."

        if not user:
            # Prevent user enumeration: return success even if user not found
            return jsonify({'status': 'success', 'message': success_message}), 200

        # Generate single-use secure reset token
        token = generate_reset_token(user)
        
        # Build the dynamic reset link using request's base URL host
        reset_link = f"{request.host_url}reset-password/{token}"

        # Send SMTP Email
        msg = Message(
            subject="FlashLearn - Password Reset Instructions",
            recipients=[email]
        )
        msg.body = (
            f"Hello {user['full_name']},\n\n"
            f"We received a request to reset your password. You can reset it by clicking the link below:\n\n"
            f"{reset_link}\n\n"
            f"This link is valid for 1 hour and can only be used once.\n\n"
            f"If you did not request this, you can safely ignore this email.\n\n"
            f"Happy Learning,\n"
            f"The FlashLearn Team"
        )
        # Send SMTP Email in background thread to avoid client timeout
        import threading
        def send_email_async(app_ctx, message):
            with app_ctx:
                try:
                    mail.send(message)
                except Exception as e:
                    app.logger.error(f"Failed to send async email: {str(e)}")

        threading.Thread(
            target=send_email_async,
            args=(app.app_context(), msg)
        ).start()

        return jsonify({'status': 'success', 'message': success_message}), 200

    except Exception as e:
        return jsonify({'status': 'error', 'message': f'Failed to process reset: {str(e)}'}), 500

# --- Web Endpoint for Resetting Password ---
@app.route('/reset-password/<token>', methods=['GET', 'POST'])
def reset_password_page(token):
    user = verify_reset_token(token)
    if not user:
        return render_template_string(HTML_TEMPLATE, error="This password reset link is invalid, expired, or has already been used.", success=False)
        
    if request.method == 'POST':
        password = request.form.get('password')
        confirm_password = request.form.get('confirm_password')
        
        if not password or len(password) < 6:
            return render_template_string(HTML_TEMPLATE, error="Password must be at least 6 characters long.", success=False)
        if password != confirm_password:
            return render_template_string(HTML_TEMPLATE, error="Passwords do not match.", success=False)
            
        # Update database
        hashed_pwd = generate_password_hash(password)
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute("UPDATE users SET password_hash = %s WHERE id = %s", (hashed_pwd, user['id']))
            conn.commit()
        conn.close()
        
        return render_template_string(HTML_TEMPLATE, success=True)
        
    return render_template_string(HTML_TEMPLATE, success=False)

if __name__ == '__main__':
    # Initialize DB tables before running
    init_db()
    # Run server on all interfaces (0.0.0.0) so phone can connect via Wi-Fi IP
    app.run(host='0.0.0.0', port=5000, debug=True)


