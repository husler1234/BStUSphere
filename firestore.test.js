const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_bstu_123";
const BOB_UID = "bob_bstu_456";
const CHARLIE_UID = "charlie_bstu_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);
const serverTs = () => new Date(Date.now() - 1000);

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

// --- 1. USER PROFILE & PII ISOLATION ---

test("Unauthenticated user cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      faculty: "Applied Math",
      academicGroup: "601i",
      studyYear: 2,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

test("User can create and read own profile, but cannot read Bob's profile (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice H.",
      email: "alice@bstu.edu.az",
      faculty: "Applied Mathematics & Cybernetics",
      academicGroup: "642a",
      studyYear: 3,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update on user profile with ghost field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      faculty: "Physics",
      academicGroup: "102a",
      studyYear: 1,
      isAdmin: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

// --- 2. PERSONAL CLASS SCHEDULES ---

test("User can CRUD own schedule, cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const schedRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("schedules")
    .doc("sched_1");

  await assertSucceeds(
    schedRef.set({
      userId: ALICE_UID,
      courseName: "Linear Algebra",
      courseCode: "MATH-201",
      instructor: "Prof. Mammadov",
      building: "Main Building",
      roomNumber: "312",
      dayOfWeek: 1,
      startTime: "09:00",
      endTime: "10:20",
      classType: "Lecture",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("schedules").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("schedules").get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("schedules")
      .doc("sched_1")
      .delete()
  );
});

// --- 3. LECTURE HALLS DIRECTORY ---

test("Lecture halls query requires campus visibility filter and protects creator ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("lectureHalls").doc("hall_main_312").set({
      createdBy: ALICE_UID,
      visibility: "campus",
      name: "Big Physics Auditorium",
      building: "Main Building",
      floor: "3rd Floor",
      roomNumber: "312",
      faculty: "Physics",
      capacity: 120,
      directions: "Central staircase up to 3rd floor, turn right.",
      amenities: ["Projector", "Wi-Fi"],
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Query with matching filter succeeds for Bob
  await assertSucceeds(
    bobDb.collection("lectureHalls").where("visibility", "==", "campus").get()
  );

  // Unfiltered query fails
  await assertFails(bobDb.collection("lectureHalls").get());

  // Bob cannot delete Alice's lecture hall entry
  await assertFails(
    bobDb.collection("lectureHalls").doc("hall_main_312").delete()
  );
});

// --- 4. POSTS & COMMENTS ---

test("Campus posts enforce query filter, author ownership, and allow comments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorFaculty: "Applied Math",
      visibility: "campus",
      category: "material",
      subject: "Algorithms",
      title: "Dynamic Programming Lecture Notes",
      body: "Attached summary from room 204 seminar.",
      attachmentName: "dp_notes.pdf",
      attachmentMimeType: "application/pdf",
      attachmentSizeBytes: 245000,
      commentsCount: 0,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Bob can query campus posts with filter
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "campus").get()
  );

  // Unfiltered list rejected
  await assertFails(bobDb.collection("posts").get());

  // Bob cannot edit Alice's post body
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      body: "Hacked body",
      updatedAt: serverTs(),
    })
  );

  // Bob can comment and increment commentsCount
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .doc("comment_1")
      .set({
        postId: "post_1",
        authorId: BOB_UID,
        authorName: "Bob",
        visibility: "campus",
        body: "Thanks for sharing!",
        createdAt: serverTs(),
      })
  );

  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      commentsCount: 1,
      updatedAt: serverTs(),
    })
  );
});

// --- 5. STUDY GROUPS & GROUP CHAT ---

test("Study groups enforce visibility, join/leave rules, and member-only messaging", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice creates a public group
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_algo").set({
      name: "BStU Algorithms Circle",
      subject: "Computer Science",
      faculty: "Applied Math",
      description: "Weekly problem solving in Main Building.",
      meetingLocation: "Main Building • 208",
      visibility: "public",
      createdBy: ALICE_UID,
      creatorName: "Alice",
      memberIds: [ALICE_UID],
      memberCount: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Alice creates a private group
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_private").set({
      name: "Thesis Team 4",
      subject: "Research",
      faculty: "Applied Math",
      description: "Private thesis group.",
      visibility: "private",
      createdBy: ALICE_UID,
      creatorName: "Alice",
      memberIds: [ALICE_UID],
      memberCount: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Bob cannot join the private group
  await assertFails(
    bobDb.collection("groups").doc("grp_private").update({
      memberIds: [ALICE_UID, BOB_UID],
      memberCount: 2,
      updatedAt: serverTs(),
    })
  );

  // Bob cannot read private group
  await assertFails(bobDb.collection("groups").doc("grp_private").get());

  // Bob CAN join the public group
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_algo").update({
      memberIds: [ALICE_UID, BOB_UID],
      memberCount: 2,
      updatedAt: serverTs(),
    })
  );

  // Charlie (non-member) CANNOT send a message in grp_algo
  await assertFails(
    charlieDb
      .collection("groups")
      .doc("grp_algo")
      .collection("messages")
      .doc("msg_1")
      .set({
        groupId: "grp_algo",
        authorId: CHARLIE_UID,
        authorName: "Charlie",
        body: "Sneaking in!",
        visibility: "public",
        memberIds: [ALICE_UID, BOB_UID],
        createdAt: serverTs(),
      })
  );

  // Bob (member) CAN send a message in grp_algo
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc("grp_algo")
      .collection("messages")
      .doc("msg_2")
      .set({
        groupId: "grp_algo",
        authorId: BOB_UID,
        authorName: "Bob",
        body: "See you in Room 208 at 14:00!",
        visibility: "public",
        memberIds: [ALICE_UID, BOB_UID],
        createdAt: serverTs(),
      })
  );

  // Querying public group messages with visibility filter succeeds
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc("grp_algo")
      .collection("messages")
      .where("visibility", "==", "public")
      .get()
  );

  // Bob can leave the public group
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_algo").update({
      memberIds: [ALICE_UID],
      memberCount: 1,
      updatedAt: serverTs(),
    })
  );
});
