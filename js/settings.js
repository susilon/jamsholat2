var modal = $('#settingsModal');
modal.on('shown.bs.modal', function() {
  $(document).off('focusin.modal');
});

  function resetModal() {
    modal.off('click');
    $('.form-calculation').off();
    modal.find('.modal-title').text('Settings');
    modal.find('.modal-body').html('Setting');
    modal.find('.modal-save').off();
    modal.find('.generalSettingsHeader').remove();
    modal.find('.modal-header h5').css('display', '');
  }

  function saveSettings() {
    try {
      localStorage.configuration = JSON.stringify(global);
      return true;
    } catch (error) {
      console.warn('Unable to save settings:', error);
      return false;
    }
  }

  function generateVideoList() {
    var listGroup = '<ul class="list-group">';
    listGroup += '<li class="list-group-item list-new-video"><span class="float-left w-50"><input type="text" class="form-control" id="new-video"></span><button class="btn btn-sm btn-success float-right btn-videolist-save">Simpan</button></li>';

    $.each(global.videolist, function (index, item) {
      listGroup += '<li class="list-group-item"><span class="float-left">' + item + '</span><button class="btn btn-sm btn-danger float-right btn-videolist-remove" data-value="' + item + '">Hapus</button></li>';
    });

    listGroup += '</ul>';

    return 'Background Video List<br><small>Copy file video MP4 ke folder videos untuk menambahkan video, <br>' +
      'kemudian klik tombol Tambah, dan masukkan nama file, kemudian klik tombol Simpan dan Save changes<br><br>' +
      listGroup + '<br>' +
      '<button class="btn btn-sm btn-primary btn-videolist-add">Tambah</button></small>';
  }

  function applyScreenZoom(value) {
    var zoom = Number(value);

    if (!isFinite(zoom) || zoom < 0.5 || zoom > 1.5) {
      zoom = 1;
    }

    global.screenZoom = zoom;
    document.documentElement.style.zoom = zoom;
    return zoom;
  }

  function buildScreenSettingsView() {
    var zoom = Number(global.screenZoom) || 1;
    var zoomPercent = Math.round(zoom * 100);

    return '<div class="screen-settings">' +
      '<div class="form-group form-label"><label for="form-screen-zoom">Browser Zoom Level <span class="screen-zoom-value">' + zoomPercent + '%</span></label>' +
      '<input type="range" min="50" max="150" step="5" value="' + zoomPercent + '" class="form-control slider" id="form-screen-zoom">' +
      '<small class="form-text text-muted">Adjust the display size of this prayer clock.</small></div>' +
      '<button type="button" class="btn btn-outline-secondary btn-screen-zoom-reset">Reset to 100%</button>' +
      '</div>';
  }

  function buildGeneralSettingsView() {
    var now = new Date();
    var selectedMadhab = global.madhab === 'hanafi' ? 'selected' : '';
    var calculationOptions = ['', '', '', '', '', '', '', '', '', ''];
    var gmtTimeSign = now.getTimezoneOffset() < 0 ? '+' : '';
    var gmtTime = -now.getTimezoneOffset() / 60;
    calculationOptions[global.calculation] = 'selected';

    var generalSettingsHeader = '<ul class="nav nav-pills generalSettingsHeader" role="tablist">' +
      '<li class="nav-item" role="presentation"><a class="nav-link active" id="pills-general-tab" data-toggle="pill" href="#pills-general" role="tab" aria-controls="pills-general" aria-selected="true">General</a></li>' +
      '<li class="nav-item" role="presentation"><a class="nav-link" id="pills-about-tab" data-toggle="pill" href="#pills-about" role="tab" aria-controls="pills-about" aria-selected="false">About</a></li>' +
      '<li class="nav-item" role="presentation"><a class="nav-link" id="pills-credits-tab" data-toggle="pill" href="#pills-credits" role="tab" aria-controls="pills-credits" aria-selected="false">Credits</a></li>' +
      '<li class="nav-item" role="presentation"><a class="nav-link" id="pills-videolist-tab" data-toggle="pill" href="#pills-videolist" role="tab" aria-controls="pills-videolist" aria-selected="false">Video List</a></li>' +
      '<li class="nav-item" role="presentation"><a class="nav-link" id="pills-screen-settings-tab" data-toggle="pill" href="#pills-screen-settings" role="tab" aria-controls="pills-screen-settings" aria-selected="false">Screen Settings</a></li>' +
      '</ul>';

    var geolocationAvailable = !!(navigator && navigator.geolocation);
    var calculationForm = '<form class="form-calculation">' +
      '<div class="row">' +
      '<div class="col-6">' +
      '<div class="form-group form-label"><label for="form">Format Tanggal <small><i>(id: indonesia, en: english, etc)</i></small></label><input type="text" class="form-control" id="form-locale" value="' + global.locale + '"></div>' +
      '<div class="form-group form-label"><label for="form">Mazhab</label><select class="form-control" id="form-madhab"><option value="safii">Safii</option><option value="hanafi" ' + selectedMadhab + '>Hanafi</option></select></div>' +
      '<div class="form-group form-label"><label for="form">Metode</label><select class="form-control" id="form-calculation"><option value="0" ' + calculationOptions[0] + '>Egyptian</option><option value="1" ' + calculationOptions[1] + '>Muslim World League</option><option value="2" ' + calculationOptions[2] + '>Karachi</option><option value="3" ' + calculationOptions[3] + '>UmmAlQura</option><option value="4" ' + calculationOptions[4] + '>Singapore</option><option value="5" ' + calculationOptions[5] + '>North America</option><option value="6" ' + calculationOptions[6] + '>Dubai</option><option value="7" ' + calculationOptions[7] + '>Qatar</option><option value="8" ' + calculationOptions[8] + '>Kuwait</option><option value="9" ' + calculationOptions[9] + '>Moon Sighting Committee</option></select></div>' +
      '<div class="form-group form-label"><label for="form">Imsak <small><i>(menit sebelum subuh)</i></small></label><input type="text" class="form-control" id="form-imsak" value="' + global.imsak + '"></div>' +
      '</div>' +
      '<div class="col-6">' +
      '<div class="form-group form-label"><label for="form">Lat, Long</label>' +
      '<div class="input-group"><input type="text" class="form-control" id="form-latlngdata" value="' + global.latlngdata + '"><div class="input-group-append"><button type="button" class="btn btn-outline-secondary btn-use-location" ' + (geolocationAvailable ? '' : 'disabled') + ' title="Use current location" aria-label="Use current location"><span aria-hidden="true">📍</span></button></div></div>' +
      '<small class="form-text text-muted location-status">' + (geolocationAvailable ? 'Use browser location' : 'Geolocation not supported in this browser') + '</small></div>' +
      '<div class="form-group form-label"><label for="form">Sudut Subuh</label><input type="text" class="form-control" id="form-fajrAngle" value="' + global.fajrAngle + '"></div>' +
      '<div class="form-group form-label"><label for="form">Sudut Isya</label><input type="text" class="form-control" id="form-ishaAngle" value="' + global.ishaAngle + '"></div>' +
      '<div class="form-group form-label"><label for="form">Beep Volume</label><input type="range" min="0" max="100" value="' + (global.beep.beepVolume * 100) + '" class="form-control slider" id="form-volume"></div>' +
      '</div>' +
      '</div>' +
      '</form>' +
      '<small><i>For Reference : https://github.com/batoulapps/adhan-js/blob/master/METHODS.md</i><br><b>*Jam komputer saat ini diset di GMT ' + gmtTimeSign + gmtTime + '</b></small>';

    var aboutForm = '<img src="images/android.png" height="150px"><br><span style="font-size: 39px;">Jam Sholat - Beta</span><br>' +
      'Yuk kita bikin petunjuk waktu sholat dengan mudah.<br>jamsholat.susilon.com<br><br>' +
      'Please support your local muslim community.<br>Author : susilonurcahyo@gmail.com';

    var creditsForm = 'Adhan js <br>' +
      'Bootstrap 4 <br>' +
      'jQuery <br>' +
      'Moment js <br>' +
      'Moment-Hijri <br>' +
      'The Roboto Light Fonts <br>' +
      'CKEditor 4 <br>' +
      '<b>Video Credits :</b><br>Tawaf around the Kaaba - Hajj and Umrah Youtube Channel';

    return {
      header: generalSettingsHeader,
      body: '<div class="tab-content">' +
        '<div class="tab-pane fade show active" id="pills-general" role="tabpanel" aria-labelledby="general-tab">' + calculationForm + '</div>' +
        '<div class="tab-pane fade" id="pills-about" role="tabpanel" aria-labelledby="about-tab">' + aboutForm + '</div>' +
        '<div class="tab-pane fade" id="pills-credits" role="tabpanel" aria-labelledby="credits-tab">' + creditsForm + '</div>' +
        '<div class="tab-pane fade" id="pills-videolist" role="tabpanel" aria-labelledby="videolist-tab">' + generateVideoList() + '</div>' +
        '<div class="tab-pane fade" id="pills-screen-settings" role="tabpanel" aria-labelledby="screen-settings-tab">' + buildScreenSettingsView() + '</div>' +
        '</div>'
    };
  }

  function showGeneralSettings(callback) {
    var now = new Date();
    var settingsView = buildGeneralSettingsView();
    var originalBeepVol = global.beep.beepVolume;

    resetModal();
    modal.find('.modal-title').text('General Settings');
    modal.find('.modal-header h5').css('display', 'none');
    modal.find('.modal-header').prepend(settingsView.header);
    modal.find('.modal-body').html(settingsView.body);

    $('.screen-settings').on('input', '#form-screen-zoom', function () {
      var zoom = applyScreenZoom(Number($(this).val()) / 100);
      $('.screen-zoom-value').text(Math.round(zoom * 100) + '%');
    });

    $('.screen-settings').on('click', '.btn-screen-zoom-reset', function () {
      var zoom = applyScreenZoom(1);
      $('#form-screen-zoom').val(zoom * 100);
      $('.screen-zoom-value').text('100%');
    });

    $('.form-calculation').on('change', '#form-volume', function () {
      global.beep.beepVolume = Number($(this).val()) / 100;
      if (typeof playBeep === 'function') {
        playBeep('s');
      }
    });

    $('.form-calculation').on('click', '.btn-use-location', function () {
      if (!navigator || !navigator.geolocation) {
        modal.find('.location-status').text('Geolocation is not supported on this browser.');
        return;
      }

      modal.find('.location-status').text('Getting current location...');
      navigator.geolocation.getCurrentPosition(function (position) {
        var latitude = position.coords.latitude;
        var longitude = position.coords.longitude;
        var latlng = latitude + ', ' + longitude;

        $('#form-latlngdata').val(latlng);
        modal.find('.location-status').text('Location updated from device/browser.');
      }, function (error) {
        console.warn('Geolocation failed:', error);
        modal.find('.location-status').text('Unable to get browser location. Please enter coordinates manually.');
      }, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      });
    });

    modal.on('hidden.bs.modal', function () {
      global.beep.beepVolume = originalBeepVol;
    });

    modal.on('click', '.modal-save', function () {
      global.locale = $('#form-locale').val();
      global.madhab = $('#form-madhab').val();
      global.calculation = Number($('#form-calculation').val());
      global.latlngdata = $('#form-latlngdata').val();
      global.fajrAngle = $('#form-fajrAngle').val();
      global.ishaAngle = $('#form-ishaAngle').val();
      global.imsak = Number($('#form-imsak').val());
      originalBeepVol = global.beep.beepVolume;
      applyScreenZoom(global.screenZoom);
      saveSettings();

    if (callback !=null) {
      callback();
    }

    modal.modal('hide');
    playBeep('d');
    $('.form-calculation').off();
  });

  modal.on('click', '.btn-videolist-add', function() {
    console.log('add video');
    modal.find(".list-new-video").show();
    modal.find("#new-video").focus();
  });

  modal.on('click', '.btn-videolist-save', function() {    
    global.videolist.push(modal.find("#new-video").val());
    console.log(global.videolist);
    var videoListForm = generateVideoList();
    modal.find("#pills-videolist").html(videoListForm);
    modal.find(".list-new-video").hide();
  });

  modal.on('click', '.btn-videolist-remove', function() {
    console.log('remove video ' + $(this).attr("data-value"));
    var removeIndex = global.videolist.indexOf($(this).attr("data-value"));  
    global.videolist.splice(removeIndex, 1);
    var videoListForm = generateVideoList();
    modal.find("#pills-videolist").html(videoListForm);
    modal.find(".list-new-video").hide();
  });

  modal.find(".list-new-video").hide();
}

