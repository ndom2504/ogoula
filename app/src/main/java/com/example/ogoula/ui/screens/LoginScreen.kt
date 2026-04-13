package com.example.ogoula.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ogoula.data.AuthRepository
import com.example.ogoula.ui.AuthViewModel
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint
import com.example.ogoula.ui.theme.OgoulaWhite

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    userViewModel: UserViewModel,
    onLoginSuccess: () -> Unit,
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthRepository.AuthState.Authenticated) onLoginSuccess()
    }

    val loginFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = GreenGabo,
        unfocusedTextColor = GreenGabo,
        cursorColor = GreenGabo,
        focusedBorderColor = OgoulaWhite,
        unfocusedBorderColor = OgoulaWhite.copy(alpha = 0.55f),
        focusedLabelColor = OgoulaWhite.copy(alpha = 0.92f),
        unfocusedLabelColor = OgoulaWhite.copy(alpha = 0.72f),
        focusedLeadingIconColor = GreenGabo,
        unfocusedLeadingIconColor = GreenGabo,
        focusedTrailingIconColor = GreenGabo,
        unfocusedTrailingIconColor = GreenGabo,
        focusedContainerColor = OgoulaWhite,
        unfocusedContainerColor = OgoulaSurfaceTint,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GreenGabo, GreenGabo.copy(alpha = 0.7f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ogoula",
                    style = MaterialTheme.typography.displayMedium,
                    color = OgoulaWhite,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Parlons des choses de notre bled",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OgoulaWhite.copy(alpha = 0.9f),
                )
            }
        }

        AnimatedContent(targetState = isRegisterMode, label = "LoginMode") { isRegister ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (isRegister) "Créer un compte" else "Connexion",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                val denial = userViewModel.accountDenialMessage
                if (denial != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            denial,
                            color = Color(0xFFB45309),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                if (authState is AuthRepository.AuthState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            (authState as AuthRepository.AuthState.Error).message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adresse email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = loginFieldColors,
                )

                // Mot de passe
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mot de passe") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = loginFieldColors,
                )

                // Confirmation mot de passe (inscription seulement)
                if (isRegister) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirmer le mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                        colors = loginFieldColors,
                    )
                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                        Text(
                            "Les mots de passe ne correspondent pas",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                val isLoading = authState is AuthRepository.AuthState.Loading
                val isFormValid = email.contains("@") && password.length >= 6 &&
                        (!isRegister || confirmPassword == password)

                Button(
                    onClick = {
                        userViewModel.clearAccountDenialMessage()
                        if (isRegister) viewModel.signUp(email, password)
                        else viewModel.signIn(email, password)
                    },
                    enabled = !isLoading && isFormValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = GreenGabo, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (isRegister) "Créer mon compte" else "Se connecter")
                    }
                }

                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    confirmPassword = ""
                }) {
                    Text(
                        if (isRegister) "Déjà un compte ? Se connecter"
                        else "Pas encore de compte ? S'inscrire",
                        color = OgoulaWhite.copy(alpha = 0.92f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bandeau discret (vert + blanc, sans tricolore)
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GreenGabo))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
        }
    }
}
