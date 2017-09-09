package de.nierbeck.floating.data.stream.flink;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "streaming", name = "vehiclecluster_by_tileid_flink", readConsistency = "QUORUM", writeConsistency = "ONE", caseSensitiveKeyspace = false, caseSensitiveTable = false)
public class TiledVehicleClusterPojo {

    @PartitionKey
    @Column(name = "tile_id")
    private String tileId;

    @ClusteringColumn(0)
    private Integer id;

    @ClusteringColumn(1)
    @Column(name = "time_stamp")
    private Long timeStamp = null;

    private Double latitude;

    private Double longitude;

    private Integer amount;

    public String getTileId() {
        return tileId;
    }

    public void setTileId(String tileId) {
        this.tileId = tileId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
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

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
