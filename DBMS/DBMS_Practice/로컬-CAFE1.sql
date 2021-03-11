create table cust_account(
    accountNo varchar2(20) primary key,
    custName varchar2(50),
    balance number(20,4)
);

insert into cust_account(
    accountNo,custNAme,balance
) values('70-490-930','홍길동',10000000
);

insert into cust_account(
    accountNo,custNAme,balance
) values('70-490-911','김유신',10000000
);
