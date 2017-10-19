var self = this;


self.getWebsocket = function(path) {
    //var path = "ws://"+ window.location.hostname +":8001/ws/vehicles";
    return new WebSocket(path);
};

var center = new ol.Feature({
    //geometry: new ol.geom.Point(ol.proj.fromLonLat([-118.23194,34.12527]))
    id: "center",
    geometry: new ol.geom.Point(ol.proj.fromLonLat([-118.30814,34.05374]))
});

center.setStyle(new ol.style.Style({
    image: new ol.style.Icon(/** @type {olx.style.IconOptions} */ ({
      color: '#8959A8',
      src: 'data/dot.png'
    }))
}));


self.vehicleSource = new ol.source.Vector();
var vehicleLayer = new ol.layer.Vector({
    source: self.vehicleSource
});
vehicleLayer.setZIndex(6)

self.routesSource = new ol.source.Vector();
var routeLayer = new ol.layer.Vector({
    source: self.routesSource
});
routeLayer.setZIndex(8)

self.hotspotSource = new ol.source.Vector();
var hotspotLayer = new ol.layer.Vector({
    source: self.hotspotSource
});
hotspotLayer.setZIndex(10)


var rasterLayer = new ol.layer.Tile({
    source: new ol.source.OSM()
});


var map = new ol.Map({
    controls: ol.control.defaults().extend([
      new ol.control.OverviewMap()
    ]),
    target: 'map',
    layers: [rasterLayer, vehicleLayer, routeLayer, hotspotLayer],
    view: new ol.View({
      projection: ol.proj.get('EPSG:3857'),
      //34.05374 | -118.30814
      //center: ol.proj.fromLonLat([-118.23194,34.12527]),
      center: ol.proj.fromLonLat([-118.30814,34.05374]),
      zoom: 14
    })
});

self.akkaServiceBasis = 'http://'+ window.location.hostname +':8000/vehicles/boundingBox?bbox='
self.akkaServiceFlinkBasis = 'http://'+ window.location.hostname +':8000/vehicles_flink/boundingBox?bbox='
self.akkaHotSpotBasis = 'http://'+ window.location.hostname +':8000/hotspots/boundingBox?bbox='
self.akkaHotSpotFlinkBasis = 'http://'+ window.location.hostname +':8000/hotspots_flink/boundingBox?bbox='
self.akkaRouteInfoService = 'http://'+ window.location.hostname +':8000/routeInfo/'
self.akkaRouteService = 'http://'+ window.location.hostname +':8000/route/'
self.websocketVehicles = 'ws://'+ window.location.hostname +':8001/ws/vehicles';

self.sparkEnabled = true;
self.flinkEnabled = false;

self.ajax = function(uri) {
  var request = {
      url: uri,
      type: 'GET',
      contentType: "application/json",
      accepts: "application/json",

      cache: false,
      dataType: 'json',
      error: function(jqXHR) {
          console.log("ajax error " + jqXHR.status);
      }
  };
  return $.ajax(request);
};


self.sessionStyle = {};
self.selectedColor = [];


self.drawDataOnMap = function(data) {
   console.log("requested data");
   if (self.vehicleSource.getFeatures().length > 0) {
     console.log("cleared vehicleSource");
     self.vehicleSource.clear();
   }

   for (var i = 0; i < data.length; i++) {
       self.draw(data[i])
   }

   console.log("feature updated");
}

self.draw = function(data) {
   var field = data;
   console.log("createing feature for data: "+field.id);
   var feature = new ol.Feature(field);
   feature.setId(field.id);

   var latitude = field.latitude;
   var longitude = field.longitude;
   var point = new ol.geom.Point(ol.proj.fromLonLat([longitude,latitude]));

   var colorStyle;

   if (self.sessionStyle.hasOwnProperty(field.route_id)) {
     colorStyle = sessionStyle[field.route_id]
   } else {
     colorStyle = self.pickColor()
     while ($.inArray(colorStyle, self.selectedColor) > -1) {
        colorStyle = self.pickColor();
     }
     self.sessionStyle[field.route_id] = colorStyle;
   }

   style = new ol.style.Style({
         image: new ol.style.Icon(/** @type {olx.style.IconOptions} */ ({
         color: colorStyle,
         src: 'data/arrow.png',
         rotation: field.heading
         }))
     });

   feature.setGeometry(point)
   feature.setStyle(style)
   console.log("feature created")
   self.vehicleSource.addFeature(feature);

}


self.map.on('moveend', function (event) {
  console.log("map move end");

  var timeRequest = $( "#amount" )

  if (self.socket != null) {
    self.socket.send("close")
    self.socket.close();
  }

  var mapExtent = self.map.getView().calculateExtent(map.getSize());
  var bottomRight = ol.extent.getBottomRight(mapExtent);
  var topLeft = ol.extent.getTopLeft(mapExtent);
  var brLonLat = ol.proj.toLonLat(bottomRight);
  var tlLonLat = ol.proj.toLonLat(topLeft);
  var streamAppend = "";

  if (timeRequest > 0) {
      self.requestVehiclesOnBoundingBox(tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0], timeRequest)
  } else {
    self.socket = self.getWebsocket(self.websocketVehicles);

    self.socket.onmessage = function (msg) {
      console.log("websocket")
      self.draw( jQuery.parseJSON(msg.data));
    }

    self.socket.onopen = function (e) {
        if (self.sparkEnabled) {
            self.socket.send("spark");
        } else if (self.flinkEnabled) {
            self.socket.send("flink");
        }
        self.socket.send(tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0])
    }
  }

});

