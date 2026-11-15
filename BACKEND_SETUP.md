# Party HMO backend setup

## Supabase database and authentication
The Android app uses Supabase Auth for email/password registration, sign-in, session refresh, and sign-out. Profile records and voice-room metadata are stored in Supabase Postgres and protected by Row Level Security.

The active project schema includes `profiles`, `rooms`, `room_members`, `chat_messages`, wallets, gifts, families, agencies, and related tables. The Android Agora voice flow uses `rooms.agora_channel` as the stable Agora channel name.

The migration `supabase/migrations/20261115000000_add_agora_channel_to_rooms.sql` adds that field and a unique partial index. It has already been applied to the connected Supabase project.

## GitHub Actions secrets
Add these repository secrets for APK builds:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `AGORA_APP_ID`

## Agora token security (required)
The deployed Supabase Edge Function `agora-token` validates the caller's Supabase JWT and verifies that the requested channel belongs to an active Supabase room before issuing a short-lived RTC publisher token.

Set the Agora App Certificate as a Supabase Edge Function secret. Do not put it in GitHub, Android Gradle properties, app resources, or the APK.

1. Open Supabase Dashboard -> Edge Functions -> Secrets:
   https://supabase.com/dashboard/project/pkqcxcfuelubonlkjgoq/functions/secrets
2. Add a secret named `AGORA_APP_CERTIFICATE`.
3. Paste the App Certificate from the Agora Console as its value and save.

Until this secret exists, the token function deliberately returns HTTP 503 and the Android app will not join the voice channel. The App ID is public and is supplied by the Android client; the App Certificate remains server-only.

## Current integration boundary
The Android application module is configured to build without the Tencent UI/IM modules. Email authentication, profile editing, logout, and the active voice-room entry point use Supabase; voice media uses Agora RTC. Old Tencent source directories remain in the repository but are not included by `Android/settings.gradle`.
