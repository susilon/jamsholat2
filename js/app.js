/*
 TODO :
 video play list settings
 alarm
*/
/*
Jam Sholat 2 Beta ver 270826 
Author : susilonurcahyo@gmail.com

Copyright 2021 Susilo Nurcahyo

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Credits :
Adhan -  MIT License
Bootstrap -  MIT License
jQuery -  MIT License
Moment js -  MIT License
Moment-Hijri -  MIT License
The Roboto Light Fonts -  Apache License, Version 2.0.
CKEditor 4
*/

console.log('app starting...');

var global = readConfig();
var firstInstallation = global === null;
var audioCtx = null;
var beepCtr = 0;
var beepHandler = null;
var infoCtr = 0;

if (global === null) {
    firstInstallation = true;
    global = buildDefaultConfig();
    saveConfig(global);
}

function buildDefaultConfig() {
    return {
        locale: 'id',
        calculation: 0,
        latlngdata: '-6.224655537226517, 106.80679437749554',
        fajrAngle: 19.5,
        ishaAngle: 17.5,
        madhab: 'syafii',
        minuteadjustment: 2,
        imsak: 10,
        timeintowarning: 10,
        prayertimewarning: 1,
        isadhan: false,
        iqomahtime: 5,
        isiqomah: false,
        prayduration: 10,
        ispraying: false,
        isjumat: false,
        currentpray: '',
        namamasjid: 'Nama Masjid',
        alamatmasjid: 'Alamat Lengkap dan Nomor Telepon.',
        prayer: {
            Subuh: { label: 'Subuh', iqomah: 5, adjustment: 2, duration: 5 },
            Terbit: { label: 'Terbit', iqomah: 5, adjustment: 2, duration: 0 },
            Dzuhur: { label: 'Dzuhur', iqomah: 5, adjustment: 2, duration: 10 },
            Ashar: { label: 'Ashar', iqomah: 5, adjustment: 2, duration: 10 },
            Maghrib: { label: 'Maghrib', iqomah: 5, adjustment: 2, duration: 5 },
            Isya: { label: 'Isya', iqomah: 5, adjustment: 2, duration: 10 }
        },
        infotextinterval: 5,
        infotextdata: [
            {
                title: 'Hadist Ilmu',
                content: '<span style="font-size: 39px;">\
            مَنْ سَلَكَ طَرِيْقًايَلْتَمِسُ فِيْهِ عِلْمًا,سَهَّلَ اللهُ لَهُ طَرِيْقًا إِلَى الجَنَّةِ . رَوَاهُ مُسْلِم\
            </span><br>\
            Barang siapa menempuh satu jalan (cara) untuk mendapatkan ilmu, maka Allah pasti mudahkan baginya jalan menuju surga." (HR. Muslim)',
                enable: true,
                duration: 10
            },
            {
                title: 'Iklan Jam Sholat 2',
                content: '<img src="images/android.png" height="150px"><br><span style="font-size: 39px;">Jam Sholat 2</span><br>Yuk kita bikin petunjuk waktu sholat dengan mudah.<br>jamsholat2.susilon.com',
                enable: true,
                duration: 5
            },
            { title: 'Slot kosong', content: '', enable: false, duration: 5 },
            { title: 'Slot kosong', content: '', enable: false, duration: 5 },
            { title: 'Slot kosong', content: '', enable: false, duration: 5 },
            { title: 'Slot kosong', content: '', enable: false, duration: 5 },
            { title: 'Slot kosong', content: '', enable: false, duration: 5 },
            {
                title: 'Info Khusus Saat Khotbah Jumat',
                content: '<span style="font-size:39px">\
            إذا قلت لصاحبك يوم الجمعة أنصت والإمام يخطب فقد لغوت\
            </span><br>Jika engkau berkata kepada temanmu pada hari jum’at, ‘diam dan perhatikanlah’, sedangkan imam sedang berkhutbah, maka engkau telah berbuat sia-sia.” (HR. Al-Bukhari [934].\
            ',
                enable: false,
                duration: 0
            }
        ],
        scrollingdata: {
            value: 'Scrolling Text, Klik disini untuk mengganti text, dan juga klik di area yang akan diedit.',
            valueonpray: 'Rapat dan luruskan barisan demi kesempurnaan sholat kita.',
            speed: '5',
            width: '100%'
        },
        beep: {
            beepTimes: 5,
            beepVolume: 0.5,
            beepFrequency: 4000,
            beepType: 'square',
            beepDuration: 150
        },
        videolist: ['tawaf.mp4']
    };
}

