package com.tuempresa.chapacollectionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.chapacollectionapp.ui.theme.*
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel
import com.tuempresa.chapacollectionapp.viewmodel.ProfileState
import java.util.Locale

@Composable
fun ProfileSetupScreen(viewModel: AuthViewModel, onProfileReady: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    val countries = remember {
        (Locale.getISOCountries().map { Locale("", it).getDisplayCountry(Locale("es")) } + "Turquía")
            .filter { it.isNotBlank() }.distinct().sorted()
    }
    val matchingCountries = remember(country, countries) {
        if (country.isBlank()) countries else countries.filter { it.contains(country, ignoreCase = true) }
    }
    val state by viewModel.profileState.collectAsState()
    var submitted by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { viewModel.getCurrentPublicProfile() }.getOrNull()?.let { profile ->
            username = profile.username
            country = profile.country.orEmpty()
        }
    }
    LaunchedEffect(state) {
        if (state is ProfileState.Success && submitted && !completed) {
            completed = true
            onProfileReady()
        } else if (state is ProfileState.Error) {
            submitted = false
        }
    }

    Box(Modifier.fillMaxSize().background(ChapaBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Completa tu perfil", color = ChapaNavy, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Elige un nombre único y tu país para identificar tu colección en la comunidad.", color = ChapaSlate, fontSize = 15.sp)
            Spacer(Modifier.height(26.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                label = { Text("Nombre público") },
                leadingIcon = { Text("@", color = ChapaOrange, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Ascii),
                shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = ChapaOrange, cursorColor = ChapaOrange
                )
            )
            Text("3–20 caracteres: letras, números y guion bajo", modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 5.dp), color = ChapaSlate, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Box {
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it; countryMenuExpanded = it.isNotBlank() },
                    label = { Text("País") },
                    placeholder = { Text("Busca y selecciona tu país") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ChapaOrange, cursorColor = ChapaOrange
                    )
                )
                DropdownMenu(
                    expanded = countryMenuExpanded && matchingCountries.isNotEmpty(),
                    onDismissRequest = { countryMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    matchingCountries.take(8).forEach { option ->
                        DropdownMenuItem(onClick = {
                            country = option
                            countryMenuExpanded = false
                        }) { Text(option) }
                    }
                }
            }
            Text("Selecciona el país donde resides.", modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 5.dp), color = ChapaSlate, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    submitted = true
                    viewModel.createPublicProfile(username, country)
                },
                enabled = username.length >= 3 && country in countries && state !is ProfileState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = ChapaOrange, contentColor = Color.White)
            ) {
                if (state is ProfileState.Loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Continuar", fontWeight = FontWeight.Bold)
            }
            if (state is ProfileState.Error) {
                Spacer(Modifier.height(12.dp))
                Text((state as ProfileState.Error).message, color = Color(0xFFB42318))
            }
        }
    }
}
