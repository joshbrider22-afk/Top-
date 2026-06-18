package com.example.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An Android representation of the Web's IndexedDB API.
 * Provides a key-value object-store database backed by SQLite,
 * storing serialized JSON records in transactional contexts.
 */
class IndexedDB private constructor(context: Context, val dbName: String, val version: Int) {

    private val dbHelper = DatabaseHelper(context, dbName, version)

    class Transaction(private val db: SQLiteDatabase, val mode: String, private val onTxEnded: (Boolean) -> Unit) {
        
        fun objectStore(storeName: String): ObjectStore {
            return ObjectStore(db, storeName)
        }

        fun commit() {
            if (db.inTransaction()) {
                db.setTransactionSuccessful()
                db.endTransaction()
                onTxEnded(true)
            }
        }

        fun abort() {
            if (db.inTransaction()) {
                db.endTransaction()
                onTxEnded(false)
            }
        }
    }

    class ObjectStore(private val db: SQLiteDatabase, val storeName: String) {
        
        suspend fun put(key: String, valueJson: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put("store_name", storeName)
                    put("key_path", key)
                    put("value_json", valueJson)
                    put("updated_at", System.currentTimeMillis())
                }
                val rowId = db.insertWithOnConflict(
                    "indexed_db",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                rowId != -1L
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
            var result: String? = null
            try {
                val cursor = db.query(
                    "indexed_db",
                    arrayOf("value_json"),
                    "store_name = ? AND key_path = ?",
                    arrayOf(storeName, key),
                    null, null, null
                )
                cursor.use {
                    if (it.moveToFirst()) {
                        result = it.getString(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            result
        }

        suspend fun delete(key: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val deleted = db.delete(
                    "indexed_db",
                    "store_name = ? AND key_path = ?",
                    arrayOf(storeName, key)
                )
                deleted > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        suspend fun getAll(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
            val list = mutableListOf<Pair<String, String>>()
            try {
                val cursor = db.query(
                    "indexed_db",
                    arrayOf("key_path", "value_json"),
                    "store_name = ?",
                    arrayOf(storeName),
                    null, null, "updated_at DESC"
                )
                cursor.use {
                    while (it.moveToNext()) {
                        val key = it.getString(0)
                        val valueJson = it.getString(1)
                        list.add(Pair(key, valueJson))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

        suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
            try {
                db.delete("indexed_db", "store_name = ?", arrayOf(storeName))
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun transaction(storeName: String, mode: String = "readwrite", onTxComplete: (String) -> Unit = {}): Transaction = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        Transaction(db, mode) { success ->
            if (success) {
                onTxComplete("Transaction on '$storeName' committed successfully in mode '$mode'")
            } else {
                onTxComplete("Transaction on '$storeName' aborted or failed in mode '$mode'")
            }
        }
    }

    private class DatabaseHelper(context: Context, name: String, version: Int) :
        SQLiteOpenHelper(context, name, null, version) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS indexed_db (
                    store_name TEXT NOT NULL,
                    key_path TEXT NOT NULL,
                    value_json TEXT,
                    updated_at INTEGER,
                    PRIMARY KEY(store_name, key_path)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS indexed_db")
            onCreate(db)
        }
    }

    companion object {
        @Volatile
        private var instance: IndexedDB? = null

        fun getDatabase(context: Context): IndexedDB {
            return instance ?: synchronized(this) {
                val inst = IndexedDB(context.applicationContext, "stickme_indexed_db", 1)
                instance = inst
                inst
            }
        }
    }
}
