package com.karenarias.proyectopmac.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.karenarias.proyectopmac.data.recharge.*
import com.karenarias.proyectopmac.ui.card.SimpleTopBar
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.launch

/** Si el punto más cercano está más lejos que esto, el mapa se queda centrado en Lima. */
private const val MAX_DISTANCE_TO_CENTER_ON_USER_METERS = 20_000.0

/**
 * HU-006 (ruta con estado): pide los permisos de ubicación y notificaciones, escucha la
 * ubicación mientras la pantalla está abierta y conecta todo con la pantalla.
 */
@Composable
fun RechargeMapRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RechargeMapViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(isLocationPermissionGranted(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onNotificationPermissionChanged() }

    fun requestNotificationsIfNeeded() {
        if (viewModel.alertsEnabled && !viewModel.notificationsAllowed &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it } || isLocationPermissionGranted(context)
        // Se pide el permiso de notificaciones después, para no mostrar dos diálogos a la vez.
        requestNotificationsIfNeeded()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            requestNotificationsIfNeeded()
        }
    }

    // Al volver desde Ajustes se revisan de nuevo los permisos.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = isLocationPermissionGranted(context)
                viewModel.onNotificationPermissionChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LocationUpdatesEffect(enabled = hasLocationPermission) { lat, lng ->
        viewModel.onLocationUpdate(lat, lng)
    }

    RechargeMapScreen(
        points = viewModel.points,
        nearby = viewModel.nearby,
        userLatitude = viewModel.userLatitude,
        userLongitude = viewModel.userLongitude,
        closeByPoint = viewModel.closeByPoint,
        hasLocationPermission = hasLocationPermission,
        alertsEnabled = viewModel.alertsEnabled,
        notificationsAllowed = viewModel.notificationsAllowed,
        mapsApiKeyMissing = viewModel.mapsApiKeyMissing,
        onBack = onBack,
        onRequestLocation = {
            locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        },
        onAlertsEnabledChanged = { enabled ->
            viewModel.onAlertsEnabledChanged(enabled)
            if (enabled) requestNotificationsIfNeeded()
        },
        onOpenDirections = { point -> openWalkingDirections(context, point) },
        modifier = modifier
    )
}

/**
 * HU-006: mapa con los puntos de recarga física, lista de los más cercanos y
 * configuración de alertas de paraderos próximos.
 */
