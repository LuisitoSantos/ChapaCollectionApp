alter table public.profiles
    add column if not exists country text;

alter table public.chapas
    add column if not exists etiqueta text not null default 'unknown';

alter table public.chapas
    drop constraint if exists chapas_etiqueta_allowed;

alter table public.chapas
    add constraint chapas_etiqueta_allowed check (
        etiqueta in (
            'unknown', 'Cerveza', 'Refresco', 'Zumo', 'Agua', 'Sidra',
            'Bebida energética', 'Bebida isotónica', 'Té', 'Café', 'Vino', 'Licor', 'Otra bebida'
        )
    );
