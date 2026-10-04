package com.github.apkelly.drool.data.local

import androidx.room.RoomDatabase
import androidx.room.TransactionScope
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

suspend fun <T> RoomDatabase.inTransaction(
    block: suspend TransactionScope<T>.() -> T,
): T = useWriterConnection { connection ->
    connection.immediateTransaction { block(this) }
}
