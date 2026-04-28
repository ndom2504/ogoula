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
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XBorderGray
import com.example.ogoula.ui.theme.XDarkGray
import com.example.ogoula.ui.theme.XTextGray
import com.example.ogoula.ui.theme.XWhite

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
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = XBlue,
        focusedBorderColor = XBlue,
        unfocusedBorderColor = XBorderGray.copy(alpha = 0.85f),
        focusedLabelColor = XBlue,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = XBlue,
        unfocusedLeadingIconColor = XTextGray,
        focusedTrailingIconColor = XBlue,
        unfocusedTrailingIconColor = XTextGray,
        focusedContainerColor = XDarkGray,
        unfocusedContainerColor = XDarkGray,
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
                        colors = listOf(XBlack, XBlue.copy(alpha = 0.78f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ogoula",
                    style = MaterialTheme.typography.displayMedium,
                    color = XWhite,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Plateforme de valorisation et d'influence",
                    style = MaterialTheme.typography.bodyLarge,
                    color = XWhite.copy(alpha = 0.9f),
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

                if (isRegister) {
                    Text(
                        text = "Ogoula est une plateforme de valorisation et d'influence où les marques, les produits et les personnalités gagnent en visibilité grâce aux interactions, aux votes et aux retours de la communauté.\n\nEn rejoignant Ogoula, tu acceptes de contribuer positivement à cette valorisation collective en respectant la charte : pertinence des apports, respect d'autrui (aucune injure, violence ou irrespect), interactions constructives et fun — conditions essentielles pour le succès de notre communauté.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

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
                        CircularProgressIndicator(color = XWhite, modifier = Modifier.size(24.dp))
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
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bandeau discret style X
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(XBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(XBorderGray))
        }
    }
}
