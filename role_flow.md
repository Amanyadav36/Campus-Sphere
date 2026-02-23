# Campus Sphere — Role-Based User Flow Document

> **Document Purpose:** This document maps the full navigation and interaction flow for each user role in the Campus Sphere application, cross-referenced with the actual codebase implementation and the official feature spec.

---

## 🗺️ Application Entry Point

```
App Launch
    └── SplashActivity (700ms delay)
            ├── Not Logged In  ──────────────────────► LoginActivity
            ├── Logged In, Profile Incomplete ───────► UserDetailsActivity
            └── Logged In, Profile Complete
                    ├── role = "admin"   ────────────► AdminActivity
                    ├── role = "leader"  ────────────► LeaderActivity
                    └── role = "user"   (default) ──► MainActivity
```

---

## 🔐 Authentication Flow (All Roles)

### Screen: `LoginActivity`

| Step | Action | Result |
|------|--------|--------|
| 1 | User opens the app for the first time | `SplashActivity` → `LoginActivity` |
| 2 | Enter email + password OR tap Google Sign-In | Firebase Authentication called |
| 3 | Login successful | Firestore `users` collection queried for `role` field |
| 4 | Role determined | Redirected to appropriate dashboard |
| 5 | Login failed | Error toast shown, user stays on login screen |

**Forgot Password:** `LoginActivity` → `ForgotPasswordActivity` → Firebase sends reset email → User returns to Login

---

### Screen: `SignupActivity`

| Step | Action | Result |
|------|--------|--------|
| 1 | Tap "Don't have an account? Sign Up" | Navigate to `SignupActivity` |
| 2 | Enter email + password | Firebase creates auth account |
| 3 | OTP Verification | `OtpVerificationActivity` shown |
| 4 | OTP verified | Navigate to `UserDetailsActivity` |

---

### Screen: `UserDetailsActivity` (Profile Setup — First Time Only)

| Step | Action | Result |
|------|--------|--------|
| 1 | Redirected after first-time sign-in | Profile setup form appears |
| 2 | Fill: Name, Enrollment, Branch, Section, Gender, Interest | Required for all roles |
| 3 | (Optional) Pick a profile picture | Uploaded to Cloudinary |
| 4 | Tap "Submit" | Data saved to Firestore `users/{uid}` with `profileCompleted: true` |
| 5 | Save success | Directed to appropriate dashboard based on role |

---

---

## 👤 ROLE 1: Student (Default User)

**Entry Point:** `MainActivity` (bottom navigation with 3 tabs)

```
MainActivity
    ├── [Tab 1: Home] ─────── HomeFragment
    ├── [Tab 2: Clubs] ────── ClubListFragment
    └── [Tab 3: Profile] ─── ProfileFragment
```

---

### Tab 1 — Home Feed (`HomeFragment`)

**Screen:** `user_home.xml`

```
HomeFragment
├── Search Bar (filters events in real time)
├── [All Events] button ── Shows all events from Firestore
├── [My Events] button ─── Shows events user is registered for (tickets)
├── Event Card List (RecyclerView with event.xml cards)
│       ├── Event image, club name, title, date/venue
│       └── [View Event Details] button
│               └── ──────► EventDetailsActivity
└── FAB (chat icon) ───────► ChatBotActivity
```

**Key flows:**
- Events are fetched from `Firestore > events` collection
- On first launch, `DatabaseSeeder` seeds 4 clubs and 5 events automatically
- Tapping any event card opens `EventDetailsActivity`

---

### Event Details (`EventDetailsActivity`)

```
EventDetailsActivity
├── Event poster image (full width, 250dp)
├── Event title, price, date/time, venue
├── "About Event" description
├── [Register Now / Register for Free] ── action button
│       ├── Free Event ────► registerUser("FREE_TICKET")
│       │                           └── Ticket saved to Firestore > tickets
│       └── Paid Event ────► PaymentMethodActivity
│                                   ├── Choose: UPI / Card / Netbanking
│                                   ├── [PAY SECURELY] ──► Razorpay Checkout
│                                   │       ├── Payment Success ──► registerUser(paymentId)
│                                   │       └── Payment Failed  ──► Toast error
│                                   └── On success: RESULT_OK → registerUser()
│
└── [View Registered Students] ─── (VISIBLE only to leaders/creators)
        └── RegisteredStudentsActivity
                └── List of all registered students (name, branch, enrollment)
```

---

### Tab 2 — Clubs (`ClubListFragment`)

**Screen:** `fragment_club_list.xml`

```
ClubListFragment
├── [My Clubs] tab ──── Shows clubs where user is a member (club_members collection)
├── [Other Clubs] tab ── Shows all other clubs (users with role="leader")
└── Club Cards (item_club.xml)
        └── Club logo, name, handle, bio
                └── Tap ──► ClubDetailsActivity (Club detail page)
```