function generateVideoList() {
  var videolistgroup = `<ul class="list-group">
  <li class="list-group-item list-new-video"><span class="float-left w-50">
  <input type="text" class="form-control" id="new-video">
  </span><button class="btn btn-sm btn-success float-right btn-videolist-save">Simpan</button></li>`;
  $.each(global.videolist, function(vlidx, vlitem) {
    videolistgroup += `<li class="list-group-item"><span class="float-left">${ vlitem }</span><button class="btn btn-sm btn-danger float-right btn-videolist-remove" data-value="${ vlitem }">Hapus</button></li>`;
  });  
  videolistgroup += `</ul>`;

  var videoListForm = `Background Video List<br><small>Copy file video MP4 ke folder videos untuk menambahkan video, <br>
  kemudian klik tombol Tambah, dan masukkan nama file, kemudian klik tombol Simpan dan Save changes<br><br>
  ${ videolistgroup }<br>
  <button class="btn btn-sm btn-primary btn-videolist-add">Tambah</button></small>`;

  return videoListForm;
}

function showSettingsLabel(me, target, callback) {
  resetModal();
  var label = $(me).attr('data-label');
  var form = '<form> \
  <div class="form-group form-label"> \
    <label for="form">Label ' + label + '</label> \
    <input type="text" class="form-control" id="form-value" value="' + $(me).attr('data-value') + '"> \
  </div></form>';
  modal.find('.modal-title').text('Setting Label ' + label);
  modal.find('.modal-body').html(form);
  modal.find('.modal-save').click(function() {    
    var value = $('#form-value').val();
    $(target).text(value);
    $(me).attr('data-value', value);

    if (callback !=null) {
      callback(value);
    }

    modal.modal('hide');
  });
  modal.modal('show');
  setTimeout(function() {    
    $('#form-value').focus();
  }, 500); 
}

