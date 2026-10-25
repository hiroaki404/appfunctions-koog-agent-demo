# appfunctions-koog-agent-demo

A demo showing a [Koog](https://github.com/JetBrains/koog) AI agent discovering and calling
Android [AppFunctions](https://developer.android.com/ai/appfunctions) it knows nothing about at
build time.

## What this demonstrates

> The agent discovers AppFunctions **at runtime**, converts their metadata into Koog tools on the
> fly, and lets an LLM (Gemini) decide which one to call — without the agent app ever having seen
> those functions at compile time.

The scenario: a travel app (**kototabi**) and a messaging app each publish AppFunctions. The agent
looks up your past trips in kototabi, plans and saves a new itinerary, books a flight, and asks a
friend over the messaging app — calling functions across both apps.

The punchline: with the messaging app **not** installed, the agent says it can't send a message.
Install just that app, ask again, and the `agent` app — untouched, not rebuilt, not even
restarted — calls its function on the next prompt.

## Structure

This repo has two **independent Gradle projects** (that's intentional: the agent genuinely doesn't
know about the tool apps at build time).

```
tool/       One Gradle project with two independent app modules that publish AppFunctions:
  kototabi/   Travel app: getTravelInfo, saveItinerary, searchItineraries, bookFlight
              (itineraries and bookings are stored with Room).
  message/    Messaging app: sendMessage (messages are only recorded and shown in the app).
agent/      Discovers whatever AppFunctions are installed on the device, converts their metadata
            into Koog tool descriptors, and runs a Gemini-backed Koog agent against them.
```

Key files:

- `tool/kototabi/src/main/java/.../KototabiFunctions.kt` and
  `tool/message/src/main/java/.../MessageFunctions.kt` — the `@AppFunction` declarations, each
  inside an `AppFunctionService` per the `androidx.appfunctions` 1.0.0-alpha10+ API.
  `KototabiFunctions.kt` uses nested `@AppFunctionSerializable` types
  (`Itinerary` → `ItineraryDay` → `Spot`) as both arguments and return values.
- `agent/app/src/main/java/.../AppFunctionDiscovery.kt` — subscribes to
  `AppFunctionManager.observeAppFunctions()`, re-querying whenever any app's metadata changes.
- `agent/app/src/main/java/.../AppFunctionSchemaConverter.kt` — converts
  `AppFunctionMetadata` into a Koog `ToolDescriptor` (the metadata → schema mapping).
- `agent/app/src/main/java/.../AppFunctionTool.kt` — a generic `Tool<JSONObject, String>` that
  converts JSON arguments into `AppFunctionData`, calls `executeAppFunction`, and converts the
  result back to JSON. One instance of this class is created per discovered function — no
  per-function code on the agent side.
- `agent/app/src/main/java/.../AgentRunner.kt` — builds a `ToolRegistry` from the discovered
  functions and runs one turn of a Koog `AIAgent` against Gemini 3.5 Flash-Lite. A new agent is
  created for every message; Koog's `ChatMemory` feature, keyed by a session id held in
  `AgentViewModel`, carries the conversation across messages.

## Requirements

- Android SDK with API 37, min SDK 36 (Android 16+).
- A **rooted** emulator or device. `agent` needs the `EXECUTE_APP_FUNCTIONS` permission, which
  (outside of the official AppFunctions Testing Agent's shell-identity trick) requires installing
  it as a priv-app — see [Granting the agent permission](#granting-the-agent-permission) below.
- A [Gemini API key](https://aistudio.google.com/apikey).

## Setup

### 1. Gemini API key

Create `agent/local.properties` (gitignored) with:

```properties
GEMINI_API_KEY=your-api-key-here
```

### 2. Build and install the tool apps

```sh
cd tool
./gradlew :kototabi:assembleDebug :message:assembleDebug
adb install -r kototabi/build/outputs/apk/debug/kototabi-debug.apk
adb install -r message/build/outputs/apk/debug/message-debug.apk
```

Verify the functions are published (the command lists every app's functions; look for
`BaseKototabiAppFunctionService#...` and `BaseMessageAppFunctionService#sendMessage`):

```sh
adb shell cmd app_function list-app-functions | grep koogdemo
```

kototabi seeds a past trip ("2025年夏の北海道") when its database is first created. Its screen has debug buttons to
insert the upcoming December trip and to reset to the initial state.

### 3. Build `agent`

```sh
cd agent
./gradlew assembleDebug
```

Don't `adb install` it yet — see the next section first. A normal `adb install` (or Android
Studio's Run/Debug) will **not** grant `EXECUTE_APP_FUNCTIONS`; the app needs to be placed as a
priv-app once, after which subsequent `adb install -r` updates work normally.

### Granting the agent permission

This demo uses the **priv-app** approach: install the agent APK under `/system/priv-app/` with an
allowlist that grants it `EXECUTE_APP_FUNCTIONS`, so ordinary Run/Debug works afterward. (The
alternative — the official Testing Agent's shell-identity approach, which borrows the `shell`
user's permission via a long-running `adb shell am instrument` process — doesn't fit a normal
Run/Debug workflow, so this repo doesn't use it.)

```sh
# 1. Start the emulator with a writable system partition (the emulator command directly,
#    not Android Studio's Run button or `android emulator start`).
$ANDROID_SDK_ROOT/emulator/emulator -avd <your-rooted-avd-name> -writable-system

# 2. Confirm that the expected emulator is connected and has completed booting.
#    `adb devices` must list `emulator-5554` with the state `device`, and this command
#    must print `1`.
adb devices
adb -s emulator-5554 shell getprop sys.boot_completed

# 3. Root and remount. `adb root` must report either "adbd is restarting as root" or
#    "adbd is already running as root". If the first remount just prints a message
#    asking you to reboot, reboot once and repeat this step — the successful remount
#    must print "Remount succeeded".
adb root
adb wait-for-device
adb remount

# 4. Push the permission allowlist (already checked into this repo) and the agent APK.
#    If the package was already installed normally, uninstall it first — priv-app placement
#    conflicts with a regular install.
adb uninstall dev.hiroaki404.appfunctions.koogdemo.agent 2>/dev/null
adb push agent/privapp-permissions-koogdemo-agent.xml /system/etc/permissions/
adb push agent/app/build/outputs/apk/debug/app-debug.apk /system/priv-app/koogdemo-agent.apk
adb shell chmod 644 /system/etc/permissions/privapp-permissions-koogdemo-agent.xml
adb reboot

# 4. After reboot, confirm the permission was granted:
adb shell dumpsys package dev.hiroaki404.appfunctions.koogdemo.agent | grep -E "codePath|EXECUTE_APP_FUNCTIONS|PRIVILEGED"
# Expect: codePath=/system/priv-app/koogdemo-agent.apk, PRIVILEGED in privateFlags,
# and "android.permission.EXECUTE_APP_FUNCTIONS: granted=true"
```

From then on, `./gradlew installDebug` / Android Studio's Run button work as usual for `agent`, as
long as the app's declared permissions don't change (a permission change needs the priv-app push
redone).

## Running the demo

Send the messages below in one conversation. While the agent runs, each Koog tool call appears in
the UI with its function name, arguments, status, and result (or error); kototabi and the
messaging app update their screens as soon as a function writes to them.

| # | Message | Expected tool calls |
|---|---|---|
| 1 | 12月に北海道旅行を計画しています。去年の北海道旅行で行かなかった場所を中心にプランを考えて、旅程の計画を作ってください | `searchItineraries` → `getTravelInfo` → `saveItinerary` |
| 2 | ではこの旅程で航空券を取っておいて | `bookFlight` |
| 3 | では12月5日〜6日のこの旅程で、メッセージアプリで友達のトムくんに予定が空いているか聞いておいて | `sendMessage` |

English prompts work too (`adb shell input text` can't type Japanese), e.g. "Plan a December trip
to Hokkaido. Check my Hokkaido trip from last year, avoid places I already visited, and save the
new itinerary in the travel app.", "Then book a flight from Tokyo for this itinerary.", and "Using
the message app, ask my friend Tom if he is free for this trip."

**The punchline**: uninstall the messaging app (`adb uninstall dev.hiroaki404.appfunctions.koogdemo.message`)
and send message 3 — the agent answers that it can't send messages. With `agent` still running
(not rebuilt, not restarted), `adb install` the messaging app and send the same message again in
the same conversation: the agent calls `sendMessage`, because it re-discovers AppFunctions on
every request instead of hardcoding a tool list.

To watch the calls from the provider apps' side, run in another terminal:

```sh
adb logcat -s AppFunctionToolApp KoogAgent
```

Each call logs `Calling AppFunction: <name> args={...}` followed by `AppFunction succeeded` and its
`returnValue`. Seeing both the agent UI card and the provider app's log confirms that Koog selected
the runtime-discovered tool and Android executed the AppFunction in the provider app.

## Known issues

- **Gemini 2.5 Flash writes tool calls as text.** With 2.5 Flash, the agent sometimes stopped
  after the first tool call because the model replied with a Python-like
  `_tool_code print(default_api....)` string instead of a function call, which ends Koog's run.
  The agent therefore uses Gemini 3.5 Flash-Lite. Koog 1.2.0 has no definition for it, so
  `AgentRunner.kt` copies `GoogleModels.Gemini3_5Flash` with the id `gemini-3.5-flash-lite`.
- **Pressing 「新しい会話」 while a request is running** clears the screen and starts a new session
  immediately, but sending stays disabled until the running request finishes; its result is
  discarded.

## References

This repo's code is original — nothing below is copied — but its design leaned on reading these
projects:

- [FilipFan/AppFunctionsPilot](https://github.com/FilipFan/AppFunctionsPilot) — the priv-app
  permission-granting approach used in [Granting the agent permission](#granting-the-agent-permission),
  and an early example of a generic metadata → argument executor.
- [android/appfunctions](https://github.com/android/appfunctions) — the official AppFunctions
  Testing Agent, referenced for its metadata → schema conversion (circular-reference detection,
  function-name sanitizing) and its shell-identity permission approach (not used here, but
  considered).
- [JetBrains/koog](https://github.com/JetBrains/koog), specifically `agents-mcp`'s `McpTool` —
  the design this repo's `AppFunctionTool` (a generic `Tool<JSONObject, TResult>` built from
  runtime-discovered metadata) follows.
