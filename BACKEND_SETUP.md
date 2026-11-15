# Party HMO backend setup

## Supabase
1. Open the Supabase Dashboard for your project.
2. Open **SQL Editor** and run `supabase/schema.sql`.
3. In **Authentication -> Providers -> Email**, enable Email.
4. For testing, either confirm new users by email or disable email confirmation.

## GitHub Actions secrets
Add these repository secrets:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `AGORA_APP_ID`

Never commit a Supabase service-role key or an Agora App Certificate.

## Agora token security
The Android client currently contains the RTC join flow but does not contain an Agora App Certificate. For production with token authentication enabled, deploy a trusted backend/Edge Function that generates short-lived RTC tokens and return only the token to authenticated users.

The App Certificate must stay on the server and must never be committed to this repository or shipped in the APK.
