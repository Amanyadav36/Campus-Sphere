# Campus Sphere
A smart, AI-powered mobile application for managing campus clubs, events, registrations, payments, attendance, and digital certifications.

## Introduction
Campus Sphere is a comprehensive mobile-first ecosystem designed to bridge the communication gap between students, club leaders, and faculty administrators in college campuses. It centralizes event discovery, seamless registration workflows, secure payment processing, automated attendance tracking, and AI-assisted content generation—all within a single, intuitive platform.

## Problem Statement
College campuses often struggle with fragmented event management systems where:
- Students miss out on relevant events due to poor discoverability
- Club leaders face manual, time-consuming registration and certificate generation processes
- Faculty administrators lack centralized oversight and data analytics
- Payment collection and ticket generation are disconnected from event workflows
- Attendance tracking and certificate issuance require significant manual effort

Campus Sphere solves these challenges by providing a unified, role-based platform with intelligent automation and real-time notifications.

## Objectives
- Create a centralized platform for campus event discovery and club management
- Implement secure, role-based access control for students, club leaders, and administrators
- Automate registration workflows, payment processing, and digital certificate generation
- Integrate AI capabilities for intelligent content generation and user assistance
- Provide real-time notifications and personalized event recommendations
- Enable faculty oversight with comprehensive data export and analytics capabilities
- Ensure scalability, security, and maintainability through clean architecture patterns

## Features

### Core Features (All Users)
- **Hybrid Authentication**: Firebase Google Sign-In + Supabase Email OTP with custom "Campus Sphere" branding
- **Role-Based Access Control**: Strict RBAC implementation using Supabase Row Level Security (RLS)
- **AI Chatbot Assistant**: Context-aware, role-based assistant powered by Gemini Pro or Hugging Face APIs
- **Real-Time Notifications**: Event alerts, reminders (24h before events), and certificate generation notifications
- **Personalized Settings**: Profile management, notification preferences, privacy controls

### Student Features
- **Smart Event Feed**: Vertical infinite-scroll feed with interest-based algorithmic prioritization (OTT-style UI)
- **Advanced Discovery**: Global search for events/clubs + monthly calendar view with date-based filtering
- **Seamless Registration**: One-tap registration with integrated Razorpay payment gateway for paid events
- **Digital Ticketing**: Auto-generated tickets with QR codes containing event details and unique ticket IDs
- **Attendance Marking**: Students can mark their attendance by clicking "Mark Attendance" button in their registered events when the club leader enables attendance collection
- **Certificate Access**: Automatic PDF certificate generation stored in Google Drive after attendance is marked and verified
- **Bookmarking & Club Joining**: Save favorite events and join clubs for personalized content
- **Comprehensive Profile**: Displays profile picture, academic details, registered events, earned certificates

### Club Leader Features
- **Event Creation Wizard**: Multi-step form with image upload, rich descriptions, venue mapping, category selection
- **AI Caption Generator**: One-click creative caption generation using Gemini/Hugging Face APIs
- **Registration Dashboard**: Tabular view of registered students with name, email, enrollment number, branch
- **Attendance Control**: Club leaders enable attendance marking for their events through an "Enable Attendance" button, allowing registered students to mark their attendance
- **Certificate Management**: Customizable HTML/PDF templates with dynamic club logo and signature overlay
- **Club Profile Management**: Update club images, descriptions, categories, and member approvals
- **Event Lifecycle Control**: Edit, delete, or republish events with full analytics

### Admin (Faculty) Features
- **Global Oversight Dashboard**: View, edit, or delete any club or event across the campus
- **Role Management System**: Search students by enrollment number and promote/revoke club leader status
- **Comprehensive Data Export**: Download CSV/XLSX reports for individual events, entire clubs, or campus-wide data
- **Content Moderation**: Override permissions to maintain content quality and policy compliance

## Tech Stack

### Frontend
- **Tech**: XML code to design the ui of app 
- **Architecture**: Clean Architecture with MVVM (Model-View-ViewModel) pattern
- **UI Components**: Material Design 3 with custom theming

