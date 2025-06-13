package device.apps.nfcreadwriteexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BuildMain(
    innerPadding: PaddingValues,
    enabled: Boolean,
    value: String,
    items: List<String>,
    onValueChange: (String) -> Unit,
    onWrite: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(paddingValues = innerPadding),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            value = value,
            onValueChange = onValueChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(size = 10.dp),
            enabled = enabled,
            content = { Text(text = "WRITE") },
            onClick = onWrite,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(items) { message -> BuildReadMessageItem(message = message) }
        }
    }
}

@Composable
fun BuildWriteProgres() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0x68000000)),
        contentAlignment = Alignment.Center,
        content = { CircularProgressIndicator() },
    )
}

@Composable
private fun BuildReadMessageItem(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(10.dp),
        content = {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
            )
        },
    )
}