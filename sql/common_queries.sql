-- 1 проверка при логине
select * from "user" where username=? and passw_hash=?;

-- 2 ищем информацию о пришельцах, за которыми следит конкретный агент
select al.id, u.username, u.user_photo, s.name, al.personality_id, al.departure_date
from agent_info ag
join agent_alien aa on ag.id = aa.agent_info_id
join alien_info al on aa.alien_info_id = al.id
join alien_status s on al.alien_status_id = s.id
join "user" u on u.id = al.user_id
where ag.id=? and s.name = 'ON EARTH';

-- 3 ищем информацию о конкретном пришельце, зная его айди
select u.username, u.user_photo,
       s.name,
       p.first_name, p.second_name, p.age, p.person_photo,
       l.city, l.country,
       prof.name as profession_name
from alien_info al
join alien_personality p on p.id = al.personality_id
join location l on l.id = p.location_id
join "user" u on al.user_id = u.id
join alien_status s on al.alien_status_id = s.id
join profession prof on prof.id = p.profession_id
where al.id = ? and s.name = 'ON EARTH';

-- 4 ищем заявки на визит планеты, которые надо обработать
select r.id as request_id, r.creator_id, r.create_date,
       s.name as status, t.name as type, u.username, u.user_photo
from request r
join request_type t on r.type_id = t.id
join request_status s on s.id = r.status_id
join "user" u on r.creator_id = u.id
where s.name = 'PENDING' and t.name = 'VISIT';

-- 5 ищем информацию о заявке на визит по её айди, включая анкету пришельца
select r.id as request_id, r.creator_id, r.create_date,
       t.name as request_type, s.name as status,
       f.planet_id, f.visit_purpose, f.stay_time, f.comment,
       p.name as planet_name, p.race, u.username, u.user_photo
from request r
join request_type t on r.type_id = t.id
join request_status s on s.id = r.status_id
join alien_form f on r.alien_form_id = f.id
join planet p on p.id = f.planet_id
join "user" u on f.user_id = u.id
where r.id=? and s.name = 'PENDING' and t.name = 'VISIT';
