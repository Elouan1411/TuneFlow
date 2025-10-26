package com.example.tuneflow.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TuneFlowDatabase(
    mContext: Context,
    name: String = DB_NAME,
    version: Int = DB_VERSION
) : SQLiteOpenHelper(mContext, name, null, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return

        // --- Table USER ---
        val createUserTable = """
            CREATE TABLE $TABLE_USER(
                $USER_ID INTEGER PRIMARY KEY,
                $USER_TOTAL_LISTENING_TIME LONG DEFAULT 0,
                $USER_DISCOVERED_SONGS INTEGER DEFAULT 0,
                $USER_LIKED_SONGS INTEGER DEFAULT 0,
                $USER_PLAYLISTS_COUNT INTEGER DEFAULT 0,
                $USER_DISCOVERED_ARTISTS INTEGER DEFAULT 0
            );
        """.trimIndent()
        db.execSQL(createUserTable)

        // --- Table SONGS ---
        val createSongsTable = """
            CREATE TABLE $TABLE_SONGS(
                $SONG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $SONG_TITLE TEXT NOT NULL,
                $SONG_AUTHOR TEXT,
                $SONG_ALBUM TEXT,
                $SONG_PREVIEW_URL TEXT,
                $SONG_ALBUM_COVER_URL TEXT,
                $SONG_RELEASE_YEAR INTEGER,
                $SONG_TRACK_TIME LONG,
                $SONG_STYLE TEXT,
                $SONG_APPLE_MUSIC_URL TEXT
            );
        """.trimIndent()
        db.execSQL(createSongsTable)

        // --- Table PLAYLISTS ---
        val createPlaylistsTable = """
            CREATE TABLE $TABLE_PLAYLISTS(
                $PLAYLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PLAYLIST_NAME TEXT NOT NULL
            );
        """.trimIndent()
        db.execSQL(createPlaylistsTable)

        // --- Table PLAYLIST_SONGS (relation many-to-many) ---
        val createPlaylistSongsTable = """
            CREATE TABLE $TABLE_PLAYLIST_SONGS(
                $PS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PS_PLAYLIST_ID INTEGER NOT NULL,
                $PS_SONG_ID INTEGER NOT NULL,
                FOREIGN KEY($PS_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS($PLAYLIST_ID) ON DELETE CASCADE,
                FOREIGN KEY($PS_SONG_ID) REFERENCES $TABLE_SONGS($SONG_ID) ON DELETE CASCADE
            );
        """.trimIndent()
        db.execSQL(createPlaylistSongsTable)

        // --- Table SEARCH HISTORY ---
        val createSearchHistoryTable = """
            CREATE TABLE $TABLE_SEARCH_HISTORY(
                $SEARCH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $SEARCH_QUERY TEXT NOT NULL,
                $SEARCH_TIMESTAMP LONG DEFAULT (strftime('%s','now') * 1000)
            );
        """.trimIndent()
        db.execSQL(createSearchHistoryTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SEARCH_HISTORY")
        onCreate(db)
    }

    fun initializeDb() {
        val db = this.writableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USER WHERE $USER_ID = 0",
            null
        )
        val exists = cursor.count > 0
        cursor.close()

        if (!exists) {
            val values = ContentValues().apply {
                put(USER_ID, 0)
                put(USER_TOTAL_LISTENING_TIME, 0)
                put(USER_DISCOVERED_SONGS, 0)
                put(USER_LIKED_SONGS, 0)
                put(USER_PLAYLISTS_COUNT, 0)
                put(USER_DISCOVERED_ARTISTS, 0)
            }
            db.insert(TABLE_USER, null, values)
        }
    }

    fun incrementDiscoverSongs(){
        val db = writableDatabase

        // get current value
        val cursor = db.rawQuery("SELECT $USER_DISCOVERED_SONGS FROM $TABLE_USER WHERE $USER_ID = ?", arrayOf("0"))
        if (cursor.moveToFirst()){
            var current = cursor.getInt(cursor.getColumnIndexOrThrow(USER_DISCOVERED_SONGS))
            val values = ContentValues().apply {
                put(USER_DISCOVERED_SONGS, ++current)
            }

            db.update(TABLE_USER, values, "$USER_ID = 0", arrayOf())
        }
        cursor.close()
    }

    companion object {
        // --- Database Info ---
        const val DB_NAME = "tuneflow_db"
        const val DB_VERSION = 1

        // --- Table Names ---
        const val TABLE_USER = "user"
        const val TABLE_SONGS = "songs"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        const val TABLE_SEARCH_HISTORY = "search_history"

        // --- User Fields ---
        const val USER_ID = "id"
        const val USER_TOTAL_LISTENING_TIME = "totalListeningTime"
        const val USER_DISCOVERED_SONGS = "discoveredSongs"
        const val USER_LIKED_SONGS = "likedSongs"
        const val USER_PLAYLISTS_COUNT = "playlistsCount"
        const val USER_DISCOVERED_ARTISTS = "discoveredArtists"

        // --- Song Fields ---
        const val SONG_ID = "id"
        const val SONG_TITLE = "title"
        const val SONG_AUTHOR = "author"
        const val SONG_ALBUM = "album"
        const val SONG_PREVIEW_URL = "previewUrl"
        const val SONG_ALBUM_COVER_URL = "albumCoverUrl"
        const val SONG_RELEASE_YEAR = "releaseYear"
        const val SONG_TRACK_TIME = "trackTimeMillis"
        const val SONG_STYLE = "style"
        const val SONG_APPLE_MUSIC_URL = "appleMusicUrl"

        // --- Playlist Fields ---
        const val PLAYLIST_ID = "id"
        const val PLAYLIST_NAME = "name"

        // --- Playlist_Songs Fields ---
        const val PS_ID = "id"
        const val PS_PLAYLIST_ID = "playlistId"
        const val PS_SONG_ID = "songId"

        // --- Search History Fields ---
        const val SEARCH_ID = "id"
        const val SEARCH_QUERY = "searchQuery"
        const val SEARCH_TIMESTAMP = "timestamp"
    }
}
