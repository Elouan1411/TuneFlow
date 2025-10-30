package com.example.tuneflow.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.tuneflow.data.PlaylistInfo
import com.example.tuneflow.data.Song

class TuneFlowDatabase(
    mContext: Context,
    name: String = DB_NAME,
    version: Int = DB_VERSION
) : SQLiteOpenHelper(mContext, name, null, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return


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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")

        onCreate(db)
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
     * The song is added to the songs table if it doesn't already exist.
     * @param song the song to add
     * @param playlistName the name of the playlist
     * @return true if the song was added, false if it was already in the playlist
     */
    fun addSongToPlaylist(song: Song, playlistName: String): Boolean {
        addListenedSong(song)
        val db = writableDatabase
        db.beginTransaction()
        try {
            // get id or create playlist
            val cursorPlaylist = db.rawQuery(
                "SELECT $PLAYLIST_ID FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
                arrayOf(playlistName)
            )
            val playlistId = if (cursorPlaylist.moveToFirst()) {
                cursorPlaylist.getLong(cursorPlaylist.getColumnIndexOrThrow(PLAYLIST_ID))
            } else {
                val values = ContentValues().apply {
                    put(PLAYLIST_NAME, playlistName)
                }
                db.insert(TABLE_PLAYLISTS, null, values)
            }
            cursorPlaylist.close()

            // insert song in table songs if doesn't exist
            val cursorSong = db.rawQuery(
                "SELECT $SONG_ID FROM $TABLE_SONGS WHERE $SONG_LISTENING_ID = ? LIMIT 1",
                arrayOf(song.trackId.toString())
            )
            val internalSongId: Long
            if (cursorSong.moveToFirst()) {
                internalSongId = cursorSong.getLong(cursorSong.getColumnIndexOrThrow(SONG_ID))
            } else {
                val valuesSong = ContentValues().apply {
                    put(SONG_LISTENING_ID, song.trackId)
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
                internalSongId = db.insert(TABLE_SONGS, null, valuesSong)
            }
            cursorSong.close()

            // check if already in playlist
            val cursorPS = db.rawQuery(
                "SELECT 1 FROM $TABLE_PLAYLIST_SONGS WHERE $PS_PLAYLIST_ID = ? AND $PS_SONG_ID = ? LIMIT 1",
                arrayOf(playlistId.toString(), internalSongId.toString())
            )
            val alreadyInPlaylist = cursorPS.moveToFirst()
            cursorPS.close()

            if (alreadyInPlaylist) {
                return false // already in a playlist
            }

            // insert into playlist_songs
            val valuesPS = ContentValues().apply {
                put(PS_PLAYLIST_ID, playlistId)
                put(PS_SONG_ID, internalSongId)
            }
            db.insert(TABLE_PLAYLIST_SONGS, null, valuesPS)

            db.setTransactionSuccessful()
            return true // song added
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
            // --- get playlist id ---
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

            // --- get internal SONG_ID ---
            val cursorSongId = db.rawQuery(
                "SELECT $SONG_ID FROM $TABLE_SONGS WHERE $SONG_LISTENING_ID = ? LIMIT 1",
                arrayOf(song.trackId.toString())
            )
            if (!cursorSongId.moveToFirst()) {
                cursorSongId.close()
                db.endTransaction()
                return // song doesn't exist
            }
            val internalSongId = cursorSongId.getLong(cursorSongId.getColumnIndexOrThrow(SONG_ID))
            cursorSongId.close()

            // --- remove song from playlist ---
            db.delete(
                TABLE_PLAYLIST_SONGS,
                "$PS_PLAYLIST_ID = ? AND $PS_SONG_ID = ?",
                arrayOf(playlistId.toString(), internalSongId.toString())
            )

            // --- remove playlist if empty ---
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
        val count =
            if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow("total")) else 0
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
        val count =
            if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow("artistCount")) else 0
        cursor.close()
        return count
    }

    /**
     * Returns the artist who appears most in listening_data
     * @return top artist or null if no data
     */
    fun getTopOneArtist(): String {
        val topAuthors = getTopAuthors(1)
        return if (topAuthors.isNotEmpty()) topAuthors[0] else "Aucun"
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
                trackViewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_APPLE_MUSIC_URL))
                    ?: "",
                previewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_PREVIEW_URL)) ?: "",
                artworkUrl60 = "", // not in DB
                artworkUrl100 = cursor.getString(cursor.getColumnIndexOrThrow(SONG_ALBUM_COVER_URL))
                    ?: "",
                releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(SONG_RELEASE_YEAR))
                    ?: "",
                trackTimeMillis = 0L,// not in DB
                country = "", // not in DB
                primaryGenreName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_STYLE)) ?: ""
            )
        } else null

        cursor.close()
        return song
    }

    /**
     * For each playlist:
     *      get the name, the number of songs (including 0) and the last song cover url
     */
    fun getPlaylistsInfo(): List<PlaylistInfo> {
        val db = readableDatabase
        val playlists = mutableListOf<PlaylistInfo>()

        val query = """
        SELECT p.$PLAYLIST_NAME AS playlistName,
               COUNT(ps.$PS_SONG_ID) AS songCount,
               (
                   SELECT s2.$SONG_ALBUM_COVER_URL
                   FROM $TABLE_PLAYLIST_SONGS ps2
                   JOIN $TABLE_SONGS s2 ON s2.$SONG_ID = ps2.$PS_SONG_ID
                   WHERE ps2.$PS_PLAYLIST_ID = p.$PLAYLIST_ID
                   ORDER BY s2.$SONG_ID DESC
                   LIMIT 1
               ) AS lastSongCover
        FROM $TABLE_PLAYLISTS p
        LEFT JOIN $TABLE_PLAYLIST_SONGS ps ON ps.$PS_PLAYLIST_ID = p.$PLAYLIST_ID
        GROUP BY p.$PLAYLIST_ID
        ORDER BY p.$PLAYLIST_NAME ASC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("playlistName"))
            val count = cursor.getInt(cursor.getColumnIndexOrThrow("songCount"))
            val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("lastSongCover")) ?: ""
            playlists.add(PlaylistInfo(name, count, coverUrl))
        }
        cursor.close()
        return playlists
    }


    /**
     * Create new playlist in db
     * If a playlist with the same name already exists, it will not be recreated
     *
     * @param playlistName name of the playlist
     * @return true if the playlist has been created, false if it already existed or in case of error
     */
    fun createPlaylist(playlistName: String): Boolean {
        val db = writableDatabase

        // Check if the playlist already exists
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
            arrayOf(playlistName)
        )

        val exists = cursor.moveToFirst()
        cursor.close()

        if (exists) {
            return false
        }

        // create new playlist
        val values = ContentValues().apply {
            put(PLAYLIST_NAME, playlistName)
        }

        val result = db.insert(TABLE_PLAYLISTS, null, values)
        val success = result != -1L

        return success
    }



    /**
     * Get all songs from a given playlist.
     *
     * @param playlistName The name of the playlist.
     * @return A list of Song objects contained in the playlist.
     */
    fun getSongsFromPlaylist(playlistName: String): List<Song> {
        val db = readableDatabase
        val songs = mutableListOf<Song>()

        val query = """
        SELECT s.$SONG_LISTENING_ID, s.$SONG_TITLE, s.$SONG_AUTHOR, s.$SONG_ALBUM, 
               s.$SONG_PREVIEW_URL, s.$SONG_ALBUM_COVER_URL, s.$SONG_RELEASE_YEAR, 
               s.$SONG_STYLE, s.$SONG_APPLE_MUSIC_URL
        FROM $TABLE_SONGS s
        JOIN $TABLE_PLAYLIST_SONGS ps ON s.$SONG_ID = ps.$PS_SONG_ID
        JOIN $TABLE_PLAYLISTS p ON p.$PLAYLIST_ID = ps.$PS_PLAYLIST_ID
        WHERE p.$PLAYLIST_NAME = ?
        ORDER BY s.$SONG_ID DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(playlistName))

        if (cursor.moveToFirst()) {
            do {
                val song = Song(
                    artistId = 0L,
                    collectionId = 0L,
                    trackId = cursor.getLong(cursor.getColumnIndexOrThrow(SONG_LISTENING_ID)),
                    artistName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_AUTHOR)) ?: "",
                    collectionName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_ALBUM)) ?: "",
                    trackName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_TITLE)) ?: "",
                    artistViewUrl = "",
                    collectionViewUrl = "",
                    trackViewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_APPLE_MUSIC_URL))
                        ?: "",
                    previewUrl = cursor.getString(cursor.getColumnIndexOrThrow(SONG_PREVIEW_URL)) ?: "",
                    artworkUrl60 = "",
                    artworkUrl100 = cursor.getString(cursor.getColumnIndexOrThrow(SONG_ALBUM_COVER_URL))
                        ?: "",
                    releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(SONG_RELEASE_YEAR)) ?: "",
                    trackTimeMillis = 0L,
                    country = "",
                    primaryGenreName = cursor.getString(cursor.getColumnIndexOrThrow(SONG_STYLE)) ?: ""
                )
                songs.add(song)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return songs
    }

    /**
     * Deletes a playlist and all its links to songs.
     * @param playlistName name of playlist to delete
     * @return true if it worked
     */
    fun deletePlaylist(playlistName: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // get id of the playlist
            val cursor = db.rawQuery(
                "SELECT $PLAYLIST_ID FROM $TABLE_PLAYLISTS WHERE $PLAYLIST_NAME = ? LIMIT 1",
                arrayOf(playlistName)
            )
            val playlistId = if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(PLAYLIST_ID))
            } else {
                cursor.close()
                return false // playlist doesn't exist
            }
            cursor.close()

            db.delete(TABLE_PLAYLIST_SONGS, "$PS_PLAYLIST_ID = ?", arrayOf(playlistId.toString()))

            db.delete(TABLE_PLAYLISTS, "$PLAYLIST_ID = ?", arrayOf(playlistId.toString()))

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }



    companion object {
        // --- Database Info ---
        const val DB_NAME = "tuneflow_db"
        const val DB_VERSION = 1

        // --- Table Names ---
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
