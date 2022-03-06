-- show all aliens requests with request_type VISIT
-- ???


-- функция для регистрации пользователя (с выбором роли)
create or replace function register_user(username text, password text, alien bool default false)
    returns int
as
$$
declare
    ret_id int;
    role_name text := 'AGENT';
begin
    if alien then role_name := 'ALIEN';
    end if;
    insert into "user" (username, passw_hash) values(username, password) returning id into ret_id;

    insert into user_roles(user_id, role_id) values(ret_id, (select id from role where name = role_name));
    return ret_id;
end;
$$ language plpgsql;


-- функция возвращает всех пользователей с ролью role
create or replace function get_user_by_role(role_name varchar(32))
    returns table
            (id int,
            username varchar(64),
            passw_hash varchar(64),
            user_photo bytea)
as
$$
begin
    return query select u.id, u.username, u.passw_hash, u.user_photo from "user" u
        join user_roles ur on u.id = ur.user_id
        join role r on ur.role_id = r.id where r.name = role_name;
end;
$$ language plpgsql;

-- функция для того, чтобы узнать базовую информацию о пришельцах, за которыми следит агент с заданным id
create or replace function get_aliens_by_agent_id_main(agent_id int)
    returns table
            (alien_info_id integer,
            username varchar(64),
            user_photo bytea,
            alien_status varchar(64),
            personality_id int,
            departure_date date
            )
as
$$
begin
    return query select al.id, u.username, u.user_photo, s.name, al.personality_id, al.departure_date
                 from agent_info ag
                 join agent_alien aa on ag.id = aa.agent_info_id
                 join alien_info al on aa.alien_info_id = al.id
                 join alien_status s on al.alien_status_id = s.id
                 join "user" u on u.id = al.user_id
                where ag.id=agent_id and s.name = 'ON EARTH';
end;
$$ language plpgsql;

-- функция для того, чтобы узнать детальную информацию о конкретном пришельце с заданным id,
-- за которым следит агент
create or replace function get_alien_details_by_alien_id(alien_id int)
    returns table
            (
                alien_info_id integer,
                username varchar(64),
                user_photo bytea,
                departure_date date,
                alien_status varchar(64),
                first_name varchar(64),
                second_name varchar(64),
                age int,
                person_photo bytea,
                city varchar(64),
                country varchar(64),
                profession varchar(64)
            )
as
$$
begin
    return query select al.id, u.username, u.user_photo, al.departure_date,
       s.name,
       p.first_name, p.second_name, p.age, p.person_photo,
       l.city, l.country,
       prof.name
            from alien_info al
            join alien_personality p on p.id = al.personality_id
            join location l on l.id = p.location_id
            join "user" u on al.id = u.id
            join alien_status s on al.alien_status_id = s.id
            join profession prof on prof.id = p.profession_id
            where al.id = alien_id and s.name = 'ON EARTH';

end;
$$ language plpgsql;



-- Функция получения всех заявок типа req_type, которые надо обработать
-- данному агенту с id user_id
create or replace function get_requests_by_agent_id(user_id integer, req_type varchar(64))
    returns table
            (
                request_id     integer,
                creator        varchar(64),
                request_type   varchar(64),
                request_status varchar(64),
                create_date    timestamp,
                alien_form_id  integer
            )
as
$$
begin
    return query select r.id, u.username, rt.name, rs.name, r.create_date, r.alien_form_id
                 from request r
                          join "user" u on r.creator_id = u.id
                          join request_type rt on r.type_id = rt.id
                          join request_status rs on r.status_id = rs.id
                 where (executor_id is null) or
                       (r.executor_id = user_id
                        and (rt.name = req_type or req_type is null));
end;
$$ language plpgsql;

-- Функция для получения всех пользователей с ролью role
create or replace function get_all_users_with_role(role varchar(32))
    returns table
            (
                user_id  integer,
                username varchar(64)
            )
as
$$
begin
    return query select ur.user_id, u.username
                 from user_roles ur
                          join role r on ur.role_id = r.id
                          join "user" u on ur.user_id = u.id
                 where r.name = role;
end;
$$ language plpgsql;


-- Функция для получения личной информации пришельца
create or replace function get_alien_info_by_user_id(uid integer)
    returns table
            (
                first_name     varchar(64),
                second_name    varchar(64),
                age            integer,
                profession     varchar(64),
                city           varchar(64),
                country        varchar(64),
                person_photo   bytea,
                departure_date date,
                alien_status   varchar(32)
            )
as
$$
begin
    return query select ap.first_name,
                        ap.second_name,
                        ap.age,
                        p.name,
                        l.city,
                        l.country,
                        ap.person_photo,
                        ai.departure_date,
                        als.name
                 from alien_info ai
                          join alien_personality ap on ai.personality_id = ap.id
                          join profession p on ap.profession_id = p.id
                          join location l on ap.location_id = l.id
                          join alien_status als on ai.alien_status_id = als.id
                 where ai.user_id = uid;
end;
$$ language plpgsql;


-- Функция для получения предупреждений для определенного пришельца
create or replace function get_warnings_by_user_id(uid integer)
    returns table
            (
                name         varchar(64),
                description  text,
                warning_date date
            )
as
$$
begin
    return query select name, description, warning_date
                 from warning
                 where alien_id = (select id from alien_info where user_id = uid);
end;
$$ language plpgsql;


-- Функция для получения всех профессий, подходящих по навыкам, отсортированные по кол-ву нужных навыков
create or replace function get_professions_by_skills(skills integer[])
    returns integer[]
as
$$
declare
    profession_ids integer[] := array[]::integer[];
    prof_row profession%rowtype;
begin
    for prof_row in (select p.id, p.name, count(sip.skill_id) from profession p
                        join skill_in_profession sip on p.id = sip.profession_id group by p.id
                        order by count(sip.skill_id) desc)
        loop
            if array(select skill_id from skill_in_profession where profession_id = prof_row.id) <@ skills then
                profession_ids := profession_ids || prof_row.id;
            end if;
        end loop;
    return profession_ids;
end;
$$ language plpgsql;
