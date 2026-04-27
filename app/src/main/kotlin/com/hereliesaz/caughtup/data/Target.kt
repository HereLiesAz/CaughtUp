package com.hereliesaz.caughtup.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "targets")
data class Target(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "area_code")
    val areaCode: String,
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String? = null,
    @ColumnInfo(name = "status")
    val status: TargetStatus = TargetStatus.AT_LARGE,
    @ColumnInfo(name = "last_scraped_timestamp")
    val lastScrapedTimestamp: Long = 0L,
    @ColumnInfo(name = "source_account")
    val sourceAccount: String? = null
)

enum class TargetStatus {
    AT_LARGE,
    INCARCERATED,
    DECEASED,
    UNKNOWN
}

