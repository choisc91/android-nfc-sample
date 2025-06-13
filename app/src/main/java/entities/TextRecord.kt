package device.demo.nfc.domain.entities

import android.annotation.SuppressLint
import android.nfc.NdefRecord
import java.nio.charset.Charset

@SuppressLint("NewApi")
class TextRecord(
    val text: String,
) : ParsedRecord {

    override val type: Int get() = ParsedRecord.TYPE_TEXT

    companion object {
        // TODO: deal with text fields which span multiple NdefRecords
        fun parse(record: NdefRecord): TextRecord {
            try {
                val payload = record.payload
                /*
                 * payload[0] contains the "Status Byte Encodings" field, per the NFC Forum
                 * "Text Record Type Definition" section 3.2.1.
                 *
                 * bit7 is the Text Encoding Field.
                 *
                 * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1): The text is encoded in
                 * UTF16
                 *
                 * Bit_6 is reserved for future use and must be set to zero.
                 *
                 * Bits 5 to 0 are the length of the IANA language code.
                 */
                val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
                val languageCodeLength = payload[0].toInt() and 0x3F
                val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName(textEncoding))
                return TextRecord(text)
            } catch (e: Exception) {
                // Should never happen unless we get a malformed tag.
                throw IllegalArgumentException(e)
            }
        }

        fun isText(record: NdefRecord): Boolean {
            return try {
                if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    return false
                }
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