---

### Tab 3 — Profile (`ProfileFragment`)

**Screen:** `activity_profile_fixed.xml`

```
ProfileFragment
├── Profile image (circle, from Cloudinary)
├── Name + Email
├── Academic Details card
│       ├── Enrollment Number
│       ├── Branch
│       ├── Section
│       └── Interest
├── [Edit Profile] ──────► UserDetailsActivity (edit mode)
└── [Log Out] ───────────► Firebase signOut + Google signOut → LoginActivity
```

---

### AI Chatbot (`ChatBotActivity`)

```
ChatBotActivity
├── Message history (RecyclerView, left = AI, right = User)
├── Input field + Send button
└── Messages sent to Supabase Edge Function (campus-ai)
        ├── Context: "You are Campus Sphere AI for CDGI"
        └── Response displayed as AI bubble
```

---

---

## 🎭 ROLE 2: Club Leader

**Entry Point:** `LeaderActivity` (bottom navigation with 4 tabs)

```
LeaderActivity
    ├── [Tab 1: Dashboard] ─── LeaderHomeFragment
    ├── [Tab 2: Create Event] ─ CreateEventFragment
    ├── [Tab 3: Manage Club] ── ManageClubFragment
    └── [Tab 4: Profile] ───── ProfileFragment (shared)
```

---

### Tab 1 — Dashboard (`LeaderHomeFragment`)

**Screen:** `fragment_leader_home.xml`

```
LeaderHomeFragment
├── Stat Cards (horizontal)
│       ├── [Active Events count] ── from Firestore events where creatorId = uid
│       └── [Members count] ──────── from club_members collection
├── "Manage Events" section label
└── Events RecyclerView (item_leader_event.xml)
        ├── Each event: title, date, venue
        └── [Edit] and [Delete] buttons per event
                ├── [Edit] ───► EditEventActivity (or dialog)
                └── [Delete] ─► Firestore event deleted
```

---

### Tab 2 — Create Event (`CreateEventFragment`)

**Screen:** `fragment_create_event.xml`

```
CreateEventFragment
├── Event Poster image selector (Cloudinary upload)
├── Event Title input
├── Category spinner (Technology, Arts, Sports, etc.)
├── Date picker + Time picker
├── Venue input
├── Description / Caption input
├── Price input (blank = Free)
├── [✨ Generate AI Caption] ──► Gemini API → auto-fills description
├── Attendance toggle (Enable/Disable attendance marking)
└── [Create Event] button
        ├── Image uploaded to Cloudinary
        └── Event document saved to Firestore > events
                ├── eventId, title, description, category
                ├── price, venue, date, time, imageUrl
                └── creatorId = current leader's UID
```

---

### Tab 3 — Manage Club (`ManageClubFragment`)

**Screen:** `fragment_manage_club.xml`

```
ManageClubFragment
├── Header Image (tap to upload new cover to Cloudinary)
├── Club logo / icon (tap to upload new logo to Cloudinary)
├── Club name, @handle, bio text
├── "[X] Members" count
├── [Edit Profile] button ──► Dialog to update clubName, clubHandle, clubBio
├── Tab Layout inside:
│       ├── [Events] tab ────────────────────────────── shows club events
│       │       └── List of created events (item_leader_event.xml)
│       └── [Members] tab ───────────────────────────── shows club members
│               └── List of Users from club_members collection
│                       (StudentAdapter → item_student.xml)
│                       (Name, Branch, Enrollment)
└── ensureLeaderMembership() ─► Auto-registers leader in their own club_members doc
```

---

### Leader Registration Management

```
EventDetailsActivity (Leader view sees extra button)
└── [View Registered Students] ─► RegisteredStudentsActivity
        ├── Fetches all tickets with matching eventId
        └── For each ticket → fetches user details from Firestore > users
                └── Displays: Profile image, Name, Branch • Enrollment
```

---

---

## 🛡️ ROLE 3: Admin (Faculty Superuser)

**Entry Point:** `AdminActivity` (bottom navigation with 5 tabs)

```
AdminActivity
    ├── [Tab 1: Home] ──────── AdminHomeFragment (Dashboard overview)
    ├── [Tab 2: Users] ─────── AdminUsersFragment
    ├── [Tab 3: Events] ────── AdminEventsFragment
    ├── [Tab 4: Clubs] ─────── AdminClubsFragment
    └── [Tab 5: Payments] ──── AdminPaymentsFragment
```

---

### Tab 1 — Dashboard (`AdminHomeFragment` / `AdminDashboardActivity`)

**Screen:** `activity_admin_dashboard.xml`