function readConfig() {
    try {
        if (!window.localStorage || !window.localStorage.configuration) {
            return null;
        }
        return JSON.parse(window.localStorage.configuration);
    } catch (error) {
        console.warn('Unable to read configuration:', error);
        return null;
    }
}

function saveConfig(configData) {
    try {
        window.localStorage.configuration = JSON.stringify(configData);
        return true;
    } catch (error) {
        console.warn('Unable to save configuration:', error);
        return false;
    }
}

function ensureAudioContext() {
    if (!audioCtx) {
        var AudioCtor = window.AudioContext || window.webkitAudioContext;
        if (AudioCtor) {
            audioCtx = new AudioCtor();
        }
    }
    return audioCtx;
}

function beep(v, f, t, d) {
    var context = ensureAudioContext();
    if (!context) {
        return;
    }

    var oscillator = context.createOscillator();
    var gainNode = context.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(context.destination);

    gainNode.gain.value = v;
    oscillator.frequency.value = f;
    oscillator.type = 'square';

    oscillator.start();

    setTimeout(function () {
        oscillator.stop();
    }, d);
}

function doubleBeep() {
    var settings = global.beep;
    beep(settings.beepVolume, settings.beepFrequency, settings.beepType, 120);
    setTimeout(function () {
        beep(settings.beepVolume, settings.beepFrequency, settings.beepType, 120);
    }, 200);
}

function longBeep() {
    var settings = global.beep;
    beep(settings.beepVolume, settings.beepFrequency, settings.beepType, 1000);
}

function multipleBeep() {
    clearInterval(beepHandler);
    var settings = global.beep;
    beepHandler = setInterval(function () {
        beep(settings.beepVolume, settings.beepFrequency, settings.beepType, settings.beepDuration);
        beepCtr++;
        if (beepCtr >= settings.beepTimes) {
            clearInterval(beepHandler);
            beepCtr = 0;
        }
    }, 1000);
}

function playBeep(type) {
    var settings = global.beep;
    switch (type) {
        case 'l':
            longBeep();
            break;
        case 'm':
            multipleBeep();
            break;
        case 's':
            beep(settings.beepVolume, settings.beepFrequency, settings.beepType, settings.beepDuration);
            break;
        case 'd':
            doubleBeep();
            break;
        default:
            break;
    }
}

function setDateTime() {
    var tanggal = moment().format('D MMMM YYYY, HH:mm:ss');
    var tanggalHijri = moment().format('iD iMMMM iYYYY, HH:mm:ss');

    if (moment().format('s') < 30) {
        $('.datetime').html(tanggal);
    } else {
        $('.datetime').html(tanggalHijri);
    }
}

function ticker() {
    setTimeout(function () {
        setDateTime();
        setPrayerTimes();
        ticker();
    }, 1000);
}

function getPrayer(prayname) {
    switch (prayname) {
        case 'fajr':
            return global.prayer.Subuh;
        case 'sunrise':
            return global.prayer.Terbit;
        case 'dhuhr':
            return global.prayer.Dzuhur;
        case 'asr':
            return global.prayer.Ashar;
        case 'maghrib':
            return global.prayer.Maghrib;
        case 'isha':
            return global.prayer.Isya;
        default:
            return null;
    }
}

function getTimeBox(prayname) {
    var timebox = $('.time-box');
    switch (prayname) {
        case 'fajr':
            timebox = $('.subuh').parent();
            break;
        case 'sunrise':
            timebox = $('.terbit').parent();
            break;
        case 'dhuhr':
            timebox = $('.dzuhur').parent();
            break;
        case 'asr':
            timebox = $('.ashar').parent();
            break;
        case 'maghrib':
            timebox = $('.maghrib').parent();
            break;
        case 'isha':
            timebox = $('.isya').parent();
            break;
        case 'imsak':
            timebox = $('.imsak').parent();
            break;
        default:
            break;
    }
    return timebox;
}

