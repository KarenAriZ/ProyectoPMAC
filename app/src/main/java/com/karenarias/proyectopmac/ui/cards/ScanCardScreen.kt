package com.karenarias.proyectopmac.ui.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karenarias.proyectopmac.data.cards.CardRepository
import com.karenarias.proyectopmac.ui.components.PrimaryButton
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * HU-003: Capturar la foto y datos de la tarjeta física usando la cámara.
 *
 * 1. El usuario toma una foto (o la elige de la galería).
 * 2. ML Kit lee el texto de la foto y se extrae el número de tarjeta.
 * 3. El usuario confirma o corrige el número, pone un alias y el tipo de tarjeta.
 * 4. Se guarda la tarjeta digitalizada en su cuenta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanCardScreen(
    onBack: () -> Unit,
    onCardSaved: (DigitalCard) -> Unit,
    userId: String,
    viewModel: ScanCardViewModel = viewModel()
){
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Cámara: guarda la foto en tamaño completo en el Uri que le damos (mejor para el OCR).
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUri
        if (success && uri != null) viewModel.onPhotoCaptured(context, uri)
    }

    // Galería: alternativa útil para probar en el emulador.
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onPhotoCaptured(context, uri)
    }

    viewModel.savedCard?.let { card ->
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.CheckCircle, null, tint = PurplePrimary) },
            title = { Text("¡Tarjeta digitalizada!", color = PurpleDark) },
            text = { Text("Tu tarjeta ${card.alias} (${card.number}) se agregó a tu cuenta.") },
            confirmButton = {
                TextButton(onClick = {
                    // HU-008: se guarda en "Mis tarjetas" (estado ACTIVA)
                    CardRepository.get(context, userId).add(card.number, card.alias, card.type)
                    onCardSaved(card)
                    viewModel.reset()
                }) { Text("Aceptar", color = PurplePrimary) }
            },
            containerColor = CardWhite
        )
    }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            TopAppBar(
                title = { Text("Agregar tarjeta", color = PurpleDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Toma una foto del lado de tu tarjeta del Metropolitano donde aparece el número. " +
                    "Usa buena luz y evita reflejos.",
                color = PurpleDark.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            PhotoPreview(uri = viewModel.photoUri, isProcessing = viewModel.isProcessing)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val uri = createPhotoUri(context)
                        pendingUri = uri
                        takePictureLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    enabled = !viewModel.isProcessing
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tomar foto")
                }
                OutlinedButton(
                    onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PurplePrimary),
                    enabled = !viewModel.isProcessing
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = PurplePrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Galería", color = PurplePrimary)
                }
            }

            viewModel.ocrMessage?.let { msg ->
                val found = viewModel.cardNumber.isNotEmpty()
                Surface(
                    color = if (found) MauveAccent.copy(alpha = 0.25f) else DustyRose.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (found) Icons.Default.DocumentScanner else Icons.Default.Info,
                            null, tint = PurplePrimary
                        )
                        Text(msg, color = PurpleDark, fontSize = 13.sp)
                    }
                }
            }

            if (viewModel.otherCandidates.isNotEmpty()) {
                Text("¿No es ese? También se detectó:", color = PurpleDark, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.otherCandidates.forEach { candidate ->
                        AssistChip(
                            onClick = { viewModel.onCandidateSelected(candidate) },
                            label = { Text(candidate) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.cardNumber,
                onValueChange = viewModel::onCardNumberChange,
                label = { Text("Número de tarjeta") },
                leadingIcon = { Icon(Icons.Default.CreditCard, null, tint = PurplePrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            OutlinedTextField(
                value = viewModel.alias,
                onValueChange = viewModel::onAliasChange,
                label = { Text("Alias (ej. Tarjeta de mamá)") },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = PurplePrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Text("Tipo de tarjeta", color = PurpleDark, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.cardTypes.forEach { type ->
                    FilterChip(
                        selected = viewModel.cardType == type,
                        onClick = { viewModel.onCardTypeChange(type) },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = CardWhite
                        )
                    )
                }
            }

            viewModel.errorMessage?.let {
                Text(it, color = ErrorWine, fontSize = 13.sp)
            }

            PrimaryButton(
                text = "Guardar tarjeta",
                onClick = { viewModel.saveCard() },
                isLoading = viewModel.isProcessing
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoPreview(uri: Uri?, isProcessing: Boolean) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = uri?.let { loadBitmap(context, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // proporción de una tarjeta (ISO/IEC 7810 ID-1)
            .clip(RoundedCornerShape(16.dp))
            .background(PurpleDark),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Foto de la tarjeta",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CreditCard, null, tint = MauveAccent, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text("Aquí verás la foto de tu tarjeta", color = MauveAccent, fontSize = 13.sp)
            }
        }
        if (isProcessing) {
            Box(
                Modifier.fillMaxSize().background(PurpleDark.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CardWhite)
                    Spacer(Modifier.height(8.dp))
                    Text("Leyendo número de tarjeta…", color = CardWhite, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = MauveAccent,
    focusedLabelColor = PurplePrimary,
    cursorColor = PurplePrimary,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite
)

/** Crea un archivo temporal para la foto y devuelve su Uri seguro (FileProvider). */
private fun createPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "card_photos").apply { mkdirs() }
    val file = File(dir, "card_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Reducimos la imagen para la vista previa y evitar usar mucha memoria.
            val maxSide = 1280
            val w = info.size.width
            val h = info.size.height
            val scale = maxOf(1, maxOf(w, h) / maxSide)
            decoder.setTargetSize(w / scale, h / scale)
        }
    }.getOrNull()
}
