package com.hereliesaz.cleanunderwear.data

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
    val status: TargetStatus = TargetStatus.MONITORING,
    @ColumnInfo(name = "last_scraped_timestamp")
    val lastScrapedTimestamp: Long = 0L,
    @ColumnInfo(name = "source_account")
    val sourceAccount: String? = null,
    @ColumnInfo(name = "residence_info")
    val residenceInfo: String? = null,
    @ColumnInfo(name = "lockup_url")
    val lockupUrl: String? = null,
    @ColumnInfo(name = "obituary_url")
    val obituaryUrl: String? = null,
    @ColumnInfo(name = "check_frequency_hours")
    val checkFrequencyHours: Int = 24,
    @ColumnInfo(name = "next_scheduled_check")
    val nextScheduledCheck: Long = 0L,
    @ColumnInfo(name = "last_status_change_timestamp")
    val lastStatusChangeTimestamp: Long = 0L,
    @ColumnInfo(name = "last_verification_snippet")
    val lastVerificationSnippet: String? = null
)

enum class TargetStatus {
    MONITORING,
    INCARCERATED,
    DECEASED,
    IGNORED,
    UNKNOWN
}

