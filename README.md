# appfunctions-koog-agent-demo

A demo showing a [Koog](https://github.com/JetBrains/koog) AI agent discovering and calling
Android [AppFunctions](https://developer.android.com/ai/appfunctions) it knows nothing about at
build time.

## What this demonstrates

> The agent discovers AppFunctions **at runtime**, converts their metadata into Koog tools on the
> fly, and lets an LLM (Gemini) decide which one to call — without the agent app ever having seen
> those functions at compile time.

The punchline: add a new `@AppFunction` to the `tool` app, reinstall only `tool`, and the `agent`
app — untouched, not rebuilt, not even restarted — can call it on the next prompt.

## Structure

This repo has two **independent Gradle projects** (not a multi-module build — that's intentional:
the agent genuinely doesn't know about the tool app at build time).

```
tool/    Publishes a few AppFunctions (a calculator-ish set of functions).
agent/   Discovers whatever AppFunctions are installed on the device, converts their metadata
         into Koog tool descriptors, and runs a Gemini-backed Koog agent against them.
```

Key files:

- `tool/app/src/main/java/.../CalculatorFunctions.kt` — the `@AppFunction` declarations
  (`add`, `calculateTotal`, `convertCurrency`), inside an `AppFunctionService` per the
  `androidx.appfunctions` 1.0.0-alpha10+ API.
- `agent/app/src/main/java/.../AppFunctionDiscovery.kt` — subscribes to
  `AppFunctionManager.observeAppFunctions()`, re-querying whenever any app's metadata changes.
- `agent/app/src/main/java/.../AppFunctionSchemaConverter.kt` — converts
  `AppFunctionMetadata` into a Koog `ToolDescriptor` (the metadata → schema mapping).
- `agent/app/src/main/java/.../AppFunctionTool.kt` — a generic `Tool<JSONObject, String>` that
  converts JSON arguments into `AppFunctionData`, calls `executeAppFunction`, and converts the
  result back to JSON. One instance of this class is created per discovered function — no
  per-function code on the agent side.
- `agent/app/src/main/java/.../AgentRunner.kt` — builds a `ToolRegistry` from the discovered
  functions and runs a single-turn Koog `AIAgent` against Gemini.

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

### 2. Build and install `tool`

```sh
cd tool
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify the functions are published:

```sh
adb shell cmd app_function list-app-functions dev.hiroaki404.appfunctions.koogdemo.tool
```

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

1. Launch `agent`. Type a prompt and hit Send — e.g. "what is 123 plus 456" or a nested-argument
   prompt like "3 apples at 100 yen and 2 pens at 50 yen with 10 percent discount, what's the
   total?". While the agent runs, each Koog tool call appears in the UI with its function name,
   arguments, status, and result (or error).
2. **The punchline**: with `agent` still running (not rebuilt, not restarted), add a new
   `@AppFunction` to `tool`, rebuild just `tool`, and `adb install -r` it. Ask the agent something
   that only the new function can answer — it can call it on the very next prompt, because
   `agent` re-discovers AppFunctions on every request instead of hardcoding a tool list.

   This repo's `convertCurrency(amount, from, to)` function (JPY/USD/EUR, fixed demo exchange
   rates so the answer obviously came from the tool and not the LLM's own knowledge) is exactly
   this kind of function — try asking "convert 100 USD to JPY" before and after installing it.

### Verifying a tool call

For an easy-to-identify end-to-end check, ask the agent:

```text
convert 10 USD to JPY
```

The UI should show a successful `convertCurrency` tool call with `amount=10`, `from=USD`,
`to=JPY`, and a result of `1000.0`. The fixed demo rate makes this distinguishable from a result
based on current exchange-rate knowledge.

The `tool` app also logs the call independently. In another terminal, run:

```sh
adb logcat -s AppFunctionToolApp KoogAgent
```

Look for `Calling AppFunction: convertCurrency` followed by `AppFunction succeeded` and
`returnValue=1000.0`. Seeing both the agent UI card and the `tool` process log confirms that Koog
selected the runtime-discovered tool and Android executed the AppFunction in the provider app.

## Known issues

- **A specific phrase can make the LLM return an empty response.** With all three `tool`
  functions installed, asking the agent literally "add 123 and 456" deterministically triggers a
  Koog `AIAgentStuckInTheNodeException`: Gemini returns an empty response (`finishReason=STOP`,
  no text, no tool call), which the agent's single-turn graph has no edge to handle. Rewording the
  question (e.g. "what is 123 plus 456") works fine, as does the same literal phrase with fewer
  tools registered. Root cause not identified; treat it as a known quirk of this Gemini model /
  tool-set combination rather than a bug in the metadata → tool conversion.

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
