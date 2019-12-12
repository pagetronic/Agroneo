var gaia = {
    form: function (where, species_id, common_name) {
        where.find('input').one('focus', function () {
            gaia.makeform(where, species_id, common_name);
        });
    },
    makeform: function (where, species_id, common_name) {
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
        location.on('click', function () {
            map.getLocation(coordinates, function (location) {
                coordinates = location;
            });
        });

        var images = $('<button class="flexable"  />').html('$svg.fa_icon_image ' + lang.get('UPLOAD_IMAGE')).addClass('flexable');

        where.append($('<div class="flexo" />').append(location).append(images));

        var imgs = $('<div class="imgs" />');
        where.append(imgs);
        blobstore.button(imgs, images, imgs, 224, 126);

        var submit = $('<button />').html('$svg.mi_save').append(lang.get('SAVE'));
        where.append(submit);
        title.focus();
        where.css({height: ''});
        var nextheight = where.height();
        where.css({height: height});
        where.animate({height: nextheight}, 400, function () {
            where.css({height: ''});
        });
    }
};

