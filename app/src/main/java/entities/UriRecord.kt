package device.demo.nfc.domain.entities

import android.net.Uri
import android.nfc.NdefRecord
import android.util.Log
import java.nio.charset.Charset

class UriRecord(
    val uri: Uri,
) : ParsedRecord {
    companion object {
        private const val TAG = "UriRecord"
        private val URI_PREFIX_MAP = arrayOf(
            "",             // 0x00
            "http://www.",  // 0x01
            "https://www.", // 0x02
            "http://",      // 0x03
            "https://",     // 0x04
            "tel:",         // 0x05
            "mailto:",      // 0x06
            "ftp://anonymous:anonymous@", // 0x07
            "ftp://ftp.",   // 0x08
            "ftps://",      // 0x09
            "sftp://",      // 0x0A
            "smb://",       // 0x0B
            "nfs://",       // 0x0C
            "ftp://",       // 0x0D
            "dav://",       // 0x0E
            "news:",        // 0x0F
            "telnet://",    // 0x10
            "imap:",        // 0x11
            "rtsp://",      // 0x12
            "urn:",         // 0x13
            "pop:",         // 0x14
            "sip:",         // 0x15
            "sips:",        // 0x16
            "tftp:",        // 0x17
            "btspp://",     // 0x18
            "btl2cap://",   // 0x19
            "btgoep://",    // 0x1A
            "tcpobex://",   // 0x1B
            "irdaobex://",  // 0x1C
            "file://",      // 0x1D
            "urn:epc:id:",  // 0x1E
            "urn:epc:tag:", // 0x1F
            "urn:epc:pat:", // 0x20
            "urn:epc:raw:", // 0x21
            "urn:epc:"      // 0x22
        )

        fun parse(record: NdefRecord): UriRecord {
            return when (val tnf = record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> parseWellKnown(record)
                NdefRecord.TNF_ABSOLUTE_URI -> parseAbsolute(record)
                else -> throw IllegalArgumentException("Unknown TNF $tnf")
            }
        }

        private fun parseAbsolute(record: NdefRecord): UriRecord {
            val payload = record.payload
            val uri = Uri.parse(String(payload, Charset.forName("UTF-8")))
            return UriRecord(uri)
        }

        private fun parseWellKnown(record: NdefRecord): UriRecord {
            val payload = record.payload
            Log.i(TAG, "payload[0] = ${Integer.toHexString(payload[0].toInt())}, length = ${payload.size}")

            val prefixCode = (payload[0].toInt() and 0xFF).let { if (it >= URI_PREFIX_MAP.size) 0 else it }
            val prefix = URI_PREFIX_MAP[prefixCode]

            val fullUri = prefix.toByteArray(Charset.forName("UTF-8")) + payload.copyOfRange(1, payload.size)
            val uri = Uri.parse(String(fullUri, Charset.forName("UTF-8")))

            return UriRecord(uri)
        }

        fun isUri(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }

    override val type: Int get() = ParsedRecord.TYPE_URI
}
