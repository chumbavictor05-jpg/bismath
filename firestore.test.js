const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validBooking(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    tutorId: "tutor_elena_vance",
    tutorName: "Dr. Elena Vance",
    tutorTitle: "PhD Applied Mathematics, MIT",
    subject: "Calculus & Linear Algebra",
    topic: "Eigenvalues and Diagonalization",
    sessionFormat: "Video + Whiteboard",
    scheduledDateLabel: "Tue, Nov 18",
    scheduledTimeLabel: "4:00 PM",
    durationMinutes: 60,
    hourlyRateUsd: 75,
    totalPriceUsd: 75,
    status: "scheduled",
    notes: "Reviewing problem set 4.",
    meetingCode: "SCH-4821",
    paymentMethodLabel: "Google Pay",
    paymentStatus: "authorized",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: cannot read or write protected collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("reviews").where("isPublished", "==", true).get());
});

test("Availability, notifications, payment methods, transactions, and payouts: owner-only and validated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  // Availability
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("availability").doc("mon").set({
      userId: ALICE_UID,
      dayLabel: "Mon, Nov 17",
      timeSlots: ["9:00 AM", "2:00 PM"],
      isEnabled: true,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("availability").get());

  // Notification
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "New Session Confirmed",
      body: "Booked with Dr. Elena Vance",
      type: "new_booking",
      bookingId: "booking_101",
      isRead: false,
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      isRead: true,
    })
  );

  // Payment method
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payment_methods").doc("pm_1").set({
      userId: ALICE_UID,
      methodType: "card",
      label: "Visa •••• 4242",
      last4: "4242",
      expiryLabel: "08/29",
      isDefault: true,
      createdAt: ts,
    })
  );

  // Transaction requires parent booking
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_orphan").set({
      userId: ALICE_UID,
      bookingId: "missing_booking",
      tutorId: "tutor_elena_vance",
      tutorName: "Dr. Elena Vance",
      amountUsd: 75,
      platformFeeUsd: 8,
      tutorNetUsd: 67,
      paymentMethodLabel: "Visa •••• 4242",
      status: "authorized",
      receiptCode: "RCP-1001",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_101").set(validBooking(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      bookingId: "booking_101",
      tutorId: "tutor_elena_vance",
      tutorName: "Dr. Elena Vance",
      amountUsd: 75,
      platformFeeUsd: 8,
      tutorNetUsd: 67,
      paymentMethodLabel: "Visa •••• 4242",
      status: "authorized",
      receiptCode: "RCP-1001",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      status: "settled",
      updatedAt: pastTimestamp(),
    })
  );

  // Payout (tutor_earnings and platform_commission to M-Pesa)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payouts").doc("pay_1").set({
      userId: ALICE_UID,
      amountUsd: 67,
      destinationLabel: "Chase Checking •••• 8810",
      payoutChannel: "Bank ACH",
      payoutType: "tutor_earnings",
      status: "requested",
      referenceCode: "PAY-4490",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payouts").doc("pay_comm_mpesa").set({
      userId: ALICE_UID,
      amountUsd: 8,
      destinationLabel: "+254712345678",
      payoutChannel: "M-Pesa",
      payoutType: "platform_commission",
      status: "requested",
      referenceCode: "COM-9910",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("payouts").doc("pay_bad_type").set({
      userId: ALICE_UID,
      amountUsd: 8,
      destinationLabel: "+254712345678",
      payoutChannel: "M-Pesa",
      payoutType: "admin_override",
      status: "requested",
      referenceCode: "COM-9911",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("payouts").doc("pay_1").update({
      status: "paid",
    })
  );
});

test("Bookings & Reviews: terminal state lock and parent gate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();
  const bookingRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_303");
  await assertSucceeds(bookingRef.set(validBooking(ALICE_UID)));
  await assertSucceeds(bookingRef.update({ status: "completed", paymentStatus: "settled", updatedAt: pastTimestamp() }));
  await assertFails(bookingRef.update({ status: "cancelled", updatedAt: pastTimestamp() }));

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      userId: ALICE_UID,
      authorName: "Alice Chen",
      tutorId: "tutor_elena_vance",
      sessionId: "booking_303",
      rating: 5,
      comment: "Great explanations!",
      subject: "Calculus",
      isPublished: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Payment-optional waived booking can be created, completed, and reviewed with $0
  const waivedRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_waived_1");
  await assertSucceeds(
    waivedRef.set({
      ...validBooking(ALICE_UID),
      totalPriceUsd: 0,
      paymentMethodLabel: "Skipped (Payment Optional)",
      paymentStatus: "waived",
    })
  );
  await assertSucceeds(
    waivedRef.update({ status: "completed", paymentStatus: "waived", updatedAt: pastTimestamp() })
  );
});

test("FCM tokens & push_outbox: owner-only, validated, and parent-gated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  // Register FCM token
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("android_primary").set({
      userId: ALICE_UID,
      token: "fcm_test_token_1234567890_abcdef",
      deviceLabel: "Pixel 9 (API 36)",
      role: "student",
      bookingConfirmationsEnabled: true,
      sessionRemindersEnabled: true,
      newMessagesEnabled: true,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("android_primary").update({
      sessionRemindersEnabled: false,
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").get()
  );

  // Parent-gated push_outbox (requires existing bookingId)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("push_outbox").doc("push_orphan").set({
      userId: ALICE_UID,
      eventType: "session_reminder",
      recipientRole: "both",
      bookingId: "missing_booking_id",
      tutorId: "tutor_elena_vance",
      title: "Upcoming Session",
      body: "Starts in 15m",
      deliveryStatus: "queued",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_fcm_1").set(validBooking(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("push_outbox").doc("push_1").set({
      userId: ALICE_UID,
      eventType: "booking_confirmation",
      recipientRole: "both",
      bookingId: "booking_fcm_1",
      tutorId: "tutor_elena_vance",
      title: "Booking Confirmed",
      body: "Calculus scheduled for Tue 4:00 PM",
      deliveryStatus: "queued",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("push_outbox").doc("push_bad_event").set({
      userId: ALICE_UID,
      eventType: "promo_spam",
      recipientRole: "both",
      bookingId: "booking_fcm_1",
      tutorId: "tutor_elena_vance",
      title: "Promo",
      body: "Promo body",
      deliveryStatus: "queued",
      createdAt: ts,
    })
  );
});
