var gaia = {
    form: function (where, species_id, common_name, specimen) {
        where.find('input').one('focus', function () {
            gaia.makeform(where, species_id, common_name, specimen);
        });
    },
    makeform: function (where, species_id, common_name, specimen) {
        var height = 29;
        where.css({height: height, maxHeight: 'initial'});
        var title = where.find('input').attr('placeholder', lang.get('TITLE')).focus();
        var species = $('<select url="/gaia/species" class="flexable expand" />').attr('placeholder', lang.get('SPECIES').ucfirst());
        var common = $('<input type="text" class="flexable expand" />').attr('placeholder', lang.get('COMMON_NAME'));
        if (common_name !== undefined && common_name !== '') {
            common.val(common_name);
        }
        where.append($('<div class="flexo flexible" />').css({
            marginBottom: 8
        }).append(species).append(common));

        species.selectable({position: 'before'});

        if (species_id !== '') {
            species.trigger('search', species_id);
        }

        var text = $('<textarea rows="4" />').attr('placeholder', lang.get('TEXT'));
        where.append($('<div class="flexible" />').append(text.autosize()));


        var coordinates = null;
        var location = $('<button class="flexable gaiabtn" />').text(lang.get('LOCATION'));
        var lat = $('<input type="number" class="flexable" size="5" />').attr('placeholder', lang.get('LATITUDE'));
        var lon = $('<input type="number" class="flexable" size="5" />').attr('placeholder', lang.get('LONGITUDE'));
        var picker = $('<div class="picker" />').hide();
        location.one('click', function () {
            location.remove();
            picker.css({height: 300, width: '100%'});
            map.getLocation(picker.show(), function (geoJson) {
                coordinates = geoJson;
                lat.val(geoJson.coordinates[1]);
                lon.val(geoJson.coordinates[0]);
            }, lat.val(), lon.val());

            lat.add(lon).on('change', function () {
                picker.locationpicker('location', {latitude: lat.val(), longitude: lon.val()});
            });
        });


        where.append($('<div class="flexo flexible" />').css({
            marginBottom: 8
        }).append(location).append(lat).append(lon));
        where.append(picker);

        var blobs = $('<button class="flexable"  />').html('$svg.fa_icon_image ' + lang.get('UPLOAD_IMAGE')).addClass('flexable');
        where.append($('<div class="flexo flexible" />').css({
            marginBottom: 8
        }).append(blobs));

        var imgs = $('<div class="imgs" />');
        where.append(imgs);
        blobstore.button(imgs, blobs, imgs, 224, 126);

        var submit = $('<button />').html('$svg.mi_save').append(lang.get('SAVE'));
        where.append(submit);
        title.focus();
        where.css({height: ''});
        var nextheight = where.height();
        where.css({height: height});
        where.animate({height: nextheight}, 400, function () {
            where.css({height: ''});
        });
        submit.on('click', function () {
            $('.error_input').removeClass('error_input');
            var loading = $('<div/>')
                .css({position: 'absolute', background: '#FFF', top: 0, bottom: 0, right: 0, left: 0})
                .html(sys.loading(70, 'div'));
            where.append(loading);
            var images = [];
            imgs.find('input[name=' + $.escapeSelector('docs[]') + ']').each(function () {
                images.push(this.value);
            });
            api.post('/gaia', {
                action: 'create',
                title: title.val(),
                species: species.val(),
                common: common.val(),
                text: text.val(),
                location: coordinates,
                images: images

            }, function (rez) {
                if (rez.error !== undefined) {
                    sys.alert(rez.error);
                } else if (rez.errors !== undefined) {
                    $.each(rez.errors, function (key, value) {
                        switch (key) {
                            case 'location':
                                location.addClass('error_input');
                                lat.addClass('error_input');
                                lon.addClass('error_input');
                                break;
                            case 'species':
                                species.prev().addClass('error_input');
                                break;
                            case 'images':
                                blobs.addClass('error_input');
                                break;
                        }
                    });
                } else if (rez.ok) {
                    ajax.reload(false);
                }

                loading.remove();

            }, function () {
                loading.remove();
            });
        });
    }
};

