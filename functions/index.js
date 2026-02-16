const { onCall, HttpsError } = require('firebase-functions/v2/https');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');

admin.initializeApp();

async function assertAdminCaller(uid) {
  const userDoc = await admin.firestore().collection('users').doc(uid).get();
  if (!userDoc.exists) {
    throw new HttpsError('permission-denied', 'Caller profile not found.');
  }

  const role = userDoc.get('role');
  if (role !== 'administrator') {
    throw new HttpsError('permission-denied', 'Only administrators can create users.');
  }
}

exports.adminCreateUser = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError('unauthenticated', 'You must be signed in.');
  }

  await assertAdminCaller(request.auth.uid);

  const data = request.data || {};
  const email = String(data.email || '').trim().toLowerCase();
  const password = String(data.password || '');
  const name = String(data.name || '').trim();
  const role = String(data.role || '').trim();

  if (!email || !password || !name) {
    throw new HttpsError('invalid-argument', 'name, email, and password are required.');
  }

  if (password.length < 6) {
    throw new HttpsError('invalid-argument', 'Password must be at least 6 characters.');
  }

  if (!['student', 'teacher'].includes(role)) {
    throw new HttpsError('invalid-argument', 'Role must be student or teacher.');
  }

  let authUser;
  try {
    authUser = await admin.auth().createUser({
      email,
      password,
      displayName: name,
      emailVerified: false,
      disabled: false
    });
  } catch (error) {
    logger.error('createUser(auth) failed', error);
    throw new HttpsError('already-exists', error.message || 'Failed to create auth user.');
  }

  const profile = {
    uid: authUser.uid,
    name,
    email,
    role,
    isSetupComplete: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  if (role === 'student') {
    profile.grade = '';
    profile.interests = [];
  }

  if (role === 'teacher') {
    profile.subjects = [];
    profile.availability = [];
  }

  try {
    await admin.firestore().collection('users').doc(authUser.uid).set(profile);
  } catch (error) {
    logger.error('createUser(firestore) failed, rolling back auth user', error);
    await admin.auth().deleteUser(authUser.uid);
    throw new HttpsError('internal', 'Failed to create user profile.');
  }

  return {
    uid: authUser.uid,
    email,
    role,
    name
  };
});
