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
    private Date time = null;

    private Double latitude;

    private Double longitude;

    private Integer heading;

    private String route_id = null;

    private String run_id = "none";

    private Integer seconds_since_report = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
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

    public String getRoute_id() {
        return route_id;
    }

    public void setRoute_id(String route_id) {
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

    @Override
    public String toString() {
        return "VehiclePojo{" +
                "id='" + id + '\'' +
                ", time=" + time +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", heading=" + heading +
                ", route_id=" + route_id +
                ", run_id='" + run_id + '\'' +
                ", seconds_since_report=" + seconds_since_report +
                '}';
    }
}
