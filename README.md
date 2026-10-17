# Dawuro

**One message, everyone hears.**

Dawuro is the Twi word for the gong-gong the town crier beats to announce news to the whole
community. The app does the same thing by SMS: write one message and send it to your church,
association, customers or family at once.

Built for everyday Ghanaian users: plain language, large tap targets, works on budget phones,
and understands how Ghanaian numbers are written (`024…`, `+233…`, `233…`, and numbers that
lost their leading zero in Excel).

## Features

- **Contacts**: pick from the phonebook, type a number, import a CSV (from Excel or Google
  Sheets), or paste a list from WhatsApp. Duplicates are skipped, and you preview the import
  before anything is saved. Groups (Choir, Youth, Customers…) let you message part of your list.
- **Write once, personalise for everyone**: "Add their name" greets each person by name.
  Templates cover meetings, church, dues, funerals, invitations, shop promotions, elections and
  festive greetings.
- **Honest SMS counter**: shows how many SMS each person will cost, warns when special letters
  (ɛ, ɔ, emoji) shrink each SMS to 70 characters, and silently fixes curly quotes that would
  double the cost.
- **Three ways to send**
  - *My SIM card*: uses your airtime or bundle; best for up to about 30 people.
  - *SMS Online GH*: for big groups, with your approved sender name (e.g. `GRACECHAPEL`).
  - *My Messages app*: opens your normal SMS app with everyone filled in.
- **Delivery tracking**: live progress per message (delivered / sent / not sent), the reason
  each failure happened in plain words, and one tap to **retry only the ones that failed**.

## SMS Online GH setup

Users can paste their API key in **Settings → SMS Online GH**. It is stored on the device and
kept out of cloud backups. For a build with a key baked in, set `SMSONLINEGH_API_KEY` in `.env`
(see `.env.example`); a key entered in the app takes priority.

## Building

Requires JDK 17+ and the Android SDK (compile SDK 36.1). Robolectric tests for SDK 36 need
**JDK 21+** to run.

```
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

The repository has no Gradle wrapper script. Use a local Gradle 9.3.1, or let Android Studio
or AI Studio generate one.

## Distribution note

Google Play only allows the `SEND_SMS` permission for the default SMS app or approved
exceptions. Publishing on Play may require removing "My SIM card" mode or applying for an
exception. Sideloaded and AI Studio builds are not affected.