function setPrayerTimes() {
    var now = new Date();
    var hari = moment(now).format('dddd');
    global.isjumat = (hari === 'Jumat');

    var latlng = global.latlngdata.split(',');
    var coordinates = new adhan.Coordinates(latlng[0].trim(), latlng[1].trim());
    var params = adhan.CalculationMethod.Egyptian();
    var timedifference = now.getTimezoneOffset();
    var diffsign = timedifference < 0 ? '+' : '-';

    switch (global.calculation) {
        case 0:
            params = adhan.CalculationMethod.Egyptian();
            break;
        case 1:
            params = adhan.CalculationMethod.MuslimWorldLeague();
            break;
        case 2:
            params = adhan.CalculationMethod.Karachi();
            break;
        case 3:
            params = adhan.CalculationMethod.UmmAlQura();
            break;
        case 4:
            params = adhan.CalculationMethod.Singapore();
            break;
        case 5:
            params = adhan.CalculationMethod.NorthAmerica();
            break;
        case 6:
            params = adhan.CalculationMethod.Dubai();
            break;
        case 7:
            params = adhan.CalculationMethod.Qatar();
            break;
        case 8:
            params = adhan.CalculationMethod.Kuwait();
            break;
        case 9:
            params = adhan.CalculationMethod.MoonsightingCommittee();
            break;
        default:
            params = adhan.CalculationMethod.Egyptian();
            break;
    }

    params.fajrAngle = global.fajrAngle;
    params.ishaAngle = global.ishaAngle;
    params.madhab = global.madhab === 'hanafi' ? adhan.Madhab.Hanafi : adhan.Madhab.Shafi;

    params.adjustments.fajr = global.prayer.Subuh.adjustment;
    params.adjustments.sunrise = global.prayer.Terbit.adjustment;
    params.adjustments.dhuhr = global.prayer.Dzuhur.adjustment;
    params.adjustments.asr = global.prayer.Ashar.adjustment;
    params.adjustments.maghrib = global.prayer.Maghrib.adjustment;
    params.adjustments.isha = global.prayer.Isya.adjustment;

    var prayerTimes = new adhan.PrayerTimes(coordinates, now, params);

    var imsakTime = moment(prayerTimes.fajr).add(-(global.imsak), 'minutes').format('HH:mm');
    var fajrTime = moment(prayerTimes.fajr).format('HH:mm');
    var sunriseTime = moment(prayerTimes.sunrise).format('HH:mm');
    var dhuhrTime = moment(prayerTimes.dhuhr).format('HH:mm');
    var asrTime = moment(prayerTimes.asr).format('HH:mm');
    var maghribTime = moment(prayerTimes.maghrib).format('HH:mm');
    var ishaTime = moment(prayerTimes.isha).format('HH:mm');

    var current = prayerTimes.currentPrayer(now);
    var next = prayerTimes.nextPrayer(now);
    global.currentpray = current;

    if (current === 'none') {
        current = 'isha';
    }
    if (next === 'none') {
        next = 'fajr';
    }

    var timepassed = now.getTime() - prayerTimes[current].getTime();
    var timeinto = prayerTimes[next].getTime() - now.getTime();

    if (prayerTimes.currentPrayer() === 'none') {
        timepassed = now.getTime() - moment(prayerTimes.isha).add(-1, 'days').toDate().getTime();
    }
    if (prayerTimes.nextPrayer() === 'none') {
        timeinto = moment(prayerTimes.fajr).add(1, 'days').toDate().getTime() - now.getTime();
    }

    var currentTimeBox = getTimeBox(current);
    var nextTimeBox = getTimeBox(next);
    var allTimeBox = $('.time-box');

    allTimeBox.find('.time-data').css('background-color', 'rgba(0,0,0,0.5)');
    allTimeBox.removeClass('pray-active');
    allTimeBox.removeClass('blink-red');
    allTimeBox.removeClass('blink-green');

    currentTimeBox.addClass('pray-active');

    if (Math.floor(timeinto / 1000 / 60) <= global.timeintowarning) {
        console.log(Math.floor(timeinto / 1000 / 60) + ' menit lagi ' + next);
        nextTimeBox.addClass('blink-green');
        nextTimeBox.find('.time-data').css('background-color', 'rgba(0,0,0,0)');

        if (next === 'fajr') {
            var imsakBox = getTimeBox('imsak');
            imsakBox.addClass('blink-red');
            imsakBox.find('.time-data').css('background-color', 'rgba(0,0,0,0)');
        }
    }

    if (Math.floor(timepassed / 1000 / 60) === 0) {
        if (current !== 'sunrise') {
            if (!global.isadhan) {
                global.isadhan = true;
                playBeep('l');
            } else {
                console.log('waktunya ' + current);
            }
        }
    }

    if (Math.floor(timepassed / 1000 / 60) <= global.prayertimewarning) {
        console.log(Math.floor(timepassed / 1000 / 60) + ' menit yang lalu');
        currentTimeBox.addClass('blink-red');
        currentTimeBox.find('.time-data').css('background-color', 'rgba(0,0,0,0)');
    }

    var currentIqomahTime = getPrayer(current).iqomah;
    if (Math.floor(timepassed / 1000 / 60) === currentIqomahTime) {
        if (current !== 'sunrise') {
            if (!global.isiqomah) {
                global.isiqomah = true;
                playBeep('m');
                setScrollingText(global.scrollingdata);
            } else {
                global.isadhan = false;
                console.log('waktunya iqomah ' + current);
            }
        }
    }

    var currentPrayDuration = currentIqomahTime + getPrayer(current).duration;
    if ((Math.floor(timepassed / 1000 / 60) > currentIqomahTime) && Math.floor(timepassed / 1000 / 60) <= currentPrayDuration) {
        console.log('waktunya sholat ' + current);
        if (current !== 'sunrise') {
            if (global.isiqomah || !global.ispraying) {
                global.isiqomah = false;
                global.ispraying = true;
                $('.hide-onpray').css('display', 'none');
                setScrollingText(global.scrollingdata);
            }
        }
    }

    if (Math.floor(timepassed / 1000 / 60) > currentPrayDuration) {
        if (global.ispraying === true) {
            $('.hide-onpray').css('display', '');
            global.ispraying = false;
            setScrollingText(global.scrollingdata);
        }
    }

    $('.timezone-gmt').text('GMT' + diffsign + (Math.abs(timedifference) / 60));
    $('.imsak').text(imsakTime);
    $('.subuh').text(fajrTime);
    $('.terbit').text(sunriseTime);
    $('.dzuhur').text(dhuhrTime);
    $('.ashar').text(asrTime);
    $('.maghrib').text(maghribTime);
    $('.isya').text(ishaTime);
}

