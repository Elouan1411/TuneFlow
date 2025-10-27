package com.example.tuneflow


import android.content.Context

object DevDatabaseUtils {

    private const val TAG = "DevDatabaseUtils"
    fun deleteDatabase(context: Context) {
        val dbName = "tuneflow_db"
        val deleted = context.deleteDatabase(dbName)
    }
}
