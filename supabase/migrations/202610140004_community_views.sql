-- Public community surfaces intentionally expose only identity, country,
-- collection totals, and basic chapa display fields.
create or replace view public.community_profiles as
select
    p.id,
    p.username,
    p.country,
    count(c.id)::bigint as chapa_count
from public.profiles p
left join public.chapas c on c.user_id = p.id
group by p.id, p.username, p.country;

create or replace view public.community_chapas as
select
    c.id,
    c.user_id,
    p.username,
    p.country,
    c.nombre,
    c.pais,
    c."imagePath" as image_path,
    c."estadoPercent" as estado_percent,
    c.etiqueta,
    count(*) over (partition by c.user_id)::bigint as chapa_count
from public.chapas c
join public.profiles p on p.id = c.user_id;

grant select on public.community_profiles to authenticated;
grant select on public.community_chapas to authenticated;

create index if not exists chapas_user_id_community_idx on public.chapas (user_id);