function showSettingsWaktuSholat(me) {
  resetModal();
  var label = $(me).find('.time-label').attr('data-label');
  var config = global.prayer[label];
  if (!config) {
    config = {
      label: label,
      iqomah: global.iqomahtime,
      adjustment: global.minuteadjustment,
    }
  }
  var form = '<form> \
  <div class="form-group form-label"> \
    <label for="form">Label ' + label + '</label> \
    <input type="text" class="form-control" id="form-label" value="' + config.label + '"> \
  </div> ';

  if (label != 'Terbit') {
    form += '<div class="form-group form-iqomah"> \
    <label for="form">Waktu Iqomah (menit, setelah adzan)</label> \
    <input type="text" class="form-control" id="form-iqomah" value="' + config.iqomah + '"></div> ';
  }

  form += '<div class="form-group form-adjustment"> \
  <label for="form">Adjustment Waktu (menit)</label> \
  <input type="text" class="form-control" id="form-adjustment" value="' + config.adjustment + '"></div> \
<div class="form-group form-adjustment"> \
  <label for="form">Durasi Sholat (menit, layar off)</label> \
  <input type="text" class="form-control" id="form-duration" value="' + config.duration + '"> </div> \
</form>';
  modal.find('.modal-title').text('Setting Waktu ' + label);
  modal.find('.modal-body').html(form);
  modal.find('.modal-save').click(function() {
    $(me).find('.time-label').text($('#form-label').val());
    var timeSettings = {
      label: $('#form-label').val(),
      iqomah: parseInt($('#form-iqomah').val()),
      adjustment: parseInt($('#form-adjustment').val()),
      duration: parseInt($('#form-duration').val()),
    };    
    global.prayer[label] = timeSettings;
    saveSettings();

    modal.modal('hide');
  });
  modal.modal('show');
  setTimeout(function() {    
    $('#form-iqomah').focus();
  }, 500); 
}

