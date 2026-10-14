package com.tuempresa.chapacollectionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuempresa.chapacollectionapp.data.CommunityChapa
import com.tuempresa.chapacollectionapp.data.CommunityProfile
import com.tuempresa.chapacollectionapp.ui.theme.*
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private val communityCountries = rememberCommunityCountries()

private fun rememberCommunityCountries(): List<String> =
    (Locale.getISOCountries().map { Locale("", it).getDisplayCountry(Locale("es")) } + "Turquía")
        .filter { it.isNotBlank() }.distinct().sorted()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    var section by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var countryFilter by remember { mutableStateOf("") }
    var myCountry by remember { mutableStateOf("") }
    var rankingCountry by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<CommunityProfile>>(emptyList()) }
    var ranking by remember { mutableStateOf<List<CommunityProfile>>(emptyList()) }
    var chapaMatches by remember { mutableStateOf<List<CommunityChapa>>(emptyList()) }
    var collection by remember { mutableStateOf<List<CommunityChapa>>(emptyList()) }
    var selectedProfile by remember { mutableStateOf<CommunityProfile?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        myCountry = runCatching { authViewModel.getCurrentPublicProfile()?.country.orEmpty() }.getOrDefault("")
        if (myCountry.isNotBlank()) rankingCountry = myCountry
    }
    LaunchedEffect(section, rankingCountry) {
        if (section == 1 && selectedProfile == null) {
            loading = true
            error = null
            try {
                ranking = authViewModel.getCommunityRanking(rankingCountry)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "No se pudo cargar el ranking."
            } finally {
                loading = false
            }
        }
    }

    fun launchRequest(request: suspend () -> Unit) {
        scope.launch {
            loading = true
            error = null
            try {
                request()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "No se pudo completar la búsqueda."
            } finally {
                loading = false
            }
        }
    }

    fun openProfile(profile: CommunityProfile) {
        selectedProfile = profile
        collection = emptyList()
        launchRequest { collection = authViewModel.getCommunityCollection(profile.id) }
    }

    Column(Modifier.fillMaxSize().background(ChapaBackground).padding(horizontal = 16.dp)) {
        if (selectedProfile != null) {
            val profile = selectedProfile!!
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedProfile = null; error = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = ChapaNavy)
                }
                Column(Modifier.weight(1f)) {
                    Text("@${profile.username}", color = ChapaNavy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${profile.country ?: "País no indicado"} · ${profile.chapaCount} chapas", color = ChapaSlate, fontSize = 14.sp)
                }
            }
            CommunityContentState(loading, error, collection.isEmpty(), "Esta colección todavía no tiene chapas.")
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(collection, key = { it.id }) { chapa -> CommunityChapaCard(chapa) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = ChapaOrangeLight, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = ChapaOrange, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Comunidad", color = ChapaNavy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Encuentra coleccionistas y chapas", color = ChapaSlate, fontSize = 13.sp)
                }
            }

            TabRow(selectedTabIndex = section, containerColor = ChapaBackground, contentColor = ChapaOrange) {
                listOf("Personas", "Ranking", "Chapas").forEachIndexed { index, title ->
                    Tab(selected = section == index, onClick = { section = index }, text = { Text(title) })
                }
            }

            when (section) {
                0 -> {
                    CommunitySearchField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = "Nombre de usuario",
                        onSearch = {
                            launchRequest { users = authViewModel.searchCommunityUsers(searchText, countryFilter.ifBlank { null }) }
                        }
                    )
                    CommunityCountryPicker(countryFilter, onCountrySelected = { countryFilter = it })
                    Button(
                        onClick = { launchRequest { users = authViewModel.searchCommunityUsers(searchText, countryFilter.ifBlank { null }) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChapaOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Buscar personas") }
                    Spacer(Modifier.height(8.dp))
                    CommunityContentState(loading, error, users.isEmpty(), "Busca por nombre o selecciona un país.")
                    LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.id }) { profile ->
                            CommunityProfileCard(profile, onClick = { openProfile(profile) })
                        }
                    }
                }
                1 -> {
                    Text("Colecciones con más chapas", modifier = Modifier.padding(top = 16.dp, bottom = 12.dp), color = ChapaSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { rankingCountry = myCountry.takeIf { it.isNotBlank() } },
                            enabled = myCountry.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChapaOrange)
                        ) { Text("Mi país") }
                        OutlinedButton(
                            onClick = { rankingCountry = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChapaOrange)
                        ) { Text("Mundo") }
                    }
                    Text(
                        if (rankingCountry.isNullOrBlank()) "Ranking mundial" else "Ranking de $rankingCountry",
                        modifier = Modifier.padding(vertical = 10.dp), color = ChapaNavy, fontWeight = FontWeight.SemiBold
                    )
                    CommunityContentState(loading, error, ranking.isEmpty(), "Aún no hay colecciones para mostrar.")
                    LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ranking, key = { it.id }) { profile ->
                            CommunityProfileCard(profile, onClick = { openProfile(profile) }, rank = ranking.indexOf(profile) + 1)
                        }
                    }
                }
                else -> {
                    CommunitySearchField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = "Nombre de chapa o marca",
                        onSearch = { launchRequest { chapaMatches = authViewModel.searchCommunityChapas(searchText) } }
                    )
                    Button(
                        onClick = { launchRequest { chapaMatches = authViewModel.searchCommunityChapas(searchText) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChapaOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Buscar chapas") }
                    Spacer(Modifier.height(8.dp))
                    val owners = chapaMatches.distinctBy { it.userId }
                    CommunityContentState(loading, error, owners.isEmpty(), "Busca un nombre para ver quién tiene esa chapa.")
                    LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(owners, key = { it.userId }) { match ->
                            CommunityChapaOwnerCard(match, onClick = {
                                openProfile(CommunityProfile(match.userId, match.username, match.country, match.chapaCount))
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySearchField(value: String, onValueChange: (String) -> Unit, label: String, onSearch: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChapaSlate) },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChapaOrange, focusedLabelColor = ChapaOrange)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityCountryPicker(value: String, onCountrySelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(value) { mutableStateOf(value) }
    val matches = remember(query) {
        if (query.isBlank()) communityCountries else communityCountries.filter { it.contains(query, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text("Filtrar por país (opcional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChapaOrange, focusedLabelColor = ChapaOrange)
        )
        ExposedDropdownMenu(
            expanded = expanded && (query.isBlank() || matches.isNotEmpty()),
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 260.dp)
        ) {
            DropdownMenuItem(text = { Text("Todos los países") }, onClick = {
                query = ""; onCountrySelected(""); expanded = false
            })
            matches.take(8).forEach { country ->
                DropdownMenuItem(text = { Text(country) }, onClick = {
                    query = country; onCountrySelected(country); expanded = false
                })
            }
        }
    }
}

@Composable
private fun CommunityContentState(loading: Boolean, error: String?, empty: Boolean, emptyMessage: String) {
    when {
        loading -> CircularProgressIndicator(modifier = Modifier.padding(14.dp), color = ChapaOrange)
        error != null -> Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
        empty -> Text(emptyMessage, modifier = Modifier.padding(12.dp), color = ChapaSlate, fontSize = 14.sp)
    }
}

@Composable
private fun CommunityProfileCard(profile: CommunityProfile, onClick: () -> Unit, rank: Int? = null) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChapaOutline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = ChapaOrangeLight, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(rank?.toString() ?: profile.username.take(1).uppercase(), color = ChapaOrange, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("@${profile.username}", color = ChapaNavy, fontWeight = FontWeight.Bold)
                Text(profile.country ?: "País no indicado", color = ChapaSlate, fontSize = 13.sp)
            }
            Text("${profile.chapaCount} chapas", color = ChapaOrange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CommunityChapaOwnerCard(chapa: CommunityChapa, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChapaOutline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CommunityThumbnail(chapa.imagePath, chapa.nombre)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("@${chapa.username}", color = ChapaNavy, fontWeight = FontWeight.Bold)
                Text("Tiene ${chapa.nombre}", color = ChapaSlate, fontSize = 13.sp, maxLines = 1)
                Text(chapa.country ?: "País no indicado", color = ChapaSlate, fontSize = 12.sp)
            }
            Text("Ver colección ›", color = ChapaOrange, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CommunityChapaCard(chapa: CommunityChapa) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChapaOutline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CommunityThumbnail(chapa.imagePath, chapa.nombre)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chapa.nombre, color = ChapaNavy, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(chapa.pais ?: "País de origen no indicado", color = ChapaSlate, fontSize = 13.sp)
                chapa.etiqueta?.takeIf { it != "unknown" }?.let { Text(it, color = ChapaOrange, fontSize = 12.sp) }
            }
            chapa.estadoPercent?.let { Text("$it%", color = ChapaOrange, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CommunityThumbnail(imagePath: String?, name: String) {
    AsyncImage(
        model = imagePath,
        contentDescription = "Imagen de $name",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(ChapaBackground)
    )
}
