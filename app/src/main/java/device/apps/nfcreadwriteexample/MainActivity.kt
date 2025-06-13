package device.apps.nfcreadwriteexample

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


const val TAG = "NfcReadWriteExample"

class MainActivity : ComponentActivity() {

    companion object {
        private const val NFC_READ = 0
        private const val NFC_WRITE = 1
    }

    private var nfcMode by mutableIntStateOf(value = NFC_READ)
    private var writeMessage by mutableStateOf(value = "")
    private var deferred: CompletableDeferred<Boolean>? = null
    private var readTagMessagesState = SnapshotStateList<String>()

    private lateinit var _nfcAdapter: NfcAdapter
    private lateinit var _readDateSource: ReadTagDataSource
    private lateinit var _writeDataSource: WriteTagDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init.
        _nfcAdapter = NfcAdapter.getDefaultAdapter(this@MainActivity)
        _readDateSource = ReadTagDataSource(context = this@MainActivity)
        _writeDataSource = WriteTagDataSource(context = this@MainActivity, adapter = _nfcAdapter)

        // build ui,
        setContent {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 16.dp),
            ) { innerPadding ->
                BuildMain(
                    innerPadding = innerPadding,
                    enabled = nfcMode == NFC_READ,
                    value = writeMessage,
                    items = readTagMessagesState,
                    onValueChange = { writeMessage = it },
                    onWrite = { registerWriteTimeout() },
                )
            }

            if (nfcMode == NFC_WRITE)
                BuildWriteProgres()

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (nfcMode) {
            NFC_READ -> handleRead(intent)
            NFC_WRITE -> handleWrite(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }

    private fun handleRead(intent: Intent) {
        val parsedMessage = _readDateSource.readMessage(intent)
        readTagMessagesState.add(parsedMessage)
    }

    private fun handleWrite(intent: Intent) {
        releaseTimeout()

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag == null) {
            Toast.makeText(applicationContext, "NFC tag not found. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val techList = tag.techList
        val supportsNdef = techList.contains("android.nfc.tech.Ndef")
        val supportsFormatable = techList.contains("android.nfc.tech.NdefFormatable")

        if (!supportsNdef && !supportsFormatable) {
            Toast.makeText(applicationContext, "This tag is not writable (NDEF not supported)", Toast.LENGTH_SHORT).show()
            return
        }

        val success = _writeDataSource.writeMessage(tag, 0, writeMessage)
        val resultMessage = if (success) {
            "NFC write completed successfully"
        } else {
            "NFC write failed"
        }

        Toast.makeText(applicationContext, resultMessage, Toast.LENGTH_SHORT).show()
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this@MainActivity, 0, intent, PendingIntent.FLAG_MUTABLE)
        val intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
        )
        _nfcAdapter.enableForegroundDispatch(this@MainActivity, pendingIntent, intentFilters, null)
    }

    private fun disableForegroundDispatch() {
        _nfcAdapter.disableForegroundDispatch(this@MainActivity)
    }

    private fun registerWriteTimeout() {
        nfcMode = NFC_WRITE
        deferred?.cancel()
        deferred = CompletableDeferred()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = withTimeout(5000L) { deferred?.await() == true }
                Log.d(TAG, "registerWriteTimeout: result = $result")

            } catch (e: Exception) {
                showToast(message = e.message ?: "NFC write failed")

            } finally {
                deferred = null
                nfcMode = NFC_READ
            }
        }
    }

    private fun releaseTimeout() {
        deferred?.complete(value = true)
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}