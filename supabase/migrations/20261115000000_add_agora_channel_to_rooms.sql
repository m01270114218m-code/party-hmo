-- Add a stable Agora channel identifier to each Supabase voice room.
alter table public.rooms add column if not exists agora_channel text;
create unique index if not exists rooms_agora_channel_unique
    on public.rooms (agora_channel)
    where agora_channel is not null;
