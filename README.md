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
- [Feature Status](#feature-status)
- [Known Limitations](#known-limitations)
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

> **Security note:** Passwords are stored in plain text in the local SQLite database in the current build. See [Known Limitations](#known-limitations).

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

> **The Bluetooth connection is a mock simulation.** The app does not communicate with a real BrainBit device over Bluetooth. See [Known Limitations](#known-limitations).

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

> **Note:** The pre-session survey fields are displayed but the responses are not currently saved or linked to the session record. This is a UI placeholder. See [Known Limitations](#known-limitations).

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

> **The app only processes bundled demo data.** Live EEG streaming from the BrainBit device is not yet implemented. See [Known Limitations](#known-limitations).

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

> **All data in this view is hardcoded mock data** mapped to specific session IDs for demonstration purposes. The view does not pull real pre-session survey responses. See [Known Limitations](#known-limitations).

---

## Feature Status

| Feature | Status |
|---|---|
| Splash screen | Complete |
| User registration & login | Complete |
| Session persistence (stay logged in) | Complete |
| Bottom navigation (Home / Results / Profile) | Complete |
| Intake quiz (8 questions, save & display answers) | Complete |
| Intake quiz gating for session start | Complete |
| Bluetooth device connection UI | Complete (mock only) |
| BT connection gating for session start | Complete |
| Session start/end with SQLite timestamps | Complete |
| EEG data processing from demo JSON | Complete |
| Focus score (variance-based, 1–10) | Complete |
| Signal quality score (1–10) | Complete |
| Aggregated history on Home tab | Complete |
| Session result cards with progress bars | Complete |
| Session detail view | Complete (mock data only) |
| Pre-session survey dialog (UI) | Partial — data not saved |
| Real Bluetooth communication with BrainBit | Not implemented |
| Live EEG signal streaming & processing | Not implemented |
| Pre-session survey responses saved to database | Not implemented |
| Session detail view using real survey data | Not implemented |
| User profile editing (name / email) | Not implemented |
| Password reset | Not implemented |
| Data export | Not implemented |

---

## Known Limitations

### Bluetooth is simulated
The Connect/Disconnect flow is a mock state machine with a 2-second artificial delay. The app does not use Android Bluetooth APIs and does not communicate with a real BrainBit Headband. No `BLUETOOTH_SCAN` or `BLUETOOTH_CONNECT` permissions are declared in the manifest. Integration with the BrainBit SDK over BLE is planned for a future sprint.

### EEG data is pre-recorded demo data
The Results tab reads from a static `demo_sessions.json` file bundled in `assets/`. There is no live data pipeline from the headband. The three demo sessions demonstrate the three scoring scenarios (stable focus, distracted, poor signal).

### Pre-session survey responses are not persisted
The pre-session survey dialog captures sleep, caffeine, mood, stress, location, and environment fields, but none of these values are read or saved. Tapping **Start Reading** dismisses the dialog and creates a bare session record with no survey data attached.

### Session detail view uses hardcoded data
The Session Detail Fragment displays metadata via hard-coded `if/else` logic keyed on session ID rather than reading from the database. Only `demo_stable_01` and `demo_distracted_01` have specific mock values; all other session IDs fall through to a default neutral scenario.

### Passwords are stored in plain text
User passwords are written directly to the local SQLite database without hashing. This must be replaced with a secure hashing scheme (e.g., bcrypt) before any deployment involving real users.

### Database operations run on the main thread
All SQLite queries are executed synchronously on the UI thread. This should be migrated to background threads (e.g., `ExecutorService` or Room with LiveData) before handling larger datasets.

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