function setScrollingText(data) {
    data = data || global.scrollingdata;

    var me = $('.marquee-container');
    var target = me.find('.marquee').find('span');
    var target2 = me.find('.marquee2').find('span');
    var value = data.value || '';
    var valueOnPray = data.valueonpray || data.valueOnPray || '';
    var speedValue = Number(data.speed) || 5;
    var speed = Math.max(1, Math.min(9, speedValue));

    me.attr('data-value', value);
    me.attr('data-value-onpray', valueOnPray);
    me.attr('data-speed', speed);
    me.attr('data-width', data.width || '100%');

    var text = '<small>jamsholat.id</small>- ' + value.split('\n').join(' -<small>jamsholat.id</small>- ') + ' -';
    if (global.ispraying || global.isiqomah) {
        text = '- ' + valueOnPray.split('\n').join(' -<small>jamsholat.id</small>- ') + ' -';
    }

    var animationSeconds = ((10 - speed) / 10) * text.length / 2;
    if (animationSeconds <= 0) {
        animationSeconds = 1;
    }

    target.html(text);
    target.css('animation', 'marquee ' + animationSeconds + 's linear infinite');
    target2.css('animation-delay', (animationSeconds / 2) + 's');
}

function tickerInfo() {
    if (global.isjumat && global.currentpray === 'dhuhr') {
        var infoJumat = global.infotextdata[7].content;
        $('.content').find('.info').html(infoJumat);
        setTimeout(function () {
            infoCtr++;
            if (infoCtr >= global.infotextdata.length) {
                infoCtr = 0;
            }
            tickerInfo();
        }, 60000);
        return;
    }

    var item = global.infotextdata[infoCtr] || global.infotextdata[0];
    var duration = item.enable ? item.duration : 0;
    $('.content').find('.info').html(item.content);

    setTimeout(function () {
        infoCtr++;
        if (infoCtr >= global.infotextdata.length) {
            infoCtr = 0;
        }
        tickerInfo();
    }, duration * 1000);
}

