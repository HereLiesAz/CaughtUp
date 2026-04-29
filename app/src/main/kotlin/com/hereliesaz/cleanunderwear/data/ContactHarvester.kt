package com.hereliesaz.cleanunderwear.data

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

    suspend fun harvestContacts(allowedAccountTypes: Set<String> = emptySet()): List<Target> = withContext(Dispatchers.IO) {
        val contactDataMap = mutableMapOf<Long, ContactData>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1, // Often the main data (number, email, street)
            ContactsContract.Data.DATA7,
            ContactsContract.Data.DATA8,
            ContactsContract.Data.DATA9,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )

        val selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
        )

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeTypeIndex = it.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Index = it.getColumnIndex(ContactsContract.Data.DATA1)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val accountTypeIndex = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val data7Index = it.getColumnIndex(ContactsContract.Data.DATA7) // City
            val data8Index = it.getColumnIndex(ContactsContract.Data.DATA8) // Region
            val data9Index = it.getColumnIndex(ContactsContract.Data.DATA9) // Postcode

            while (it.moveToNext()) {
                val contactId = it.getLong(idIndex)
                if (contactId <= 0) continue

                val mimeType = it.getString(mimeTypeIndex)
                val displayName = it.getString(nameIndex) ?: continue
                val accountType = if (accountTypeIndex >= 0) it.getString(accountTypeIndex) else null

                val contactData = contactDataMap.getOrPut(contactId) { ContactData(displayName) }

                val mappedAccount = when {
                    accountType?.contains("google", ignoreCase = true) == true -> "Google"
                    accountType?.contains("facebook", ignoreCase = true) == true || 
                    accountType?.contains("whatsapp", ignoreCase = true) == true -> "Meta"
                    accountType?.contains("apple", ignoreCase = true) == true -> "Apple"
                    else -> "Device"
                }

                if (allowedAccountTypes.isEmpty() || allowedAccountTypes.contains(mappedAccount)) {
                    contactData.accounts.add(mappedAccount)
                } else {
                    continue
                }

                when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val number = it.getString(data1Index)
                        if (number != null && contactData.phoneNumber == null) {
                            contactData.phoneNumber = number
                        }
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val city = it.getString(data7Index)
                        val region = it.getString(data8Index)
                        val zip = it.getString(data9Index)

                        val locationParts = listOfNotNull(city, region, zip).filter { part -> part.isNotBlank() }
                        if (locationParts.isNotEmpty() && contactData.residenceInfo == null) {
                            contactData.residenceInfo = locationParts.joinToString(", ")
                        }
                    }
                }
            }
        }
        
        val targets = contactDataMap.values.mapNotNull { data ->
            val number = data.phoneNumber ?: return@mapNotNull null
            val cleanNumber = number.filter { char -> char.isDigit() }
            val areaCode = if (cleanNumber.length >= 10) {
                cleanNumber.takeLast(10).take(3)
            } else {
                "UNKNOWN"
            }

            val sourceAccounts = if (data.accounts.isEmpty()) {
                "Device Contacts"
            } else {
                data.accounts.joinToString(", ")
            }

            Target(
                displayName = data.displayName,
                phoneNumber = number,
                areaCode = areaCode,
                sourceAccount = sourceAccounts,
                residenceInfo = data.residenceInfo
            )
        }

        targets.distinctBy { it.phoneNumber }
    }

    private class ContactData(
        val displayName: String,
        var phoneNumber: String? = null,
        var residenceInfo: String? = null,
        val accounts: MutableSet<String> = mutableSetOf()
    )
}