function showSettingsInfotext(me) {
  resetModal();
  var data = global.infotextdata;  
  var editor;
  CKEDITOR.on('instanceReady', function(ev) {
      editor = ev.editor;
  });

  modal.find('.modal-title').text('Setting Info');
  modal.find('.modal-body').html(getInfoTextList(data));

  $('.modal-body').off('click','.infotext-item');
  $('.modal-body').on('click','.infotext-item',function() {    
    var index = $(this).attr('data-index');
    var itemdata = data[index];     

    $('.form-infotext').off();
    modal.find('.modal-body').html(getInfoTextForm(itemdata, index));
    $('.form-infotext').on('click','.btn-cancel',function() {      
      modal.find('.modal-body').html(getInfoTextList(data));
      if (editor) {
        editor.destroy();
      }
    });
    $('.form-infotext').on('click','.btn-save',function() {      
      itemdata.title = $('.form-infotext').find('.form-title').val();
      itemdata.content = $('.form-infotext').find('.form-content').html();
      itemdata.enable = $('.form-infotext').find('.form-enable').is(":checked");
      itemdata.duration = $('.form-infotext').find('.form-duration').val();
      data[index] = itemdata;
      modal.find('.modal-body').html(getInfoTextList(data));
      if (editor) { 
        editor.destroy();
      }
    });

    $('#text-editor').attr('contenteditable', true);       
    CKEDITOR.inline('text-editor', {
      // Allow some non-standard markup that we used in the introduction.
      extraAllowedContent: 'a(documentation);abbr[title];code',
      removePlugins: 'stylescombo',
      extraPlugins: 'sourcedialog',
    });    
  });

  modal.find('.modal-save').click(function() {
    global.infotextdata = data;
    saveSettings();        

    modal.modal('hide');
  });

  modal.on('hidden.bs.modal', function () {
    if (editor) {      
      editor.destroy();
    }    
  })

  modal.modal('show');
}

