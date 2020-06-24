create table quiz (
	quiz_id uuid primary key,
	name varchar not null
);

create table quiz_game (
	quiz_game_id uuid primary key,
	quiz_id uuid not null,
	started timestamp with time zone not null,

	foreign key (quiz_id) references quiz
);
