package com.poc.atmdepositbalancereader

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "SlipDetailDatabase"
        private val TABLE_SLIPS = "SlipDetailTable"
        private val KEY_ID = "id"
        private val KEY_DATE = "date"
        private val KEY_ADDRESS = "address"
        private val KEY_AMOUNT = "amount"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_SLIP_TABLE = ("CREATE TABLE" + TABLE_SLIPS + "("
                + KEY_ID + "INTEGER PRIMARY KEY," + KEY_DATE + "TEXT," + KEY_ADDRESS + "TEXT,"
                + KEY_AMOUNT + "TEXT" + ")")
        db?.execSQL(CREATE_SLIP_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_SLIPS")
        onCreate(db)
    }

    fun addSlipDetails(slipDetail: SlipDetails): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, slipDetail.id)
        contentValues.put(KEY_DATE, slipDetail.date)
        contentValues.put(KEY_ADDRESS, slipDetail.address)
        contentValues.put(KEY_AMOUNT, slipDetail.amount)

        val success = db.insert(TABLE_SLIPS, null, contentValues)
        db.close()
        return success
    }

    fun viewSlipDetails(): List<SlipDetails?> {
        val slipDetailList: ArrayList<SlipDetails> = ArrayList<SlipDetails>()
        val selectQuery = "SELECT * FROM $TABLE_SLIPS"
        val db = this.readableDatabase
        var cursor: Cursor?
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        var mId: Int
        var date: String
        var address: String
        var amount: String
        if (cursor.moveToFirst()) {
            do {
                mId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE))
                address = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ADDRESS))
                amount = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AMOUNT))
                val slipDetails = SlipDetails(id = mId, date = date, address = address, amount = amount)
                slipDetailList.add(slipDetails)
            } while (cursor.moveToNext())
        }
        return slipDetailList
    }

    fun updateSlipDetail(slipDetail: SlipDetails): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, slipDetail.id)
        contentValues.put(KEY_DATE, slipDetail.date)
        contentValues.put(KEY_ADDRESS, slipDetail.address)
        contentValues.put(KEY_AMOUNT, slipDetail.amount)
        val success = db.update(TABLE_SLIPS, contentValues, "id=" + slipDetail.id, null)

        db.close()
        return success
    }

    fun deleteSlipDetail(slipDetail: SlipDetails): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, slipDetail.id)
        val success = db.delete(TABLE_SLIPS, "id=" + slipDetail.id, null)
        db.close()
        return success
    }
}