function getInfoTextList(data) {
  var list = '<div class="list-group infotext-list">';
  $.each(data, function( index, value ) {
    var isactive = value.enable?'Active':'Not Active';
    var infoactive = value.enable?'badge-primary':'badge-default';
    if (index == 7) {
      list += '<button type="button" class="list-group-item list-group-item-action infotext-item" data-index="' + index + '">'+value.title+' <small class="float-right"><i>(hanya muncul saat dzuhur di hari jumat)</i></small></button>';
    } else {
      list += '<button type="button" class="list-group-item list-group-item-action infotext-item" data-index="' + index + '">'+value.title+' <span class="badge '+infoactive+' badge-pill float-right btn-setactive">'+isactive+'</span></button>';  
    }    
  });  
  list += '</div>';
  return list;
}

function getInfoTextForm(data, index) {  
  var ischecked = data.enable?'checked':'';  
  var form = '<form class="form-infotext"> \
  <div class="row"> \
  <div class="col-6"><div class="form-group form-label"> \
    <label for="form">Judul Info</label> \
    <input type="text" class="form-control form-title" value="' + data.title + '"> \
  </div> </div> \
  <div class="col-3"> <div class="form-group form-label"> \
    <label for="form">Durasi Tayang (detik)</label> \
    <input type="text" class="form-control form-duration" value="' + data.duration + '"> \
  </div> </div>\
  <div class="col-3"> <div class="form-group form-label"> \
    <label for="form">Set Aktif/Non Aktif</label> \
  <div class="form-check"> \
      <input class="form-check-input form-enable" type="checkbox" value="'+data.enable+'" id="setActive" '+ischecked+'> \
    <label class="form-check-label" for="setActive"> \
      Set Active \
    </label> \
  </div></div></div> \
  </div>';
  if (index == 7){
    form = '<form class="form-infotext"><div class="form-group"> \
    Info Khusus Saat Khotbah Jum\'at\
  </div>';
  } 
  form += '<div class="form-group"> \
    <div class="form-content" id="text-editor">' + data.content + '</div> \
  </div> \
  <div class="form-group form-label mt-3"> \
    <button type="button" class="btn btn-default btn-cancel">Cancel</button> \
    <button type="button" class="btn btn-primary btn-save">Save Info</button> \
  </div> \
  </form>';
  return form;
}

