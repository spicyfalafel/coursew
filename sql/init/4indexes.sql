create index ag_al_idx on agent_alien using btree(agent_info_id, alien_info_id);

create index alien_info_user_id on alien_info using hash(user_id);

create index warning_alien_id_idx on warning using hash(alien_id);

create index request_type_id_status_id_idx on request using btree(type_id, status_id);
