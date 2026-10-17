package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.MainViewModel
import com.example.presentation.components.SimulatorDisclaimerBanner
import com.example.ui.theme.*

@Composable
fun SignInScreen(
  viewModel: MainViewModel,
  onNavigateToSignUp: () -> Unit,
  onNavigateToForgotPassword: () -> Unit,
  onSignInSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  var emailOrHandle by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var clientError by remember { mutableStateOf<String?>(null) }

  val isLoading by viewModel.isOperationLoading.collectAsState()

  Scaffold(
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))

        // Brand Icon & Header
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ForestGreenPrimary),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "B",
            fontWeight = FontWeight.Bold,
            color = MintLight,
            fontSize = 36.sp
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "BearWallet",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Text(
          text = "Social Banking & Economy Simulator",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))
        SimulatorDisclaimerBanner()

        Spacer(modifier = Modifier.height(24.dp))

        // Input Fields
        OutlinedTextField(
          value = emailOrHandle,
          onValueChange = {
            emailOrHandle = it
            clientError = null
          },
          label = { Text("Email atau @Username") },
          placeholder = { Text("user@email.com atau @handle") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signin_email_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            clientError = null
          },
          label = { Text("Password") },
          placeholder = { Text("Minimal 6 karakter") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary) },
          trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
              Icon(
                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null
              )
            }
          },
          visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signin_password_input"),
          shape = RoundedCornerShape(14.dp)
        )

        if (clientError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = NegativeRedBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = clientError ?: "",
              color = NegativeRed,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onNavigateToForgotPassword) {
            Text("Lupa Password?", color = MintSecondary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (emailOrHandle.isBlank()) {
              clientError = "Silakan masukkan email atau username Anda"
              return@Button
            }
            if (password.isBlank()) {
              clientError = "Silakan masukkan password Anda"
              return@Button
            }
            clientError = null
            viewModel.signIn(emailOrHandle, password, onSignInSuccess)
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("signin_submit_button"),
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
          shape = RoundedCornerShape(16.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
          } else {
            Text("Masuk ke Akun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          color = SageSurface,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Kredensial Akun Komunitas:\n• @maya (Pass: bearpassword123)\n• @alex (Pass: bearpassword123)\n• @centralbank (Pass: bearpassword123)\nAtau daftarkan akun baru Anda sendiri di bawah.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondary,
            modifier = Modifier.padding(10.dp),
            lineHeight = 16.sp
          )
        }
      }

      Row(
        modifier = Modifier.padding(bottom = 16.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Belum punya akun?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToSignUp) {
          Text("Daftar Sekarang", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun SignUpScreen(
  viewModel: MainViewModel,
  onNavigateToSignIn: () -> Unit,
  onSignUpSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  var username by remember { mutableStateOf("") }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  var usernameError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var nameError by remember { mutableStateOf<String?>(null) }

  val isLoading by viewModel.isOperationLoading.collectAsState()

  val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
  val usernameRegex = Regex("^[a-zA-Z0-9_]+$")

  Scaffold(
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Daftar Akun Baru",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Text(
          text = "Saldo Awal 0 B • Terhubung Langsung ke Supabase Auth",
          style = MaterialTheme.typography.bodyMedium,
          color = MintSecondary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Username Input
        OutlinedTextField(
          value = username,
          onValueChange = {
            username = it
            usernameError = when {
              it.isBlank() -> "Username tidak boleh kosong"
              it.trim().trimStart('@').length < 3 -> "Username minimal 3 karakter"
              !it.trim().trimStart('@').matches(usernameRegex) -> "Hanya boleh huruf, angka, dan underscore"
              else -> null
            }
          },
          label = { Text("Transfer Handle (@username)") },
          placeholder = { Text("contoh: windu, maya, alex") },
          leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = ForestGreenPrimary) },
          isError = usernameError != null,
          supportingText = { usernameError?.let { Text(it, color = NegativeRed) } },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signup_username_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Full Name Input
        OutlinedTextField(
          value = fullName,
          onValueChange = {
            fullName = it
            nameError = if (it.isBlank()) "Nama lengkap tidak boleh kosong" else null
          },
          label = { Text("Nama Lengkap") },
          placeholder = { Text("contoh: Windu Mariposa") },
          leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ForestGreenPrimary) },
          isError = nameError != null,
          supportingText = { nameError?.let { Text(it, color = NegativeRed) } },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email Input
        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            emailError = when {
              it.isBlank() -> "Email tidak boleh kosong"
              !it.trim().matches(emailRegex) -> "Format email tidak valid (harus mengandung @ dan domain)"
              else -> null
            }
          },
          label = { Text("Alamat Email") },
          placeholder = { Text("user@email.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary) },
          isError = emailError != null,
          supportingText = { emailError?.let { Text(it, color = NegativeRed) } },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            passwordError = when {
              it.isBlank() -> "Password tidak boleh kosong"
              it.length < 6 -> "Password minimal 6 karakter"
              else -> null
            }
          },
          label = { Text("Password (Minimal 6 Karakter)") },
          placeholder = { Text("Minimal 6 karakter") },
          visualTransformation = PasswordVisualTransformation(),
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary) },
          isError = passwordError != null,
          supportingText = { passwordError?.let { Text(it, color = NegativeRed) } },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            val cleanEmail = email.trim()
            val cleanHandle = username.trim().trimStart('@')
            val cleanName = fullName.trim()

            var hasError = false
            if (cleanHandle.length < 3 || !cleanHandle.matches(usernameRegex)) {
              usernameError = "Username minimal 3 karakter, alfanumerik"
              hasError = true
            }
            if (cleanName.isBlank()) {
              nameError = "Nama lengkap wajib diisi"
              hasError = true
            }
            if (!cleanEmail.matches(emailRegex)) {
              emailError = "Format email tidak valid"
              hasError = true
            }
            if (password.length < 6) {
              passwordError = "Password minimal 6 karakter"
              hasError = true
            }

            if (hasError) return@Button

            viewModel.signUp(cleanEmail, password, cleanHandle, cleanName, onSignUpSuccess)
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("signup_submit_button"),
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
          shape = RoundedCornerShape(16.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
          } else {
            Text("Daftar Sekarang (Saldo Awal 0 B)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      Row(
        modifier = Modifier.padding(bottom = 16.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Sudah punya akun?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToSignIn) {
          Text("Masuk", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var email by remember { mutableStateOf("") }
  val isLoading by viewModel.isOperationLoading.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ForestGreenPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhite)
      )
    },
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "Masukkan alamat email terdaftar Anda untuk menerima tautan pemulihan kata sandi via Supabase Auth.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Alamat Email") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )
      }

      Button(
        onClick = {
          viewModel.recoverPassword(email)
          onNavigateBack()
        },
        enabled = !isLoading && email.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text("Kirim Tautan Pemulihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}