function showSettingsScrollText(me, callback) {
  resetModal();
  /*
  if ($(me).attr('data-width')=="100%") {
    var selectContent = "";
    var selectContainer = "selected";
  } else {
    var selectContent = "selected";
    var selectContainer = "";
  }*/
  var form = '<form> \
  <div class="form-group form-label"> \
    <label for="form">Scrolling Text</label> \
    <textarea class="form-control" id="form-value">' + $(me).attr('data-value') + '</textarea> \
  </div> \
  <div class="form-group form-label"> \
    <label for="form">Scrolling Text Saat Sholat Berlangsung</label> \
    <textarea class="form-control" id="form-value-onpray">' + $(me).attr('data-value-onpray') + '</textarea> \
  </div> \
  <div class="row">\
  <div class="form-group form-label col-6"> \
    <label for="form">Speed </label> \
    <input type="range" min="1" max="9" value="' + global.scrollingdata.speed + '" class="form-control slider" id="form-speed">\
  </div></form>';
/*
  '<div class="form-group form-label col-6"> \
    <label for="form">Lebar </label> \
    <select class="form-control" id="form-width"><option value="" ' + selectContent + '>Content</option><option value="100%" ' + selectContainer + '>Container</option></select> \
    </div> </div></form>';*/
  modal.find('.modal-title').text('Setting Scrolling Text');
  modal.find('.modal-body').html(form);
  modal.find('.modal-save').click(function() {    
    var value = $('#form-value').val();
    var valueonpray = $('#form-value-onpray').val();
    var speed = $('#form-speed').val();
    var width = $('#form-width').val();    
    $(me).attr('data-value', value);
    $(me).attr('data-value-onpray', valueonpray);
    $(me).attr('data-speed', speed);
    $(me).attr('data-width', width);

    var scrollingdata = {value:value,valueOnPray:valueonpray,speed:speed,width:width};

    if (callback !=null) {
      callback(scrollingdata);
    }

    global.scrollingdata = scrollingdata;
    saveSettings();

    modal.modal('hide');
  });
  modal.modal('show');
  setTimeout(function() {    
    $('#form-value').focus();
  }, 500); 
}

function saveSettings() {
  localStorage.configuration = JSON.stringify(global);
}

$('.nama').click(function() {
  showSettingsLabel(this, this, function(value) {
    global.namamasjid = value;
    saveSettings()
  });
});

$('.alamat').click(function() {
  showSettingsLabel(this, this, function(value) {
    global.alamatmasjid = value;
    saveSettings()
  });
});

$('.info').click(function() {
  showSettingsInfotext(this);
});

$('.time-box').click(function() {
  if($(this).find('.time-label').attr('data-label') != "Imsak") {
    showSettingsWaktuSholat(this);
  }
});

$('.marquee-container').click(function() {
  console.log('marquee');  
  showSettingsScrollText(this, setScrollingText);
});

    $('.datetime').off('click').on('click', function () {
      showGeneralSettings(function () {
        console.log('done');
      });
    });
  }

  window.showGeneralSettings = showGeneralSettings;
  window.showSettingsLabel = showSettingsLabel;
  window.showSettingsWaktuSholat = showSettingsWaktuSholat;
  window.showSettingsInfotext = showSettingsInfotext;
  window.showSettingsScrollText = showSettingsScrollText;
  window.generateVideoList = generateVideoList;
  window.saveSettings = saveSettings;

  bindElementInteractions();
  applyScreenZoom(global.screenZoom);

  if (typeof firstInstallation !== 'undefined' && firstInstallation) {
    showGeneralSettings();
  }
})();
