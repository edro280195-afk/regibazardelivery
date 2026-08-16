package com.example.regibazarapp.ui.screens.routes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddRoad
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.data.models.CamiAlertDto
import com.example.regibazarapp.data.models.CommonProductDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.theme.LocalThemeController
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutesListScreen(
    onManageRoute: (routeId: Int) -> Unit,
    onOpenDriver: (driverToken: String) -> Unit,
    onCreateRoute: () -> Unit,
    onOpenInventory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: RoutesListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    var selectedTabName by rememberSaveable { mutableStateOf(RoutesListTab.PENDING.name) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    val selectedTab = RoutesListTab.valueOf(selectedTabName)
    val visibleRoutes = remember(uiState.routes, searchQuery, selectedTab) {
        routesForTab(uiState.routes, searchQuery, selectedTab)
    }

    Scaffold(
        topBar = {
            RoutesHeader(
                palette = palette,
                alerts = uiState.camiAlerts,
                commonProducts = uiState.commonProducts,
                isOffline = uiState.isOffline,
                isSearchVisible = isSearchVisible,
                onSearchClick = { isSearchVisible = !isSearchVisible },
                onRefresh = { viewModel.loadRoutes(showLoading = false) },
                onOpenInventory = onOpenInventory,
                onLogout = {
                    viewModel.logout()
                    onLogout()
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoute,
                icon = { Icon(Icons.Rounded.AddRoad, contentDescription = null) },
                text = { Text("Nueva ruta", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                containerColor = palette.accent,
                contentColor = Color(0xFFF8FAFF)
            )
        },
        containerColor = palette.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(palette.background)
        ) {
            RoutesSegmentedControl(
                selectedTab = selectedTab,
                palette = palette,
                onTabSelected = { selectedTabName = it.name }
            )

            AnimatedVisibility(
                visible = isSearchVisible,
                enter = fadeIn(tween(160)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                RoutesSearchField(
                    query = searchQuery,
                    palette = palette,
                    onQueryChange = { searchQuery = it },
                    onClose = {
                        searchQuery = ""
                        isSearchVisible = false
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.loadRoutes(showLoading = false) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> RoutesLoadingState(palette)
                    uiState.error != null && uiState.routes.isEmpty() -> {
                        RoutesErrorState(
                            message = uiState.error.orEmpty(),
                            palette = palette,
                            onRetry = { viewModel.loadRoutes() }
                        )
                    }
                    uiState.routes.isEmpty() -> {
                        RoutesEmptyState(
                            palette = palette,
                            onCreateRoute = onCreateRoute
                        )
                    }
                    else -> {
                        RoutesList(
                            routes = visibleRoutes,
                            selectedTab = selectedTab,
                            searchQuery = searchQuery,
                            uiState = uiState,
                            palette = palette,
                            onManageRoute = onManageRoute,
                            onOpenDriver = onOpenDriver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutesHeader(
    palette: RegiBazar2026Palette,
    alerts: List<CamiAlertDto>,
    commonProducts: List<CommonProductDto>,
    isOffline: Boolean,
    isSearchVisible: Boolean,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInventory: () -> Unit,
    onLogout: () -> Unit
) {
    val themeController = LocalThemeController.current
    var notificationsExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(
                start = RegiBazar2026Tokens.HeaderHorizontalPadding,
                end = 12.dp,
                top = 6.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rutas",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )

        HeaderIconButton(
            icon = if (isSearchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = if (isSearchVisible) "Cerrar búsqueda" else "Buscar rutas",
            palette = palette,
            onClick = onSearchClick
        )
        Spacer(Modifier.width(4.dp))

        Box {
            HeaderIconButton(
                icon = Icons.Rounded.NotificationsNone,
                contentDescription = "Notificaciones",
                palette = palette,
                onClick = { notificationsExpanded = true }
            )
            if (alerts.isNotEmpty() || isOffline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-9).dp, y = 9.dp)
                        .size(9.dp)
                        .background(palette.error, CircleShape)
                )
            }
            NotificationsMenu(
                expanded = notificationsExpanded,
                alerts = alerts,
                commonProducts = commonProducts,
                isOffline = isOffline,
                palette = palette,
                onDismiss = { notificationsExpanded = false }
            )
        }
        Spacer(Modifier.width(4.dp))

        Box {
            HeaderIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones",
                palette = palette,
                onClick = { accountExpanded = true }
            )
            DropdownMenu(
                expanded = accountExpanded,
                onDismissRequest = { accountExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (themeController.isDark) "Usar tema claro" else "Usar tema oscuro")
                    },
                    leadingIcon = {
                        Icon(
                            if (themeController.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        accountExpanded = false
                        themeController.toggle()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Mi bodega") },
                    leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                    onClick = {
                        accountExpanded = false
                        onOpenInventory()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Actualizar rutas") },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                    onClick = {
                        accountExpanded = false
                        onRefresh()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", color = palette.error) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = palette.error)
                    },
                    onClick = {
                        accountExpanded = false
                        onLogout()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    palette: RegiBazar2026Palette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget),
        shape = RoundedCornerShape(13.dp),
        color = palette.surface,
        contentColor = palette.textPrimary,
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun NotificationsMenu(
    expanded: Boolean,
    alerts: List<CamiAlertDto>,
    commonProducts: List<CommonProductDto>,
    isOffline: Boolean,
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp)
    ) {
        Text(
            text = "Actividad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        if (isOffline) {
            NotificationRow(
                icon = Icons.Rounded.CloudOff,
                text = "Sin conexión. Se muestra la última información guardada.",
                color = palette.warning
            )
        }
        alerts.take(4).forEach { alert ->
            NotificationRow(Icons.Rounded.Bolt, alert.message, palette.accent)
        }
        if (!isOffline && alerts.isEmpty()) {
            NotificationRow(
                Icons.Rounded.CheckCircleOutline,
                "No hay alertas pendientes.",
                palette.success
            )
        }
        if (commonProducts.isNotEmpty()) {
            HorizontalDivider(color = palette.divider)
            Text(
                text = "Productos frecuentes",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
            Text(
                text = commonProducts.take(4).joinToString("  ·  ") { "${it.name} ×${it.count}" },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun NotificationRow(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.widthIn(max = 270.dp))
    }
}

@Composable
private fun RoutesSegmentedControl(
    selectedTab: RoutesListTab,
    palette: RegiBazar2026Palette,
    onTabSelected: (RoutesListTab) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RegiBazar2026Tokens.HeaderHorizontalPadding)
            .height(48.dp)
            .background(palette.surfaceMuted, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val indicatorWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedTab == RoutesListTab.PENDING) 0.dp else indicatorWidth,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "route-tab-indicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .background(palette.surface, RoundedCornerShape(11.dp))
        )
        Row(Modifier.fillMaxSize()) {
            RoutesTabButton(
                label = "Pendientes",
                selected = selectedTab == RoutesListTab.PENDING,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(RoutesListTab.PENDING) }
            )
            RoutesTabButton(
                label = "Completadas",
                selected = selectedTab == RoutesListTab.COMPLETED,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(RoutesListTab.COMPLETED) }
            )
        }
    }
}

@Composable
private fun RoutesTabButton(
    label: String,
    selected: Boolean,
    palette: RegiBazar2026Palette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) palette.textPrimary else palette.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoutesSearchField(
    query: String,
    palette: RegiBazar2026Palette,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textSecondary)
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Limpiar y cerrar búsqueda")
            }
        },
        placeholder = { Text("Ruta, clienta, teléfono o dirección") },
        shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.divider,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            focusedPlaceholderColor = palette.textSecondary,
            unfocusedPlaceholderColor = palette.textSecondary
        )
    )
}

@Composable
private fun RoutesList(
    routes: List<RouteListDto>,
    selectedTab: RoutesListTab,
    searchQuery: String,
    uiState: RoutesListUiState,
    palette: RegiBazar2026Palette,
    onManageRoute: (Int) -> Unit,
    onOpenDriver: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = RegiBazar2026Tokens.ScreenPadding,
            end = RegiBazar2026Tokens.ScreenPadding,
            top = 14.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isOffline || uiState.notice != null) {
            item(key = "offline-notice") {
                OfflineNotice(
                    message = uiState.notice ?: "Sin internet. Algunas acciones requieren conexión.",
                    palette = palette
                )
            }
        }
        item(key = "motivation-banner") {
            RoutesMotivationBanner(routes.size, selectedTab, palette)
        }
        if (routes.isEmpty()) {
            item(key = "filtered-empty") {
                FilteredEmptyState(searchQuery.isNotBlank(), selectedTab, palette)
            }
        } else {
            items(routes, key = { it.id }) { route ->
                RouteCard(
                    route = route,
                    palette = palette,
                    onClick = { onManageRoute(route.id) },
                    onOpenDriver = { onOpenDriver(route.driverToken) }
                )
            }
        }
    }
}

@Composable
private fun RoutesMotivationBanner(
    count: Int,
    selectedTab: RoutesListTab,
    palette: RegiBazar2026Palette
) {
    val pending = selectedTab == RoutesListTab.PENDING
    val colors = if (pending) {
        listOf(palette.accent, Color(0xFF3E7BFF))
    } else {
        listOf(palette.success, Color(0xFF3FBF6E))
    }
    val title = if (pending) {
        when (count) {
            0 -> "No tienes rutas pendientes"
            1 -> "Tienes 1 ruta por completar"
            else -> "Tienes $count rutas por completar"
        }
    } else {
        when (count) {
            0 -> "Aún no hay rutas completadas"
            1 -> "1 ruta completada"
            else -> "$count rutas completadas"
        }
    }
    val subtitle = if (pending) {
        if (count == 0) "Todo está al día" else "Organiza el reparto desde aquí"
    } else {
        if (count == 0) "Las rutas cerradas aparecerán aquí" else "Buen trabajo cerrando la jornada"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors), RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0x2EFCFDFF), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (pending) Icons.Rounded.Bolt else Icons.Rounded.CheckCircleOutline,
                contentDescription = null,
                tint = Color(0xFFF8FAFF),
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF8FAFF)
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xE6F8FAFF))
        }
    }
}

