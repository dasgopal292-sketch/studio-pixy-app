const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_pixy_1";
const ADMIN_EMAIL = "studiopixy64@gmail.com";
const CUSTOMER_UID = "customer_123";
const OTHER_UID = "customer_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validServicePayload(overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    name: "4x6 Glossy Photo Print",
    category: "PRINT",
    size: "4x6 inch",
    frameType: "",
    frameDesign: "",
    price: 25,
    unitLabel: "per print",
    description: "Archival lab-grade glossy print with rich color.",
    deliverables: "High-gloss 260gsm paper",
    imageUrl: "drawable:img_photo_printing",
    published: true,
    popular: true,
    sortOrder: 2,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validOrderPayload(overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    orderCode: "PX-2026-1042",
    serviceId: "print_4x6",
    serviceName: "4x6 Glossy Photo Print",
    category: "PRINT",
    selectedSize: "4x6 inch",
    selectedFrameType: "",
    selectedFrameDesign: "",
    quantity: 10,
    unitPrice: 25,
    totalAmount: 250,
    customerName: "Aarav Sharma",
    customerPhone: "9876543210",
    customerNotes: "Borderless glossy finish please",
    photoCount: 3,
    photoSummary: "IMG_01.jpg, IMG_02.jpg, IMG_03.jpg",
    status: "PENDING",
    customerUid: "",
    adminRemarks: "",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validConfigPayload(overrides = {}) {
  return {
    businessName: "Studio Pixy",
    tagline: "Capturing Timeless Memories in Every Frame",
    whatsappNumber: "919876543210",
    phoneNumber: "+91 98765 43210",
    email: "studiopixy64@gmail.com",
    address: "Station Road, Near City Center, Kolkata 700001",
    openingHours: "Mon - Sun: 9:30 AM - 9:00 PM",
    mapsUrl: "https://maps.google.com/?q=Studio+Pixy",
    welcomeBannerTitle: "Pro Prints, Custom Frames & Wedding Cinema",
    welcomeBannerSubtitle: "Same-day passport prints & instant WhatsApp ordering",
    updatedAt: new Date(Date.now() - 2000),
    ...overrides,
  };
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

test("Customers without login can read published services but not unpublished drafts", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("services").doc("svc_pub").set(validServicePayload({ published: true }));
    await db.collection("services").doc("svc_draft").set(validServicePayload({ published: false }));
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("services").doc("svc_pub").get());
  await assertSucceeds(
    unauthDb.collection("services").where("published", "==", true).get()
  );
  await assertFails(unauthDb.collection("services").doc("svc_draft").get());
  await assertFails(unauthDb.collection("services").get());
});

test("Only verified Admin can create, update, or delete services; email spoofing fails", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const customerDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "random@gmail.com", email_verified: true })
    .firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await assertFails(unauthDb.collection("services").doc("svc_1").set(validServicePayload()));
  await assertFails(customerDb.collection("services").doc("svc_1").set(validServicePayload()));
  await assertFails(spoofedAdminDb.collection("services").doc("svc_1").set(validServicePayload()));

  // Valid admin create
  await assertSucceeds(adminDb.collection("services").doc("svc_1").set(validServicePayload()));

  // Reject ghost field / @DocumentId 'id' inside doc body
  await assertFails(
    adminDb.collection("services").doc("svc_bad").set(validServicePayload({ id: "svc_bad" }))
  );

  // Valid admin partial update (preserves createdAt)
  await assertSucceeds(
    adminDb.collection("services").doc("svc_1").update({
      price: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    adminDb.collection("services").doc("svc_1").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(adminDb.collection("services").doc("svc_1").delete());
});

test("Customer can submit valid PENDING order without login, cannot tamper price or status", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Valid guest order succeeds
  await assertSucceeds(unauthDb.collection("orders").doc("order_1").set(validOrderPayload()));

  // Tampered totalAmount fails
  await assertFails(
    unauthDb.collection("orders").doc("order_2").set(validOrderPayload({ totalAmount: 10 }))
  );

  // Tampered initial status fails
  await assertFails(
    unauthDb.collection("orders").doc("order_3").set(validOrderPayload({ status: "COMPLETED" }))
  );

  // Impersonating another user's customerUid fails
  await assertFails(
    unauthDb.collection("orders").doc("order_4").set(validOrderPayload({ customerUid: OTHER_UID }))
  );

  // Future timestamp fails
  await assertFails(
    unauthDb
      .collection("orders")
      .doc("order_5")
      .set(validOrderPayload({ createdAt: new Date(Date.now() + 120000) }))
  );
});

test("Customers cannot read, list, edit, or delete other customers' orders; Admin manages all orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("orders")
      .doc("order_guest")
      .set(validOrderPayload({ customerUid: "" }));
    await db
      .collection("orders")
      .doc("order_cust")
      .set(validOrderPayload({ customerUid: CUSTOMER_UID }));
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const otherDb = testEnv
    .authenticatedContext(OTHER_UID, { email: "other@gmail.com", email_verified: true })
    .firestore();
  const customerDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "cust@gmail.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  // Unauthenticated cannot read or list orders
  await assertFails(unauthDb.collection("orders").doc("order_guest").get());
  await assertFails(unauthDb.collection("orders").get());

  // Other customer cannot read, list, update, or delete someone else's order
  await assertFails(otherDb.collection("orders").doc("order_cust").get());
  await assertFails(otherDb.collection("orders").get());
  await assertFails(
    otherDb.collection("orders").doc("order_cust").update({ status: "REJECTED" })
  );
  await assertFails(otherDb.collection("orders").doc("order_cust").delete());

  // Signed-in customer can read/list ONLY their own orders
  await assertSucceeds(customerDb.collection("orders").doc("order_cust").get());
  await assertSucceeds(
    customerDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get()
  );
  await assertFails(customerDb.collection("orders").doc("order_guest").get());

  // Admin can list all orders and update status
  await assertSucceeds(adminDb.collection("orders").get());
  await assertSucceeds(
    adminDb.collection("orders").doc("order_guest").update({
      status: "ACCEPTED",
      adminRemarks: "Ready for printing today",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("order_guest").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Studio config readable by public, writable only by verified Admin", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await assertFails(unauthDb.collection("studio_config").doc("main").set(validConfigPayload()));
  await assertSucceeds(adminDb.collection("studio_config").doc("main").set(validConfigPayload()));
  await assertSucceeds(unauthDb.collection("studio_config").doc("main").get());
  await assertFails(unauthDb.collection("studio_config").get());
});
