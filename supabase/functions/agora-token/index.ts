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

function stableUid(userId: string): number {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) hash = (Math.imul(31, hash) + userId.charCodeAt(i)) | 0;
  return (hash & 0x7fffffff) || 1;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);
  try {
    const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
    if (!jwt) return json({ error: "Authentication required" }, 401);
    const appCertificate = Deno.env.get("AGORA_APP_CERTIFICATE");
    if (!appCertificate) return json({ error: "Agora certificate is not configured on the server" }, 503);

    const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error: authError } = await supabase.auth.getUser(jwt);
    if (authError || !user) return json({ error: "Invalid or expired Supabase session" }, 401);

    const payload = await req.json();
    const appId = String(payload.appId ?? "");
    const channel = String(payload.channel ?? "");
    const uid = stableUid(user.id);
    if (!appId || !channel || channel.length > 64 || Number(payload.uid) !== uid) {
      return json({ error: "Invalid appId, channel, or user id" }, 400);
    }

    const { data: room, error: roomError } = await supabase
      .from("rooms")
      .select("id")
      .eq("agora_channel", channel)
      .eq("status", "active")
      .maybeSingle();
    if (roomError || !room) return json({ error: "Room not found or not accessible" }, 403);

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
