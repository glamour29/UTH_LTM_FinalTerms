package com.example.client.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.view.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewContactScreen(
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+62") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Add New Contact",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Name input
            Text(
                "Name",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            // Phone number input
            Text(
                "Phone Number",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Phone Number", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE53935),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "ID",
                                fontSize = 16.sp
                            )
                        }
                    }
                    Text(
                        " $countryCode",
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(Modifier.height(32.dp))

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = { 
                    if (name.isNotBlank() || phoneNumber.isNotBlank()) {
                        onSave(name, "$countryCode$phoneNumber")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() || phoneNumber.isNotBlank()
            ) {
                Text(
                    "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
