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
    val phoneNumber: String? = null,
    @ColumnInfo(name = "area_code")
    val areaCode: String? = null,
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
    val lastVerificationSnippet: String? = null,
    @ColumnInfo(name = "email")
    val email: String? = null,
    @ColumnInfo(name = "monitorability_state")
    val monitorabilityState: MonitorabilityState = MonitorabilityState.READY
)

enum class TargetStatus {
    MONITORING,
    INCARCERATED,
    DECEASED,
    IGNORED,
    UNKNOWN
}

/**
 * Lightweight projection of [Target] used by the list UI and the dedup/triage phases.
 *
 * Excludes the three largest columns — lockup_url, obituary_url, last_verification_snippet —
 * plus the unused jurisdiction column. With ~1700+ rows and a 2 MB Android CursorWindow, loading
 * full Target rows into memory crashes; the lite projection keeps per-row size small enough that
 * the entire registry fits comfortably.
 *
 * The TargetDetailScreen still fetches the full [Target] by id when the user opens a profile.
 */
data class TargetLite(
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String?,
    @ColumnInfo(name = "area_code")
    val areaCode: String?,
    @ColumnInfo(name = "status")
    val status: TargetStatus,
    @ColumnInfo(name = "last_scraped_timestamp")
    val lastScrapedTimestamp: Long,
    @ColumnInfo(name = "source_account")
    val sourceAccount: String?,
    @ColumnInfo(name = "residence_info")
    val residenceInfo: String?,
    @ColumnInfo(name = "check_frequency_hours")
    val checkFrequencyHours: Int,
    @ColumnInfo(name = "next_scheduled_check")
    val nextScheduledCheck: Long,
    @ColumnInfo(name = "last_status_change_timestamp")
    val lastStatusChangeTimestamp: Long,
    @ColumnInfo(name = "email")
    val email: String?,
    @ColumnInfo(name = "monitorability_state")
    val monitorabilityState: MonitorabilityState
)

/**
 * Whether a Target has the minimum identifying data required for the daily monitoring scrape.
 *
 *  READY               — has a usable name + at least one of phone or location/area code
 *  NEEDS_ENRICHMENT    — missing critical fields; queued for cyberbackgroundchecks lookup
 *  ENRICHMENT_FAILED   — cybg lookup returned nothing usable; will retry on a slower cadence
 */
enum class MonitorabilityState {
    READY,
    NEEDS_ENRICHMENT,
    ENRICHMENT_FAILED
}

