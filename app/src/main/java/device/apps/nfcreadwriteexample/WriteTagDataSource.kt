package device.apps.nfcreadwriteexample

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

class WriteTagDataSource(
    private val context: Context,
    private val adapter: NfcAdapter,
) {
    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_URI = 1
    }

    val writeProgressState = MutableStateFlow(value = false)

    fun isEnable() = adapter.isEnabled

    fun setProgressState(isProgress: Boolean) {
        writeProgressState.value = isProgress
    }

    fun requestActivation() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            Intent("com.android.settings.ADVANCED_CONNECTED_DEVICE_SETTINGS")
        else
            Intent(Settings.ACTION_NFC_SETTINGS)
        context.startActivity(intent)
    }

    fun writeMessage(tag: Tag, type: Int, strText: String): Boolean {
        val message = createTagMessage(strText, type)
        return try {
            processWriteTag(message, tag)

        } catch (e: TimeoutCancellationException) {
            resetNfc()
            false
        }
    }

     fun processWriteTag(message: NdefMessage, tag: Tag): Boolean {
        val size = message.toByteArray().size
        return try {
            val ndef = Ndef.get(tag)
            val formatable = NdefFormatable.get(tag)

            when {
                ndef != null -> handleNdefTag(ndef, message, size)
                formatable != null -> handleFormatableTag(formatable, message)
                else -> false
            }

        } catch (ex: IOException) {
            handleIOException(tag, message)

        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    private fun handleNdefTag(ndef: Ndef, message: NdefMessage, size: Int): Boolean {
        ndef.connect()
        if (!ndef.isWritable || ndef.maxSize < size) return false
        ndef.writeNdefMessage(message)
        return true
    }

    private fun handleFormatableTag(formatable: NdefFormatable, message: NdefMessage): Boolean {
        return try {
            formatable.connect()
            formatable.format(message)
            true

        } catch (ex: IOException) {
            ex.printStackTrace()
            false
        }
    }

    private fun handleIOException(tag: Tag?, message: NdefMessage): Boolean {
        return try {
            Thread.sleep(500)
            val ndef = Ndef.get(tag)
            ndef?.writeNdefMessage(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun appendData(firstObject: ByteArray?, secondObject: ByteArray?, thirdObject: ByteArray?): ByteArray {
        val outputStream = ByteArrayOutputStream()
        try {
            firstObject?.takeIf { it.isNotEmpty() }?.let { outputStream.write(it) }
            secondObject?.takeIf { it.isNotEmpty() }?.let { outputStream.write(it) }
            thirdObject?.takeIf { it.isNotEmpty() }?.let { outputStream.write(it) }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return outputStream.toByteArray()
    }

    private fun createTagMessage(msg: String, type: Int): NdefMessage {
        val records = arrayOfNulls<NdefRecord>(size = 1)
        records[0] = when (type) {
            TYPE_TEXT -> createTextRecord(msg, Locale.getDefault())
            TYPE_URI -> createUriRecord(msg)
            else -> null
        }
        return NdefMessage(records.filterNotNull().toTypedArray())
    }

    private fun createTextRecord(text: String, locale: Locale): NdefRecord {
        val langBytes = locale.language.toByteArray(Charset.forName("UTF-8"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val utfBit = 0 shl 7
        val status = (utfBit + langBytes.size).toChar().code.toByte()
        val data = appendData(byteArrayOf(status), langBytes, textBytes)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
    }

    private fun createUriRecord(uri: String): NdefRecord {
        val uriBytes = uri.toByteArray(Charset.forName("UTF-8"))
        val recordBytes = ByteArray(uriBytes.size + 1)
        recordBytes[0] = 0x00
        System.arraycopy(uriBytes, 0, recordBytes, 1, uriBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), recordBytes)
    }

    private fun resetNfc() {
        activateNfc(isEnable = false)
        activateNfc(isEnable = true)
    }

    private fun activateNfc(isEnable: Boolean): Boolean {
        try {
            val nfcManagerClass = Class.forName(adapter.javaClass.name)
            val methodName = if (isEnable) "enable" else "disable"
            val setNfcEnabled = nfcManagerClass.getDeclaredMethod(methodName)
            setNfcEnabled.isAccessible = true
            return setNfcEnabled.invoke(adapter) as Boolean

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}