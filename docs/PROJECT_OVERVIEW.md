# Campus Sphere (Android) Project Overview

## What This Repo Is
Campus Sphere is an Android (Java) application for campus event + club management with role-based experiences:
- `user` (student): discover events, join clubs, bookmark, register (free/paid), profile.
- `leader` (club leader): create/manage events, manage club profile, view registrations.
- `admin` (faculty/admin): oversight dashboards, manage users/events/clubs/payments.

The app uses Firebase for authentication and Firestore for the primary app data model, plus Supabase Edge Functions for email OTP verification and a campus AI chatbot. Media uploads use Cloudinary, and payments use Razorpay (with optional UPI intent UI).

## Tech Stack
- Android: Java, XML layouts, Material Components
- Build: Gradle (AGP via Version Catalog in `gradle/libs.versions.toml`)
- Auth: Firebase Auth (email/password, Google Sign-In)
- Database: Firebase Firestore
- Storage / Media: Cloudinary (profile images, club/event images), Firebase Storage dependency present
- Payments: Razorpay Android SDK, UPI intents UI in `PaymentMethodActivity`
- Networking: OkHttp
- Images: Glide

## Modules
- `app/`: single Android application module (`com.example.campus_sphere`)

## App Entry + Role Routing
Launcher activity: `SplashActivity` (see `app/src/main/AndroidManifest.xml`).

Routing logic (high-level):
1. If no Firebase user session: go to `LoginActivity`
2. If Firestore `users/{uid}` doc missing or `profileCompleted != true`: go to `UserDetailsActivity`
3. Otherwise route by `role`:
   - `admin` -> `AdminActivity`
   - `leader` -> `LeaderActivity`
   - default -> `MainActivity`

## Navigation (By Role)
### Student (`MainActivity`)
Bottom navigation with fragments:
- `HomeFragment`: event feed + search + basic date filters + chatbot entry
- `ClubListFragment`: "My Clubs" vs "Other Clubs"
- `ProfileFragment`: profile details + sections (bookmarks/events/certificates/receipts) + logout

Key screens launched from student flows:
- `EventDetailsActivity`: event details + registration flow
- `PaymentMethodActivity`: paid-event checkout UI (Razorpay + UPI)
- `ChatBotActivity`: campus AI assistant chat
- `ClubDetailsActivity`: club details + join/leave (also used for leader/admin workflows)
- `ListSectionActivity`: shows items for profile sections (bookmarks, etc.)

### Club Leader (`LeaderActivity`)
Bottom navigation with fragments:
- `LeaderHomeFragment`: leader dashboard + list of created events
- `CreateEventFragment`: create event (image upload + Firestore write)
- `ManageClubFragment`: edit club profile/images + view members + manage club content
- `ProfileFragment`: shared profile view

### Admin (`AdminActivity`)
Bottom navigation with fragments:
- `AdminHomeFragment`: overview counts/metrics (users/events/tickets)
- `AdminUsersFragment`: view/search users and manage roles (see adapters)
- `AdminEventsFragment`: view/edit/delete events
- `AdminClubsFragment`: view clubs (leaders) + entry points for club management
- `AdminPaymentsFragment`: view/verify/reject ticket payments

Additional admin activities:
- `AdminCreateClubActivity`: create club by assigning leader and metadata
- `AdminUsersActivity`, `AdminEventsActivity`, `AdminPaymentsActivity`: list/management screens
- `AdminDashboardActivity`: high-level metrics screen used by some flows
- `AdminAuditLogger`: writes audit records to Firestore

## Data Model (Firestore)
The code reads/writes these collections (non-exhaustive, based on usage in `app/src/main/java`):

### `users` (documents keyed by Firebase `uid`)
Common fields referenced across screens:
- `name`, `email`, `mobile`, `bio`
- `enrollment`, `branch`, `year` (and legacy `section`)
- `role`: `user` | `leader` | `admin`
- `profileImage`
- `profileCompleted` (boolean)

Leader (club) related fields (for leader/admin club features):
- `clubName`, `clubHandle`, `clubBio`
- `clubLogo`, `headerImage`

Subcollections:
- `users/{uid}/bookmarks/{eventId}`: bookmark records used by `EventAdapter` and `ListSectionActivity`

### `events`
Stored from leader create/edit flows and loaded in feeds.
Typical fields (see `Event` model + Firestore usage):
- `eventId`, `title`, `description`, `category`
- `price`, `venue`, `date`, `time`, `imageUrl`
- `creatorId` (leader uid)
- `attendanceEnabled` (boolean)

### `club_members`
Membership mapping for user joining clubs:
- `clubId` (leader uid)
- `userId` (student uid)

