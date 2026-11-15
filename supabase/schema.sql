-- Party HMO Supabase schema note
-- The connected project already has the full application schema
-- (profiles, rooms, room_members, chat_messages, wallets, gifts, families,
-- agencies, follows, notifications, VIP tables, and their RLS policies).
--
-- Do not run the old minimal voice_rooms bootstrap from earlier revisions:
-- the Android app now uses public.rooms and public.room_members.
--
-- For a fresh checkout, apply the tracked migration in
-- supabase/migrations/20261115000000_add_agora_channel_to_rooms.sql
-- after provisioning the Party HMO base schema.

alter table public.rooms add column if not exists agora_channel text;

create unique index if not exists rooms_agora_channel_unique
    on public.rooms (agora_channel)
    where agora_channel is not null;
