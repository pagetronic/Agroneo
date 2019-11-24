sys.specimens = {
  init:function (specimen_id) {
      sys.threads.follow('specimens/' + specimen_id);

      if (sys.user.id === null) {
          return;
      }
      var area = $('#reply .boxarea');
      var postbox = sys.threads.postbox(area, false, 'specimen_' + specimen_id);
      blobstore.button(postbox.imgs, postbox.upload_button, area, 224, 126);

      area.locker = false;
      postbox.submit.on('click', function () {
          if (area.locker) {
              return;
          }
          area.locker = true;
          sys.threads.send(area, function (msg) {

              var post_send = $(msg.html);
              var post = $('#' + msg.post.id);
              if (post.length > 0) {
                  post.replaceWith(post_send.pulse());
              } else {
                  $('#pushadd').before(post_send.pulse());
              }

          });
      });

      var follow = $('#reply .follow');
      var submit = $('#reply .submit');
      var flexible = $('<div/>').addClass('flexible');
      submit.after(flexible);
      flexible.append(submit).append($('<div/>').addClass('grower')).append(follow);
  }  
};