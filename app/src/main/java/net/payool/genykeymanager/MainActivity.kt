package net.payool.genykeymanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
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

private val Ink = Color(0xFF081321)
private val Panel = Color(0xFF122238)
private val PanelLight = Color(0xFF1A3048)
private val Mint = Color(0xFF25D6A0)
private val Muted = Color(0xFF9CB0C5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GenyKeyManagerScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenyKeyManagerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var deviceId by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var customDuration by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var imported by remember { mutableStateOf(prefs.contains(KEY_CIPHER)) }
    var history by remember { mutableStateOf(readHistory(prefs)) }
    var durationExpanded by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(true) }
    val pulse = rememberInfiniteTransition(label = "brand-pulse")
    val glow by pulse.animateFloat(initialValue = 0.82f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    val importKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            val pem = context.contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream(); input.copyTo(output); output.toByteArray()
            } ?: error("Impossible de lire le fichier.")
            val privateKey = parsePrivateKey(pem)
            verifyKeyPair(privateKey)
            storeEncryptedKey(context, pem)
            imported = true
            status = "Clé importée et paire RSA vérifiée avec succès."
        } catch (e: Exception) {
            status = "Import refusé : ${e.message ?: "fichier PEM invalide"}"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint, onPrimary = Ink, secondary = Color(0xFF65E8C1),
            background = Ink, surface = Panel, surfaceVariant = PanelLight,
            onBackground = Color.White, onSurface = Color.White,
            onSurfaceVariant = Muted, error = Color(0xFFFF7C89)
        ),
        typography = Typography(
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    ) {
        Scaffold(containerColor = Ink, topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GENY KEY MANAGER", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, letterSpacing = 1.4.sp)
                        Text("SECURE ACTIVATION CONSOLE", style = MaterialTheme.typography.labelSmall, color = Muted, letterSpacing = 1.8.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Ink, titleContentColor = Color.White)
            )
        }) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brand hero
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().background(Panel).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Image(painterResource(R.drawable.ic_geny_logo), null, modifier = Modifier.size(76.dp).alpha(glow))
                                Column(Modifier.weight(1f)) {
                                    Text("GENY", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                                    Text("KEY MANAGER", style = MaterialTheme.typography.titleMedium, color = Mint, letterSpacing = 2.sp)
                                    Text("La maîtrise des activations, en toute sécurité.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF29415A))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(if (imported) Mint else Color(0xFFFFB454)))
                                Text(if (imported) "COFFRE CRYPTÉ • CLÉ PRÊTE" else "CONFIGURATION REQUISE • CLÉ ABSENTE", color = if (imported) Mint else Color(0xFFFFB454), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                SectionHeading("01", "SÉCURITÉ DE LA CLÉ", Icons.Default.Security)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Mint, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (imported) "Clé privée protégée" else "Importer la clé administrateur", fontWeight = FontWeight.Bold)
                                Text(if (imported) "Stockage local chiffré avec Android Keystore." else "Importe le fichier PEM original pour activer la signature.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(if (imported) Icons.Default.CheckCircle else Icons.Default.Key, null, tint = if (imported) Mint else Muted)
                        }
                        Button(onClick = { importKey.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Key, null); Spacer(Modifier.width(8.dp))
                            Text(if (imported) "REMPLACER LA CLÉ PRIVÉE" else "IMPORTER LA CLÉ PRIVÉE")
                        }
                    }
                }

                SectionHeading("02", "NOUVELLE ACTIVATION", Icons.Default.Key)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = deviceId, onValueChange = { deviceId = it; status = ""; code = "" },
                            label = { Text("DEVICE_ID DU CLIENT") }, placeholder = { Text("Coller l'identifiant exact") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mint, focusedLabelColor = Mint, cursorColor = Mint)
                        )
                        ExposedDropdownMenuBox(expanded = durationExpanded, onExpandedChange = { durationExpanded = it }) {
                            OutlinedTextField(
                                value = when (duration) { "never" -> "Illimitée"; "custom" -> "Personnalisée"; else -> "$duration jours" },
                                onValueChange = {}, readOnly = true, label = { Text("DURÉE DE VALIDITÉ") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(durationExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mint, focusedLabelColor = Mint)
                            )
                            ExposedDropdownMenu(expanded = durationExpanded, onDismissRequest = { durationExpanded = false }) {
                                listOf("7" to "7 jours", "30" to "30 jours", "90" to "90 jours", "365" to "365 jours", "custom" to "Personnalisée", "never" to "Illimitée").forEach { (v, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { duration = v; durationExpanded = false })
                                }
                            }
                        }
                        AnimatedVisibility(duration == "custom") {
                            OutlinedTextField(value = customDuration, onValueChange = { customDuration = it.filter(Char::isDigit).take(4) }, label = { Text("Nombre de jours (1 à 3650)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                        }
                        Button(
                            enabled = imported && deviceId.isNotBlank(),
                            onClick = {
                                try {
                                    val id = deviceId.trim()
                                    require(id.isNotEmpty() && id.length <= 256 && !id.contains('|') && !id.contains('\n') && !id.contains('\r')) { "DEVICE_ID invalide." }
                                    val validity = when (duration) {
                                        "never" -> 0L
                                        "custom" -> customDuration.toLongOrNull()?.also { require(it in 1..3650) { "Durée personnalisée : 1 à 3650 jours." } } ?: error("Saisis une durée valide.")
                                        else -> duration.toLong()
                                    }
                                    val signingKey = parsePrivateKey(loadEncryptedKey(context))
                                    verifyKeyPair(signingKey)
                                    val expiry = if (validity == 0L) 0L else System.currentTimeMillis() / 1000L + validity * 86400L
                                    val serial = ByteArray(12).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                                    val payload = "GENY1|$id|$expiry|$serial"
                                    val signer = Signature.getInstance("SHA256withRSA")
                                    signer.initSign(signingKey); signer.update(payload.toByteArray(Charsets.UTF_8))
                                    val encoder = Base64.getUrlEncoder().withoutPadding()
                                    code = encoder.encodeToString(payload.toByteArray(Charsets.UTF_8)) + "." + encoder.encodeToString(signer.sign())
                                    val record = JSONObject().put("deviceId", id).put("expiresAt", expiry).put("createdAt", System.currentTimeMillis()).put("code", code)
                                    val array = JSONArray(prefs.getString(HISTORY, "[]") ?: "[]")
                                    val updated = JSONArray().put(record)
                                    for (i in 0 until minOf(array.length(), 99)) updated.put(array.getJSONObject(i))
                                    prefs.edit().putString(HISTORY, updated.toString()).apply()
                                    history = readHistory(prefs)
                                    status = "Activation signée et enregistrée avec succès."
                                } catch (e: Exception) { code = ""; status = e.message ?: "Erreur pendant la génération." }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Security, null); Spacer(Modifier.width(9.dp))
                            Text("GÉNÉRER LE CODE SÉCURISÉ", fontWeight = FontWeight.Bold)
                        }
                        AnimatedVisibility(status.isNotBlank()) {
                            Text(status, color = if (status.startsWith("Erreur") || status.startsWith("Import refusé")) MaterialTheme.colorScheme.error else Mint, style = MaterialTheme.typography.bodySmall)
                        }
                        AnimatedVisibility(code.isNotBlank()) {
                            Card(colors = CardDefaults.cardColors(containerColor = PanelLight), shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("CODE D'ACTIVATION", color = Mint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    SelectionContainer { Text(code, style = MaterialTheme.typography.bodySmall, color = Color.White) }
                                    OutlinedButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code Geny-Abess", code))
                                        Toast.makeText(context, "Code copié", Toast.LENGTH_SHORT).show()
                                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("COPIER LE CODE")
                                    }
                                }
                            }
                        }
                    }
                }

                SectionHeading("03", "HISTORIQUE DES ACTIVATIONS", Icons.Default.History, trailing = {
                    TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "MASQUER" else "AFFICHER") }
                })
                AnimatedVisibility(showHistory) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (history.isEmpty()) Text("Aucune activation enregistrée sur cet appareil.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        history.forEach { item ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(item.optString("deviceId"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (item.optLong("expiresAt") == 0L) "Expiration : illimitée" else "Expiration : " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.optLong("expiresAt") * 1000L)), color = Muted, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { code = item.optString("code"); status = "Code restauré depuis l'historique." }) { Text("AFFICHER / COPIER LE CODE") }
                                }
                            }
                        }
                        if (history.isNotEmpty()) TextButton(onClick = { prefs.edit().remove(HISTORY).apply(); history = emptyList(); code = "" }) { Text("EFFACER L'HISTORIQUE", color = MaterialTheme.colorScheme.error) }
                    }
                }

                SectionHeading("04", "ASSISTANCE & DÉVELOPPEUR", Icons.Default.Send)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ABESS237", style = MaterialTheme.typography.titleLarge, color = Mint)
                        Text("Développeur • GENY KEY MANAGER", color = Muted, style = MaterialTheme.typography.bodySmall)
                        ContactRow("Telegram", "@abess237", "https://t.me/abess237") { openLink(context, "https://t.me/abess237") }
                        HorizontalDivider(color = Color(0xFF29415A))
                        ContactRow("WhatsApp", "+237 695 140 419", "https://wa.me/237695140419") { openLink(context, "https://wa.me/237695140419") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("GENY KEY MANAGER  •  VERSION 1.2.0\nConçu avec soin par ABESS237", color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(number: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF163D42)), contentAlignment = Alignment.Center) {
            Text(number, color = Mint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(10.dp))
        Icon(icon, null, tint = Mint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun ContactRow(label: String, value: String, url: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, modifier = Modifier.width(86.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("OUVRIR ↗", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

private fun openLink(context: Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    catch (_: Exception) { Toast.makeText(context, "Aucune application compatible trouvée.", Toast.LENGTH_SHORT).show() }
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
    val expected = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(ACTIVATION_PUBLIC_KEY))) as RSAPublicKey
    require(rsa.modulus == expected.modulus && rsa.publicExponent == expected.publicExponent) {
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
