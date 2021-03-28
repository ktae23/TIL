create table orders(
    order_group_no number(10),
    orderdate date,
    ordermethod varchar2(20),
    product_name varchar2(50),
    quantity number(10)
);

create sequence order_group_no_seq start with 1 increment by 1;

insert into orders(orderdate,ordermethod,
product_name,quantity,order_group_no) 
values(SYSDATE,'web',
'아메리카노',1,order_group_no_seq.nextval );
