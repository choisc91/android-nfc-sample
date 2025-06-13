package device.demo.nfc.domain.entities

interface ParsedRecord {
    val type: Int

    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_URI = 2
    }
}