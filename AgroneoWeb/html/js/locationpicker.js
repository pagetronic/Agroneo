/*!
 * https://github.com/Logicify/jquery-locationpicker-plugin is licensed under the MIT License
 */
var sys = (sys === undefined) ? {} : sys;
sys = $.extend({}, sys, {
    locationpicker: function (where, options) {


        (function ($) {

            /**
             * Holds google map object and related utility entities.
             * @constructor
             */
            function GMapContext(domElement, options) {
                var _map = new google.maps.Map(domElement, options);
                var _marker = new google.maps.Marker({
                    position: new google.maps.LatLng(54.19335, -3.92695),
                    map: _map,
                    title: "Drag Me",
                    visible: options.markerVisible,
                    draggable: options.markerDraggable,
                    icon: (options.markerIcon !== undefined) ? options.markerIcon : undefined
                });
                return {
                    map: _map,
                    marker: _marker,
                    circle: null,
                    location: _marker.position,
                    radius: options.radius,
                    locationName: options.locationName,
                    addressComponents: {
                        formatted_address: null,
                        addressLine1: null,
                        addressLine2: null,
                        streetName: null,
                        streetNumber: null,
                        city: null,
                        district: null,
                        state: null,
                        stateOrProvince: null
                    },
                    settings: options.settings,
                    domContainer: domElement,
                    geodecoder: new google.maps.Geocoder()
                }
            }

            // Utility functions for Google Map Manipulations
            var GmUtility = {

                /**
                 *
                 * @param gMapContext
                 * @param location
                 * @param callback
                 */
                setPosition: function (gMapContext, location, callback) {
                    gMapContext.location = location;
                    gMapContext.marker.setPosition(location);
                    gMapContext.map.panTo(location);
                    if (callback) {
                        callback.call(this, gMapContext);
                    }

                },
                locationFromLatLng: function (lnlg) {
                    return {latitude: lnlg.lat(), longitude: lnlg.lng()}
                }
            };

            function isPluginApplied(domObj) {
                return getContextForElement(domObj) !== undefined;
            }

            function getContextForElement(domObj) {
                return $(domObj).data("locationpicker");
            }

            function updateInputValues(inputBinding, gmapContext) {
                if (!inputBinding) return;
                var currentLocation = GmUtility.locationFromLatLng(gmapContext.marker.position);
                if (inputBinding.latitudeInput) {
                    inputBinding.latitudeInput.val(currentLocation.latitude).change();
                }
                if (inputBinding.longitudeInput) {
                    inputBinding.longitudeInput.val(currentLocation.longitude).change();
                }
                if (inputBinding.radiusInput) {
                    inputBinding.radiusInput.val(gmapContext.radius).change();
                }
                if (inputBinding.locationNameInput) {
                    inputBinding.locationNameInput.val(gmapContext.locationName).change();
                }
            }

            function setupInputListenersInput(inputBinding, gmapContext) {
                if (inputBinding) {
                    if (inputBinding.radiusInput) {
                        inputBinding.radiusInput.on("change", function (e) {
                            var radiusInputValue = $(this).val();
                            if (!e.originalEvent || isNaN(radiusInputValue)) {
                                return
                            }
                            gmapContext.radius = radiusInputValue;
                            GmUtility.setPosition(gmapContext, gmapContext.location, function (context) {
                                context.settings.onchanged.apply(gmapContext.domContainer,
                                    [GmUtility.locationFromLatLng(context.location), context.radius, false]);
                            });
                        });
                    }
                    if (inputBinding.locationNameInput && gmapContext.settings.enableAutocomplete) {
                        var blur = false;
                        gmapContext.autocomplete = new google.maps.places.Autocomplete(inputBinding.locationNameInput.get(0), gmapContext.settings.autocompleteOptions);
                        google.maps.event.addListener(gmapContext.autocomplete, 'place_changed', function () {
                            blur = false;
                            var place = gmapContext.autocomplete.getPlace();
                            if (!place.geometry) {
                                gmapContext.settings.onlocationnotfound(place.name);
                                return;
                            }
                            GmUtility.setPosition(gmapContext, place.geometry.location, function (context) {
                                updateInputValues(inputBinding, context);
                                context.settings.onchanged.apply(gmapContext.domContainer,
                                    [GmUtility.locationFromLatLng(context.location), context.radius, false]);
                            });
                        });
                        if (gmapContext.settings.enableAutocompleteBlur) {
                            inputBinding.locationNameInput.on("change", function (e) {
                                if (!e.originalEvent) {
                                    return
                                }
                                blur = true;
                            });
                            inputBinding.locationNameInput.on("blur", function (e) {
                                if (!e.originalEvent) {
                                    return
                                }
                                setTimeout(function () {
                                    var address = $(inputBinding.locationNameInput).val();
                                    if (address.length > 5 && blur) {
                                        blur = false;
                                        gmapContext.geodecoder.geocode({'address': address}, function (results, status) {
                                            if (status === google.maps.GeocoderStatus.OK && results && results.length) {
                                                GmUtility.setPosition(gmapContext, results[0].geometry.location, function (context) {
                                                    updateInputValues(inputBinding, context);
                                                    context.settings.onchanged.apply(gmapContext.domContainer,
                                                        [GmUtility.locationFromLatLng(context.location), context.radius, false]);
                                                });
                                            }
                                        });
                                    }
                                }, 1000);
                            });
                        }
                    }
                    if (inputBinding.latitudeInput) {
                        inputBinding.latitudeInput.on("change", function (e) {
                            var latitudeInputValue = $(this).val();
                            if (!e.originalEvent || isNaN(latitudeInputValue)) {
                                return
                            }
                            GmUtility.setPosition(gmapContext, new google.maps.LatLng(latitudeInputValue, gmapContext.location.lng()), function (context) {
                                context.settings.onchanged.apply(gmapContext.domContainer,
                                    [GmUtility.locationFromLatLng(context.location), context.radius, false]);
                                updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                            });
                        });
                    }
                    if (inputBinding.longitudeInput) {
                        inputBinding.longitudeInput.on("change", function (e) {
                            var longitudeInputValue = $(this).val();
                            if (!e.originalEvent || isNaN(longitudeInputValue)) {
                                return
                            }
                            GmUtility.setPosition(gmapContext, new google.maps.LatLng(gmapContext.location.lat(), longitudeInputValue), function (context) {
                                context.settings.onchanged.apply(gmapContext.domContainer,
                                    [GmUtility.locationFromLatLng(context.location), context.radius, false]);
                                updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                            });
                        });
                    }
                }
            }

            function autosize(gmapContext) {
                google.maps.event.trigger(gmapContext.map, 'resize');
                setTimeout(function () {
                    gmapContext.map.setCenter(gmapContext.marker.position);
                }, 300);
            }

            function updateMap(gmapContext, $target, options) {
                var settings = $.extend({}, $.fn.locationpicker.defaults, options),
                    latNew = settings.location.latitude,
                    lngNew = settings.location.longitude,
                    radiusNew = settings.radius,
                    latOld = gmapContext.settings.location.latitude,
                    lngOld = gmapContext.settings.location.longitude,
                    radiusOld = gmapContext.settings.radius;

                if (latNew === latOld && lngNew === lngOld && radiusNew === radiusOld)
                    return;

                gmapContext.settings.location.latitude = latNew;
                gmapContext.settings.location.longitude = lngNew;
                gmapContext.radius = radiusNew;

                GmUtility.setPosition(gmapContext, new google.maps.LatLng(gmapContext.settings.location.latitude, gmapContext.settings.location.longitude), function (context) {
                    setupInputListenersInput(gmapContext.settings.inputBinding, gmapContext);
                    context.settings.oninitialized($target);
                });
            }

            /**
             * Initializeialization:
             *  $("#myMap").locationpicker(options);
             * @param options
             * @param params
             * @returns {*}
             */
            $.fn.locationpicker = function (options, params) {
                if (typeof options == 'string') { // Command provided
                    var _targetDomElement = this.get(0);
                    // Plug-in is not applied - nothing to do.
                    if (!isPluginApplied(_targetDomElement)) return;
                    var gmapContext = getContextForElement(_targetDomElement);
                    switch (options) {
                        case "location":
                            if (params === undefined) { // Getter
                                var location = GmUtility.locationFromLatLng(gmapContext.location);
                                location.radius = gmapContext.radius;
                                location.name = gmapContext.locationName;
                                return location;
                            } else { // Setter
                                if (params.radius) {
                                    gmapContext.radius = params.radius;
                                }
                                GmUtility.setPosition(gmapContext, new google.maps.LatLng(params.latitude, params.longitude), function (gmapContext) {
                                    updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                                });
                            }
                            break;
                        case "map":
                            /**
                             * Returns object which allows access actual google widget and marker paced on it.
                             * Structure: {
                             *  map: Instance of the google map widget
                             *  marker: marker placed on map
                             * }
                             */
                            if (params === undefined) { // Getter
                                var locationObj = GmUtility.locationFromLatLng(gmapContext.location);
                                locationObj.formattedAddress = gmapContext.locationName;
                                locationObj.addressComponents = gmapContext.addressComponents;
                                return {
                                    map: gmapContext.map,
                                    marker: gmapContext.marker,
                                    location: locationObj
                                }
                            } else { // Setter is not available
                                return null;
                            }
                        case "autosize":
                            autosize(gmapContext);
                            return this;
                    }
                    return null;
                }
                return this.each(function () {
                    var $target = $(this);
                    // If plug-in hasn't been applied before - initialize, otherwise - skip
                    if (isPluginApplied(this)) {
                        updateMap(getContextForElement(this), $(this), options);
                        return;
                    }
                    // Plug-in initialization is required
                    // Defaults
                    var settings = $.extend({}, $.fn.locationpicker.defaults, options);

                    // Initialize
                    var gmapContext = new GMapContext(this, $.extend({}, {
                        zoom: settings.zoom,
                        center: new google.maps.LatLng(settings.location.latitude, settings.location.longitude),
                        mapTypeId: settings.mapTypeId,
                        mapTypeControl: false,
                        styles: settings.styles,
                        disableDoubleClickZoom: false,
                        scrollwheel: settings.scrollwheel,
                        streetViewControl: false,
                        radius: settings.radius,
                        locationName: settings.locationName,
                        settings: settings,
                        autocompleteOptions: settings.autocompleteOptions,
                        addressFormat: settings.addressFormat,
                        draggable: settings.draggable,
                        markerIcon: settings.markerIcon,
                        markerDraggable: settings.markerDraggable,
                        markerVisible: settings.markerVisible
                    }, settings.mapOptions));

                    settings.onchanged.apply(gmapContext.domContainer, [settings.location]);

                    $target.data("locationpicker", gmapContext);

                    // Subscribe GMap events
                    function displayMarkerWithSelectedArea() {
                        GmUtility.setPosition(gmapContext, gmapContext.marker.position, function (context) {
                            var currentLocation = GmUtility.locationFromLatLng(gmapContext.location);
                            updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                            context.settings.onchanged.apply(gmapContext.domContainer, [currentLocation, context.radius, true]);
                        });
                    }

                    if (settings.markerInCenter) {
                        gmapContext.map.addListener("bounds_changed", function () {
                            if (!gmapContext.marker.dragging) {
                                gmapContext.marker.setPosition(gmapContext.map.center);
                                updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                            }
                        });
                        gmapContext.map.addListener("idle", function () {
                            if (!gmapContext.marker.dragging) {
                                displayMarkerWithSelectedArea();
                            }
                        });
                    }
                    google.maps.event.addListener(gmapContext.marker, "drag", function (event) {
                        updateInputValues(gmapContext.settings.inputBinding, gmapContext);
                    });
                    google.maps.event.addListener(gmapContext.marker, "dragend", function (event) {
                        displayMarkerWithSelectedArea();
                    });
                    GmUtility.setPosition(gmapContext, new google.maps.LatLng(settings.location.latitude, settings.location.longitude), function (context) {
                        updateInputValues(settings.inputBinding, gmapContext);
                        // Set  input bindings if needed
                        setupInputListenersInput(settings.inputBinding, gmapContext);
                        context.settings.oninitialized($target);
                    });
                });
            };
            $.fn.locationpicker.defaults = {
                location: {latitude: 10, longitude: 10},
                locationName: "",
                radius: 500,
                zoom: 1,
                mapTypeId: google.maps.MapTypeId.HYBRID,
                styles: [],
                mapOptions: {},
                scrollwheel: true,
                inputBinding: {
                    latitudeInput: null,
                    longitudeInput: null,
                    radiusInput: null,
                    locationNameInput: null
                },
                enableAutocomplete: false,
                enableAutocompleteBlur: false,
                autocompleteOptions: null,
                addressFormat: 'postal_code',
                enableReverseGeocode: true,
                draggable: true,
                onchanged: function (currentLocation, radius, isMarkerDropped) {
                },
                onlocationnotfound: function (locationName) {
                },
                oninitialized: function (component) {
                },
                // must be undefined to use the default gMaps marker
                markerIcon: undefined,
                markerDraggable: true,
                markerVisible: true
            }
        }(jQuery));
        where.locationpicker(options);
    }
});