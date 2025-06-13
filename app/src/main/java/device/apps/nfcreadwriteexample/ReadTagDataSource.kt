package device.apps.nfcreadwriteexample

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcBarcode
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import device.demo.nfc.domain.entities.ParsedRecord
import device.demo.nfc.domain.entities.TextRecord
import device.demo.nfc.domain.entities.UriRecord
import java.io.IOException

class ReadTagDataSource(
    private val context: Context,
) {
    fun readMessage(intent: Intent): String {
        return when (intent.action) {
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                val sb = StringBuilder()
                val rawMsgs: Array<NdefMessage>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
                else
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.mapNotNull { it as? NdefMessage }?.toTypedArray()

                // val tag = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG)
                val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                else
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

                if (tag != null)
                    sb.append(dumpTagData(tag))

                if (rawMsgs != null)
                    sb.append(getRecordData(rawMsgs))

                "$sb"   // result.
            }

            else -> ""
        }
    }

    private fun dumpTagData(tag: Tag): String {
        val sb = StringBuilder()
        sb.append("Technologies=${getTechnologies(tag)}\n")
        for (tech in tag.techList) {
            when (tech) {
                NfcA::class.java.name -> handleNfcA(tag, sb)
                NfcB::class.java.name -> handleNfcB(tag, sb)
                NfcF::class.java.name -> sb.append("Type=Type F\n")
                NfcV::class.java.name -> sb.append("Type=Type V\n")
                NfcBarcode::class.java.name -> handleNfcBarcode(tag, sb)
            }
        }
        sb.append("Tag id=${getHex(tag.id, false)}")
        return sb.toString()
    }

    private fun handleNfcA(tag: Tag, sb: StringBuilder) {
        val nfcA = NfcA.get(tag)
        val isoDep = IsoDep.get(tag)
        val atqa = nfcA?.atqa?.let { getHex(it, true) } ?: ""
        val sak = getHex(shortToByteArray(nfcA?.sak ?: 0))
        val ats = isoDep?.historicalBytes?.let { getHex(it, false) } ?: ""

        val typeResId = getTagIdentifier(atqa, sak, ats)
        val type = if (typeResId == R.string.tag_unknown) {
            when {
                MifareClassic.get(tag) != null -> "MIFARE Classic"
                MifareUltralight.get(tag) != null -> "MIFARE Ultralight"
                else -> context.getString(R.string.tag_unknown)
            }
        } else {
            context.getString(typeResId)
        }

        sb.append("Type=$type\n")
        if (atqa.isNotEmpty()) sb.append("ATQA=$atqa\n")
        if (sak.isNotEmpty()) sb.append("SAK=$sak\n")
    }

    private fun handleNfcB(tag: Tag, sb: StringBuilder) {
        sb.append("Type=Type B\n")
        val isoDep = IsoDep.get(tag)
        try {
            isoDep?.connect()
            if (isoDep?.isConnected == true) {
                val apdu = byteArrayOf(
                    0x00, 0xA4.toByte(), 0x04, 0x00,
                    0x07, 0xD4.toByte(), 0x10, 0x65, 0x09, 0x90.toByte(), 0x00, 0x20, 0x00
                )
                val result = isoDep.transceive(apdu)
                sb.append("SelectAID=${getHex(result, false)}\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun handleNfcBarcode(tag: Tag, sb: StringBuilder) {
        val nfcBarcode = NfcBarcode.get(tag)
        when (nfcBarcode?.type) {
            NfcBarcode.TYPE_KOVIO -> sb.append("Type=KOVIO\n")
            else -> sb.append("Type=Type Barcode\n")
        }
    }

    private fun getTagIdentifier(atqa: String, sak: String, ats: String): Int {
        val prefix = "tag_"
        val combined = arrayOf(atqa, sak, ats).joinToString("")
        val resName = prefix + combined
        val resId = context.resources.getIdentifier(resName, "string", "device.demo.nfc")
        return if (resId != 0) resId else R.string.tag_unknown
    }

    private fun getTechnologies(tag: Tag): String {
        val prefix = "android.nfc.tech."
        return tag.techList.joinToString(", ") { it.removePrefix(prefix) }
    }

    private fun getRecordData(rawMsgs: Array<NdefMessage>): String {
        if (rawMsgs.isEmpty())
            return ""

        val sb = StringBuilder()
        rawMsgs.forEach { rawMsg ->
            val records = parse(rawMsg)
            records.forEach { record ->
                when (record.type) {
                    ParsedRecord.TYPE_TEXT -> sb.append("\nTEXT : ${(record as TextRecord).text}")
                    ParsedRecord.TYPE_URI -> sb.append("\nURI : ${(record as UriRecord).uri}")
                }
            }
        }
        return sb.toString()
    }

    private fun getHex(bytes: ByteArray, reversed: Boolean = false): String {
        val sb = StringBuilder()
        val byteArray = if (reversed) bytes.reversedArray() else bytes
        byteArray.forEach { byte ->
            sb.append(String.format("%02X", byte))
        }
        return sb.toString()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf((value.toInt() shr 8).toByte(), value.toByte())
    }

    private fun parse(message: NdefMessage): List<ParsedRecord> {
        return getRecords(message.records)
    }

    private fun getRecords(records: Array<NdefRecord>): List<ParsedRecord> {
        val elements = mutableListOf<ParsedRecord>()
        for (record in records) {
            when {
                TextRecord.isText(record) -> elements.add(TextRecord.parse(record))
                UriRecord.isUri(record) -> elements.add(UriRecord.parse(record))
            }
        }
        return elements
    }
}