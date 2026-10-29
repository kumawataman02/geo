# GeoTrack

Android app where you create a group with friends, set a geofence for that group, and everyone else in the group gets a notification when a member leaves the area.

Built with Kotlin, Jetpack Compose and Firebase.

## Features

- Google sign in (Credential Manager + Firebase Auth)
- Create a group and invite people by their email
- Accept or reject group invites
- Group owner picks the fence on a map (tap to set center, slider for radius)
- Members can see the group's fence
- Phone monitors the fence in the background and detects when you exit
- Other members get a push notification when someone exits

## How the exit alert works

1. `GeofenceManager` registers one geofence per group (request id = group id) with the Play Services Geofencing API.
2. When the user leaves, `GeofenceReceiver` gets the EXIT broadcast and writes a doc to `groups/{groupId}/events`.
3. The Cloud Function `notifyGroupOnExit` (in `functions/index.js`) triggers on that write, reads the other members' FCM tokens and sends them a data message.
4. `GeoMessagingService` receives it and shows the notification.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with Hilt for DI
- Coroutines and Flow
- Navigation Compose
- Firebase Auth, Firestore, Cloud Messaging, Cloud Functions
- Google Maps Compose, Play Services Location

## Project structure

```
app/src/main/java/com/geo/
  auth/         login screen, Google sign in, saving user + FCM token
  screen/       home (groups, invites), fence screen, repository, models
  geofence/     geofence registration, exit receiver, background location prompt
  messaging/    FCM service, notification permission
  navigation/   nav graph
  commontes/    shared UI bits (bottom sheet, FAB, UiState)
functions/      Cloud Function that sends the exit alert
firestore.rules Firestore security rules
```

## Firestore data

- `users/{uid}`: name, email, image (used for search by email)
- `groups/{groupId}`: groupName, ownerId, memberIds, fence { lat, lng, radiusM }
- `groups/{groupId}/members/{uid}`: member info and role (OWNER / MEMBER)
- `groups/{groupId}/events/{eventId}`: exit events
- `invites/{groupId_toUid}`: pending invite, deleted on accept or reject
- `fcmTokens/{uid}`: FCM token, kept separate so other users can't read it

The rules in `firestore.rules` make sure only members can read a group, only the owner can edit the fence, and an invited user can only add themselves to `memberIds`.

## Setup

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project, add an Android app with package `com.geo` and your debug SHA-1, then put your `google-services.json` in `app/`.
3. In Firebase enable Google sign in, Firestore and Cloud Messaging.
4. Add your Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
5. Deploy rules and the function (Cloud Functions needs the Blaze plan):
   ```
   firebase deploy --only firestore:rules,functions
   ```
6. Run the app on a device.

## Permissions

The app asks for fine location, background location ("Allow all the time") and notifications (Android 13+). Without background location the geofence won't fire when the app is closed.

## Testing the exit alert

Use two devices (or one device + emulator) with different Google accounts in the same group. Set a fence, then move the device out of it. On an emulator you can do this by changing the location from Extended Controls. Geofence events can take a minute or two to arrive.

## Not included

- Push notification for new invites (invites show up in the app instead)
- Live location of members on the map