### `tickets`
Created when registering for an event (`EventDetailsActivity`):
- `ticketId`, `eventId`, `eventTitle`
- `userId`, `userName`
- `paymentId`, `amount`, `currency`
- `verified` (admin payment verification flag)
- `timestamp`
- `isCheckedIn` (attendance / check-in support)

### `admin_audit_logs`
Written via `AdminAuditLogger` for changes like role updates, moderation, etc.

## OTP + AI (Supabase Edge Functions)
Supabase is used as a lightweight backend for:
- OTP send: `SignupActivity` -> `send-otp` function
- OTP verify: `OtpVerificationActivity` -> `smart-processor` function
- Chatbot: `ChatBotActivity` -> `campus-ai` function

## Payments
Paid registration happens through:
- `EventDetailsActivity` -> launches `PaymentMethodActivity`
- `PaymentMethodActivity` -> Razorpay checkout (and a UPI app grid UI)
- On success: returns a `payment_id` to `EventDetailsActivity`, which stores a `tickets/{ticketId}` doc

## Media Uploads
Cloudinary is initialized in:
- `MainActivity` (safe init)
- `UserDetailsActivity` (profile photo upload)
- leader/admin club/event creation flows (image uploads)

Glide is used for displaying remote images in lists and detail screens.

## Local Setup (Typical)
1. Open the repo in Android Studio.
2. Ensure JDK 11 is configured (project uses Java 11 compile options).
3. Sync Gradle and run the `app` configuration on an emulator/device.

## Repo Layout (Recommended)
This repo is already in the standard Android Gradle layout for source code (`app/`, `gradle/`, wrapper scripts).
For professional repo hygiene, keep extra documentation in `docs/` and build logs/artifacts in `logs/` (not in the repo root).

## Notes / Risks (Professionalization)
- Secrets/keys are present in source and resources (Supabase anon key, Razorpay test key, Cloudinary config). Consider moving these to build configs or remote config and restricting them by environment.
- `requestLegacyExternalStorage=true` + storage permissions are present; Android 13+ storage access should be reviewed.
- Some files contain mis-decoded UTF-8 sequences (visible as `Ã¢â‚¬`/`ðŸ”¥` in some terminals); standardize file encoding to UTF-8.
## Key Source Files (Entry Points)
Activities:
- `app/src/main/java/com/example/campus_sphere/SplashActivity.java`
- `app/src/main/java/com/example/campus_sphere/LoginActivity.java`
- `app/src/main/java/com/example/campus_sphere/SignupActivity.java`
- `app/src/main/java/com/example/campus_sphere/OtpVerificationActivity.java`
- `app/src/main/java/com/example/campus_sphere/UserDetailsActivity.java`
- `app/src/main/java/com/example/campus_sphere/MainActivity.java`
- `app/src/main/java/com/example/campus_sphere/LeaderActivity.java`
- `app/src/main/java/com/example/campus_sphere/AdminActivity.java`
- `app/src/main/java/com/example/campus_sphere/EventDetailsActivity.java`
- `app/src/main/java/com/example/campus_sphere/PaymentMethodActivity.java`
- `app/src/main/java/com/example/campus_sphere/ChatBotActivity.java`

Fragments:
- `app/src/main/java/com/example/campus_sphere/HomeFragment.java`
- `app/src/main/java/com/example/campus_sphere/ClubListFragment.java`
- `app/src/main/java/com/example/campus_sphere/ProfileFragment.java`
- `app/src/main/java/com/example/campus_sphere/LeaderHomeFragment.java`
- `app/src/main/java/com/example/campus_sphere/CreateEventFragment.java`
- `app/src/main/java/com/example/campus_sphere/ManageClubFragment.java`
- `app/src/main/java/com/example/campus_sphere/AdminHomeFragment.java`
- `app/src/main/java/com/example/campus_sphere/AdminUsersFragment.java`
- `app/src/main/java/com/example/campus_sphere/AdminEventsFragment.java`
- `app/src/main/java/com/example/campus_sphere/AdminClubsFragment.java`
- `app/src/main/java/com/example/campus_sphere/AdminPaymentsFragment.java`

Data models:
- `app/src/main/java/com/example/campus_sphere/User.java`
- `app/src/main/java/com/example/campus_sphere/Club.java`
- `app/src/main/java/com/example/campus_sphere/Event.java`

Firestore collections are primarily accessed from:
- `app/src/main/java/com/example/campus_sphere/SplashActivity.java`
- `app/src/main/java/com/example/campus_sphere/EventDetailsActivity.java`
- `app/src/main/java/com/example/campus_sphere/HomeFragment.java`
- `app/src/main/java/com/example/campus_sphere/ClubListFragment.java`
- `app/src/main/java/com/example/campus_sphere/ManageClubFragment.java`
- `app/src/main/java/com/example/campus_sphere/CreateEventFragment.java`
