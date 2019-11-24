var gaia = {
    init: function () {

        var map = $('<div id="gaia"/>');
        $('#middle').append(map);
        var inmap = $('<div class="inmap"/>');
        map.html(inmap)
        inmap.html(sys.loading(60, 'div').css({marginTop: 50}));
        if (self !== top) {
            $('#menu, #lateral').remove();
            $('a, form').attr('target', '_top');
            $('logo a').on('click', function () {
                top.location.replace(document.location.href);
                return false;
            });
        }
        var loading = sys.loading(40);
        inmap.html(loading);

        $.getScript('https://maps.googleapis.com/maps/api/js?key=AIzaSyBI3aiKNQg_HvHrF7tHKvEfeCPuFNRRsP8', function () {
            loading.remove();
            $('#middle').scrollTo(0);
            gaia.load(inmap);
        });

    },
    load: function (dest) {


        var level_zone = 6;
        window.debug = false;


        var conf = {
            zoom: 3,
            mapTypeId: google.maps.MapTypeId.TERRAIN,
            center: {lat: 10, lng: 10},
            zoomControl: false,
            mapTypeControl: false,
            scaleControl: true,
            streetViewControl: false,
            fullscreenControl: false
        };

        var xhrapi = {
            abort: function () {
            }
        };
        var sync = $('<div class="sync"/>');
        var speciess = $('<select placeholder="' + lang.get('SEARCH_SPECIES') + '" url="/gaia"/>');
        var families = $('<select placeholder="' + lang.get('FAMILY') + '" url="/gaia?families"/>');

        var init_url = gaia.getUrl();

        var poped = null;
        if (!isNaN(init_url.lat)) {
            conf.center.lat = init_url.lat;
        }
        if (!isNaN(init_url.lng)) {
            conf.center.lng = init_url.lng;
        }
        if (!isNaN(init_url.zoom)) {
            conf.zoom = init_url.zoom;
        }
        if (init_url.marker !== '') {
            poped = init_url.marker;
        }
        if (init_url.mapType !== '') {
            conf.mapTypeId = init_url.mapType;
        }
        var species = init_url.species;
        var family = init_url.family;


        var map = new google.maps.Map(dest[0], conf);

        var info = new google.maps.InfoWindow();
        google.maps.event.addListener(info, 'closeclick', function () {
            gaia.setUrl(null, null, null, '');
        });
        google.maps.event.addListener(map, "click", function () {
            info.close();
            gaia.setUrl(null, null, null, '');
            $('input').blur();
        });
        google.maps.event.addListener(info, 'position_changed', function () {
            info.setOptions({maxWidth: Math.min(map.getDiv().offsetWidth * 0.6, 450)});
        });

        window.mapserv = new OverlappingMarkerSpiderfier(map, {
            keepSpiderfied: true,
            markersWontMove: true,
            markersWontHide: true,
            basicFormatEvents: true,
            event: 'mouseover'
        });


        var convertDMS = function (lat, lng) {
            var tdms = function (coordinate) {
                var absolute = Math.abs(coordinate);
                var degrees = Math.floor(absolute);
                var minutesNotTruncated = (absolute - degrees) * 60;
                var minutes = Math.floor(minutesNotTruncated);
                var seconds = Math.floor((minutesNotTruncated - minutes) * 60);

                return degrees + "°" + minutes + '’' + seconds + '”';
            }
            var latitude = tdms(lat);
            var latitudeCardinal = Math.sign(lat) >= 0 ? "N" : "S";

            var longitude = tdms(lng);
            var longitudeCardinal = Math.sign(lng) >= 0 ? "E" : "W";

            return latitude + latitudeCardinal + ' ' + longitude + longitudeCardinal;
        };

        var loadMarker = function (marker) {
            if (marker.bounds !== undefined) {
                if (map.getZoom() < level_zone) {
                    map.setZoom(level_zone);
                    map.panTo(marker.center);
                } else {
                    map.panTo(marker.center);
                    map.fitBounds(marker.bounds);
                }
                mapserv.removeAllMarkers();
                return;
            }
            var specimen = marker.specimen;
            var content = $('<div class="bubble"/>');
            var title = $('<h1/>');
            var species_title = $('<span/>').css({cursor: 'pointer'}).on('click', function () {
                speciess.trigger('search', specimen.species.id);
                info.close();
                species = specimen.species.id;
                gaia.setUrl(null, null, null, '', null, species);
                loadData({}, true);
                document.title = specimen.species.name;

            }).text(specimen.species.name);
            var family_title = $('<span/>').css({cursor: 'pointer'}).on('click', function () {
                families.trigger('search', specimen.species.family.toLocaleLowerCase());
                info.close();
                family = specimen.species.family.toLocaleLowerCase();
                gaia.setUrl(null, null, null, '', null, null, family);
                loadData({}, true);
                document.title = specimen.species.family;

            }).text('(' + specimen.species.family.ucfirst() + ')');

            content.append(title.append(species_title).append(" ").append(family_title));

            var imgs = $('<div class="imgs" />');

            content.append(imgs);
            $.each(specimen.images, function (index, image) {
                imgs.append($('<div class="img" />').append($('<a/>').attr('href', image.url).append($('<img/>')
                    .attr('src', image.url + '@180x100.jpg')
                    .attr('width', 180)
                    .attr('height', 100)
                )).append($('<span/>').html(image.text)));
            });
            imgs.on("mousewheel", function (ev) {
                ev.preventDefault();
                if (ev.deltaY !== 0) {
                    imgs.scrollLeft(imgs.scrollLeft() - ev.deltaY * ev.deltaFactor / 2)
                }
            });
            var desc = $('<div class="desc"/>');
            try {
                desc.append($('<p/>').html(time.since(specimen.date, 2) + ' (' + new Date(specimen.date).toLocaleDateString() + ')'));
            } catch (e) {
            }

            if (specimen.desc !== undefined) {
                desc.append($('<p/>').html(specimen.desc));
            }

            desc.append($('<p/>').attr('title', specimen.location.coordinates[1] + ',' + specimen.location.coordinates[0]).html(convertDMS(specimen.location.coordinates[1], specimen.location.coordinates[0])));

            desc.append($('<p/>').html(specimen.author));

            content.append(desc);
            if (specimen.tropicos !== undefined) {
                content.append(
                    $('<span class="cc"/>').html($('<img width="45" height="15" alt="Creative Commons BY-SA"/>').attr('src', constants.cdnurl + '/ui/cc-by-sa@45x15')).on('click', function () {
                        window.open('https://creativecommons.org/licenses/by-nc-sa/3.0/', '_blank');
                    })).append(
                    $('<a/>').attr('target', '_blank').attr('href', specimen.tropicos).append(
                        $('<img width="109" height="23" alt="Tropicos"/>').attr('src', constants.cdnurl + '/ui/tropicos@109x23').attr('width', 109).attr('height', 23)
                    ));
            }


            info.setContent(content[0]);
            info.open(map, marker);

        };


        var loadData = function (data, fit) {

            xhrapi.abort();
            sync.loading(true);
            var zoom = map.getZoom();
            if ((data === undefined || data === null)) {
                data = {};
                var bounds = map.getBounds();
                if (zoom > 5 && bounds !== undefined) {
                    data.bounds = bounds.toJSON();
                }
                data.zoom = zoom;
            }
            data.action = 'specimens';

            if (species !== '' && species !== undefined && species !== null) {
                data.species = species;
            }
            if (family !== '' && family !== undefined && family !== null) {
                data.family = family;
            }

            xhrapi = api.post('/gaia', data, function (rez) {
                if (rez.result === undefined) {
                    sync.loading(false);
                    return;
                }

                var specimens_do = [];
                var zones_do = [];
                var centers = [];
                $.each(rez.result, function (index, zone) {

                    if (zone.specimens !== undefined) {

                        $.each(zone.specimens, function (index, specimen) {
                            centers.push({
                                lng: specimen.location.coordinates[0],
                                lat: specimen.location.coordinates[1]
                            });
                            var marker_ = new google.maps.Marker({
                                position: {
                                    lng: specimen.location.coordinates[0],
                                    lat: specimen.location.coordinates[1]
                                },
                                icon: {
                                    url: constants.cdnurl + '/css/map/marker.png',
                                    scaledSize: new google.maps.Size(32, 32)
                                },
                                zIndex: 2,
                                title: specimen.species.name
                            });
                            marker_.specimen = specimen;
                            var exist = false;
                            $.each(mapserv.getMarkers(), function (index, marker) {
                                if (marker.specimen !== undefined && marker.specimen.id === specimen.id) {
                                    exist = true;
                                }
                            });
                            if (!exist) {
                                mapserv.addMarker(marker_);
                            }
                            specimens_do.push(specimen.id);
                            if (poped === specimen.id) {
                                poped = null;
                                loadMarker(marker_);
                            }
                        });

                    }

                    if (zone.count !== undefined) {


                        var location = (zone.points !== undefined) ? [[zone.points]] : zone.location.coordinates;
                        var bounds = new google.maps.LatLngBounds();
                        $.each(location, function (index, country) {
                            $.each(country[0], function (index, element) {
                                bounds.extend({lat: element[1], lng: element[0]});
                            });
                        });

                        var center = bounds.getCenter();
                        centers.push({
                            lng: center.lng(),
                            lat: center.lat()
                        });

                        var ind = 0.5 + (0.001 * zone.count);
                        var size = parseInt(64 * ind);


                        var marker = new google.maps.Marker({
                            position: {
                                lng: center.lng(),
                                lat: center.lat()
                            },
                            icon: new google.maps.MarkerImage(constants.cdnurl + '/ui/map/cluster@' + size,
                                new google.maps.Size(size, size),
                                new google.maps.Point(0, 0),
                                new google.maps.Point(parseInt(size / 2 * ind), parseInt(size / 2 * ind))
                            ),
                            label: {
                                text: '' + (zone.count >= rez.max ? '+' + rez.max : zone.count),
                                color: '#333',
                                fontSize: (14 * ind) + 'px',
                                fontWeight: 'bold'
                            },
                            zIndex: 10,
                            title: zone.country
                        });

                        marker.bounds = bounds;
                        marker.center = center;
                        marker.data = JSON.stringify(center.toJSON());

                        zones_do.push(marker.data);
                        var exist = false;
                        $.each(mapserv.getMarkers(), function (index, marker_) {
                            if (marker_.data !== undefined && marker_.data === marker.data) {
                                exist = true;
                            }
                        });
                        if (!exist) {
                            mapserv.addMarker(marker);
                        }

                    }

                });


                if (fit !== undefined && fit) {
                    var bounds = new google.maps.LatLngBounds();
                    $.each(centers, function (index, center) {
                        bounds.extend(new google.maps.Marker({
                            position: center
                        }).getPosition());
                    });
                    map.fitBounds(bounds, 15);
                    /*
                    if (map.getZoom()<5) {
                        map.setCenter({lat: 10, lng: 10});
                    }*/
                }

                $.each(mapserv.getMarkers(), function (index, marker) {
                    if (marker.specimen !== undefined && specimens_do.indexOf(marker.specimen.id) < 0) {
                        mapserv.removeMarker(marker);
                    } else if (marker.data !== undefined && zones_do.indexOf(marker.data) < 0) {
                        mapserv.removeMarker(marker);
                    }

                });

                sync.loading(false);


            }, function () {
                sync.loading(false);
            });

        };

        /* UI */
        gaia.ui(map, sync);


        map.controls[google.maps.ControlPosition.LEFT_TOP].push($('<div class="map_search"/>').append(speciess).append(families)[0]);
        speciess.selectable({
            filter: function () {
                return family !== '' ? {'family': family} : {};
            }
        });
        families.selectable();

        sync.attr('title', lang.get('LOADING')).html('$svg.mi_sync').css({display: 'none'});
        sync.timeout = 1;
        sync.loading = function (start) {
            if (start) {
                sync.stop(true, true).fadeIn(100);
                clearTimeout(sync.timeout);
                sync.timeout = setTimeout(function () {
                    sync.loading(false);
                }, 10000);
            } else {
                sync.stop(true, true).fadeOut(400, function () {
                    $(this).css({display: 'none'})
                });
            }
        };
        map.controls[google.maps.ControlPosition.LEFT_TOP].push(sync[0]);

        if (species !== '') {
            speciess.trigger('search', species);
        } else if (family !== '') {
            families.trigger('search', family);
        }
        speciess.on('change', function () {
            info.close();
            gaia.setUrl(null, null, null, '', null, this.value);
            species = speciess.val();
            if (species === null) {
                species = '';
            }
            if (species === '' && family === '') {
                loadData();
                document.title = lang.get('PLANTS_MAP');
            } else {
                loadData({}, true);
                if (species !== '') {

                    document.title = speciess.find('option[value=' + species + ']').text();
                }
            }
        });
        families.on('change', function () {
            info.close();
            gaia.setUrl(null, null, null, '', null, null, this.value);
            family = families.val();
            if (family === null) {
                family = '';
            }
            if (family === '') {
                document.title = lang.get('PLANTS_MAP');
            } else {
                document.title = families.find('option[value=' + family + ']').text();
            }

            speciess.trigger('clear');
        });


        loadData();


        mapserv.addListener('spiderfy', function () {
            info.close();
        });

        mapserv.addListener('click', function (marker, e) {
            $('input').blur();
            loadMarker(marker);
            if (marker.specimen !== undefined) {
                gaia.setUrl(null, null, null, marker.specimen.id);
            }
        });

        var tempo = -1;
        google.maps.event.addListener(map, 'bounds_changed', function () {

            var center = map.getCenter();
            gaia.setUrl(center.lat(), center.lng(), map.getZoom());
            clearTimeout(tempo);
            tempo = setTimeout(loadData, 700);

        });


    },
    ui: function (map, sync) {
        window.map = map;
        map.controls[google.maps.ControlPosition.RIGHT_TOP].push($('<a class="cmdmap"/>').attr('title', lang.get('SHARE')).html('$svg.fa_icon_share_alt_square').on('click', function () {
            var popin = pop();
            var cnt = $('<div />');
            cnt.append($('<h4/>').html(lang.get('INTEGRATE')));
            var area = $('<textarea rows="5"/>');
            cnt.append(
                area.val('<iframe src="' + document.location.href + '" style="border-radius:10px 10px 0px 0px;border:1px solid #CCC;height:500px;width:100%"><a href="' + document.location.href + '">' + document.title + '</a></iframe>').autosize().on('click', function () {
                    this.select()
                })
            );

            cnt.append($('<h4/>').html(lang.get('LINK_HTML')));
            cnt.append(
                $('<input/>').val('<a href="' + document.location.href + '">' + document.title + '</a>').on('click', function () {
                    this.select()
                })
            );

            cnt.append($('<h4/>').html(lang.get('VIEW_LINK')));
            cnt.append(
                $('<input/>').val(document.location.href).on('click', function () {
                    this.select()
                })
            );
            popin.content(cnt);
            area.trigger('input');
            popin.header(lang.get('SHARE'));

        })[0]);
        map.controls[google.maps.ControlPosition.RIGHT_TOP].push($('<div class="groupmap social"/>').append($('<a class="facebook"/>').attr('title', lang.get('SHARE')).html('$svg.fa_icon_facebook_official').on('click', function () {
            window.open('https://facebook.com/sharer/sharer.php?u=' + encodeURIComponent(document.location.href), '_blank', 'left=' + ((screen.width - 600) / 2) + ',top=50,width=600,height=400,resizable=yes').focus();
        })).append($('<a class="twitter"/>').attr('title', lang.get('SHARE')).html('$svg.fa_icon_twitter').on('click', function () {
            window.open('https://twitter.com/intent/tweet/?url=' + encodeURIComponent(document.location.href) + '&text=' + encodeURIComponent(document.title), '_blank', 'left=' + ((screen.width - 600) / 2) + ',top=50,width=600,height=400,resizable=yes').focus();
        })).append($('<a class="google"/>').attr('title', lang.get('SHARE')).html('$svg.fa_icon_google_plus_square').on('click', function () {
            window.open('https://plus.google.com/share?url=' + encodeURIComponent(document.location.href) + '&text=' + encodeURIComponent(document.title), '_blank', 'left=' + ((screen.width - 600) / 2) + ',top=50,width=600,height=600,resizable=yes').focus();
        })).append($('<a class="paypal"/>').attr('title', 'PayPal').html('$svg.fa_icon_paypal').on('click', function () {
            window.open('https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8TULTLMAYUVL8', '_blank', 'left=' + ((screen.width - 600) / 2) + ',top=50,width=600,height=700,resizable=yes').focus();
            return false;
        }))[0]);
        map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($('<div class="groupmap zoom"/>').append($('<a/>').html('$svg.fa_icon_plus').on('click', function () {
            map.setZoom(map.getZoom() + 1);
        })).append($('<a/>').html('$svg.fa_icon_minus').on('click', function () {
            map.setZoom(map.getZoom() - 1);
        }))[0]);
        map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($('<a class="cmdmap type"/>').html('$svg.fa_icon_globe').on('click', function () {
            var mapTypeIds = [google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.SATELLITE, google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.TERRAIN];
            if (this.maptype === undefined) {
                this.maptype = 0;
            }

            var mapType = mapTypeIds[(this.maptype++) % mapTypeIds.length];
            map.setMapTypeId(mapType);
            gaia.setUrl(null, null, null, null, mapType);
            dest.pulse(50);

        })[0]);

    },
    getUrl: function () {
        try {
            var path = document.location.pathname.toString();
            var current = document.location.hash.split(/[#;]/);
            var species_regex = /^\/gaia\/([a-z0-9]+)\/([a-z0-9\-]+)$/;
            var species = path.match(species_regex) ? path.replace(species_regex, '$2') : null;
            var family_regex = /^\/gaia\/([a-z0-9]+).*/;
            var family = path.match(family_regex) ? path.replace(family_regex, '$1') : null;
            return {
                lat: parseFloat((current.length >= 2) ? current[1] : ''),
                lng: parseFloat((current.length >= 3) ? current[2] : ''),
                zoom: parseInt((current.length >= 4) ? current[3] : ''),
                marker: (current.length >= 5) ? current[4] : '',
                mapType: (current.length >= 6) ? current[5] : '',
                species: species !== null ? species : '',
                family: family !== null ? family : ''
            };
        } catch (e) {
            return {};
        }
    },
    setUrl: function (lat, lng, zoom, marker, mapType, species, family) {
        var data = gaia.getUrl();
        var url = '/gaia';

        if (family !== null && family !== undefined && family !== '') {
            url += '/' + family;
        } else if (family !== '' && data.family !== '') {
            url += '/' + data.family;
        }

        if (species !== null && species !== undefined && species !== '') {
            url += '/' + species;
        } else if (species !== '' && data.species !== '') {
            url += '/' + data.species;
        }

        url += '#';
        url += ((lat !== null && lat !== undefined) ? lat : data.lat);
        url += ";" + ((lng !== null && lng !== undefined) ? lng : data.lng);
        url += ";" + ((zoom !== null && zoom !== undefined) ? zoom : data.zoom);
        url += ";" + ((marker !== null && marker !== undefined) ? marker : data.marker);
        url += ";" + ((mapType !== null && mapType !== undefined) ? mapType : data.mapType);

        try {
            history.replaceState(undefined, document.title, url);
        } catch (e) {

        }
    }
};
