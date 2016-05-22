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
    route_id text,
    longitude double,
    latitude double,
    display_name text,
    PRIMARY KEY (id, route_id)
);

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
    PRIMARY KEY ((tile_id, time_id), id, time)
) WITH CLUSTERING ORDER BY (time DESC);