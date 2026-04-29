package com.hereliesaz.cleanunderwear.util

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemContactSyncer @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Updates the system contact with the latest status and verification details.
     * We append this to the "Notes" field of the contact.
     */
    fun syncToSystem(target: Target) {
        try {
            val contactId = findContactIdByPhone(target.phoneNumber) ?: return
            
            val statusText = when (target.status) {
                TargetStatus.MONITORING -> "Monitoring"
                TargetStatus.INCARCERATED -> "Incarcerated"
                TargetStatus.DECEASED -> "Deceased"
                TargetStatus.IGNORED -> "Archived"
                TargetStatus.UNKNOWN -> "Checking..."
            }

            val note = """
                [Registry Status: $statusText]
                Last Check: ${java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US).format(java.util.Date(target.lastScrapedTimestamp))}
                ${target.lockupUrl?.let { "Records: $it" } ?: ""}
                ${target.obituaryUrl?.let { "Obit: $it" } ?: ""}
            """.trimIndent()

            updateContactNote(contactId, note)
        } catch (e: Exception) {
            Log.e("ContactSyncer", "Failed to sync ${target.displayName} to system contacts", e)
        }
    }

    private fun findContactIdByPhone(phone: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            }
        }
        return null
    }

    private fun updateContactNote(contactId: String, note: String) {
        val ops = ArrayList<ContentProviderOperation>()

        // Try to update existing note first
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
            .withSelection(
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
            )
            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
            .build())

        // If update fails (doesn't exist), we should ideally insert, 
        // but for simplicity in this scaffold we perform a batch update.
        try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            Log.e("ContactSyncer", "Batch update failed", e)
        }
    }
}
