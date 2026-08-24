(function () {
  'use strict';

  var modal = $('#settingsModal');

  modal.on('shown.bs.modal', function () {
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
      '</ul>';

    var calculationForm = '<form class="form-calculation">' +
      '<div class="row">' +
      '<div class="col-6">' +
      '<div class="form-group form-label"><label for="form">Format Tanggal <small><i>(id: indonesia, en: english, etc)</i></small></label><input type="text" class="form-control" id="form-locale" value="' + global.locale + '"></div>' +
      '<div class="form-group form-label"><label for="form">Mazhab</label><select class="form-control" id="form-madhab"><option value="safii">Safii</option><option value="hanafi" ' + selectedMadhab + '>Hanafi</option></select></div>' +
      '<div class="form-group form-label"><label for="form">Metode</label><select class="form-control" id="form-calculation"><option value="0" ' + calculationOptions[0] + '>Egyptian</option><option value="1" ' + calculationOptions[1] + '>Muslim World League</option><option value="2" ' + calculationOptions[2] + '>Karachi</option><option value="3" ' + calculationOptions[3] + '>UmmAlQura</option><option value="4" ' + calculationOptions[4] + '>Singapore</option><option value="5" ' + calculationOptions[5] + '>North America</option><option value="6" ' + calculationOptions[6] + '>Dubai</option><option value="7" ' + calculationOptions[7] + '>Qatar</option><option value="8" ' + calculationOptions[8] + '>Kuwait</option><option value="9" ' + calculationOptions[9] + '>Moon Sighting Committee</option></select></div>' +
      '<div class="form-group form-label"><label for="form">Imsak <small><i>(menit sebelum subuh)</i></small></label><input type="text" class="form-control" id="form-imsak" value="' + global.imsak + '"></div>' +
      '</div>' +
      '<div class="col-6">' +
      '<div class="form-group form-label"><label for="form">Lat, Long</label><input type="text" class="form-control" id="form-latlngdata" value="' + global.latlngdata + '"></div>' +
      '<div class="form-group form-label"><label for="form">Sudut Subuh</label><input type="text" class="form-control" id="form-fajrAngle" value="' + global.fajrAngle + '"></div>' +
      '<div class="form-group form-label"><label for="form">Sudut Isya</label><input type="text" class="form-control" id="form-ishaAngle" value="' + global.ishaAngle + '"></div>' +
      '<div class="form-group form-label"><label for="form">Beep Volume</label><input type="range" min="0" max="100" value="' + (global.beep.beepVolume * 100) + '" class="form-control slider" id="form-volume"></div>' +
      '</div>' +
      '</div>' +
      '</form>' +
      '<small><i>For Reference : https://github.com/batoulapps/adhan-js/blob/master/METHODS.md</i><br><b>*Jam komputer saat ini diset di GMT ' + gmtTimeSign + gmtTime + '</b></small>';

    var aboutForm = '<img src="images/android.png" height="150px"><br><span style="font-size: 39px;">Jam Sholat 2 - Beta</span><br>' +
      'Yuk kita bikin petunjuk waktu sholat dengan mudah.<br>jamsholat2.susilon.com<br><br>' +
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

    $('.form-calculation').on('change', '#form-volume', function () {
      global.beep.beepVolume = Number($(this).val()) / 100;
      if (typeof playBeep === 'function') {
        playBeep('s');
      }
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
      saveSettings();

      if (typeof callback === 'function') {
        callback();
      }

      modal.modal('hide');
      if (typeof playBeep === 'function') {
        playBeep('d');
      }
      $('.form-calculation').off();
    });

    modal.on('click', '.btn-videolist-add', function () {
      modal.find('.list-new-video').show();
      modal.find('#new-video').focus();
    });

    modal.on('click', '.btn-videolist-save', function () {
      var newVideo = modal.find('#new-video').val();
      if (!newVideo) {
        return;
      }

      global.videolist.push(newVideo);
      modal.find('#pills-videolist').html(generateVideoList());
      modal.find('.list-new-video').hide();
    });

    modal.on('click', '.btn-videolist-remove', function () {
      var removeValue = $(this).attr('data-value');
      var removeIndex = global.videolist.indexOf(removeValue);
      if (removeIndex >= 0) {
        global.videolist.splice(removeIndex, 1);
      }
      modal.find('#pills-videolist').html(generateVideoList());
      modal.find('.list-new-video').hide();
    });

    modal.find('.list-new-video').hide();
    modal.modal('show');

    setTimeout(function () {
      $('#form-value').focus();
    }, 500);
  }

  function showSettingsLabel(me, target, callback) {
    resetModal();
    var label = $(me).attr('data-label');
    var form = '<form><div class="form-group form-label"><label for="form">Label ' + label + '</label><input type="text" class="form-control" id="form-value" value="' + $(me).attr('data-value') + '"></div></form>';

    modal.find('.modal-title').text('Setting Label ' + label);
    modal.find('.modal-body').html(form);
    modal.find('.modal-save').click(function () {
      var value = $('#form-value').val();
      $(target).text(value);
      $(me).attr('data-value', value);

      if (typeof callback === 'function') {
        callback(value);
      }

      modal.modal('hide');
    });
    modal.modal('show');

    setTimeout(function () {
      $('#form-value').focus();
    }, 500);
  }

  function showSettingsWaktuSholat(me) {
    resetModal();
    var label = $(me).find('.time-label').attr('data-label');
    var config = global.prayer[label] || {
      label: label,
      iqomah: global.iqomahtime,
      adjustment: global.minuteadjustment,
      duration: 0
    };

    var form = '<form><div class="form-group form-label"><label for="form">Label ' + label + '</label><input type="text" class="form-control" id="form-label" value="' + config.label + '"></div>';

    if (label !== 'Terbit') {
      form += '<div class="form-group form-iqomah"><label for="form">Waktu Iqomah (menit, setelah adzan)</label><input type="text" class="form-control" id="form-iqomah" value="' + config.iqomah + '"></div>';
    }

    form += '<div class="form-group form-adjustment"><label for="form">Adjustment Waktu (menit)</label><input type="text" class="form-control" id="form-adjustment" value="' + config.adjustment + '"></div>' +
      '<div class="form-group form-adjustment"><label for="form">Durasi Sholat (menit, layar off)</label><input type="text" class="form-control" id="form-duration" value="' + config.duration + '"></div></form>';

    modal.find('.modal-title').text('Setting Waktu ' + label);
    modal.find('.modal-body').html(form);
    modal.find('.modal-save').click(function () {
      $(me).find('.time-label').text($('#form-label').val());

      var timeSettings = {
        label: $('#form-label').val(),
        iqomah: parseInt($('#form-iqomah').val(), 10) || 0,
        adjustment: parseInt($('#form-adjustment').val(), 10) || 0,
        duration: parseInt($('#form-duration').val(), 10) || 0
      };

      global.prayer[label] = timeSettings;
      saveSettings();
      modal.modal('hide');
    });
    modal.modal('show');

    setTimeout(function () {
      $('#form-iqomah').focus();
    }, 500);
  }

  function getInfoTextList(data) {
    var list = '<div class="list-group infotext-list">';

    $.each(data, function (index, value) {
      var isActive = value.enable ? 'Active' : 'Not Active';
      var badgeClass = value.enable ? 'badge-primary' : 'badge-default';

      if (index === 7) {
        list += '<button type="button" class="list-group-item list-group-item-action infotext-item" data-index="' + index + '">' + value.title + ' <small class="float-right"><i>(hanya muncul saat dzuhur di hari jumat)</i></small></button>';
      } else {
        list += '<button type="button" class="list-group-item list-group-item-action infotext-item" data-index="' + index + '">' + value.title + ' <span class="badge ' + badgeClass + ' badge-pill float-right btn-setactive">' + isActive + '</span></button>';
      }
    });

    list += '</div>';
    return list;
  }

  function getInfoTextForm(data, index) {
    var isChecked = data.enable ? 'checked' : '';
    var form = '<form class="form-infotext"><div class="row"><div class="col-6"><div class="form-group form-label"><label for="form">Judul Info</label><input type="text" class="form-control form-title" value="' + data.title + '"></div></div>' +
      '<div class="col-3"><div class="form-group form-label"><label for="form">Durasi Tayang (detik)</label><input type="text" class="form-control form-duration" value="' + data.duration + '"></div></div>' +
      '<div class="col-3"><div class="form-group form-label"><label for="form">Set Aktif/Non Aktif</label><div class="form-check"><input class="form-check-input form-enable" type="checkbox" value="' + data.enable + '" id="setActive" ' + isChecked + '><label class="form-check-label" for="setActive">Set Active</label></div></div></div></div>';

    if (index === 7) {
      form = '<form class="form-infotext"><div class="form-group">Info Khusus Saat Khotbah Jum\'at</div>';
    }

    form += '<div class="form-group"><div class="form-content" id="text-editor">' + data.content + '</div></div>' +
      '<div class="form-group form-label mt-3"><button type="button" class="btn btn-default btn-cancel">Cancel</button><button type="button" class="btn btn-primary btn-save">Save Info</button></div></form>';

    return form;
  }

  function showSettingsInfotext() {
    resetModal();
    var data = global.infotextdata;
    var editor;

    CKEDITOR.on('instanceReady', function (event) {
      editor = event.editor;
    });

    modal.find('.modal-title').text('Setting Info');
    modal.find('.modal-body').html(getInfoTextList(data));

    $('.modal-body').off('click', '.infotext-item');
    $('.modal-body').on('click', '.infotext-item', function () {
      var index = $(this).attr('data-index');
      var itemData = data[index];

      $('.form-infotext').off();
      modal.find('.modal-body').html(getInfoTextForm(itemData, Number(index)));

      $('.form-infotext').on('click', '.btn-cancel', function () {
        modal.find('.modal-body').html(getInfoTextList(data));
        if (editor && !editor.destroyed) {
          editor.destroy();
        }
      });

      $('.form-infotext').on('click', '.btn-save', function () {
        itemData.title = $('.form-infotext').find('.form-title').val();
        itemData.content = $('.form-infotext').find('.form-content').html();
        itemData.enable = $('.form-infotext').find('.form-enable').is(':checked');
        itemData.duration = $('.form-infotext').find('.form-duration').val();
        data[index] = itemData;
        modal.find('.modal-body').html(getInfoTextList(data));

        if (editor && !editor.destroyed) {
          editor.destroy();
        }
      });

      $('#text-editor').attr('contenteditable', true);
      CKEDITOR.inline('text-editor', {
        extraAllowedContent: 'a(documentation);abbr[title];code',
        removePlugins: 'stylescombo',
        extraPlugins: 'sourcedialog'
      });
    });

    modal.find('.modal-save').click(function () {
      global.infotextdata = data;
      saveSettings();
      modal.modal('hide');
    });

    modal.on('hidden.bs.modal', function () {
      if (editor && !editor.destroyed) {
        editor.destroy();
      }
    });

    modal.modal('show');
  }

  function showSettingsScrollText(me, callback) {
    resetModal();
    var form = '<form>' +
      '<div class="form-group form-label"><label for="form">Scrolling Text</label><textarea class="form-control" id="form-value">' + $(me).attr('data-value') + '</textarea></div>' +
      '<div class="form-group form-label"><label for="form">Scrolling Text Saat Sholat Berlangsung</label><textarea class="form-control" id="form-value-onpray">' + $(me).attr('data-value-onpray') + '</textarea></div>' +
      '<div class="row"><div class="form-group form-label col-6"><label for="form">Speed</label><input type="range" min="1" max="9" value="' + global.scrollingdata.speed + '" class="form-control slider" id="form-speed"></div></div></form>';

    modal.find('.modal-title').text('Setting Scrolling Text');
    modal.find('.modal-body').html(form);
    modal.find('.modal-save').click(function () {
      var value = $('#form-value').val();
      var valueOnPray = $('#form-value-onpray').val();
      var speed = $('#form-speed').val();
      var width = $(me).attr('data-width') || '100%';

      $(me).attr('data-value', value);
      $(me).attr('data-value-onpray', valueOnPray);
      $(me).attr('data-speed', speed);
      $(me).attr('data-width', width);

      var scrollingData = {
        value: value,
        valueOnPray: valueOnPray,
        speed: speed,
        width: width
      };

      if (typeof callback === 'function') {
        callback(scrollingData);
      }

      global.scrollingdata = scrollingData;
      saveSettings();
      modal.modal('hide');
    });
    modal.modal('show');

    setTimeout(function () {
      $('#form-value').focus();
    }, 500);
  }

  function bindElementInteractions() {
    $('.nama').off('click').on('click', function () {
      showSettingsLabel(this, this, function (value) {
        global.namamasjid = value;
        saveSettings();
      });
    });

    $('.alamat').off('click').on('click', function () {
      showSettingsLabel(this, this, function (value) {
        global.alamatmasjid = value;
        saveSettings();
      });
    });

    $('.info').off('click').on('click', function () {
      showSettingsInfotext(this);
    });

    $('.time-box').off('click').on('click', function () {
      if ($(this).find('.time-label').attr('data-label') !== 'Imsak') {
        showSettingsWaktuSholat(this);
      }
    });

    $('.marquee-container').off('click').on('click', function () {
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

  if (typeof firstInstallation !== 'undefined' && firstInstallation) {
    showGeneralSettings();
  }
})();
