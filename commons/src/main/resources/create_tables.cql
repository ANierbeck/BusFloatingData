CREATE KEYSPACE streaming WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE routeInfos (
    id text,
    display_name text,
    PRIMARY KEY (id)
);

CREATE TABLE vehicles (
    id text,
    time timestamp,
    longitude double,
    latitude double,
    heading int,
    route_id text,
    run_id text,
    seconds_since_report int,
    PRIMARY KEY (id, time)
);

CREATE TABLE routes (
    id text,
    order_id int,
    route_id text,
    longitude double,
    latitude double,
    display_name text,
    PRIMARY KEY (route_id, order_id, id)
) WITH CLUSTERING ORDER BY (order_id ASC);

CREATE TABLE vehicles_by_tileid (
    tile_id text,
    time_id bigint,
    id text,
    time timestamp,
    longitude double,
    latitude double,
    heading int,
    route_id text,
    run_id text,
    seconds_since_report int,
    PRIMARY KEY ((tile_id, time_id), time, id)
) WITH CLUSTERING ORDER BY (time DESC);

CREATE TABLE vehiclecluster (
    id int PRIMARY KEY,
    longitude double,
    latitude double,
    amount int
);

CREATE TABLE vehiclecluster_by_tileid (
    tile_id text PRIMARY KEY,
    id int,
    longitude double,
    latitude double,
    amount int
);
