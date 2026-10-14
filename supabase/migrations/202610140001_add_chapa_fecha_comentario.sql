alter table public.chapas
    add column if not exists fecha_obtencion date,
    add column if not exists comentario varchar(500);
