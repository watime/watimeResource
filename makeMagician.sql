begin transaction;
create table temp(pinyin text, zi text, priority integer);
insert into temp select pinyin, zi, priority from solo;
drop table solo;
alter table temp rename to solo;

create table temp(pinyin text, zi text, priority integer);
insert into temp select pinyin, zi, priority from duet;
drop table duet;
alter table temp rename to duet;

create table temp(pinyin text, zi text, priority integer);
insert into temp select pinyin, zi, priority from trio;
drop table trio;
alter table temp rename to trio;

create table temp(pinyin text, zi text, priority integer);
insert into temp select pinyin, zi, priority from quartet;
drop table quartet;
alter table temp rename to quartet;

create table temp(pinyin text, zi text, priority integer);
insert into temp select pinyin, zi, priority from concerto;
drop table concerto;
alter table temp rename to concerto;

end transaction;

begin transaction;
create index idx_solo on solo(pinyin collate binary, zi collate binary);
create index idx_duet on duet(pinyin collate binary, zi collate binary);
create index idx_trio on trio(pinyin collate binary, zi collate binary);
create index idx_quartet on quartet(pinyin collate binary, zi collate binary);
create index idx_concerto on concerto(pinyin collate binary, zi collate binary);
end transaction;


create index idx_solo_zi on solo(zi collate binary);
create index idx_duet_zi on duet(zi collate binary);
create index idx_trio_zi on trio(zi collate binary);
create index idx_quartet_zi on quartet(zi collate binary);
create index idx_concerto_zi on concerto(zi collate binary);
end transaction;

	****expirements****
create index idx1 on duet(pinyin collate binary);
create index idx2 on duet(priority);
create index idx3 on duet(zi collate binary);
create index idx4 on duet(pinyin);
create index idx5 on duet(zi);
create index idx6 on duet(pinyin collate binary, priority);
create index idx7 on duet(pinyin collate binary, zi collate binary);*
create index idx8 on duet(zi collate binary, pinyin collate binary);
create index idx9 on duet(zi collate binary, priority);

select length(zi), zi, priority from duet where pinyin glob 'AaA?' and zi glob '?拔'



