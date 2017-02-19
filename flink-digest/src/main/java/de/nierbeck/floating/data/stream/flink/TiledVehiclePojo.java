package de.nierbeck.floating.data.stream.flink;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.math.BigInteger;
import java.util.Date;

@Table(keyspace = "streaming", name = "vehicles_by_tileid", readConsistency = "QUORUM", writeConsistency = "ONE", caseSensitiveKeyspace = false, caseSensitiveTable = false)
public class TiledVehiclePojo {

    @PartitionKey(value = 0)
    @Column(name = "tile_id")
    private String tileId;

    @PartitionKey(value = 1)
    @Column(name = "time_id")
    private Long timeId;

    @ClusteringColumn(value = 1)
    private String id;

    @ClusteringColumn(value = 0)
    private Date time;

    private Double latitude;

    private Double longitude;

    private Integer heading;

    private String route_id = null;

    private String run_id = "none";

    private Integer seconds_since_report = 0;

    public String getTileId() {
        return tileId;
    }

    public void setTileId(String tileId) {
        this.tileId = tileId;
    }

    public Long getTimeId() {
        return timeId;
    }

    public void setTimeId(Long timeId) {
        this.timeId = timeId;
    }

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
        return "TiledVehiclePojo{" +
                "tileId='" + tileId + '\'' +
                ", timeId=" + timeId +
                ", id='" + id + '\'' +
                ", time=" + time +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", heading=" + heading +
                ", route_id='" + route_id + '\'' +
                ", run_id='" + run_id + '\'' +
                ", seconds_since_report=" + seconds_since_report +
                '}';
    }
}
