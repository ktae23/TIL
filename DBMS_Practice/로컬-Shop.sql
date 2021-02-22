
create table usertbl
(userId char(8)  primary key,
userName nvarchar2(10) not null,
birthYear number(4) not null,
addr nchar(2) not null,
mobile1 char(3) null,
mobile2 char(8) null,
height number(3) null,
mDate date null
);

create table buytbl
( idNum number(8)  primary key ,
aaa char(8) not null ,
prodName nchar(6) not null,
groupName nchar(4) null,
price number(8)  null,
amount number(3) not null,
FOREIGN KEY(userId) REFERENCES usertbl(userId)
);

CREATE SEQUENCE idSEQ;

insert into usertbl values('LSG','이승기',1987,'서울','011','11111111',182,'2008-8-8');
insert into usertbl values('KBS','김범수',1979,'경남','011','22222222',173,'2012-4-4');
insert into usertbl values('KKH','김경호',1971,'전남','019','33333333',177,'2007-7-7');

insert into buytbl values(idSEQ.NEXTVAL, 'KBS', '운동화',NULL, 30,2);
insert into buytbl values(idSEQ.NEXTVAL, 'KBS', '노트북','전자', 1000,1);
insert into buytbl values(idSEQ.NEXTVAL, 'JYP', '모니터','전자', 200,1);

COMMIT;

select * from user_constraints where table_name='USERTBL' and constraint_type='P';

DROP TABLE usertbl;
DROP TABLE buytbl;

create table usertbl
(userId char(8)  ,
userName nvarchar2(10) not null,
birthYear number(4) not null,
addr nchar(2) not null,
mobile1 char(3) null,
mobile2 char(8) null,
height number(3) null,
mDate date null,
constraint PK_userTBL_userId primary key (userId)
);
alter table usertbl add constraint PK_userTBL_userId primary key (userId);

create table prodTbl(
prodCode char(3) not null,
prodID char(4) not null,
prodDate date not null,
prodCur char(10) null);

alter table prodTbl
add CONSTRAINT PK_prodTbl_prodCode_prodID 
primary key (prodCode,prodID);

create table buytbl
( idNum number(8)  primary key ,
userId char(8) not null ,
prodName nchar(6) not null,
groupName nchar(4) null,
price number(8)  null,
amount number(3) not null
,constraint FK_userTBL_buyTBL FOREIGN KEY (userId) references usertbl(userId)
);