```
AdminDashboard
├── Header: "Admin Dashboard" + overview text
├── Stat Cards (2×2 grid)
│       ├── Total Users ───── from Firestore > users collection
│       ├── Total Events ──── from Firestore > events collection
│       ├── Total Tickets ─── from Firestore > tickets collection
│       └── Revenue ───────── count of paid tickets (paymentId ≠ "FREE_TICKET")
├── Quick Actions
│       ├── [Manage Users] ──────► AdminUsersActivity
│       ├── [Moderate Events] ───► AdminEventsActivity
│       └── [Review Payments] ───► AdminPaymentsActivity
└── [Log Out] button ──────────── Firebase + Google signOut → LoginActivity
```

> **Note:** Stats auto-refresh on `onResume()` — every time the admin returns to this screen.

---

### Tab 2 — User Management (`AdminUsersFragment`)

```
AdminUsersFragment
├── List of all registered users
├── Search/filter by name or enrollment
└── Per user actions:
        ├── Promote to Leader ─► Updates role = "leader" in Firestore
        └── Revoke Leader ────► Updates role = "user" in Firestore
```

---

### Tab 3 — Event Management (`AdminEventsFragment`)

```
AdminEventsFragment
├── List of ALL events across ALL clubs
└── Per event actions:
        ├── View details
        ├── Edit event ──► (Edit Event form)
        └── Delete event → Removes from Firestore > events
```

---

### Tab 4 — Club Management (`AdminClubsFragment`)

```
AdminClubsFragment
├── List of all clubs (users with role="leader")
├── [Create New Club] ──► AdminCreateClubActivity
│       └── Creates a leader user document + assigns role
└── Per club: view details, edit, or remove
```

---

### Tab 5 — Payments (`AdminPaymentsFragment`)

```
AdminPaymentsFragment
├── List of all payment transactions from Firestore > tickets
├── Shows: studentName, eventTitle, paymentId, amount, timestamp
└── (Future: CSV export of payment data)
```

---

---

## 💳 Payment Flow (All Paying Users)

```
EventDetailsActivity
    └── [Register Now] (Paid event)
            └── PaymentMethodActivity
                    ├── Shows event name + amount
                    ├── Detected UPI Apps (horizontal scroll gallery)
                    ├── Radio options: UPI / Debit-Credit Card / Net Banking
                    └── [PAY SECURELY]
                            └── Razorpay Checkout SDK opens
                                    ├── Success → onPaymentSuccess(paymentId)
                                    │       └── returns RESULT_OK to EventDetailsActivity
                                    │               └── registerUser(paymentId)
                                    │                       └── Ticket saved to Firestore > tickets
                                    └── Failure → onPaymentError()
                                            └── Toast "Payment failed" shown
```

---

---

## 🔄 Data Flow Summary

| Collection | Written By | Read By |
|---|---|---|
| `users` | Signup, UserDetails, Leader profile | All roles, SplashActivity |
| `events` | Leaders (CreateEvent), DatabaseSeeder | Students (Home), Admins |
| `tickets` | EventDetailsActivity (on registration) | Student (My Events), Leader (Registered list), Admin |
| `club_members` | ManageClubFragment (ensureLeaderMembership), User joining | ClubListFragment (My Clubs), ManageClubFragment (Members tab) |

---

## ⚠️ Current Implementation Status

| Feature | Status |
|---|---|
| Login / Signup / OTP | ✅ Implemented |
| Profile Setup (UserDetailsActivity) | ✅ Implemented |
| Role-based routing (Splash) | ✅ Implemented |
| Student Home Feed | ✅ Implemented |
| Event Details + Free Registration | ✅ Implemented |
| Razorpay Payment | ✅ Integrated |
| Club List (Tabs: My / Other) | ✅ Implemented |
| Profile Screen | ✅ Implemented |
| AI Chatbot (Supabase Edge) | ✅ Implemented |
| Leader Dashboard | ✅ Implemented |
| Create Event (AI Caption) | ✅ Implemented |
| Manage Club (Members + Events) | ✅ Implemented |
| Admin Dashboard (stats) | ✅ Implemented |
| Admin User Role Management | ✅ Implemented |
| Admin Events Moderation | ✅ Implemented |
| Admin Club Management | ✅ Implemented |
| Admin Payments View | ✅ Implemented |
| Database Seeder (4 clubs, 5 events) | ✅ Implemented |
| Attendance Marking (Student) | 🔲 Planned |
| QR Code Ticket Display | 🔲 Planned |
| PDF Certificate Generation | 🔲 Planned |
| Event Calendar View | 🔲 Planned |
| CSV Data Export (Admin) | 🔲 Planned |
| Push Notifications (FCM) | 🔲 Planned |
