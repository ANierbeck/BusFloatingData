package de.nierbeck.floating.data.stream.flink;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.Optional;

@Table(keyspace = "streaming", name = "vehicles", readConsistency = "QUORUM", writeConsistency = "ONE", caseSensitiveKeyspace = false, caseSensitiveTable = false)
public class VehiclePojo {

    @PartitionKey
    private String id;

    @ClusteringColumn
    private Optional<Date> time = Optional.empty();

    private Double latitude;

    private Double longitude;

    private Integer heading;

    private Optional<String> route_id = Optional.empty();

    private String run_id = "none";

    private Integer seconds_since_report = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<Date> getTime() {
        return time;
    }

    public void setTime(Optional<Date> time) {
        this.time = time;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getHeading() {
        return heading;
    }

    public void setHeading(Integer heading) {
        this.heading = heading;
    }

    public Optional<String> getRoute_id() {
        return route_id;
    }

    public void setRoute_id(Optional<String> route_id) {
        this.route_id = route_id;
    }

    public String getRun_id() {
        return run_id;
    }

    public void setRun_id(String run_id) {
        this.run_id = run_id;
    }

    public Integer getSeconds_since_report() {
        return seconds_since_report;
    }

    public void setSeconds_since_report(Integer seconds_since_report) {
        this.seconds_since_report = seconds_since_report;
    }
}
