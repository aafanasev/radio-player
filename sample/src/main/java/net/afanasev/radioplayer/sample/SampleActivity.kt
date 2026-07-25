package net.afanasev.radioplayer.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

// TODO(#8): replace with a real player screen wired to :core once RadioPlayerConfig
// and the player Compose UI have been extracted from otonfm.
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                Text("radio-player sample")
            }
        }
    }
}
