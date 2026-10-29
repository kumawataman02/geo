package com.geo.commontes

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    modifier: Modifier = Modifier,
    onSearchFriend: (String) -> Unit,
    onCreateGroup: (String) -> Unit
) {
    var groupName by rememberSaveable {
        mutableStateOf("")
    }

    var friendEmail by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Group",
            style = MaterialTheme.typography.titleLarge
        )

        // Group Name
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Group Name") },
            placeholder = { Text("Enter group name") },
            singleLine = true
        )

        HorizontalDivider()

        // Search Friends
        Text(
            text = "Add Friends",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = friendEmail,
            onValueChange = { friendEmail = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Friend's Email") },
            placeholder = { Text("Enter email address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        val email = friendEmail.trim()
                        if (email.isNotBlank() &&
                            android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email).matches()
                        ) {
                            onSearchFriend(email)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search friend"
                    )
                }
            }
        )

        // Create Group
        Button(
            onClick = {
                onCreateGroup(groupName.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupName.isNotBlank()
        ) {
            Text("Create Group")
        }
    }
}
