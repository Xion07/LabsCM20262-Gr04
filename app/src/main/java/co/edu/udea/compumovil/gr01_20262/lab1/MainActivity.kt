package co.edu.udea.compumovil.gr04_20262.lab1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme

/**
 * Punto de entrada de la aplicación. Solo contiene dos botones que
 * llevan a las dos actividades pedidas en el laboratorio:
 * PersonalDataActivity y ContactDataActivity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab1UITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.main_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { context.startActivity(Intent(context, PersonalDataActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_personal_data))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { context.startActivity(Intent(context, ContactDataActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_contact_data))
        }
    }
}
