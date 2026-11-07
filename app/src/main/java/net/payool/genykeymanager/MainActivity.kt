package net.payool.genykeymanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GenyKeyManagerScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenyKeyManagerScreen() {
    val context = LocalContext.current
    var deviceId by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("30") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(loadHistory(context)) }
    val keyFile = File(context.filesDir, "private/geny-abess-private.pem")

    MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF25C18A))) {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(title = {
                Text("GENY KEY MANAGER", fontWeight = FontWeight.Bold)
            })
        }) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ADMINISTRATION DES ACTIVATIONS", style = MaterialTheme.typography.titleMedium)
                Text("Entre le DEVICE_ID du client et choisis la durée de son accès.")
                OutlinedTextField(
                    value = deviceId, onValueChange = { deviceId = it.trim(); error = "" },
                    label = { Text("DEVICE_ID du client") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                var expanded by remember { mutableStateOf(false) }
                val options = listOf("7" to "7 jours", "30" to "30 jours", "90" to "90 jours", "365" to "365 jours", "never" to "Illimitée")
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = options.firstOrNull { it.first == days }?.second ?: "$days jours",
                        onValueChange = {}, readOnly = true, label = { Text("Durée de validité") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { days = value; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = if (days == "never") "Illimitée" else days,
                    onValueChange = { if (it.all(Char::isDigit)) days = it },
                    label = { Text("Durée personnalisée (jours)") },
                    supportingText = { Text("Valeur de 1 à 3650. Choisir une durée prédéfinie pour l'illimité.") },
                    enabled = days != "never", modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        error = ""
                        code = ""
                        val id = deviceId.trim()
                        val duration = days.trim()
                        if (id.isBlank() || id.length > 256 || id.contains('|') || id.contains('\n')) {
                            error = "Saisis un DEVICE_ID valide."
                        } else if (duration != "never" && (duration.toIntOrNull() !in 1..3650)) {
                            error = "La durée doit être comprise entre 1 et 3650 jours."
                        } else {
                            error = "La clé privée doit d'abord être installée dans le stockage privé de l'application. Consulte le README : l'import sécurisé sera activé dans la prochaine étape."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("GÉNÉRER LE CODE") }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                if (code.isNotBlank()) {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("CODE D'ACTIVATION", fontWeight = FontWeight.Bold)
                            Text(code)
                            Button(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Code Geny-Abess", code))
                                Toast.makeText(context, "Code copié", Toast.LENGTH_SHORT).show()
                            }) { Text("COPIER") }
                        }
                    }
                }
                HorizontalDivider()
                Text("HISTORIQUE LOCAL", style = MaterialTheme.typography.titleMedium)
                if (history.isEmpty()) Text("Aucune activation enregistrée.")
                history.take(20).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Clé attendue : private/geny-abess-private.pem. Ne partage jamais cette clé.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun loadHistory(context: Context): List<String> =
    context.getSharedPreferences("activation_history", Context.MODE_PRIVATE)
        .getString("items", "")!!.split("\n").filter { it.isNotBlank() }
