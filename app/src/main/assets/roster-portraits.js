(function () {
  'use strict';
  if (window.__ktdashRosterPortraits) {
    window.__ktdashRosterPortraits.refresh();
    return;
  }

  var rosterPath = /^\/rosters\/([A-Za-z0-9_-]+)\/?$/;
  var currentId = '';
  var roster = null;
  var pending = false;
  var timer = null;
  var request = null;
  var watchedImages = new WeakSet();
  var observer = new MutationObserver(schedule);
  var style = document.createElement('style');
  style.id = 'ktdash-app-roster-portraits';
  style.textContent =
    '.opcard.ktdash-app-portrait>div:first-child{grid-template-columns:82px minmax(0,1fr)!important;align-items:start;}' +
    '.opcard.ktdash-app-portrait>div:first-child>:is(.col-span-3,.col-span-4){grid-column:auto!important;min-width:0;}' +
    '.ktdash-app-artwork{margin:0;grid-column:auto!important;width:82px;overflow:hidden;border:2px solid #000;border-radius:6px;background:#e58b55;color:#000;}' +
    '.ktdash-app-artwork img{display:block;width:78px;height:78px;object-fit:cover;}' +
    '.ktdash-app-artwork figcaption{font:10px/1.2 sans-serif;text-align:center;padding:3px 2px;color:#000!important;}' +
    '.ktdash-app-broken-portrait{display:none!important;}' +
    '.ktdash-app-artwork-unavailable{display:flex;align-items:center;justify-content:center;width:78px;height:78px;text-align:center;font:11px/1.3 sans-serif;padding:4px;color:#000;}';
  (document.head || document.documentElement).appendChild(style);

  function observe() {
    observer.observe(document.body || document.documentElement, {
      childList: true, subtree: true, attributes: true, attributeFilter: ['src', 'class']
    });
  }

  function schedule() {
    if (timer !== null) return;
    timer = setTimeout(function () { timer = null; refresh(); }, 150);
  }

  function enableUploadedPortraits() {
    try {
      var value = JSON.parse(localStorage.getItem('settings') || '{}');
      if (!value || typeof value !== 'object' || Array.isArray(value)) value = {};
      if (value.showPortraits === true) return;
      value.showPortraits = true;
      var next = JSON.stringify(value);
      localStorage.setItem('settings', next);
      window.dispatchEvent(new StorageEvent('storage', { key: 'settings', newValue: next }));
    } catch (_) { /* A storage restriction must not break the roster. */ }
  }

  function fetchRoster(id) {
    pending = true;
    var controller = new AbortController();
    request = controller;
    var timeout = setTimeout(function () { controller.abort(); }, 10000);
    fetch('/api/rosters/' + encodeURIComponent(id), {
      credentials: 'same-origin', signal: controller.signal,
      headers: { Accept: 'application/json' }
    }).then(function (response) {
      if (!response.ok) throw new Error('Roster unavailable');
      return response.json();
    }).then(function (data) {
      if (currentId !== id || request !== controller) return;
      if (!data || !data.killteamId) return;
      roster = data;
    }).catch(function () {
      // Leave existing portraits alone if roster details cannot be read.
    }).finally(function () {
      clearTimeout(timeout);
      if (currentId === id && request === controller) {
        pending = false;
        request = null;
        schedule();
      }
    });
  }

  function teamDetails() {
    if (roster) return {
      id: roster.killteamId,
      name: roster.killteam && roster.killteam.killteamName || 'Kill-team',
      custom: !!(roster.killteam && roster.killteam.userId)
    };
    // This also allows a labeled fallback when the metadata request is unavailable.
    var link = document.querySelector('a[href^="/killteams/"]');
    var match = link && link.getAttribute('href').match(/^\/killteams\/([A-Za-z0-9_-]+)\/?$/);
    return match ? { id: match[1], name: link.textContent.trim() || 'Kill-team', custom: false } : null;
  }

  function addArtwork(card, header, team) {
    var old = header.querySelector('[data-ktdash-artwork]');
    if (old && old.dataset.ktdashArtwork === team.id) return;
    if (old) old.remove();
    var heading = card.querySelector('h5');
    var name = heading ? heading.textContent.trim() : 'operative';
    var figure = document.createElement('figure');
    figure.className = 'ktdash-app-artwork';
    figure.dataset.ktdashArtwork = team.id;
    figure.title = team.name + ' artwork — fallback for ' + name;
    var image = document.createElement('img');
    image.width = 78; image.height = 78;
    image.loading = 'lazy'; image.decoding = 'async';
    image.alt = team.name + ' kill-team artwork (fallback for ' + name + ')';
    var id = encodeURIComponent(team.id);
    var urls = ['/img/killteams/' + id + '_thumb.webp', '/api/killteams/' + id + '/portrait?thumb=1'];
    if (team.custom) urls.reverse();
    var nextUrl = 0;
    var caption = document.createElement('figcaption');
    caption.textContent = 'Kill-team artwork';
    image.addEventListener('error', function () {
      if (nextUrl < urls.length) {
        image.src = urls[nextUrl++];
      } else {
        var unavailable = document.createElement('span');
        unavailable.className = 'ktdash-app-artwork-unavailable';
        unavailable.textContent = 'No artwork available';
        image.replaceWith(unavailable);
      }
    });
    image.src = urls[nextUrl++];
    figure.appendChild(image); figure.appendChild(caption);
    header.insertBefore(figure, header.firstChild);
  }

  function refresh() {
    var match = location.pathname.match(rosterPath);
    var id = match ? match[1] : '';
    if (id !== currentId) {
      if (request) request.abort();
      currentId = id; roster = null; pending = false; request = null;
      if (id) fetchRoster(id);
    }
    if (!id) return;
    enableUploadedPortraits();
    var team = teamDetails();
    observer.disconnect();
    try {
      document.querySelectorAll('.opcard').forEach(function (card) {
        var header = card.firstElementChild;
        if (!header || !header.classList.contains('grid')) return;
        var nativeImage = header.querySelector('img[src*="/api/ops/"][src*="/portrait"]');
        if (nativeImage && !watchedImages.has(nativeImage)) {
          watchedImages.add(nativeImage);
          nativeImage.addEventListener('error', function () {
            nativeImage.dataset.ktdashFailedPortrait = nativeImage.getAttribute('src') || '';
            schedule();
          });
          nativeImage.addEventListener('load', function () {
            delete nativeImage.dataset.ktdashFailedPortrait; schedule();
          });
          if (!nativeImage.alt) nativeImage.alt = 'Uploaded operative portrait';
        }
        var broken = nativeImage && (
          nativeImage.dataset.ktdashFailedPortrait === nativeImage.getAttribute('src') ||
          (nativeImage.complete && nativeImage.naturalWidth === 0)
        );
        if (nativeImage && !broken) {
          nativeImage.parentElement.classList.remove('ktdash-app-broken-portrait');
          card.classList.remove('ktdash-app-portrait');
          var fallback = header.querySelector('[data-ktdash-artwork]');
          if (fallback) fallback.remove();
        } else if (team) {
          if (nativeImage) nativeImage.parentElement.classList.add('ktdash-app-broken-portrait');
          card.classList.add('ktdash-app-portrait');
          addArtwork(card, header, team);
        }
      });
    } finally { observe(); }
  }

  window.__ktdashRosterPortraits = { refresh: schedule };
  window.addEventListener('popstate', schedule);
  window.addEventListener('pageshow', schedule);
  window.addEventListener('storage', function (event) { if (event.key === 'settings') schedule(); });
  observe(); schedule();
})();
