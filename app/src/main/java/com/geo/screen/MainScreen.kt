
package com.geo.screen

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.geo.commontes.BottomSheet
import com.geo.commontes.CustomFloatingActionButton
import com.geo.commontes.UiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchState by viewModel.searchState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showCreateForm by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    val invitedEmails = remember { mutableStateListOf<String>() }

    fun closeSheet() {
        showAddOptions = false
        showCreateForm = false
        groupName = ""
        emailInput = ""
        invitedEmails.clear()
        viewModel.resetSearchState()
    }

    val trimmedEmail = emailInput.trim()
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()


    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(Icons.Default.Group, contentDescription = "Groups")
                    },
                    label = { Text("Groups") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(Icons.Default.Map, contentDescription = "Map")
                    },
                    label = { Text("Map") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    },
                    label = { Text("Profile") }
                )
            }
        },
        floatingActionButton = {
            CustomFloatingActionButton {
                showAddOptions = true
            }
        }
    ) { paddingValues ->

        MainItem(
            paddingValues = paddingValues,
            selectedTab = selectedTab
        )
    }

    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            )
        ) {
            BottomSheet(
                onSearchFriend = { email ->
                    viewModel.searchFriend(email)
                },
                onCreateGroup = { groupName ->
                    viewModel.groupMake(Group(groupName = groupName))
                }
            )

            val modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            when (val state = searchState) {
                UiState.Idle -> Unit
                UiState.Loading -> CircularProgressIndicator(modifier)
                is UiState.Success -> Text("${state.data.userName} (${state.data.userEmail})", modifier)
                is UiState.Error -> Text(state.message, modifier, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}




@Composable
fun MainItem(
    paddingValues: PaddingValues,
    selectedTab: Int
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxWidth()
    ) {
        when (selectedTab) {
            0 -> Text(
                "Home",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            1 -> Text(
                "My Groups",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            2 -> Text(
                "Map",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            3 -> Text(
                "Profile",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