@Composable
fun RechargeMapScreen(
    points: List<RechargePoint>,
    nearby: List<NearbyPoint>?,
    userLatitude: Double?,
    userLongitude: Double?,
    closeByPoint: NearbyPoint?,
    hasLocationPermission: Boolean,
    alertsEnabled: Boolean,
    notificationsAllowed: Boolean,
    mapsApiKeyMissing: Boolean,
    onBack: () -> Unit,
    onRequestLocation: () -> Unit,
    onAlertsEnabledChanged: (Boolean) -> Unit,
    onOpenDirections: (RechargePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(DEFAULT_MAP_LATITUDE, DEFAULT_MAP_LONGITUDE), 13f)
    }
    val scope = rememberCoroutineScope()
    var selectedPointId by rememberSaveable { mutableStateOf<String?>(null) }
    var centeredOnUser by rememberSaveable { mutableStateOf(false) }
    val nearestDistance = nearby?.firstOrNull()?.distanceMeters
    val userIsFar = nearestDistance != null && nearestDistance > MAX_DISTANCE_TO_CENTER_ON_USER_METERS

    // La primera vez que llega la ubicación, el mapa se centra en el usuario (si está en Lima).
    LaunchedEffect(userLatitude, userLongitude) {
        if (!centeredOnUser && userLatitude != null && userLongitude != null && !userIsFar) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(userLatitude, userLongitude), 15f)
            centeredOnUser = true
        }
    }

    fun focusOn(point: RechargePoint) {
        selectedPointId = point.id
        scope.launch {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), 16f),
                    600
                )
            } catch (e: Exception) {
                // El mapa aún no terminó de cargar (por ejemplo, sin API key); se ignora.
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCream,
        topBar = { SimpleTopBar(title = "Puntos de recarga", onBack = onBack) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
            ) {
                if (mapsApiKeyMissing) {
                    // Sin API key: se muestra el mapa de Google My Maps que compartió el equipo.
                    MyMapsEmbed(modifier = Modifier.fillMaxSize())
                } else GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = hasLocationPermission,
                        zoomControlsEnabled = false,
                        mapToolbarEnabled = true
                    )
                ) {
                    points.forEach { point ->
                        key(point.id) {
                            val distance = nearby?.firstOrNull { it.point.id == point.id }?.distanceMeters
                            Marker(
                                state = rememberMarkerState(
                                    key = point.id,
                                    position = LatLng(point.latitude, point.longitude)
                                ),
                                title = point.name,
                                snippet = if (distance != null) "${point.district} · a ${formatDistance(distance)}"
                                else point.district,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (point.id == selectedPointId) BitmapDescriptorFactory.HUE_ROSE
                                    else BitmapDescriptorFactory.HUE_VIOLET
                                ),
                                onClick = {
                                    selectedPointId = point.id
                                    false // false = el mapa muestra el globo con el nombre
                                }
                            )
                        }
                    }
                    if (userLatitude != null && userLongitude != null) {
                        Circle(
                            center = LatLng(userLatitude, userLongitude),
                            radius = NEARBY_ALERT_RADIUS_METERS,
                            fillColor = PurplePrimary.copy(alpha = 0.12f),
                            strokeColor = PurplePrimary,
                            strokeWidth = 3f
                        )
                    }
                }

            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                color = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (closeByPoint != null) {
                        item {
                            InfoBanner(
                                text = "Estás a ${formatDistance(closeByPoint.distanceMeters)} de " +
                                    "${closeByPoint.point.name}. Puedes recargar tu tarjeta ahí.",
                                isWarning = false
                            )
                        }
                    }

                    if (!hasLocationPermission) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Activa tu ubicación para ver los puntos más cercanos y recibir alertas.",
                                    color = PurpleDark,
                                    fontSize = 13.sp
                                )
                                TextButton(onClick = onRequestLocation, contentPadding = PaddingValues(0.dp)) {
                                    Text("Permitir ubicación", color = PurplePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (userIsFar && nearestDistance != null) {
                        item {
                            Text(
                                "Estás a ${formatDistance(nearestDistance)} del punto más cercano. " +
                                    "En el emulador, cambia la ubicación a Lima para probar las alertas.",
                                color = PurpleDark.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    item {
                        AlertsSwitchRow(
                            alertsEnabled = alertsEnabled,
                            notificationsAllowed = notificationsAllowed,
                            onAlertsEnabledChanged = onAlertsEnabledChanged
                        )
                    }

                    item {
                        Text(
                            text = if (nearby != null) "Más cercanos a ti" else "Estaciones con recarga",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleDark
                        )
                    }

                    val rows: List<Pair<RechargePoint, Double?>> =
                        nearby?.map { it.point to it.distanceMeters } ?: points.map { it to null }

                    items(rows, key = { it.first.id }) { (point, distance) ->
                        RechargePointRow(
                            point = point,
                            distanceMeters = distance,
                            isSelected = point.id == selectedPointId,
                            onClick = { focusOn(point) },
                            onDirections = { onOpenDirections(point) }
                        )
                    }

                    item {
                        Text(
                            "Ubicaciones referenciales. Verifica el horario de atención en la estación.",
                            fontSize = 11.sp,
                            color = PurpleDark.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsSwitchRow(
    alertsEnabled: Boolean,
    notificationsAllowed: Boolean,
    onAlertsEnabledChanged: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PurplePrimary)
        Column(Modifier.weight(1f)) {
            Text("Alertas de paraderos próximos", color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = when {
                    !alertsEnabled -> "Desactivadas"
                    !notificationsAllowed -> "Activa las notificaciones de MetroGo para recibirlas"
                    else -> "Te avisamos al estar a menos de ${NEARBY_ALERT_RADIUS_METERS.toInt()} m"
                },
                color = if (alertsEnabled && !notificationsAllowed) ErrorWine else PurpleDark.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = alertsEnabled,
            onCheckedChange = onAlertsEnabledChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CardWhite,
                checkedTrackColor = PurplePrimary,
                uncheckedBorderColor = MauveAccent,
                uncheckedThumbColor = MauveAccent
            )
        )
    }
}

@Composable
private fun RechargePointRow(
    point: RechargePoint,
    distanceMeters: Double?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDirections: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MauveAccent.copy(alpha = 0.25f) else BackgroundCream)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PurplePrimary)
        Column(Modifier.weight(1f)) {
            Text(
                point.name,
                color = PurpleDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (distanceMeters != null) "${point.district} · ${formatDistance(distanceMeters)}"
                else point.district,
                color = PurpleDark.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onDirections) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = "Cómo llegar a ${point.name}",
                tint = PurplePrimary
            )
        }
    }
}

@Composable
private fun InfoBanner(text: String, isWarning: Boolean, modifier: Modifier = Modifier) {
    val color = if (isWarning) ErrorWine else PurplePrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isWarning) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = color
        )
        Text(text, color = PurpleDark, fontSize = 13.sp)
    }
}

/** Mapa de Google My Maps compartido por el equipo (no necesita API key). */
private const val MY_MAPS_EMBED_URL =
    "https://www.google.com/maps/d/embed?mid=1yAw_GY9R8jQ28znyNOSaeTL5nRcSas-6&ehbc=2E312F"

@SuppressLint("SetJavaScriptEnabled") // My Maps necesita JavaScript para dibujar el mapa
@Composable
private fun MyMapsEmbed(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient() // mantiene la navegación dentro del mapa
                loadUrl(MY_MAPS_EMBED_URL)
            }
        },
        onRelease = { it.destroy() }
    )
}

/** Escucha la ubicación del usuario mientras la pantalla esté visible en la app. */
@SuppressLint("MissingPermission") // solo se activa cuando enabled = true (permiso concedido)
@Composable
private fun LocationUpdatesEffect(enabled: Boolean, onLocation: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val currentOnLocation by rememberUpdatedState(onLocation)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { currentOnLocation(it.latitude, it.longitude) }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        try {
            client.lastLocation.addOnSuccessListener { location ->
                location?.let { currentOnLocation(it.latitude, it.longitude) }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // El permiso se retiró mientras la pantalla estaba abierta.
        }

        onDispose { client.removeLocationUpdates(callback) }
    }
}

private fun isLocationPermissionGranted(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Abre Google Maps con la ruta a pie hasta el punto de recarga. */
private fun openWalkingDirections(context: Context, point: RechargePoint) {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=${point.latitude},${point.longitude}&travelmode=walking"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No hay app de mapas ni navegador; no se puede abrir la ruta.
    }
}