self.requestVehiclesOnBoundingBox = function(bboxString, timeRequest) {
    if (self.sparkEnabled)
        var akkaService = self.akkaServiceBasis+bboxString;
    else
        var akkaService = self.akkaServiceFlinkBasis+bboxString;
    self.ajax(akkaService+"&time="+timeRequest).done(function(data){
        console.log("got data")
        self.drawDataOnMap(data);
    });
}

self.drawRoutes = function(routeIds) {
    if(self.routesSource.getFeatures().length > 0) {
        console.log("clear old routes")
        self.routesSource.clear();
    }
    for (var i = 0, ii = routeIds.length; i < ii; ++i) {
        var requestUrl = self.akkaRouteService+routeIds[i];
        self.ajax(requestUrl).done(function(data){

            if (data.length == 0)
                return;

            var feature = new ol.Feature(data)
            var lineString = new ol.geom.LineString();
            for (var j = 0; j < data.length; j++) {
                var latitude = data[j].latitude;
                var longitude = data[j].longitude;
                var coord = ol.proj.fromLonLat([longitude,latitude]);
                lineString.appendCoordinate(coord);
            }
            feature.setGeometry(lineString)
            feature.setId("Route-"+data[0].routeId);

            feature.setStyle(new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(255, 255, 255, 0.2)',
                    weight: 4
                }),
                stroke: new ol.style.Stroke({
                    color: '#808080',
                    width: 4
                })
              }));

            self.routesSource.addFeature(feature);
        });
    }
}

self.createOverlay = function() {
    return new ol.Overlay({
        element: document.getElementById('myOverlay'),
        positioning: 'top-right',
        stopEvent: false,
        insertFirst: false
    });
}

self.queryRoutes = function(routeIds) {
    for (var i = 0, ii = routeIds.length; i < ii; ++i) {
        var requestUrl = self.akkaRouteInfoService+routeIds[i];
        $('#coordinate').text();
        self.ajax(requestUrl).done(function(data){
            var txt = "<li>" + data[0].display_name + "</li>";
            $('#routeInfos').append(txt)
        });
    }

}

self.setCoordinateAndShow = function(coordinate, pixel) {
    $(overlay.getElement()).hide();
    $('#routeInfos').empty();
    // Set position
    overlay.setPosition(coordinate);

    var features = [];
    self.map.forEachFeatureAtPixel(pixel, function(feature, layer) {
      features.push(feature);
    });

    if (features.length > 0) {
      var info = [];
      var uniqueIds = [];


      for (var i = 0, ii = features.length; i < ii; ++i) {
        var routeId = features[i].get('route_id');

        if (typeof routeId != 'undefined' && $.inArray(routeId, uniqueIds) == -1) {
            uniqueIds.push(routeId);
        }

        info.push(features[i].get('id'));
        //maybe it's a lineString
        console.log("Geom is: " + features[i].getId());
        if (features[i].getId().indexOf("Route-") == 0) {
            info.push(features[i].get('routeId'))
            console.log("found a linestring");
            var closestPoint = features[i].getGeometry().getClosestPoint(coordinate);
            console.log("now query backend for route with closest point: "+closestPoint);
            self.routesSource.clear();
            return;
        }

        if (features[i].getId().indexOf("Cluster-") == 0) {
            console.log("found a cluster");
        }
      }
      if (uniqueIds.length > 0) {
        self.queryRoutes(uniqueIds);
        self.drawRoutes(uniqueIds);
      }


      $('#coordinate').text(info.join(', ') || '(unknown)');
      $(overlay.getElement()).show();
    }
}

self.overlay = createOverlay();

console.log("created overlay: "+self.overlay);

self.map.addOverlay(self.overlay);

self.map.on('click', function(event) {
    var coordinate = event.coordinate;
    var pixel = event.pixel;
    setCoordinateAndShow(coordinate, pixel);
});

self.drawHotSpotsOnMap = function(data) {
   console.log("draw clusters")
   if (self.hotspotSource.getFeatures().length > 0) {
     console.log("cleared hotspotSource");
     self.hotspotSource.clear();
   }

   if (data.length <= 0) {
    console.log("No Hotspots found from server")
   }

   for (var i = 0; i < data.length; i++) {
       var field = data[i];
       var feature = new ol.Feature(field);
       feature.setId("Cluster-"+field.id);

       var latitude = field.latitude;
       var longitude = field.longitude;
       var point = new ol.geom.Point(ol.proj.fromLonLat([longitude,latitude]));

       style = new ol.style.Style({
             image: new ol.style.Icon(/** @type {olx.style.IconOptions} */ ({
             src: 'data/bus.png'
             }))
         });

       feature.setGeometry(point)
       feature.setStyle(style)

       self.hotspotSource.addFeature(feature);
   }

}

