package com.language.finebulary.util

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.os.ParcelFileDescriptor
import com.language.finebulary.data.pref.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FinebularyBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        // Adding helpers for Database and DataStore files
        // Note: DataStore file name is usually [name].preferences_pb
        FileBackupHelper(this, "../databases/finebulary_database").also { addHelper("database", it) }
        FileBackupHelper(this, "../files/datastore/user_settings.preferences_pb").also { addHelper("settings", it) }
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        val prefs = UserPreferences(this)
        val isEnabled = runBlocking { prefs.autoCloudBackup.first() }

        if (isEnabled) {
            super.onBackup(oldState, data, newState)
        } else {
            // Do nothing, skipping the backup
        }
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // Always allow restore if data exists
        super.onRestore(data, appVersionCode, newState)
    }
}
