package de.kindermaenner.playmymusic.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import java.io.IOException
import kotlin.math.min

class SmbDataSource(
    private val smbUsername: String,
    private val smbPassword: String,
    private val smbDomain: String
) : BaseDataSource(false) {

    private var randomAccessFile: SmbRandomAccessFile? = null
    private var currentUri: Uri? = null
    private var bytesRemaining: Long = 0
    private var transferStarted = false

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        transferInitializing(dataSpec)

        val accessFile = openWithFallbacks(uri)
        val fileLength = accessFile.length()

        if (dataSpec.position > fileLength) {
            accessFile.close()
            throw IOException("Requested read position is beyond the SMB file length")
        }

        accessFile.seek(dataSpec.position)

        randomAccessFile = accessFile
        currentUri = uri
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            fileLength - dataSpec.position
        } else {
            dataSpec.length
        }
        transferStarted = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    private fun openWithFallbacks(uri: Uri): SmbRandomAccessFile {
        val contexts = buildCandidateContexts()
        var lastError: Exception? = null

        for (context in contexts) {
            try {
                val smbFile = SmbFile(uri.toString(), context)
                return SmbRandomAccessFile(smbFile, "r")
            } catch (error: Exception) {
                lastError = error
            }
        }

        val reason = lastError?.message ?: "unbekannt"
        throw IOException("SMB open failed for all auth variants: $reason", lastError)
    }

    private fun buildCandidateContexts(): List<CIFSContext> {
        val baseContext = SingletonContext.getInstance()
        val contexts = mutableListOf<CIFSContext>()

        // Prefer anonymous modes first for NAS shares that explicitly allow guest access.
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("", "")))
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("guest", "")))
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("anonymous", "")))

        val configuredUser = buildConfiguredUser()
        if (configuredUser.isNotBlank()) {
            contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator(configuredUser, smbPassword)))
        }

        contexts.add(baseContext)

        return contexts
    }

    private fun buildConfiguredUser(): String {
        val trimmedUser = smbUsername.trim()
        if (trimmedUser.isBlank()) {
            return ""
        }

        val trimmedDomain = smbDomain.trim()
        return if (trimmedDomain.isBlank()) {
            trimmedUser
        } else {
            "$trimmedDomain;$trimmedUser"
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }

        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = min(bytesRemaining, length.toLong()).toInt()
        val bytesRead = randomAccessFile?.read(buffer, offset, bytesToRead) ?: C.RESULT_END_OF_INPUT

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            return C.RESULT_END_OF_INPUT
        }

        bytesRemaining -= bytesRead.toLong()
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        try {
            randomAccessFile?.close()
        } finally {
            randomAccessFile = null
            currentUri = null
            bytesRemaining = 0
            if (transferStarted) {
                transferStarted = false
                transferEnded()
            }
        }
    }

    class Factory(
        private val smbUsername: String = "",
        private val smbPassword: String = "",
        private val smbDomain: String = ""
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbDataSource(
            smbUsername = smbUsername,
            smbPassword = smbPassword,
            smbDomain = smbDomain
        )
    }
}