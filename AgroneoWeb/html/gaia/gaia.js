var gaia = {
    form: function (where, species_id) {
        var title = $('<input type="text" />').attr('placeholder', lang.get('TITLE'));
        where.append($('<div class="flexible" />').append(title));

        var species = $('<select url="/gaia/species" class="flexable expand" />').attr('placeholder', lang.get('SPECIES'));
        var common = $('<input type="text" class="flexable expand" />').attr('placeholder', lang.get('COMMON_NAME'));
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
        var location = $('<button class="flexable gaiabtn short" />').text(lang.get('LOCATION'));
        location.on('click', function () {
            map.getLocation(coordinates, function (location) {
                coordinates = location;
            });
        });

        var images = $('<button class="flexable short"  />').html('$svg.mi_cloud_upload ' + lang.get('UPLOAD_IMAGE')).addClass('flexable');

        where.append($('<div class="flexo flexible" />').append(location).append(images));

        var imgs = $('<div class="imgs" />');
        where.append(imgs);
        blobstore.button(imgs, images, imgs, 224, 126);

        var submit = $('<button />').text(lang.get('SAVE'));
        where.append(submit);

    }
};