### Backend & Database
- **Primary Backend**: Supabase (PostgreSQL)
  - Real-time notifications using the firebasse
- **Authentication**: 
  - Firebase Authentication (Google Sign-In, email password)
  - Supabase Auth (Email OTP with googles SMTP service and A good Ui formate for mail)
- **Storage Strategy**:
  - Firebase Storage or cloudinary (Profile pictures, club images, event posters)
  - Google Drive API (Certificate PDFs with Drive file IDs stored in Supabase)

### AI Integration
- **Caption Generation**: Gemini API or Hugging Face Inference API for creative text captions
- **Chatbot**: Gemini API for the context based chatbot, the context or information will be provided to which the chatbot gives answer to users

### Payment Gateway
- **Platform**: Razorpay Android SDK for seamless in-app payment processing

### Additional Tools
- **Notifications**: Firebase Cloud Messaging (FCM)
- **PDF Generation**: pdf library for Dart or server-side generation via Supabase Edge Functions

## User Roles

### Student (Default Role)
- Browse and discover events through personalized feed and search
- Register for events with integrated payment processing
- Mark attendance when enabled by club leader through "Mark Attendance" button in registered events
- Receive and view digital certificates in profile after attendance verification
- Join clubs and bookmark favorite events
- Access AI chatbot for registration and event guidance ansd information

### Club Leader (Assigned by Admin)
- Create and manage events with AI-assisted caption generation
- Upload event posters and manage event lifecycle
- View registration lists and student details
- Enable attendance marking for events through "Enable Attendance" button, allowing registered students to mark their own attendance
- Trigger certificate generation after attendance verification
- Manage club profile, images, and member approvals
- Access analytics for event performance

### Admin (Faculty - Superuser)
- Oversee all clubs, events, and user activities campus-wide
- Promote students to club leader role or revoke permissions
- Moderate content with edit/delete override capabilities
- Export comprehensive reports (event-wise, club-wise, system-wide)
- Manage platform-wide settings and policies

## Application Pages

### Authentication & Onboarding
- **Login/Signup Screen**: Unified interface with Google Auth and Email OTP options , and forghet password
-**verification screen** : to verify the OTP generated match
- **Profile Setup Wizard**: Multi-step form collecting name, branch, enrollment number, gender, contact info, interests, bio

### Student Module (8-9 Screens)
- **Home Feed**: Vertical scrolling event cards with algorithmic prioritization, 2 section ["my events", "all events"]
- **Event Details**: Full event information with registration/payment button
- **Event Calendar View**: Monthly calendar with date-based event gallery
- **Search & Discovery**: Global search for events and clubs with filters
- **Ticket Screen**: Digital ticket display with QR code and event details
- **My Registrations**: List of registered events with "Mark Attendance" button (when enabled by club leader)
- **Clubs List & Details**: Browse clubs, view details, join clubs
- **User Profile**: Complete profile with registered events and certificates
- **Certificates Gallery**: Grid view of earned certificates with Google Drive links
- **Settings**: Profile editing, notification preferences, privacy controls, change password

### Club Leader Module (4-5 Screens)
- **Leader Dashboard**: Overview of managed clubs and events
- **Event Creation Wizard**: Multi-step form with AI caption generation
- **Registration Management**: Tabular view of registered students with "Enable Attendance" button to allow students to mark their attendance
- **Attendance Overview**: View which students have marked their attendance
- **Certificate Designer**: Template selection with logo and signature overlay
- **Club Profile Editor**: Update club information and manage members

### Admin Module (3-4 Screens)
- **Admin Dashboard**: Global overview of all clubs, events, and metrics
- **Role Management**: Search and manage user roles (promote/revoke leaders)
- **Data Export Panel**: Generate and download reports with granular filtering
- **Content Moderation**: Review, edit, or delete any content campus-wide

**Total Estimated Screens**: 10-16 main screens with various sub-screens and modals
