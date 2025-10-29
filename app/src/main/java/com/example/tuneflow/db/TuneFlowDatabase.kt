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
                $USER_ID INTEGER PRIMARY KEY
            );
        """.trimIndent()
        db.execSQL(createUserTable)

        // --- Table SONGS ---
        val createSongsTable = """
            CREATE TABLE $TABLE_SONGS(
                $SONG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $SONG_LISTENING_ID INTEGER NOT NULL,
                $SONG_LIKED INTEGER DEFAULT 0,                
                $SONG_TITLE TEXT NOT NULL,
                $SONG_AUTHOR TEXT,
                $SONG_ALBUM TEXT,
                $SONG_PREVIEW_URL TEXT,
                $SONG_ALBUM_COVER_URL TEXT,
                $SONG_RELEASE_YEAR INTEGER,
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

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")

        onCreate(db)
    }


    /**
     * Initializes the user table in the database if it doesn’t already exist.
     *
     * Checks if the default user (ID = 0) is present in the table.
     * If not, it creates the entry with default values.
     */
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
            }
            db.insert(TABLE_USER, null, values)
        }
    }




    /**
     * Adds the given sound to the database of listened sounds
     * @param song the sound listened
     */
    fun addListenedSong(song: Song) {
        val db = writableDatabase

        // check if already exist (when swipe the other way)
        val existsQuery = """
        SELECT 1
        FROM $TABLE_SONGS
        WHERE $SONG_LISTENING_ID = ?
        LIMIT 1
    """.trimIndent()

        val cursor = db.rawQuery(existsQuery, arrayOf(song.trackId.toString()))
        val alreadyExists = cursor.moveToFirst()
        cursor.close()

        if (!alreadyExists) {
            val values = ContentValues().apply {
                put(SONG_LISTENING_ID, song.trackId)
                put(SONG_STYLE, song.primaryGenreName)
                put(SONG_AUTHOR, song.artistName)
                put(SONG_RELEASE_YEAR, song.releaseDate.substringBefore("-"))
                put(SONG_TITLE, song.trackName)
                put(SONG_ALBUM, song.collectionName)
                put(SONG_PREVIEW_URL, song.previewUrl)
                put(SONG_ALBUM_COVER_URL, song.artworkUrl100)
                put(SONG_APPLE_MUSIC_URL, song.trackViewUrl)

            }
            db.insert(TABLE_SONGS, null, values)
        }
    }

    /**
     * Adds a like to the database for the given song
     * @param songId ID of the liked sound
     * @param isLiked boolean to know if we should add the like or remove it
     */
    fun addLikedSong(songId: Long, isLiked: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(SONG_LIKED, if (isLiked) 1 else 0)
        }

        db.update(
            TABLE_SONGS,
            values,
            "$SONG_LISTENING_ID = ?",
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
     * Delete the playlist if it is empty
     * @param song the song to remove
     * @param playlistName the name of the playlist
     */
    fun removeSongFromPlaylist(song: Song, playlistName: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // get playlist id
            val cursorPlaylist = db.rawQuery(
                "SELECT $PLAYLIST_ID FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
                arrayOf(playlistName)
            )
            if (!cursorPlaylist.moveToFirst()) {
                cursorPlaylist.close()
                db.endTransaction()
                return // playlist doesn't exist
            }
            val playlistId =
                cursorPlaylist.getLong(cursorPlaylist.getColumnIndexOrThrow(PLAYLIST_ID))
            cursorPlaylist.close()

            // remove songs in playlist_songs
            db.delete(
                TABLE_PLAYLIST_SONGS,
                "$PS_PLAYLIST_ID = ? AND $PS_SONG_ID = ?",
                arrayOf(playlistId.toString(), song.trackId.toString())
            )

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


    /**
     * Returns the top values for a given column in the user's listening data.
     *
     * Counts how many liked songs there are for each value in the column,
     * then returns the most frequent ones.
     *
     * @param column The column to check (e.g., artist, genre, etc.).
     * @param limit Maximum number of values to return (default 5). If <= 0, returns all.
     * @return A list of the top values for the column, sorted by how often they appear.
     */
    fun getTopValues(column: String, limit: Int = 5): List<String> {
        val db = readableDatabase
        val topValues = mutableListOf<String>()

        // Build the query dynamically based on the limit
        val limitClause = if (limit > 0) "LIMIT $limit" else ""
        val query = """
        SELECT $column, COUNT(DISTINCT $SONG_LISTENING_ID) AS count
        FROM $TABLE_SONGS
        WHERE $SONG_LIKED = 1 AND $column IS NOT NULL
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


    /**
     * Retrieves the top N year groups of songs liked by the user.
     *
     * Each year returned represents a 5-year group. For example, 2020 means the range 2015–2020.
     *
     * @param limit The maximum number of year groups to retrieve (default is 5).
     * @return A list of integers representing the top `limit` year groups of liked songs.
     */
    fun getTopYearGroups(limit: Int = 5): List<Int> {
        val db = readableDatabase
        val topYears = mutableListOf<Int>()

        val query = """
            SELECT (CAST($SONG_RELEASE_YEAR AS INTEGER) / 5) * 5 AS yearGroup,
                   COUNT(DISTINCT $SONG_LISTENING_ID) AS count
            FROM $TABLE_SONGS
            WHERE $SONG_LIKED = 1 AND $SONG_RELEASE_YEAR IS NOT NULL
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


    /**
     * Retrieves the top N favorite styles of the user.
     *
     * Based on the more general `getTopValues` function.
     *
     * @param limit The maximum number of top styles to retrieve (default is 5).
     * @return A list of the user's top `limit` favorite styles.
     */
    fun getTopStyles(limit: Int = 5): List<String> {
        return getTopValues(SONG_STYLE, limit)
    }

    /**
     * Retrieves the top N favorite artists of the user.
     *
     * Based on the more general `getTopValues` function.
     *
     * @param limit The maximum number of top artists to retrieve (default is 5).
     * @return A list of the user's top `limit` favorite artists.
     */
    fun getTopAuthors(limit: Int = 5): List<String> {
        return getTopValues(SONG_AUTHOR, limit)
    }

    /**
     * Determines if the user has already listened to the sound
     * @return true if he has already listened
     */
    fun soundAlreadyListened(songId: Long): Boolean {
        val db = readableDatabase
        val query = """
            SELECT *
            FROM $TABLE_SONGS
            WHERE $SONG_LISTENING_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(songId.toString()))
        val alreadyListened = cursor.moveToFirst() // true if one line
        cursor.close()
        return alreadyListened
    }

    /**
     * counts the number of likes on the user's account
     * @return number of likes
     */
    fun getLikedCount(): Int {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as nbLike
            FROM $TABLE_SONGS
            WHERE $SONG_LIKED = 1
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val count = if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow("nbLike"))
        } else 0
        cursor.close()
        return count
    }

    /**
     * Checks if a song is liked in the database.
     * @param songId id of the song
     * @return true if song lidek
     */
    fun isSongLiked(songId: Long): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SONGS,
            arrayOf(SONG_LIKED),
            "$SONG_LISTENING_ID = ? AND $SONG_LIKED = 1",
            arrayOf(songId.toString()),
            null, null, null
        )
        val liked = cursor.count > 0
        cursor.close()
        return liked
    }

    /**
     * Counts all rows in listening_data table
     * @return total number of entries
     */
    fun getTotalListeningCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) AS total FROM $TABLE_SONGS", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow("total")) else 0
        cursor.close()
        return count
    }

    /**
     * Counts the number of distinct artists in listening_data
     * @return number of unique artists
     */
    fun getDistinctArtistsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT $SONG_AUTHOR) AS artistCount FROM $TABLE_SONGS",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow("artistCount")) else 0
        cursor.close()
        return count
    }

    /**
     * Returns the artist who appears most in listening_data
     * @return top artist or null if no data
     */
    fun getTopOneArtist(): String {
        return getTopAuthors(1)[0]
    }

    /**
     * Returns the 10 most recently liked songs (highest ID first)
     * @return list of song IDs
     */
    fun getRecentLikedSongs(limit: Int = 10): List<Long> {
        val db = readableDatabase
        val songIds = mutableListOf<Long>()
        val cursor = db.rawQuery(
            """
            SELECT $SONG_ID 
            FROM $TABLE_SONGS 
            WHERE $SONG_LIKED = 1
            ORDER BY $SONG_ID DESC
            LIMIT $limit
            """.trimIndent(), null
        )
        while (cursor.moveToNext()) {
            songIds.add(cursor.getLong(cursor.getColumnIndexOrThrow(SONG_ID)))
        }
        cursor.close()
        return songIds
    }

    /**
     * Get a Song object from the database based on its ID.
     *
     * @param songId The ID of the song in the database.
     * @return A Song object if found, null otherwise.
     */
    fun getSongFromDb(songId: Long): Song? {
        val db = readableDatabase

        val query = """
        SELECT $SONG_LISTENING_ID, $SONG_TITLE, $SONG_AUTHOR, $SONG_ALBUM, 
               $SONG_PREVIEW_URL, $SONG_ALBUM_COVER_URL, $SONG_RELEASE_YEAR, $SONG_STYLE, $SONG_APPLE_MUSIC_URL
        FROM $TABLE_SONGS
        WHERE $SONG_ID = ?
        LIMIT 1
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(songId.toString()))
        val song: Song? = if (cursor.moveToFirst()) {
            Song(
                artistId = 0L, // not in DB
                collectionId = 0L, // not in DB
                trackId = cursor.getLong(cursor.getColumnIndexOrThrow(SONG_LISTENING_ID)),
                artistName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_AUTHOR)) ?: "",
                collectionName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_ALBUM)) ?: "",
                trackName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_TITLE)) ?: "",
                artistViewUrl = "", // not in DB
                collectionViewUrl = "", // not in DB
                trackViewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_APPLE_MUSIC_URL)) ?: "",
                previewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_PREVIEW_URL)) ?: "",
                artworkUrl60 = "", // not in DB
                artworkUrl100 = cursor.getString(cursor.getColumnIndexOrThrow(SONG_ALBUM_COVER_URL)) ?: "",
                releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(SONG_RELEASE_YEAR)) ?: "",
                trackTimeMillis = 0L,// not in DB
                country = "", // not in DB
                primaryGenreName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_STYLE)) ?: ""
            )
        } else null

        cursor.close()
        return song
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

        // --- User Fields ---
        const val USER_ID = "id"
        const val USER_TOTAL_LISTENING_TIME = "totalListeningTime"

        // --- Song Fields ---
        const val SONG_ID = "id"
        const val SONG_LISTENING_ID = "songId"
        const val SONG_TITLE = "title"
        const val SONG_AUTHOR = "author"
        const val SONG_ALBUM = "album"
        const val SONG_PREVIEW_URL = "previewUrl"
        const val SONG_ALBUM_COVER_URL = "albumCoverUrl"
        const val SONG_RELEASE_YEAR = "releaseYear"
        const val SONG_TRACK_TIME = "trackTimeMillis"
        const val SONG_STYLE = "style"
        const val SONG_APPLE_MUSIC_URL = "appleMusicUrl"
        const val SONG_LIKED = "liked"

        // --- Playlist Fields ---
        const val PLAYLIST_ID = "id"
        const val PLAYLIST_NAME = "name"

        // --- Playlist_Songs Fields ---
        const val PS_ID = "id"
        const val PS_PLAYLIST_ID = "playlistId"
        const val PS_SONG_ID = "songId"
    }
}
