package androidx.wear.compose.material.samples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
)
@Composable
fun ScalingLazyColumnPreview() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(100) {
            Text("Item $it")
        }
    }
}
