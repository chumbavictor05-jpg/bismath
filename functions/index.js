/**
 * Firebase Cloud Functions v2 (Node.js) push dispatcher for Scholarly.
 * Deploy in Firebase Console / CLI (`firebase deploy --only functions`) on Blaze plan
 * to fan out queued events in `push_outbox` to student and tutor FCM tokens.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-scholarl-8484a989-0192-4278-aab0-44822cc44f53";

exports.dispatchScholarlyPushNotification = onDocumentCreated(
  {
    document: "users/{userId}/push_outbox/{dispatchId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const payload = snap.data();
    const db = getFirestore(DATABASE_ID);

    const tokensSnap = await db
      .collection("users")
      .doc(event.params.userId)
      .collection("fcm_tokens")
      .get();

    const tokens = [];
    tokensSnap.forEach((doc) => {
      const data = doc.data();
      if (!data.token) return;
      if (payload.eventType === "booking_confirmation" && data.bookingConfirmationsEnabled === false) return;
      if (payload.eventType === "session_reminder" && data.sessionRemindersEnabled === false) return;
      if (payload.eventType === "new_message" && data.newMessagesEnabled === false) return;
      tokens.push(data.token);
    });

    if (tokens.length === 0) return;

    const channelMap = {
      booking_confirmation: "scholarly_bookings_channel",
      session_reminder: "scholarly_reminders_channel",
      new_message: "scholarly_messages_channel",
    };

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: {
        type: payload.eventType,
        bookingId: payload.bookingId || "",
        tutorId: payload.tutorId || "",
        recipientRole: payload.recipientRole || "both",
      },
      android: {
        priority: "high",
        notification: {
          channelId: channelMap[payload.eventType] || "scholarly_bookings_channel",
        },
      },
    });

    if (response.successCount > 0) {
      await snap.ref.update({ deliveryStatus: "sent" });
    }
  }
);
