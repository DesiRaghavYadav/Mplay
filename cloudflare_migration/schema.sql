-- Cloudflare D1 SQL Schema for OTT Application
CREATE TABLE IF NOT EXISTS comics (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    imageUrl TEXT NOT NULL,
    description TEXT,
    likesCount TEXT DEFAULT '12k',
    isPremium INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS comic_chapters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    comicId INTEGER NOT NULL,
    chapterNumber INTEGER NOT NULL,
    title TEXT NOT NULL,
    pagesCsv TEXT NOT NULL,
    pdfUrl TEXT,
    FOREIGN KEY(comicId) REFERENCES comics(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stories (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    imageUrl TEXT NOT NULL,
    content TEXT,
    category TEXT NOT NULL,
    isPremium INTEGER DEFAULT 0,
    audioUrl TEXT NOT NULL,
    duration TEXT,
    releaseDate INTEGER
);

CREATE TABLE IF NOT EXISTS channels (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    imageUrl TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS media_items (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    imageUrl TEXT NOT NULL,
    category TEXT NOT NULL,
    isPremium INTEGER DEFAULT 0,
    rating REAL DEFAULT 4.5,
    reelsUrl TEXT,
    likesCount TEXT
);

CREATE TABLE IF NOT EXISTS reels (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    imageUrl TEXT NOT NULL,
    videoUrl TEXT NOT NULL,
    likesCount TEXT,
    isLiked INTEGER DEFAULT 0,
    createdAt INTEGER,
    userStatus TEXT
);

CREATE TABLE IF NOT EXISTS batchvideo (
    id TEXT PRIMARY KEY,
    data TEXT NOT NULL
);

