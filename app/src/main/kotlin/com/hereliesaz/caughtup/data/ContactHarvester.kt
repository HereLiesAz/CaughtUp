package com.hereliesaz.caughtup.data

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The grim reaper's scythe for the local phonebook. 
 * Extracts the living so we can monitor them for their inevitable transition to incarcerated or deceased.
 */
class ContactHarvester(private val contentResolver: ContentResolver) {

    suspend fun harvestContacts(): List<Target> = withContext(Dispatchers.IO) {
        val targets = mutableListOf<Target>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: continue
                val number = it.getString(numberIndex) ?: continue
                
                val cleanNumber = number.filter { char -> char.isDigit() }
                
                // Naive extraction. Assuming the standard North American numbering plan for now.
                // We'll have to get clever later if they have international accomplices.
                val areaCode = if (cleanNumber.length >= 10) {
                    cleanNumber.takeLast(10).take(3)
                } else {
                    "UNKNOWN"
                }

                targets.add(
                    Target(
                        displayName = name,
                        phoneNumber = number,
                        areaCode = areaCode,
                        sourceAccount = "Device Contacts"
                    )
                )
            }
        }
        
        targets.distinctBy { it.phoneNumber }
    }
}

