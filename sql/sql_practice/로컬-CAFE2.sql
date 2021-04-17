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
'�븘硫붾━移대끂',1,order_group_no_seq.nextval );

create sequence mem_count_seq start with 1 increment by 1;


insert into member
values('zz1234','1234',
'�뀒�뒪�꽣',mem_count_seq.nextval );

insert into member
values('zz5678','5678',
'�솉湲몃룞',mem_count_seq.nextval );

insert into member
values('zz9123','9123',
'源�愿묒꽍',mem_count_seq.nextval );
                  
