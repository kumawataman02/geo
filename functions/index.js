const {setGlobalOptions} = require("firebase-functions");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// Cost control: at most 10 instances of a function at once
setGlobalOptions({maxInstances: 10});

// Phase 3 writes groups/{groupId}/events/{eventId} when a member leaves the
// fence. This reacts to that write and pushes an alert to everyone else.
exports.notifyGroupOnExit = onDocumentCreated(
    "groups/{groupId}/events/{eventId}",
    async (event) => {
      const exit = event.data && event.data.data();
      if (!exit || exit.type !== "EXIT") return;

      const db = getFirestore();
      const {groupId} = event.params;

      const groupSnap = await db.doc(`groups/${groupId}`).get();
      if (!groupSnap.exists) return;
      const group = groupSnap.data();

      // Everyone except the member who left
      const others = (group.memberIds || []).filter((id) => id !== exit.uid);
      if (others.length === 0) return;

      // Admin SDK ignores security rules, so it can read fcmTokens
      const tokenDocs = await db.getAll(
          ...others.map((id) => db.doc(`fcmTokens/${id}`)),
      );
      const tokens = tokenDocs.map((doc) => doc.get("token")).filter(Boolean);
      if (tokens.length === 0) {
        logger.info("No tokens to notify", {groupId});
        return;
      }

      // Data-only message: the app always gets onMessageReceived and builds
      // the notification itself. High priority wakes a closed app.
      const response = await getMessaging().sendEachForMulticast({
        tokens,
        data: {
          type: "EXIT",
          groupId,
          groupName: group.groupName || "",
          uid: exit.uid,
          name: exit.name || "A member",
        },
        android: {priority: "high"},
      });

      logger.info("Exit alert sent", {
        groupId,
        sent: response.successCount,
        failed: response.failureCount,
      });
    },
);
