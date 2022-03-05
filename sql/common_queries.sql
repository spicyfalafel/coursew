select * from "user" where username=? and passw_hash=?;

select al.id, u.username, u.user_photo, s.name, al.personality_id, al.departure_date
from agent_info ag
join agent_alien aa on ag.user_id = aa.agent_info_id
join alien_info al on aa.alien_info_id = al.id
join alien_status s on al.alien_status_id = s.id
join "user" u on u.id = al.user_id
where ag.id=?;


select u.username, u.user_photo,
       s.name,
       p.first_name, p.second_name, p.age, p.person_photo,
       l.city, l.country,
       prof.name,
       w.name, w.description, w.warning_date,
from alien_info al
join alien_personality p on o.id = al.personality_id
join location l on l.id = p.location_id
join user u on al.user_id = u.id
join alien_status s on al.alien_status_id = s.id
join warning w on w.alien_id = al.id
join profession prof on prof.id = p.profession_id
where al.id = ? and s.name = 'ON EARTH';


select r.id, r.creator_id, r.create_date,
       s.name, t.name
from request r
join request_type t on r.type_id = t.id
join request_status s on s.id = r.status_id
where s.name = 'PENDING' and t.name = 'VISIT';


select r.id, r.creator_id, r.create_date, 
       t.name, s.name,
       f.planet_id, f.visit_purpose, f.stay_time, f.comment,
       p.name, p.race
from request r
join request_type t on r.type_id = t.id
join request_status s on s.id = r.status_id
join alien_form f on r.alien_form_id = f.id
join planet p on p.id = f.planet_id
where r.id=? and s.name = 'PENDING' and t.name = 'VISIT';

join skill_in_alien_form sf on sf.alien_form_id = f.id
join skill s on sf.skill_id = s.id;