self.pickColor = function() {
    return "#000000".replace(/0/g,function(){return (~~(Math.random()*16)).toString(16);});
};

$(function() {
    $( "#slider" ).slider({
        range: "max",
        min: 0,
        max: 10,
        value: 0,
        slide: function( event, ui ) {
            $( "#amount" ).val( ui.value );

            var mapExtent = self.map.getView().calculateExtent(map.getSize());
            var bottomRight = ol.extent.getBottomRight(mapExtent);
            var topLeft = ol.extent.getTopLeft(mapExtent);
            var brLonLat = ol.proj.toLonLat(bottomRight);
            var tlLonLat = ol.proj.toLonLat(topLeft);

            if (ui.value > 0) {
                self.socket.send("close")
                self.socket.close();
                self.vehicleSource.clear();
                self.requestVehiclesOnBoundingBox(tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0], ui.value)
            } else {
                //clean old data
                console.log("cleaning old data")
                self.vehicleSource.clear();
            }
        }
    });
    $( "#amount" ).val( $( "#slider" ).slider( "value" ) );
    $( "#hotspot" ).button().click(function() {
        if ($('#hotspot').is(':checked')) {
            console.log("draw hotspots spark")
            var mapExtent = self.map.getView().calculateExtent(map.getSize());
            var bottomRight = ol.extent.getBottomRight(mapExtent);
            var topLeft = ol.extent.getTopLeft(mapExtent);
            var brLonLat = ol.proj.toLonLat(bottomRight);
            var tlLonLat = ol.proj.toLonLat(topLeft);

            self.ajax(self.akkaHotSpotBasis+tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0]).done(function(data){
              self.drawHotSpotsOnMap(data)
            });
        } else {
          if (self.hotspotSource.getFeatures().length > 0) {
             console.log("cleared hotspotSource");
             self.hotspotSource.clear();
          }
        }
    });

    //flink
    $( "#slider_flink" ).slider({
            range: "max",
            min: 0,
            max: 10,
            value: 0,
            slide: function( event, ui ) {
                $( "#amount_flink" ).val( ui.value );

                var mapExtent = self.map.getView().calculateExtent(map.getSize());
                var bottomRight = ol.extent.getBottomRight(mapExtent);
                var topLeft = ol.extent.getTopLeft(mapExtent);
                var brLonLat = ol.proj.toLonLat(bottomRight);
                var tlLonLat = ol.proj.toLonLat(topLeft);

                if (ui.value > 0) {
                    self.socket.send("close")
                    self.socket.close();
                    self.vehicleSource.clear();
                    self.requestVehiclesOnBoundingBox(tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0], ui.value)
                } else {
                    //clean old data
                    console.log("cleaning old data")
                    self.vehicleSource.clear();
                }
            }
        });
        $( "#amount_flink" ).val( $( "#slider_flink" ).slider( "value" ) );
        $( "#hotspot_flink" ).button().click(function() {
            if ($('#hotspot_flink').is(':checked')) {
                console.log("draw hotspots flink")
                var mapExtent = self.map.getView().calculateExtent(map.getSize());
                var bottomRight = ol.extent.getBottomRight(mapExtent);
                var topLeft = ol.extent.getTopLeft(mapExtent);
                var brLonLat = ol.proj.toLonLat(bottomRight);
                var tlLonLat = ol.proj.toLonLat(topLeft);

                self.ajax(self.akkaHotSpotFlinkBasis+tlLonLat[1]+","+tlLonLat[0]+","+brLonLat[1]+","+brLonLat[0]).done(function(data){
                  self.drawHotSpotsOnMap(data)
                });
            } else {
              if (self.hotspotSource.getFeatures().length > 0) {
                 console.log("cleared hotspotSource");
                 self.hotspotSource.clear();
              }
            }
        });


    //toggle
    $( "#sparkEnable" ).button().click(function() {
        if ($('#sparkEnable').is(':checked')) {
            console.log("spark enabled")
            self.sparkEnabled = true;
            self.flinkEnabled = false;
            $( "#flinkEnable" ).prop('checked', false);
            if (self.socket != null) {
                self.socket.send("spark");
                self.vehicleSource.clear();
            }
        } else {
            console.log("spark disabled")
            self.sparkEnabled = false;
            self.flinkEnabled = true;
            $( "#flinkEnabled" ).prop('checked', true);
        }
    });

    $( "#flinkEnable" ).button().click(function() {
        if ($('#flinkEnable').is(':checked')) {
            console.log("flink enabled")
            self.flinkEnabled = true;
            self.sparkEnabled = false;
            $( "#sparkEnabled" ).prop('checked', false);
            if (self.socket != null) {
                self.socket.send("flink");
                self.vehicleSource.clear();
            }
        } else {
            console.log("flink disabled")
            self.flinkEnabled = false;
            self.sparkEnabled = true;
            $( "#sparkEnabled" ).prop('checked', true);
        }
    });


    $( "#sparkEnabled" ).prop('checked', self.sparkEnabled);
    $( "#flinkEnabled" ).prop('checked', self.flinkEnabled);
});