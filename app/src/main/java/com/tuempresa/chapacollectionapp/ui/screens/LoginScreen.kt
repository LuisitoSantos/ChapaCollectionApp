package com.tuempresa.chapacollectionapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.chapacollectionapp.R
import com.tuempresa.chapacollectionapp.ui.theme.*
import com.tuempresa.chapacollectionapp.viewmodel.AuthState
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.authState.collectAsState()
    var handledSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Success && !handledSuccess) {
            handledSuccess = true
            onLoginSuccess()
        } else if (state !is AuthState.Success) handledSuccess = false
    }

    Box(Modifier.fillMaxSize().background(ChapaBackground), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = ChapaOrangeLight, modifier = Modifier.size(76.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_chapa_outline), contentDescription = "ChapApp",
                    modifier = Modifier.padding(18.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(ChapaOrange)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("ChapApp", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ChapaNavy)
            Text("Tu colección, siempre contigo", fontSize = 15.sp, color = ChapaSlate)
            Spacer(Modifier.height(30.dp))

            if (state is AuthState.Loading) {
                CircularProgressIndicator(color = ChapaOrange)
            } else {
                OutlinedButton(
                    onClick = { viewModel.loginWithGoogle() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White)
                ) {
                    Image(painterResource(R.drawable.ic_gmail), contentDescription = "Gmail", modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Continuar con Google", color = ChapaNavy, fontWeight = FontWeight.SemiBold)
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Divider(Modifier.weight(1f), color = ChapaOutline)
                    Text("  o con tu correo  ", color = ChapaSlate, fontSize = 13.sp)
                    Divider(Modifier.weight(1f), color = ChapaOutline)
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ChapaOrange, cursorColor = ChapaOrange
                    )
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ChapaOrange, cursorColor = ChapaOrange
                    )
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.login(email.trim(), password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChapaOrange, contentColor = Color.White)
                ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { viewModel.signUp(email.trim(), password) }) {
                    Text("¿No tienes cuenta? Regístrate", color = ChapaOrange)
                }
            }

            if (state is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text((state as AuthState.Error).message, color = Color(0xFFB42318), fontSize = 14.sp)
            }
        }
    }
}
