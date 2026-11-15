# Agora RTC token function

This function requires an authenticated Supabase user JWT and generates a short-lived Agora RTC publisher token.

Before production use, add the Agora App Certificate as a Supabase Edge Function secret:

- Key: `AGORA_APP_CERTIFICATE`
- Value: the App Certificate from Agora Console

Never place the App Certificate in Android resources, Gradle properties committed to Git, or the APK. The public App ID is sent by the app; the certificate remains server-side.