@Composable
private fun OfflineNotice(message: String, palette: RegiBazar2026Palette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.warningSoft, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = palette.warning,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            color = palette.textPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteCard(
    route: RouteListDto,
    palette: RegiBazar2026Palette,
    onClick: () -> Unit,
    onOpenDriver: () -> Unit
) {
    val isCompleted = route.status.equals("Completed", ignoreCase = true)
    val statusStyle = routeStatusStyle(route.status, palette)
    val tags = remember(route) { routeTagTypes(route) }
    val expectedCollection = remember(route) { expectedRouteCollection(route) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.76f else 1f)
            .animateContentSize(),
        shape = RoundedCornerShape(RegiBazar2026Tokens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ruta #${route.id}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    routeStatusLabel(route.status),
                    statusStyle.foreground,
                    statusStyle.background
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                routeSummary(route),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Cobro esperado",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
            Text(
                formatMoney(expectedCollection),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.money
            )
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    tags.forEach { RouteTag(it, palette) }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = palette.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Toca la tarjeta para administrar",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onOpenDriver,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
                ) {
                    Icon(
                        Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reparto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class RouteStatusStyle(val foreground: Color, val background: Color)

private fun routeStatusStyle(
    status: String,
    palette: RegiBazar2026Palette
): RouteStatusStyle = when {
    status.equals("Active", ignoreCase = true) ->
        RouteStatusStyle(palette.accent, palette.accentSoft)
    status.equals("Completed", ignoreCase = true) ->
        RouteStatusStyle(palette.success, palette.successSoft)
    status.equals("Canceled", ignoreCase = true) ->
        RouteStatusStyle(palette.error, palette.errorSoft)
    else -> RouteStatusStyle(palette.warning, palette.warningSoft)
}

@Composable
private fun StatusBadge(label: String, foreground: Color, background: Color) {
    Surface(shape = CircleShape, color = background, contentColor = foreground) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun RouteTag(type: RouteTagType, palette: RegiBazar2026Palette) {
    val (icon, label, foreground, background) = when (type) {
        RouteTagType.TRANSFER ->
            TagStyle(Icons.Rounded.SyncAlt, "Transferencias", palette.accent, palette.accentSoft)
        RouteTagType.TANDA ->
            TagStyle(Icons.Rounded.Groups, "Tandas", palette.money, palette.moneySoft)
        RouteTagType.MISSING_LOCATION ->
            TagStyle(Icons.Rounded.LocationOff, "Sin ubicación", palette.error, palette.errorSoft)
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(RegiBazar2026Tokens.TagRadius))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

private data class TagStyle(
    val icon: ImageVector,
    val label: String,
    val foreground: Color,
    val background: Color
)

@Composable
private fun FilteredEmptyState(
    hasSearch: Boolean,
    selectedTab: RoutesListTab,
    palette: RegiBazar2026Palette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Route,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (hasSearch) {
                "No encontramos rutas"
            } else if (selectedTab == RoutesListTab.COMPLETED) {
                "No hay rutas completadas"
            } else {
                "No hay rutas pendientes"
            },
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (hasSearch) {
                "Prueba con otro número, nombre o dirección."
            } else {
                "Cambia de pestaña para revisar el resto."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun RoutesLoadingState(palette: RegiBazar2026Palette) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LoadingBlock(Modifier.fillMaxWidth().height(72.dp), 18.dp, palette)
        }
        items(3) {
            LoadingBlock(
                Modifier.fillMaxWidth().height(190.dp),
                RegiBazar2026Tokens.CardRadius,
                palette
            )
        }
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier, radius: Dp, palette: RegiBazar2026Palette) {
    Box(modifier.background(palette.surfaceMuted, RoundedCornerShape(radius)))
}

@Composable
private fun RoutesErrorState(
    message: String,
    palette: RegiBazar2026Palette,
    onRetry: () -> Unit
) {
    StateColumn {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = palette.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No pudimos cargar las rutas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Text(message, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = Color(0xFFF8FAFF)
            )
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
private fun RoutesEmptyState(
    palette: RegiBazar2026Palette,
    onCreateRoute: () -> Unit
) {
    StateColumn {
        Icon(
            Icons.Rounded.Route,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Todavía no hay rutas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary
        )
        Text(
            "Crea la primera ruta para organizar las entregas del día.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCreateRoute,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = Color(0xFFF8FAFF)
            )
        ) {
            Icon(Icons.Rounded.AddRoad, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Crear ruta")
        }
    }
}

@Composable
private fun StateColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

private fun routeSummary(route: RouteListDto): AnnotatedString = buildAnnotatedString {
    append("${formatRouteDate(route.createdAt)}  ·  ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(route.totalCount.toString()) }
    append(" paradas  ·  ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(route.deliveredCount.toString()) }
    append(" entregadas  ·  ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(
            if (route.status.equals("Completed", ignoreCase = true)) {
                route.failedCount.toString()
            } else {
                route.pendingCount.toString()
            }
        )
    }
    append(
        if (route.status.equals("Completed", ignoreCase = true)) " fallidas" else " pendientes"
    )
}

private fun formatMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(value)

private fun formatRouteDate(value: String): String {
    val dateTime = parseRouteDate(value) ?: return value.take(16).replace("T", " ")
    val today = LocalDate.now()
    val dayLabel = when (dateTime.toLocalDate()) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> dateTime.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.forLanguageTag("es-MX"))
        )
    }
    return "$dayLabel · ${dateTime.format(DateTimeFormatter.ofPattern("H:mm"))}"
}

private fun parseRouteDate(value: String): LocalDateTime? {
    if (value.isBlank()) return null
    return try {
        OffsetDateTime.parse(value).toLocalDateTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value.take(19))
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
