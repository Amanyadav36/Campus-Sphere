# Campus Sphere — Student (User) Module Detailed Specification

## 1. Overview
The Student (User) module is the primary interface for standard application users. It provides an intuitive, modern, and engaging platform to discover campus events, manage club memberships, handle registrations, and track personal academic/extracurricular participation.

The UI is inspired by top-tier modern modern ticketing platforms (like Zomato Events, Paytm Insider, BookMyShow), featuring clean typography, edge-to-edge imagery, smooth scrollable galleries, and a bright, accessible color palette.

---

## 2. Core Screens & Navigation

The user navigates the app primarily through a Bottom Navigation Bar containing:
1. **Home (Dashboard)**
2. **Clubs**
3. **Profile**

### 2.1. Home Dashboard
The home feed acts as an event discovery hub. It uses a clean, vertical scroll with horizontal categories.

**Layout & Components:**
- **Top App Bar:** 
  - Location/Campus indicator on the top left.
  - Profile avatar bubble on the top right.
- **Search Bar:**
  - Modern, rounded search input spanning the width of the screen underneath the top bar.
- **Toggle/Tabs:**
  - **My Events:** Displays events hosted by the specific clubs the user has already joined.
  - **All Events:** A global feed discovering events from *all other clubs* the user hasn't joined yet.
- **Filter Chips (Horizontal Scroll):** Options like *Today*, *Tomorrow*, *This Week*, *Free*, etc.
- **Event Feed (RecyclerView):**
  - **Premium Event Cards:** Large, highly visual cards featuring an edge-to-edge image at the top, a subtle overlay gradient, and a floating price/tag indicator.
  - **Card Data:** High-quality image poster, Club Name (with verified tick if applicable), Short Description / Event Title, Date and Time.
  - **Actions:** A clear **Register Now** button, and a **Bookmark (Save)** icon. Let users save events for later without registering immediately.

*Note: Students will absolutely NOT see any "Registered Students" list or management buttons.*

### 2.2. Clubs Screen
The Clubs screen focuses on community building, broken into two main tabs:

- **My Clubs Tab:**
  - Shows a list or grid of clubs the user currently belongs to.
  - Tapping a club opens its detail page.
- **Other Clubs Tab:**
  - A discovery list of campus clubs the user has not yet joined.
  - When opening a club from this list, an explicit **Join Club** button is prominently displayed.
- **Club Details View:**
  - Full cover image and rounded club logo.
  - Club Bio, total members count.
  - List of past and upcoming events hosted strictly by this club.

### 2.3. Event Registration & Payment Flow (Modernized)
- **Event Details Screen:** Immersive full-screen poster layout, collapsing toolbar, rich text description, clear venue mapping, and dynamic pricing.
- **Modern Payment UI:** 
  - When the user taps "Register", if the event is paid, a bottom sheet or a highly polished checkout screen slides up.
  - Incorporates dynamic UPI deep-linking (GPay, PhonePe, Paytm).
  - Razorpay SDK handles the backend securely but feels native and instantaneous to the user.
- **Tickets:** Successful payments generate a Digital Ticket with a QR code stored securely in the app.

### 2.4. Comprehensive Profile Screen
The profile is a detailed portfolio of the student's campus life.

**Profile Information Displayed:**
1. **Profile Image**
2. **Full Name**
3. **Email Address**
4. **Mobile Number**
5. **Enrollment Number**
6. **Academic Branch**
7. **Year of Study**
8. **Bio / About Me**
9. **Areas of Interest**

**Interactive Sections (Grid or Tabs):**
- **Bookmarks:** Saved events the user is interested in.
- **Events:** History of registered events & upcoming tickets.
- **Certificates:** Rendered grid of digital certificates generated automatically post-attendance.
- **Receipts:** Payment history and Razorpay invoice records for transparent tracking.

---

## 3. Key User Actions & Data Flow
- **Feed Generation:** Home fragment cross-references the `club_members` collection against `events` to cleanly split the feed into "My Events" and "All Events".
- **Joining Clubs:** Mutates the `club_members` table by adding a document `[clubUid]_[userUid]`.
- **Payment & Registration:** Handled securely via Razorpay, generating a locked ticket document under the `tickets` collection linked uniquely to the User's UID.
- **AI Chatbot (Floating Action Button):** A floating action button persists on the Home feed, opening the campus AI assistant to answer queries about event timings, locations, and club activities.
