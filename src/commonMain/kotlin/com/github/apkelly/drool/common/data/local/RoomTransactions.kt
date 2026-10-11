package com.github.apkelly.drool.common.data.local

import androidx.room.RoomDatabase
import androidx.room.TransactionScope
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Runs [block] on Room's writer connection inside an immediate transaction.
 *
 * Shared repository refreshes replace several related tables atomically. Reserving the write
 * transaction up front prevents observers from seeing partially refreshed data and avoids a
 * deferred transaction being upgraded after reads have already occurred.
 */
suspend fun <T> RoomDatabase.inTransaction(
    block: suspend TransactionScope<T>.() -> T,
): T = useWriterConnection { connection ->
    connection.immediateTransaction { block(this) }
}