function initializeDisplay() {
    $('.nama').text(global.namamasjid);
    $('.nama').attr('data-value', global.namamasjid);
    $('.alamat').text(global.alamatmasjid);
    $('.alamat').attr('data-value', global.alamatmasjid);

    $.each(global.prayer, function (index, value) {
        $('.time-box').each(function () {
            if ($(this).find('.time-label').attr('data-label') === index) {
                $(this).find('.time-label').text(value.label);
            }
        });
    });

    var videoPlayer = document.getElementById('bg-video');
    var imagePlayer = document.getElementById('bg-image');
    var backgroundItems = global.videolist && global.videolist.length ? global.videolist : ['tawaf.mp4'];
    var backgroundCtr = 0;
    var imageTimer;
    var fadeTimer;

    function isImageBackground(item) {
        return /\.(avif|gif|jpe?g|png|webp)(\?.*)?$/i.test(item) ||
            (/^https?:\/\//i.test(item) && !isVideoBackground(item));
    }

    function isVideoBackground(item) {
        return /\.(mp4|m4v|webm|mov)(\?.*)?$/i.test(item) || /\/download\/video(?:\/|$)/i.test(item);
    }

    function getBackgroundSource(item) {
        return /^https?:\/\//i.test(item) ? item : 'videos/' + item;
    }

    function showNextBackground() {
        clearTimeout(imageTimer);
        clearTimeout(fadeTimer);
        if (!backgroundItems.length) {
            return;
        }

        var item = backgroundItems[backgroundCtr];
        var source = getBackgroundSource(item);
        backgroundCtr = (backgroundCtr + 1) % backgroundItems.length;

        if (isImageBackground(item)) {
            if (videoPlayer) {
                videoPlayer.pause();
                videoPlayer.onended = null;
                videoPlayer.onerror = null;
            }
            if (imagePlayer) {
                imagePlayer.onerror = showNextBackground;
                imagePlayer.onload = function () {
                    imagePlayer.style.opacity = '1';
                    if (videoPlayer) {
                        fadeTimer = setTimeout(function () {
                            videoPlayer.style.display = 'none';
                            videoPlayer.style.opacity = '0';
                            videoPlayer.removeAttribute('src');
                            videoPlayer.load();
                        }, 700);
                    }
                    imageTimer = setTimeout(showNextBackground, 10000);
                };
                var imageWasHidden = imagePlayer.style.display === 'none';
                imagePlayer.style.display = 'block';
                if (imageWasHidden) {
                    imagePlayer.style.opacity = '0';
                }
                imagePlayer.src = source;
            }
            console.log('showing image ' + source + ' for 10 seconds');
            return;
        }

        if (imagePlayer) {
            imagePlayer.onload = null;
            imagePlayer.onerror = null;
        }
        if (!isVideoBackground(item)) {
            console.warn('Unsupported background format:', item);
            showNextBackground();
            return;
        }
        if (videoPlayer) {
            videoPlayer.onended = function () {
                videoPlayer.style.opacity = '0';
                fadeTimer = setTimeout(showNextBackground, 700);
            };
            videoPlayer.onerror = showNextBackground;
            videoPlayer.oncanplay = function () {
                videoPlayer.style.display = 'block';
                videoPlayer.style.opacity = '1';
                if (imagePlayer) {
                    fadeTimer = setTimeout(function () {
                        imagePlayer.style.display = 'none';
                        imagePlayer.style.opacity = '0';
                        imagePlayer.removeAttribute('src');
                    }, 700);
                }
            };
            videoPlayer.style.display = 'block';
            videoPlayer.style.opacity = '0';
            videoPlayer.src = source;
            videoPlayer.load();
            videoPlayer.play().catch(function () {});
            console.log('playing ' + source + ' until it ends');
        }
    }

    showNextBackground();

    setScrollingText(global.scrollingdata);
    tickerInfo();
    ticker();
}

moment.locale(global.locale);

$(document).ready(function () {
    console.log('ready');
    ensureAudioContext();
    global.isadhan = false;
    global.isiqomah = false;
    global.ispraying = false;
    initializeDisplay();
    playBeep('l');
});
