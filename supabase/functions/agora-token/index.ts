import { createClient } from "npm:@supabase/supabase-js@2";
import { RtcRole, RtcTokenBuilder } from "npm:agora-token@2.0.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);
  try {
    const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\\s+/i, "");
    if (!jwt) return json({ error: "Authentication required" }, 401);
    const appCertificate = Deno.env.get("AGORA_APP_CERTIFICATE");
    if (!appCertificate) return json({ error: "Agora certificate is not configured on the server" }, 503);
    const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error } = await supabase.auth.getUser(jwt);
    if (error || !user) return json({ error: "Invalid or expired Supabase session" }, 401);
    const payload = await req.json();
    const appId = String(payload.appId ?? "");
    const channel = String(payload.channel ?? "");
    const uid = Number(payload.uid);
    if (!appId || !channel || channel.length > 64 || !Number.isInteger(uid) || uid < 1 || uid > 2147483647) {
      return json({ error: "Invalid appId, channel, or uid" }, 400);
    }
    const expiresAt = Math.floor(Date.now() / 1000) + 3600;
    const rtcToken = RtcTokenBuilder.buildTokenWithUid(
      appId, appCertificate, channel, uid, RtcRole.PUBLISHER, expiresAt,
    );
    return json({ rtcToken, uid, expiresAt });
  } catch (error) {
    console.error("agora-token error", error);
    return json({ error: "Unable to generate Agora token" }, 500);
  }
});
