create table if not exists users(
      id              uuid not null primary key,
      username        varchar(255),
      password        varchar(255)
);

insert into users (id, username, password)
values ('bd3e6ad0-d450-11ed-b685-a16a1e4ead17', 'user', 'password');