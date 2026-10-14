package com.tuempresa.chapacollectionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

@Composable
fun ProfileSetupScreen(viewModel: AuthViewModel, onProfileReady: () -> Unit) {
    var username by remember { mutableStateOf("") }
    val state by viewModel.profileState.collectAsState()
    var submitted by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
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
            Modifier.fillMaxWidth().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Elige tu nombre público", color = ChapaNavy, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Será único y permitirá encontrar tu colección más adelante.", color = ChapaSlate, fontSize = 15.sp)
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
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    submitted = true
                    viewModel.createPublicProfile(username)
                },
                enabled = username.length >= 3 && state !is ProfileState.Loading,
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
