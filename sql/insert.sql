insert into request_type
values (1, 'VISIT'),
       (2, 'WARNING'),
       (3, 'NEUTRALIZATION'),
       (4, 'DEPORTATION');

insert into request_status
values (1, 'PENDING'),
       (2, 'APPROVED'),
       (3, 'REJECTED');

insert into alien_status
values (1, 'NOT ON EARTH'),
       (2, 'ON EARTH'),
       (3, 'DEPORTED'),
       (4, 'NEUTRALIZED');

insert into role
values (1, 'AGENT'),
       (2, 'ALIEN');



CREATE OR REPLACE FUNCTION generate_user(UserNum int) RETURNS int[] AS
$$
declare
    generated_users_ids int[];
    curr_user_id        bigint;
begin
    select last_value from user_id_seq into curr_user_id;
    if curr_user_id = 1 then
        perform nextval('user_id_seq');
    end if;
    with ins as (
        insert into "user" (USERNAME, PASSW_HASH, USER_PHOTO)
            select md5(cast(currval('user_id_seq') as text)), md5(i::text), null
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_users_ids
    from ins;
    return generated_users_ids;
end;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_user_roles(RoleStr varchar(32), UsersIds int[]) RETURNS VOID AS
$$
declare
    role int := (select id
                 from role
                 where name = RoleStr);
begin
    for i in 1..array_length(UsersIds, 1)
        loop
            insert into user_roles(user_id, role_id) values (UsersIds[i], role);
        end loop;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_aliens_and_agents(AliensNum int, AgentsNum int) RETURNS VOID AS
$$
begin
    perform generate_user_roles('ALIEN', generate_user($1));
    perform generate_user_roles('AGENT', generate_user($2));
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_planets(PlanetNum int) RETURNS int[] AS
$$
declare
    generated_planets_ids int[];
begin
    perform nextval('planet_id_seq');
    with ins as (
        insert into planet (name, race)
            select md5(cast(currval('planet_id_seq') as text)), md5(i::text)
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_planets_ids
    from ins;
    return generated_planets_ids;
end;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION generate_skills(SkillNum int) RETURNS int[] AS
$$
declare
    generated_skills_ids int[];
begin
    perform nextval('skill_id_seq');
    with ins as (
        insert into skill (name)
            select md5(cast(currval('skill_id_seq') as text))
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_skills_ids
    from ins;
    return generated_skills_ids;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_professions(ProfessionsNum int) RETURNS int[] AS
$$
declare
    generated_professions_ids int[];
begin
    perform nextval('profession_id_seq');
    with ins as (
        insert into profession (name)
            select md5(cast(currval('profession_id_seq') as text))
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_professions_ids
    from ins;
    return generated_professions_ids;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_skill_in_profession(SkillsIds int[], ProfessionsIds int[]) RETURNS VOID AS
$$
declare
    skills_num      int;
    professions_num int;
begin
    professions_num = array_length(ProfessionsIds, 1);
    select array_length(SkillsIds, 1) into skills_num;

    -- каждую профессию связываем с i скиллами
    for i in 1..professions_num
        loop
            for j in 1..i % skills_num
                loop
                    insert into skill_in_profession(profession_id, skill_id)
                    values (ProfessionsIds[i], SkillsIds[j]);
                end loop;
        end loop;
end;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_skills_and_professions(SkillsNum int, ProfessionsNum int) RETURNS VOID AS
$$
begin
    perform generate_skill_in_profession(generate_skills(SkillsNum), generate_professions(ProfessionsNum));
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_locations(LocationsNnum int) RETURNS int[] AS
$$
declare
    generated_ids int[];
begin
    perform nextval('location_id_seq');
    with ins as (
        insert into location (city, country)
            select md5(cast(currval('location_id_seq') as text)), md5(cast(currval('location_id_seq') as text))
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_ids
    from ins;
    return generated_ids;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION
    generate_alien_personality(PersonalityNum int)
    RETURNS int[] AS
$$
declare
    generated_ids   int[];
    professions_ids int[] := ARRAY(select id
                                   from profession);
    locations_ids   int[] := ARRAY(select id
                                   from location);
begin
    with ins as (
        insert into alien_personality (first_name, second_name,
                                       age, profession_id, location_id, person_photo)
            select 'Ivan',
                   'Ivanov',
                   25,
                   professions_ids[1 + floor(random() * array_length(professions_ids, 1))::int],
                   locations_ids[1 + floor(random() * array_length(locations_ids, 1))::int],
                   E'\\xDEADBEEF'
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_ids
    from ins;
    return generated_ids;
end;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_alien_form(AlienFormsNum int) RETURNS int[] AS
$$
declare
    generated_ids int[];
    planets_ids   int[] := ARRAY(select id
                                 from planet);
    user_ids      int[] := ARRAY(select user_id
                                 from get_all_users_with_role('ALIEN'));
begin
    with ins as (
        insert into alien_form (user_id, planet_id, visit_purpose, stay_time, comment)
            select user_ids[1 + floor(random() * array_length(user_ids, 1))::int],
                   planets_ids[1 + floor(random() * array_length(planets_ids, 1))::int],
                   md5(i::text),
                   floor(random() * 500 + 1)::int,
                   md5(md5(i::text))
            from generate_series(1, $1) s(i) returning id
    )
    select array_agg(id)
    into generated_ids
    from ins;
    return generated_ids;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_skill_in_alien_form(AlienFormsIds int[])
    RETURNS VOID AS
$$
declare
    skills_ids int[] := ARRAY(select id
                              from skill);
    skills_num int   := array_length(skills_ids, 1);
    forms_num  int   := array_length(AlienFormsIds, 1);
begin
    for i in 1..forms_num
        loop
            for j in 1..(i % skills_num + 1)
                loop
                    insert into skill_in_alien_form(alien_form_id, skill_id)
                    values (AlienFormsIds[i], skills_ids[j]);
                end loop;
        end loop;
end;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_alien_forms_connect_skills(AlienForms int)
    RETURNS VOID AS
$$
begin
    perform generate_skill_in_alien_form(generate_alien_form($1));
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_agent_info() RETURNS VOID AS
$$
declare
    agents_ids   int[]       := ARRAY(select user_id
                                      from get_all_users_with_role('AGENT'));
    agents       int         := array_length(agents_ids, 1);
    agents_nicks char[]      = '{A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z}';
    c            int         = 1;
    curr_letter  int         = 1;
    last_nick    varchar(64) = 'A';
begin
    -- для каждого агента
    for i in 1..agents
        loop
            -- если у прошлого ника была буква Z значит надо добавить одну букву и начинать с А
            if RIGHT(last_nick, 1) = 'Z' then
                c := c + 1;
                curr_letter := 1;
                -- берем букву и если это А тогда добавляем ее справа
                last_nick := last_nick || agents_nicks[curr_letter];
            else
                -- если не А - заменяем последнюю
                last_nick := overlay(last_nick placing agents_nicks[curr_letter] from c);
            end if;

            curr_letter := curr_letter + 1;

            insert into agent_info(user_id, nickname, is_alive)
            values (agents_ids[i], last_nick, true),
                   (agents_ids[i], last_nick, false); -- умершие агенты
        end loop;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_request()
    RETURNS VOID AS
$$
declare
    temp              int   := 1;
    aliens_ids        int[] := ARRAY(select user_id
                                     from get_all_users_with_role('ALIEN'));
    agents_ids        int[] := ARRAY(select user_id
                                     from get_all_users_with_role('AGENT'));
    alien_forms_ids   int[] := ARRAY(select id
                                     from alien_form
                                     where user_id = any (aliens_ids));
    visit_id          int   := (select id
                                from request_type
                                where name = 'VISIT');
    warning_id        int   := (select id
                                from request_type
                                where name = 'WARNING');
    neutralization_id int   := (select id
                                from request_type
                                where name = 'NEUTRALIZATION');
    deportation_id    int   := (select id
                                from request_type
                                where name = 'DEPORTATION');
    not_on_earth      int   := (select id
                                from alien_status
                                where name = 'NOT ON EARTH');
    on_earth          int   := (select id
                                from alien_status
                                where name = 'ON EARTH');
    forms             int   := array_length(alien_forms_ids, 1);
begin
    for i in 1..forms
        loop
            if (i % 4 = 0) then -- VISIT
                insert into request(creator_id, executor_id, type_id, status_id, alien_form_id)
                values (aliens_ids[i], null, visit_id, not_on_earth, alien_forms_ids[(temp % forms) + 1]);

            elsif (i % 4 = 1) then -- WARNING
                insert into request(creator_id, executor_id, type_id, status_id, alien_form_id)
                values (agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        warning_id, not_on_earth, alien_forms_ids[(temp % forms) + 1]);
            elsif (i % 4 = 2) then --neutralization
                insert into request(creator_id, executor_id, type_id, status_id, alien_form_id)
                values (agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        neutralization_id, on_earth, alien_forms_ids[(temp % forms) + 1]);
            elsif (i % 4 = 3) then
                insert into request(creator_id, executor_id, type_id, status_id, alien_form_id)
                values (agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        agents_ids[1 + floor(random() * array_length(agents_ids, 1))::int],
                        deportation_id, on_earth, alien_forms_ids[(temp % forms) + 1]);
            end if;
            temp := temp + 1;
        end loop;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_warning(WarningNumber int)
    RETURNS VOID AS
$$
declare
    aliens_ids int[] := ARRAY(select id
                              from alien_info);
begin
    insert into warning(alien_id, name, description)
    select aliens_ids[1 + floor(random() * array_length(aliens_ids, 1))::int],
           md5(i::text),
           md5(i::text)
    from generate_series(1, $1) s(i);
end;
$$ LANGUAGE plpgsql;

select *
from alien_info;

CREATE OR REPLACE FUNCTION generate_alien_info() RETURNS VOID AS
$$
declare
    aliens_ids        int[] := ARRAY(select user_id
                                     from get_all_users_with_role('ALIEN'));
    aliens            int   := array_length(aliens_ids, 1);
    alien_status_ids  int[] := ARRAY(select id
                                     from alien_status);
    personalities_ids int[] := ARRAY(select id
                                     from alien_personality);
    temp              int   := 1;
begin

    for i in 1..aliens
        loop
            insert into alien_info(departure_date, alien_status_id, user_id, personality_id)
            values (null, alien_status_ids[1 + floor(random() * array_length(alien_status_ids, 1))::int],
                    aliens_ids[i], personalities_ids[temp]);
            temp := temp + 1;
            if temp > (array_length(personalities_ids, 1)) then
                EXIT;
            end if;
        end loop;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_agent_alien() RETURNS VOID AS
$$
declare
    agent_info_ids  int[] := ARRAY(select id
                                   from agent_info);
    aliens_info_ids int[] := ARRAY(select id
                                   from alien_info);
begin

    for i in 1..array_length(agent_info_ids, 1)
        loop
            for j in 1..i % array_length(aliens_info_ids, 1)
                loop
                    insert into agent_alien(alien_info_id, agent_info_id, start_date, end_date)
                    values (aliens_info_ids[i], agent_info_ids[j], (current_date - INTERVAL '2 YEAR')::date,
                            (current_date + INTERVAL '2 YEAR')::date);
                end loop;
        end loop;
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_tracking_report(DaysOfReporting int) RETURNS VOID AS
$$
declare
    agent_aliens_ids int[] := ARRAY(select id
                                    from agent_alien);
    curr_date        date  := current_date;
begin

    for i in 1..$1
        loop
            for j in 1..(1 + floor(1 + random() * 100))
                loop
                    insert into tracking_report(report_date, behavior, description, agent_alien_id)
                    values (curr_date,
                            floor(random() * 10)::int, md5(i::text),
                            agent_aliens_ids[floor(random() * array_length(agent_aliens_ids, 1))::int]);
                end loop;
            curr_date := (curr_date - INTERVAL '1 DAY')::date;
        end loop;
end;
$$ LANGUAGE plpgsql;
select * from tracking_report;
select * from agent_alien;
select generate_aliens_and_agents(1000, 100);
select generate_planets(20);
select generate_skills_and_professions(500, 100);
select generate_locations(100);
select generate_alien_personality(1000);
select generate_agent_info();
select generate_alien_forms_connect_skills(1000);
select generate_request();
select generate_alien_info();
select generate_warning(2000);
select generate_agent_alien();
select generate_tracking_report(365);
