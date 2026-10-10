# RASTA AI — Complete Native Android & PHP Backend AI Mobile Assistant

## Overview
**Rasta AI** is an advanced, production-oriented personal mobile AI assistant designed for Android, featuring:
- **Assistant Persona:** Rasta AI
- **User Personalization:** Shoaib
- **Default Roman Urdu Greeting:** *"Assalam-o-Alaikum Shoaib! Main Rasta AI hoon. Aap ko kis cheez mein madad chahiye?"*
- **Multilingual Engine:** Urdu, Roman Urdu, English, Hindi, Punjabi, Arabic, French, Spanish, German, Chinese with bidirectional Layout Direction (Urdu/Arabic RTL & English LTR).
- **Voice Activation:** Speech recognition, Text-to-Speech (TTS), speech interruption, voice pitch/rate tuning, and wake phrase detection (*"Hey Rasta"*, *"Rasta AI"*).
- **Android Mobile Control & Device Automation:** Real hardware flashlight toggle, battery telemetry, one-tap installed app launching, phone dialer intents, SMS drafting with confirmation review, alarms, and Google web search.
- **Enterprise-Grade Role & Subscription Management:** Administrator dashboard, pending user registration approvals, plan quotas (Free, Basic, Pro, Premium, Enterprise), and audit logs.
- **Persistent PHP 8+ Backend:** REST API with atomic JSON file storage, installation wizard (`backend/install.php`), and Bootstrap 5 Admin Portal.

---

## 1. Initial Administrator Account
- **Username:** `Malik`
- **Initial Password:** `asad`
- **Security Requirement:** Initial login triggers an automatic password change requirement before administrative access.

---

## 2. User Accounts & Approval Workflow
- User registration requires: Full name, username, email/phone, password, confirm password, terms acceptance.
- All new registrations default to `PENDING` status.
- Pending users are blocked from protected assistant queries until approved by Administrator `Malik`.
- Pre-configured demo user:
  - **Username:** `Shoaib`
  - **Password:** `shoaib123`
  - **Role:** `USER` (Approved, Pro Plan)

---

## 3. Android Application Architecture
- **Application ID:** `com.rastaai.assistant`
- **Framework:** 100% Jetpack Compose with Material 3
- **Dual-Storage Engine:** Works standalone out of the box with embedded local persistence, or connected to the PHP REST API backend via Settings.
- **Foreground Service:** `RastaVoiceForegroundService` with notification channel for active background listening.

---

## 4. PHP Backend Installation
1. Place the `/backend` folder on any PHP 8.0+ web server (Apache/Nginx).
2. Open `http://your-server/backend/install.php` in a web browser.
3. The wizard validates system requirements (PHP 8+, cURL, JSON, write permissions).
4. Click **Complete Backend Installation** to initialize storage and lock setup.
5. Access the Web Admin Portal at `backend/admin/index.html`.

---

## 5. Automated Tests
- **Android Tests:** Local JVM unit and Robolectric tests in `app/src/test/java/com/example/ExampleRobolectricTest.kt`.
  Run: `gradle :app:testDebugUnitTest`
- **Backend Tests:** Automated PHP verification script in `backend/tests/test_backend.php`.
  Run: `php backend/tests/test_backend.php`
