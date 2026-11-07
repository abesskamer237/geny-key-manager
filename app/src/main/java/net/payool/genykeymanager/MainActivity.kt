package net.payool.genykeymanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREFS = "geny_key_manager"
private const val KEY_CIPHER = "encrypted_private_key"
private const val KEY_IV = "private_key_iv"
private const val HISTORY = "activation_history"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val AES_ALIAS = "GenyKeyManagerAesKey"
private const val ACTIVATION_PUBLIC_KEY = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA0kERdsCWLZA95JA62TghJ9aiI5xpgfVP+gzg9f/eoB8K0fUSF7PSmtuCJVT1tNCSyH+YGM73QnaVKjfAPC4dRfPPiHM/rXNuF94s0fFYixzAWIE014Po+E7xXYVUCGzPVELp3nFHb7Jr4BU9ukiVD11COrBzBZN0CCTLpLPci9tbRKxl4Ou5ajLAfwzJfvS4II3kPX+Lej901oeZ8k9uW7j2HyeNhJvycJtwFORC59QoH1WTbXdaDSR32cYQEeFyaTW3M1lHaeeTseX0VnihFnJqnfCxQI5rkHes6aUD3OsRwLJ+h0llVxiMIACBQOgYsAPdXhRjiE+vs42Yyy9xJ5fQPTec0Qo2PfOOCHS95qLJ+1B8yh6aMOPOUz21slGuNLVzcODm4yPKlElvgaw/t1nviu773QlsNDZBC1Oh8mC7X67dHCP5lXIlWpYgjQg77aOKWdSnIJoIcGEcthwpPBToOYPlrnRsrrTAzr92ifkQp2V9rUBw2ATR9HOesdYHAgMBAAE="

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
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var deviceId by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var customDuration by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var imported by remember { mutableStateOf(prefs.contains(KEY_CIPHER)) }
    var history by remember { mutableStateOf(readHistory(prefs)) }
    var durationExpanded by remember { mutableStateOf(false) }

    val importKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val pem = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    input.copyTo(output)
                    output.toByteArray()
                } ?: error("Impossible de lire le fichier.")
                val privateKey = parsePrivateKey(pem)
                verifyKeyPair(privateKey)
                storeEncryptedKey(context, pem)
                imported = true
                status = "Clé importée et paire RSA vérifiée."
            } catch (e: Exception) {
                status = "Import refusé : ${e.message ?: "fichier PEM invalide"}"
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF25C18A), background = Color(0xFF101A2B), surface = Color(0xFF1B2A40))) {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(title = { Text("GENY KEY MANAGER", fontWeight = FontWeight.Bold) })
        }) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("GESTION DES ACCÈS GENY-ABESS", style = MaterialTheme.typography.titleMedium)
                Text(if (imported) "Clé privée installée sur cet appareil." else "Aucune clé installée. Importe la clé privée originale.")
                Button(onClick = { importKey.launch(arrayOf("application/x-pem-file", "application/octet-stream", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (imported) "REMPLACER / RÉIMPORTER LA CLÉ" else "IMPORTER LA CLÉ PRIVÉE PEM")
                }
                OutlinedTextField(
                    value = deviceId, onValueChange = { deviceId = it; status = ""; code = "" },
                    label = { Text("DEVICE_ID du client") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                ExposedDropdownMenuBox(expanded = durationExpanded, onExpandedChange = { durationExpanded = it }) {
                    OutlinedTextField(
                        value = when (duration) { "never" -> "Illimitée"; "custom" -> "Personnalisée"; else -> "$duration jours" },
                        onValueChange = {}, readOnly = true, label = { Text("Durée d'accès") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(durationExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = durationExpanded, onDismissRequest = { durationExpanded = false }) {
                        listOf("7" to "7 jours", "30" to "30 jours", "90" to "90 jours", "365" to "365 jours", "custom" to "Personnalisée", "never" to "Illimitée").forEach { (v, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { duration = v; durationExpanded = false })
                        }
                    }
                }
                if (duration == "custom") {
                    OutlinedTextField(value = customDuration, onValueChange = { customDuration = it.filter(Char::isDigit) }, label = { Text("Nombre de jours (1 à 3650)") }, modifier = Modifier.fillMaxWidth())
                }
                Button(
                    enabled = imported,
                    onClick = {
                        try {
                            val id = deviceId.trim()
                            require(id.isNotEmpty() && id.length <= 256 && !id.contains('|') && !id.contains('\n') && !id.contains('\r')) { "DEVICE_ID invalide." }
                            val validity = when (duration) {
                                "never" -> 0L
                                "custom" -> customDuration.toLongOrNull()?.also { require(it in 1..3650) { "Durée personnalisée : 1 à 3650 jours." } } ?: error("Saisis une durée valide.")
                                else -> duration.toLong()
                            }
                            val pem = loadEncryptedKey(context)
                            val signingKey = parsePrivateKey(pem)
                            verifyKeyPair(signingKey)
                            val expiry = if (validity == 0L) 0L else System.currentTimeMillis() / 1000L + validity * 86400L
                            val serialBytes = ByteArray(12).also { SecureRandom().nextBytes(it) }
                            val serial = serialBytes.joinToString("") { "%02x".format(it) }
                            val payload = "GENY1|$id|$expiry|$serial"
                            val signer = Signature.getInstance("SHA256withRSA")
                            signer.initSign(signingKey)
                            signer.update(payload.toByteArray(Charsets.UTF_8))
                            val signature = signer.sign()
                            val encoder = Base64.getUrlEncoder().withoutPadding()
                            code = encoder.encodeToString(payload.toByteArray(Charsets.UTF_8)) + "." + encoder.encodeToString(signature)
                            val record = JSONObject().put("deviceId", id).put("expiresAt", expiry).put("createdAt", System.currentTimeMillis()).put("code", code)
                            val array = JSONArray(prefs.getString(HISTORY, "[]") ?: "[]")
                            val updated = JSONArray().put(record)
                            for (i in 0 until minOf(array.length(), 99)) updated.put(array.getJSONObject(i))
                            prefs.edit().putString(HISTORY, updated.toString()).apply()
                            history = readHistory(prefs)
                            status = "Code généré. Paire RSA vérifiée avec Geny-Abess."
                        } catch (e: Exception) {
                            code = ""
                            status = e.message ?: "Erreur pendant la génération."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("GÉNÉRER LE CODE D'ACTIVATION") }

                if (status.isNotBlank()) Text(status, color = if (status.startsWith("Erreur") || status.startsWith("Import refusé")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                if (code.isNotBlank()) {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("CODE D'ACTIVATION", fontWeight = FontWeight.Bold)
                            SelectionContainer { Text(code, style = MaterialTheme.typography.bodySmall) }
                            Button(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Code Geny-Abess", code))
                                Toast.makeText(context, "Code copié", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("COPIER LE CODE") }
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HISTORIQUE LOCAL", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { prefs.edit().remove(HISTORY).apply(); history = emptyList() }) { Text("EFFACER") }
                }
                if (history.isEmpty()) Text("Aucune activation enregistrée.")
                history.forEach { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.optString("deviceId"), fontWeight = FontWeight.Bold)
                            Text(if (item.optLong("expiresAt") == 0L) "Expiration : illimitée" else "Expiration : " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.optLong("expiresAt") * 1000L)))
                            TextButton(onClick = { code = item.optString("code") }) { Text("AFFICHER / RECOPIER LE CODE") }
                        }
                    }
                }
                Text("La clé est chiffrée localement avec Android Keystore. Elle n'est jamais envoyée au réseau ni intégrée à l'APK.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun parsePrivateKey(pemBytes: ByteArray): java.security.PrivateKey {
    val pem = String(pemBytes, Charsets.UTF_8)
    val base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").filterNot(Char::isWhitespace)
    require(base64.isNotBlank() && !pem.contains("BEGIN RSA PRIVATE KEY")) { "Format requis : PEM PKCS#8 (BEGIN PRIVATE KEY)." }
    val der = Base64.getDecoder().decode(base64)
    return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
}

private fun verifyKeyPair(privateKey: java.security.PrivateKey) {
    val rsa = privateKey as? RSAPrivateCrtKey ?: error("Clé RSA PKCS#8 avec paramètres CRT requise.")
    val expected = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(ACTIVATION_PUBLIC_KEY)))
    require(rsa.modulus == (expected as java.security.interfaces.RSAPublicKey).modulus &&
        rsa.publicExponent == expected.publicExponent) {
        "Cette clé privée ne correspond pas à la clé publique de l'APK Geny-Abess. Aucune activation créée."
    }
}

private fun getAesKey(): SecretKey {
    val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (ks.getKey(AES_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    generator.init(KeyGenParameterSpec.Builder(AES_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
    return generator.generateKey()
}

private fun storeEncryptedKey(context: Context, pem: ByteArray) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getAesKey())
    val encrypted = cipher.doFinal(pem)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_CIPHER, Base64.getEncoder().encodeToString(encrypted))
        .putString(KEY_IV, Base64.getEncoder().encodeToString(cipher.iv)).apply()
}

private fun loadEncryptedKey(context: Context): ByteArray {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val encrypted = Base64.getDecoder().decode(prefs.getString(KEY_CIPHER, null) ?: error("Importe d'abord la clé privée."))
    val iv = Base64.getDecoder().decode(prefs.getString(KEY_IV, null) ?: error("Données de clé incomplètes. Réimporte la clé."))
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(128, iv))
    return cipher.doFinal(encrypted)
}

private fun readHistory(prefs: android.content.SharedPreferences): List<JSONObject> = try {
    val array = JSONArray(prefs.getString(HISTORY, "[]") ?: "[]")
    (0 until array.length()).map { array.getJSONObject(it) }
} catch (_: Exception) { emptyList() }
