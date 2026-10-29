
package com.geo.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geo.commontes.BottomSheet
import com.geo.commontes.CustomFloatingActionButton
import com.geo.commontes.UiState
import com.geo.geofence.BackgroundLocationCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenFence: (String) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val groupState by viewModel.makeGroup.collectAsStateWithLifecycle()
    val selectedFriends by viewModel.selectedFriends.collectAsStateWithLifecycle()
    val invites by viewModel.invites.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val inviteAction by viewModel.inviteAction.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddOptions by remember { mutableStateOf(false) }

    fun closeSheet() {
        showAddOptions = false
        viewModel.resetSearchState()
        viewModel.resetGroupState()
    }

    LaunchedEffect(groupState) {
        if (groupState is UiState.Success) closeSheet()
    }

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
            selectedTab = selectedTab,
            invites = invites,
            groups = groups,
            myUid = viewModel.myUid,
            onGroupClick = onOpenFence,
            onLocationGranted = viewModel::resyncGeofences,
            inviteAction = inviteAction,
            onAccept = viewModel::acceptInvite,
            onReject = viewModel::rejectInvite
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
                selectedFriends = selectedFriends,
                onSearchFriend = { email ->
                    viewModel.searchFriend(email)
                },
                onRemoveFriend = viewModel::removeFriend,
                onCreateGroup = { groupName ->
                    viewModel.groupMake(groupName)
                }
            )

            val modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            when (val state = searchState) {
                UiState.Idle -> Unit
                UiState.Loading -> CircularProgressIndicator(modifier)
                is UiState.Success -> Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.data.userName} (${state.data.userEmail})", Modifier.weight(1f))
                    TextButton(onClick = { viewModel.addFriend(state.data) }) { Text("Add") }
                }
                is UiState.Error -> Text(state.message, modifier, color = MaterialTheme.colorScheme.error)
            }

            when (val state = groupState) {
                UiState.Loading -> CircularProgressIndicator(modifier)
                is UiState.Error -> Text(state.message, modifier, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}




@Composable
fun MainItem(
    paddingValues: PaddingValues,
    selectedTab: Int,
    invites: List<Invite>,
    groups: List<Group>,
    myUid: String?,
    onGroupClick: (String) -> Unit,
    onLocationGranted: () -> Unit,
    inviteAction: UiState<Unit>,
    onAccept: (Invite) -> Unit,
    onReject: (Invite) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxWidth()
    ) {
        when (selectedTab) {
            0 -> {
                Title("Invites")
                if (inviteAction is UiState.Error) {
                    Text(
                        inviteAction.message,
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (invites.isEmpty()) EmptyText("No pending invites")
                LazyColumn {
                    items(invites, key = { it.inviteId }) { invite ->
                        InviteCard(
                            invite = invite,
                            enabled = inviteAction !is UiState.Loading,
                            onAccept = { onAccept(invite) },
                            onReject = { onReject(invite) }
                        )
                    }
                }
            }

            1 -> {
                Title("My Groups")
                // Only ask when there is actually a fence to monitor
                if (groups.any { it.fence != null }) BackgroundLocationCard(onGranted = onLocationGranted)
                if (groups.isEmpty()) EmptyText("No groups yet")
                LazyColumn {
                    items(groups.sortedByDescending { it.createdAt }, key = { it.groupId }) { group ->
                        val isOwner = group.ownerId == myUid
                        val fenceText = group.fence?.let { "Fence: ${it.radiusM.roundToInt()} m" } ?: "No fence set"
                        ListItem(
                            headlineContent = { Text(group.groupName) },
                            supportingContent = { Text("${group.memberIds.size} members · $fenceText") },
                            leadingContent = { Icon(Icons.Default.Group, contentDescription = null) },
                            // Owner edits the fence, members can view it
                            trailingContent = {
                                if (isOwner) Icon(Icons.Default.EditLocation, contentDescription = "Edit geofence")
                                else Icon(Icons.Default.Visibility, contentDescription = "View geofence")
                            },
                            modifier = Modifier.clickable { onGroupClick(group.groupId) }
                        )
                    }
                }
            }

            2 -> Title("Map")

            3 -> Title("Profile")
        }
    }
}

@Composable
private fun InviteCard(
    invite: Invite,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${invite.fromName} invited you to")
            Text(invite.groupName, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReject, enabled = enabled) { Text("Reject") }
                Button(onClick = onAccept, enabled = enabled) { Text("Accept") }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text,
        modifier = Modifier.padding(20.dp),
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
