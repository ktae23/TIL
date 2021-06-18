drop table orders;
CREATE TABLE orders (
    orderno            INTEGER NOT NULL,
    orderdate          DATE NOT NULL,
    ordermethod        VARCHAR2(10) NOT NULL,
    memberid           VARCHAR2(20),
    prodcode           VARCHAR2(10),
    quantity   integer not null
);

ALTER TABLE orders ADD CONSTRAINT orders_pk PRIMARY KEY ( orderno );

ALTER TABLE orders
    ADD CONSTRAINT orders_member_fk FOREIGN KEY ( memberid )
        REFERENCES member ( memid );

ALTER TABLE orders
    ADD CONSTRAINT orders_product_fk FOREIGN KEY ( prodcode )
        REFERENCES product ( prodcode );

CREATE SEQUENCE orders_orderno_seq2 START WITH 1 NOCACHE ORDER;

CREATE OR REPLACE TRIGGER orders_orderno_trg2 BEFORE
    INSERT ON orders
    FOR EACH ROW
    WHEN (
        new.orderno IS NULL
    )
BEGIN
    :new.orderno := orders_orderno_seq2.nextval;
END;
/
