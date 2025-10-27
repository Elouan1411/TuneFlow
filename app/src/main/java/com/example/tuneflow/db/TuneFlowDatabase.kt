package com.example.tuneflow.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.tuneflow.data.Song

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

        // --- Table LISTENING DATA ---
        val createTableListeningData = """
            CREATE TABLE $TABLE_LISTENING_DATA(
                $LISTENING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $LISTENING_SONG_ID INTEGER NOT NULL,
                $LISTENING_LIKED INTEGER DEFAULT 0,
                $LISTENING_STYLE TEXT,
                $LISTENING_AUTHOR TEXT,
                $LISTENING_RELEASE_YEAR INTEGER
            );
        """.trimIndent()
        db.execSQL(createTableListeningData)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SEARCH_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LISTENING_DATA")

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

    fun incrementDiscoverSongs() {
        val db = writableDatabase

        // get current value
        val cursor = db.rawQuery(
            "SELECT $USER_DISCOVERED_SONGS FROM $TABLE_USER WHERE $USER_ID = ?",
            arrayOf("0")
        )
        if (cursor.moveToFirst()) {
            var current = cursor.getInt(cursor.getColumnIndexOrThrow(USER_DISCOVERED_SONGS))
            val values = ContentValues().apply {
                put(USER_DISCOVERED_SONGS, ++current)
            }

            db.update(TABLE_USER, values, "$USER_ID = 0", arrayOf())
        }
        cursor.close()
    }

    fun addListenedSong(song: Song) {
        val db = writableDatabase

        // check if already exist (when swipe the other way)
        val existsQuery = """
        SELECT 1
        FROM $TABLE_LISTENING_DATA
        WHERE $LISTENING_SONG_ID = ?
        LIMIT 1
    """.trimIndent()

        val cursor = db.rawQuery(existsQuery, arrayOf(song.trackId.toString()))
        val alreadyExists = cursor.moveToFirst()
        cursor.close()

        if (!alreadyExists) {
            val values = ContentValues().apply {
                put(LISTENING_SONG_ID, song.trackId)
                put(LISTENING_STYLE, song.primaryGenreName)
                put(LISTENING_AUTHOR, song.artistName)
                put(LISTENING_RELEASE_YEAR, song.releaseDate.substringBefore("-"))
            }
            db.insert(TABLE_LISTENING_DATA, null, values)
        }
    }


    fun addLikedSong(songId: Long, isLiked: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(LISTENING_LIKED, if (isLiked) 1 else 0)
        }

        db.update(
            TABLE_LISTENING_DATA,
            values,
            "$LISTENING_SONG_ID = ?",
            arrayOf(songId.toString())
        )
    }

    /**
     * Add a song to a playlist.
     * If the playlist doesn't exist, it is created.
     * The song is added to the songs table if doesn't already exist.
     * @param song the song to add
     * @param playlistName the name of the playlist
     */
    fun addSongToPlaylist(song: Song, playlistName: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // get id or create playlist
            var playlistId: Long
            val cursorPlaylist = db.rawQuery(
                "SELECT $PLAYLIST_ID FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
                arrayOf(playlistName)
            )
            playlistId = if (cursorPlaylist.moveToFirst()) {
                cursorPlaylist.getLong(cursorPlaylist.getColumnIndexOrThrow(PLAYLIST_ID))
            } else {
                val values = ContentValues().apply {
                    put(PLAYLIST_NAME, playlistName)
                }
                db.insert(TABLE_PLAYLISTS, null, values)
            }
            cursorPlaylist.close()

            // insert song in table (songs) if doesn't exist
            val cursorSong = db.rawQuery(
                "SELECT 1 FROM $TABLE_SONGS WHERE $SONG_ID = ? LIMIT 1",
                arrayOf(song.trackId.toString())
            )
            val songExists = cursorSong.moveToFirst()
            cursorSong.close()

            if (!songExists) {
                val valuesSong = ContentValues().apply {
                    put(SONG_ID, song.trackId)
                    put(SONG_TITLE, song.trackName)
                    put(SONG_AUTHOR, song.artistName)
                    put(SONG_ALBUM, song.collectionName)
                    put(SONG_PREVIEW_URL, song.previewUrl)
                    put(SONG_ALBUM_COVER_URL, song.artworkUrl100)
                    put(SONG_RELEASE_YEAR, song.releaseDate.substringBefore("-"))
                    put(SONG_TRACK_TIME, song.trackTimeMillis)
                    put(SONG_STYLE, song.primaryGenreName)
                    put(SONG_APPLE_MUSIC_URL, song.trackViewUrl)
                }
                db.insert(TABLE_SONGS, null, valuesSong)
            }

            // manage in table playlist_songs
            val cursorPS = db.rawQuery(
                "SELECT 1 FROM $TABLE_PLAYLIST_SONGS WHERE $PS_PLAYLIST_ID = ? AND $PS_SONG_ID = ? LIMIT 1",
                arrayOf(playlistId.toString(), song.trackId.toString())
            )
            val alreadyInPlaylist = cursorPS.moveToFirst()
            cursorPS.close()

            if (!alreadyInPlaylist) {
                val valuesPS = ContentValues().apply {
                    put(PS_PLAYLIST_ID, playlistId)
                    put(PS_SONG_ID, song.trackId)
                }
                db.insert(TABLE_PLAYLIST_SONGS, null, valuesPS)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Remove a song from a playlist.
     * Also removes the song from songs table if it is not in another playlist
     * Delete the playlist if it is empty
     * @param song the song to remove
     * @param playlistName the name of the playlist
     */
    fun removeSongFromPlaylist(song: Song, playlistName: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // gte playlist id
            val cursorPlaylist = db.rawQuery(
                "SELECT $PLAYLIST_ID FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
                arrayOf(playlistName)
            )
            if (!cursorPlaylist.moveToFirst()) {
                cursorPlaylist.close()
                db.endTransaction()
                return // playlist doesn't exist
            }
            val playlistId = cursorPlaylist.getLong(cursorPlaylist.getColumnIndexOrThrow(PLAYLIST_ID))
            cursorPlaylist.close()

            // remove songs in playlist_songs
            db.delete(
                TABLE_PLAYLIST_SONGS,
                "$PS_PLAYLIST_ID = ? AND $PS_SONG_ID = ?",
                arrayOf(playlistId.toString(), song.trackId.toString())
            )

            // Remove song from songs if not in any playlist
            val cursorSongInPlaylists = db.rawQuery(
                "SELECT 1 FROM $TABLE_PLAYLIST_SONGS WHERE $PS_SONG_ID = ? LIMIT 1",
                arrayOf(song.trackId.toString())
            )
            val songInOtherPlaylists = cursorSongInPlaylists.moveToFirst()
            cursorSongInPlaylists.close()

            if (!songInOtherPlaylists) {
                db.delete(
                    TABLE_SONGS,
                    "$SONG_ID = ?",
                    arrayOf(song.trackId.toString())
                )
            }

            // remove playlist if empty
            val cursorPlaylistEmpty = db.rawQuery(
                "SELECT 1 FROM $TABLE_PLAYLIST_SONGS WHERE $PS_PLAYLIST_ID = ? LIMIT 1",
                arrayOf(playlistId.toString())
            )
            val playlistHasSongs = cursorPlaylistEmpty.moveToFirst()
            cursorPlaylistEmpty.close()

            if (!playlistHasSongs) {
                db.delete(
                    TABLE_PLAYLISTS,
                    "$PLAYLIST_ID = ?",
                    arrayOf(playlistId.toString())
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }





    // If limit == -1  -> no limit
    fun getTopValues(column: String, limit: Int = 5): List<String> {
        val db = readableDatabase
        val topValues = mutableListOf<String>()

        // Build the query dynamically based on the limit
        val limitClause = if (limit > 0) "LIMIT $limit" else ""
        val query = """
        SELECT $column, COUNT(DISTINCT $LISTENING_SONG_ID) AS count
        FROM $TABLE_LISTENING_DATA
        WHERE $LISTENING_LIKED = 1 AND $column IS NOT NULL
        GROUP BY $column
        ORDER BY count DESC
        $limitClause
    """.trimIndent()

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            topValues.add(cursor.getString(cursor.getColumnIndexOrThrow(column)))
        }
        cursor.close()
        return topValues
    }


    fun getTopYearGroups(limit: Int = 5): List<Int> {
        val db = readableDatabase
        val topYears = mutableListOf<Int>()

        val query = """
            SELECT (CAST($LISTENING_RELEASE_YEAR AS INTEGER) / 5) * 5 AS yearGroup,
                   COUNT(DISTINCT $LISTENING_SONG_ID) AS count
            FROM $TABLE_LISTENING_DATA
            WHERE $LISTENING_LIKED = 1 AND $LISTENING_RELEASE_YEAR IS NOT NULL
            GROUP BY yearGroup
            ORDER BY count DESC
            LIMIT $limit
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            topYears.add(cursor.getInt(cursor.getColumnIndexOrThrow("yearGroup")))
        }
        cursor.close()
        return topYears
    }



    fun getTopStyles(limit: Int = 5): List<String> {
        return getTopValues(LISTENING_STYLE, limit)
    }

    fun getTopAuthors(limit: Int = 5): List<String> {
        return getTopValues(LISTENING_AUTHOR, limit)
    }

    fun soundAlreadyListened(songId: Long): Boolean {
        val db = readableDatabase
        val query = """
            SELECT *
            FROM $TABLE_LISTENING_DATA
            WHERE $LISTENING_SONG_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(songId.toString()))
        val alreadyListened = cursor.moveToFirst() // true if one line
        cursor.close()
        return alreadyListened
    }

    fun getLikedCount(): Int {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as nbLike
            FROM $TABLE_LISTENING_DATA
            WHERE $LISTENING_LIKED = 1
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val count = if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow("nbLike"))
        } else 0
        cursor.close()
        return count
    }



    companion object {
        // --- Database Info ---
        const val DB_NAME = "tuneflow_db"
        const val DB_VERSION = 2

        // --- Table Names ---
        const val TABLE_USER = "user"
        const val TABLE_SONGS = "songs"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        const val TABLE_SEARCH_HISTORY = "search_history"
        const val TABLE_LISTENING_DATA = "listening_data"

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

        // --- Listening Data Fields ---
        const val LISTENING_ID = "id"
        const val LISTENING_SONG_ID = "songId"
        const val LISTENING_LIKED = "liked"
        const val LISTENING_STYLE = "style"
        const val LISTENING_AUTHOR = "author"
        const val LISTENING_RELEASE_YEAR = "releaseYear"
    }
}
