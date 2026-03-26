# Attune — EEG Focus Tracker

Attune is an Android application that helps users track their cognitive focus and mental performance using EEG data from a Bluetooth-connected BrainBit headband. The app collects a personal health profile, records EEG sessions, and provides visual analysis of focus and signal quality metrics.

> **Project:** EECS 582 Capstone — Team 15
> **Platform:** Android (Java, minSdk 28 / Android 9.0, targetSdk 36)
> **Package:** `com.example.eecs582capstone`

---

## Table of Contents

- [Getting Started](#getting-started)
- [App Walkthrough](#app-walkthrough)
  - [1. Splash Screen](#1-splash-screen)
  - [2. Login & Registration](#2-login--registration)
  - [3. Home Tab](#3-home-tab)
  - [4. Results Tab](#4-results-tab)
  - [5. Profile Tab](#5-profile-tab)
  - [6. Intake Quiz](#6-intake-quiz)
  - [7. Session Detail View](#7-session-detail-view)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)

---

## Getting Started

### Requirements

- Android Studio Hedgehog or newer
- Android SDK 36 installed
- A physical Android device (API 28+) or an emulator running Android 9.0+
- Java 11

### Build & Run

1. Clone the repository and open the project root in Android Studio.
2. Let Gradle sync finish automatically.
3. Select a device/emulator from the device dropdown and click **Run**, or run from the terminal:

```bash
./gradlew installDebug
```

4. The app will launch directly to the splash screen.

> No external API keys or server configuration are required. All data is stored locally on the device.

### Branching

When contributing, create a new branch with a short description of your work and open a pull request to merge back into `main`.

---

## App Walkthrough

### 1. Splash Screen

On launch, the Attune branding screen displays for approximately 2 seconds before automatically navigating to the Login screen. No user interaction is required.

---

### 2. Login & Registration

#### First-time users

1. On the Login screen, tap **Register**.
2. Fill in your first name, last name, email address, password, and password confirmation.
3. All fields are required. The password and confirmation must match.
4. Tap **Register** to create your account and return to the Login screen.

#### Returning users

1. Enter your registered email address and password.
2. Tap **Login**.
3. On success, the app navigates to the main screen. Your session is persisted — you will remain logged in on future launches until you manually log out from the Profile tab.

---

### 3. Home Tab

The Home tab is the primary screen for managing EEG recording sessions. It contains three sections.

#### EEG Device Connection

At the top of the screen is a card showing the status of your BrainBit Headband connection.

| State | Indicator | Button |
|---|---|---|
| Disconnected | Gray dot | "Connect" |
| Connecting | Yellow dot | "Connecting…" (disabled) |
| Connected | Green dot | "Disconnect" |

1. Tap **Connect** to initiate pairing. The app simulates a ~2-second connection handshake.
2. Once the dot turns green and the status reads "Connected," session recording becomes available.
3. Tap **Disconnect** at any time to sever the connection.

#### Session Controls

Two requirements must both be satisfied before **Start Session** becomes active:

- The intake quiz must be completed (one-time setup — see [Intake Quiz](#6-intake-quiz)).
- The BrainBit Headband must show as **Connected**.

**Starting a session:**
1. Ensure both requirements above are met.
2. Tap **Start Session**.
3. A pre-session survey dialog appears asking for context about your current state (sleep, caffeine, mood, stress, location, environment). Fill in the fields and tap **Start Reading**.
4. The status text updates to "Session in progress" and **End Session** becomes active.

**Ending a session:**
1. Tap **End Session** when finished recording.
2. The session is saved with an end timestamp in the local database.

#### Aggregated Results

If EEG data has been processed in the Results tab, this section displays a summary across all stored sessions:

- **Overall Focus Score** — weighted average focus score out of 10 (higher-quality sessions carry more weight)
- **Overall Signal Quality** — average signal quality score out of 10
- **Session count** — number of sessions included in the averages

This section is hidden until results have been loaded at least once.

---

### 4. Results Tab

The Results tab is where EEG data is processed and visualized.

1. Tap **Read EEG Data** to load and analyze the bundled demo sessions.
2. Three session result cards appear, each showing:
   - **Variance Score (1–10):** Focus consistency. Low EEG voltage variance = high score; high variance (distracted state) = low score.
   - **Quality Score (1–10):** Signal reliability based on quality values (`q`) in each sample and the proportion of valid (non-null) readings.
3. Tap any session card to open the [Session Detail View](#7-session-detail-view).

Results are persisted automatically after the first load and reappear on future visits without pressing the button again.

#### Demo Sessions

The app ships with three pre-recorded EEG sessions in `assets/demo_sessions.json`:

| Session ID | Label | Expected Result |
|---|---|---|
| `demo_stable_01` | Stable Focus (Low Variance) | High variance score, high quality score |
| `demo_distracted_01` | Distracted (High Variance) | Low variance score, moderate quality score |
| `demo_poor_signal_01` | Poor Signal (Dropouts) | Low quality score due to null samples and low `q` values |

---

### 5. Profile Tab

The Profile tab shows your account information and provides access to the intake quiz.

- **Name and email** are displayed at the top, pulled from the local database.
- **Logout** — clears your session and returns to the Login screen.
- **Take Intake Quiz** — launches the health questionnaire required to unlock session recording.
- **Quiz Summary Card** — once the quiz has been completed, your 8 responses are shown here. You can re-take the quiz at any time by tapping the button again, which updates your stored answers.

---

### 6. Intake Quiz

The intake quiz is a one-time setup step required before **Start Session** becomes active. It is accessed from the Profile tab.

The quiz consists of 8 yes/no questions covering health and lifestyle factors relevant to EEG focus analysis:

1. Are you currently on any medication that affects cognition?
2. Do you have any neurological conditions?
3. Have you been diagnosed with ADHD?
4. Do you regularly consume alcohol?
5. Do you regularly use tobacco or nicotine products?
6. Do you maintain a balanced diet?
7. Do you exercise regularly?
8. Do you typically get 8 or more hours of sleep?

All 8 questions must be answered before submitting. Tap **Submit** to save your responses. Your answers appear in the Profile tab quiz summary card and unlock session recording.

---

### 7. Session Detail View

Tapping a session card in the Results tab opens a detail view showing contextual metadata for that session:

- Sleep hours, time since last meal, caffeine intake
- Mood and stress level
- Test location and date/time
- Environment metrics (light level, noise level, location familiarity) as progress bars

Tap **Back** to return to the Results tab.

---

## Project Structure

```
app/src/main/
├── java/com/example/eecs582capstone/
│   ├── SplashActivity.java            — 2-second intro, navigates to Entry
│   ├── Entry.java                     — Login screen
│   ├── register.java                  — Registration screen
│   ├── MainActivity.java              — BottomNavigationView host (3 tabs)
│   ├── HomeFragment.java              — Session controls + BT connection card
│   ├── ResultsFragment.java           — EEG data processing + result cards
│   ├── ProfileFragment.java           — User info, logout, quiz access
│   ├── SessionDetailFragment.java     — Per-session metadata view
│   ├── UserIntakeQuizActivity.java    — 8-question health questionnaire
│   ├── Users.java                     — User entity (id, name, email, password)
│   └── dbConnect.java                 — SQLite helper (users + sessions tables)
├── res/
│   ├── layout/
│   │   ├── splash_screen.xml
│   │   ├── login.xml
│   │   ├── activity_register.xml
│   │   ├── activity_main.xml
│   │   ├── fragment_first.xml          — Home fragment layout
│   │   ├── fragment_results.xml
│   │   ├── fragment_profile.xml
│   │   ├── fragment_session_detail.xml
│   │   ├── activity_intake_form.xml
│   │   ├── dialog_pre_session_survey.xml
│   │   └── item_session_result.xml     — Session result card item
│   └── drawable/
│       ├── status_dot_connected.xml
│       ├── status_dot_connecting.xml
│       ├── status_dot_disconnected.xml
│       └── rounded_card_background.xml
└── assets/
    └── demo_sessions.json              — 3 pre-recorded EEG sessions
```

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| `androidx.appcompat:appcompat` | 1.7.1 | AndroidX compatibility |
| `androidx.activity:activity` | 1.8.0 | Fragment/activity support |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Flexible layouts |
| `com.google.android.material:material` | 1.13.0 | Material Design components |
| `org.json` | built-in | JSON parsing for demo session data |

---

## Documentation

Sprint artifacts and architecture documents are in the `/Documentation` folder:

- `Initial Architecture Document.pdf`
- `Reference Stories + Requirement Stack.xlsx`
- `Sprint 1/` through `Sprint 4/` — per-sprint deliverable PDFs
- `Team 15 Presentation.pptx`
