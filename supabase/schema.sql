-- Party HMO / Supabase schema
-- Run this in Supabase Dashboard -> SQL Editor.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  username text,
  avatar_url text,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Users can read profiles"
on public.profiles for select
to authenticated
using (true);

create policy "Users can insert their own profile"
on public.profiles for insert
to authenticated
with check (auth.uid() = id);

create policy "Users can update their own profile"
on public.profiles for update
to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, email)
  values (new.id, new.email)
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_user();

create table if not exists public.voice_rooms (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  channel_name text not null unique,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now()
);

alter table public.voice_rooms enable row level security;

create policy "Authenticated users can read rooms"
on public.voice_rooms for select
to authenticated using (true);

create policy "Authenticated users can create rooms"
on public.voice_rooms for insert
to authenticated
with check (auth.uid() = owner_id);

create policy "Owners can update rooms"
on public.voice_rooms for update
to authenticated
using (auth.uid() = owner_id)
with check (auth.uid() = owner_id);

create policy "Owners can delete rooms"
on public.voice_rooms for delete
to authenticated
using (auth.uid() = owner_id);
