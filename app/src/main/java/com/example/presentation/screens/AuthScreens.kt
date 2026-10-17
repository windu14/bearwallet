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
  var emailOrHandle by remember { mutableStateOf("bearboss") }
  var password by remember { mutableStateOf("bearpassword123") }
  var isPasswordVisible by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(20.dp))
        SimulatorDisclaimerBanner()

        Spacer(modifier = Modifier.height(28.dp))

        // Input Fields
        OutlinedTextField(
          value = emailOrHandle,
          onValueChange = { emailOrHandle = it },
          label = { Text("Email or @Username") },
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
          onValueChange = { password = it },
          label = { Text("Password") },
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

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onNavigateToForgotPassword) {
            Text("Forgot Password?", color = MintSecondary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            viewModel.signIn(emailOrHandle, password, onSignInSuccess)
          },
          enabled = !isLoading && emailOrHandle.isNotBlank(),
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
            Text("Sign In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Don't have an account?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToSignUp) {
          Text("Sign Up", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
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
        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Create Bear Account",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Text(
          text = "Includes 500 B welcome grant from distribution pool",
          style = MaterialTheme.typography.bodyMedium,
          color = MintSecondary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text("Transfer Handle (@username)") },
          placeholder = { Text("e.g. maya, alex, bearuser") },
          leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signup_username_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name") },
          placeholder = { Text("e.g. Maya Hartono") },
          leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          visualTransformation = PasswordVisualTransformation(),
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            viewModel.signUp(email, password, username, fullName, onSignUpSuccess)
          },
          enabled = !isLoading && username.isNotBlank() && email.isNotBlank(),
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
            Text("Create Account & Claim 500 B", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Already have an account?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onNavigateToSignIn) {
          Text("Sign In", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
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
          text = "Enter your registered email address to receive password recovery instructions via Supabase Auth.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
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
        Text("Send Recovery Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}
