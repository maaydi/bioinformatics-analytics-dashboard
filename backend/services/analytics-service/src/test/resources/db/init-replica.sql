CREATE TABLE routing_test
(
    id  INT PRIMARY KEY,
    src VARCHAR(50)
);
INSERT INTO routing_test (id, src)
VALUES (1, 'REPLICA_DB');