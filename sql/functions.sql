
-- функция создать заявку на VISIT
create or replace function create_visit_request(userid int, planet_name varchar(64), planet_race varchar(64), visit_purp varchar(64),
                                                staytime int, comm text)
    returns void
as
$$
declare
    planet int := (select id from planet where planet.name = planet_name and planet.race = planet_race);
    form_id int;
    type int := (select id from request_type where name = 'VISIT');
    status int := (select id from request_status where name = 'PENDING');
begin
    if planet is null or type is null then
        raise 'no such planet % %', planet_name, planet_race;
    end if;

    insert into alien_form(user_id, planet_id, visit_purpose, stay_time, comment)
    values (userid, planet, visit_purp, staytime, comm) returning id into form_id;
    insert into request(creator_id, type_id, status_id, alien_form_id)
    values (userid, type, status, form_id);
end;
$$ language plpgsql;


-- функция заполнения skill_in_alien_form для конкретной анкеты по именам умений
create or replace function insert_skill_in_alien_form(form_id int, skills varchar(32)[])
    returns void
as
$$
declare
    i int;
begin
    for i in select id from skill where name = ANY (skills) loop
            insert into skill_in_alien_form(alien_form_id, skill_id) values (form_id, i);
        end loop;
end;
$$ language plpgsql;


-- функция для регистрации пользователя (с выбором роли)
-- вообще регистрация агента - другая функция. для пришельца этой достаточно
-- т.к. потом все равно отправлять анкету
create or replace function register_user(uname text, password text, alien bool default false)
    returns table (user_id int,
                   username varchar(64),
                   user_photo bytea)
as
$$
declare
    ret_id int;
    role_name text := 'AGENT';
begin
    if alien then role_name := 'ALIEN';
    end if;
    insert into "user" (username, passw_hash) values (uname, password) returning id into ret_id;

    insert into user_roles(user_id, role_id) values (ret_id, (select id from role where name = role_name));
    return query (select u.id, u.username, u.user_photo from "user" u where u.id = ret_id);
end;
$$ language plpgsql;


-- Функция для получения всех профессий, подходящих по навыкам, отсортированные по кол-ву нужных навыков
-- для конкретного пришельца
create or replace function get_professions_by_user_id(u_id int)
    returns table (id integer, name varchar(64))
as
$$
declare
    user_skills integer[] := array(select skill_id from skill_in_alien_form sf
                                    join alien_form f on f.id = sf.alien_form_id
                                    join "user" u on f.user_id = u.id
                                    where u.id = u_id);
    prof_ids integer[] := array(select * from get_professions_by_skills(user_skills));
begin

    return query select * from profession p where p.id = ANY (prof_ids);
end;
$$ language plpgsql;



-- функция для того, чтобы получить наименьший по длине незарегистрированный никнейм
create or replace function get_available_nickname()
    returns varchar(64)
as
$$
declare

    agents_nicks char[] = '{A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z}';
    letters int = array_length(agents_nicks, 1);
    nice_nick boolean := false;
    nick_len int := (select length(nickname) nick_len from agent_info
                 where is_alive = true
                 group by nick_len
                 having count(*) < power(letters, length(nickname))
                 order by nick_len
                 limit 1);
    curr_nick varchar(64) := repeat('A', nick_len);
    nicks varchar(64)[] := ARRAY (select nickname from agent_info where is_alive = true);
begin

    if nick_len = 1 then
        for i in 1..letters loop
            curr_nick := agents_nicks[i];
            if (not (curr_nick = ANY(nicks))) then
                return curr_nick;
            end if;
            end loop;
    end if;
    for i in 1..letters loop
        for j in 1..letters loop
            curr_nick := agents_nicks[i] || agents_nicks[j];
            if (not (curr_nick = ANY(nicks))) then
                return curr_nick;
            end if;
            end loop;
    end loop;
    return curr_nick;
end;
$$ language plpgsql;


drop function register_agent(uname text, password text);
-- функция для регистрации агента
create or replace function register_agent(uname text, password text)
    returns TABLE(user_id integer, username character varying, agent_info_id int, nickname character varying)
    language plpgsql
as
$$
declare
    ret_id int;
    nick varchar(64);
    ag_id int;
begin
    insert into "user" (username, passw_hash) values (uname, password) returning id into ret_id;
    insert into user_roles(user_id, role_id) values (ret_id, (select id from role where name = 'AGENT'));
    nick := (select * from get_available_nickname());
    insert into agent_info(user_id, nickname, is_alive) values (ret_id, nick, true) returning id into ag_id;

    return query select u.id, u.username, ag_id, nick from "user" u where u.id = ret_id;
end;
$$;


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


-- функция возвращает всех пользователей с ролью role
create or replace function get_all_users_with_role(role_name varchar(32))
    returns table
            (id int,
            username varchar(64),
            user_photo bytea)
as
$$
begin
    return query select u.id, u.username, u.user_photo from "user" u
        join user_roles ur on u.id = ur.user_id
        join role r on ur.role_id = r.id where r.name = role_name;
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
