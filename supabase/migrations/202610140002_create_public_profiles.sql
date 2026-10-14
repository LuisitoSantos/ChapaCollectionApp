-- Public, searchable identity for each authenticated user.
create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    username text not null,
    created_at timestamptz not null default now(),
    constraint profiles_username_format check (username ~ '^[a-z0-9_]{3,20}$')
);

-- Case-insensitive uniqueness protects against collisions from other clients too.
create unique index if not exists profiles_username_lower_unique
    on public.profiles (lower(username));

alter table public.profiles enable row level security;

drop policy if exists "Profiles are publicly readable" on public.profiles;
create policy "Profiles are publicly readable"
    on public.profiles for select
    using (true);

drop policy if exists "Users can create their own profile" on public.profiles;
create policy "Users can create their own profile"
    on public.profiles for insert
    to authenticated
    with check (auth.uid() = id);

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
    on public.profiles for update
    to authenticated
    using (auth.uid() = id)
    with check (auth.uid() = id);

grant select on public.profiles to anon, authenticated;
grant insert, update on public.profiles to authenticated;
