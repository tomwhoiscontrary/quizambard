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

create table quiz_game_player (
	quiz_game_player_id uuid primary key,
	quiz_game_id uuid not null,
	name varchar not null,

	foreign key (quiz_game_id) references quiz_game
);
