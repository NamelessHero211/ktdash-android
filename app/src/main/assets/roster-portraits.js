(function () {
  'use strict';
  if (window.__ktdashRosterPortraits) { window.__ktdashRosterPortraits.refresh(); return; }
  var routePattern = /^\/rosters\/([A-Za-z0-9_-]+)\/?$/;
  var route = null, metadata = null, metadataAttempted = false, metadataRequest = null;
  var catalog = window.__ktdashModelCatalog || null, catalogPromise = null, indexes = {};
  var dirty = new Set(), fullScan = true, frame = null, watchedImages = new WeakSet(), preferences = {};
  var prefixes = ['Aquilon ', 'Arbites ', 'Battleclade ', 'Brood Brother ', 'Death Korps ',
    'Fellgor ', 'Goremonger ', 'Hearthkyn ', 'Kabalite ', 'Kasrkin ', 'Kommando ', 'Kroot ',
    'Legionary ', 'Mandrake ', 'Navis ', 'Night Lord ', 'Novitiate ', 'Plague Marine ',
    'Plasmacyte ', 'Ratling ', 'Ravener ', 'Sanctifier ', 'Scout ', 'Space Hulk Veteran ',
    'Traitor ', 'Vespid ', 'Voidscarred ', 'Warpdiver ', 'Yaegir '];
  var css =
    '.opcard.ktdash-app-portrait>div:first-child{grid-template-columns:82px minmax(0,1fr)!important;align-items:start;}' +
    '.opcard.ktdash-app-portrait>div:first-child>:is(.col-span-3,.col-span-4){grid-column:auto!important;min-width:0;}' +
    '.ktdash-app-artwork{margin:0;grid-column:auto!important;width:82px;overflow:hidden;border:2px solid #000;border-radius:6px;background:#e58b55;color:#000;}' +
    '.ktdash-app-artwork img,.ktdash-app-model{display:block;width:78px;height:78px;object-fit:cover;}' +
    '.ktdash-app-artwork figcaption{font:700 10px/1.2 sans-serif;text-align:center;padding:3px 2px;color:#000!important;}' +
    '.ktdash-app-broken-portrait{display:none!important;}' +
    '.ktdash-app-artwork-unavailable{display:flex;align-items:center;justify-content:center;width:78px;height:78px;text-align:center;font:700 11px/1.3 sans-serif;padding:4px;color:#000;}';

  function normalize(value) { return String(value || '').toLowerCase().replace(/[^a-z0-9]/g, ''); }
  function readPreferences() {
    try {
      var value = JSON.parse(localStorage.getItem('settings') || '{}');
      preferences = value && typeof value === 'object' && !Array.isArray(value) ? value : {};
    } catch (_) { preferences = {}; }
  }
  function enableUploads() {
    if (preferences.showPortraits === true) return;
    preferences.showPortraits = true;
    try {
      var next = JSON.stringify(preferences); localStorage.setItem('settings', next);
      window.dispatchEvent(new StorageEvent('storage', { key: 'settings', newValue: next }));
    } catch (_) {}
  }
  function schedule(card) {
    if (card && card.nodeType === 1) dirty.add(card);
    if (frame !== null) return;
    frame = requestAnimationFrame(function () { frame = null; refresh(); });
  }
  function scanAll() { fullScan = true; schedule(); }
  function loadCatalog() {
    if (catalog || catalogPromise) return;
    // This is a local APK resource. Matching models needs no roster/database request.
    catalogPromise = fetch('/_ktdash_android/models/1.7/catalog.json', { credentials: 'omit', cache: 'force-cache' })
      .then(function (response) { if (!response.ok) throw new Error('Model catalog unavailable'); return response.json(); })
      .then(function (data) {
        if (!data || typeof data !== 'object') throw new Error('Invalid model catalog');
        catalog = data; window.__ktdashModelCatalog = data; indexes = {}; scanAll();
      }).catch(function () { catalog = {}; scanAll(); });
  }
  function teamDetails() {
    var links = document.querySelectorAll('a[href^="/killteams/"]'), fallback = null;
    for (var i = 0; i < links.length; i++) {
      var match = links[i].getAttribute('href').match(/^\/killteams\/([A-Za-z0-9_-]+)\/?$/);
      if (!match) continue;
      var team = { id: match[1], name: links[i].textContent.trim() || 'Kill-team', custom: false };
      if (catalog && catalog[team.id]) return team;
      if (!fallback) fallback = team;
    }
    return metadata || fallback;
  }
  function readMissingTeam(id) {
    if (metadataAttempted) return;
    metadataAttempted = true;
    var controller = new AbortController(); metadataRequest = controller;
    var timeout = setTimeout(function () { controller.abort(); }, 6000);
    fetch('/api/rosters/' + encodeURIComponent(id), { credentials: 'same-origin', signal: controller.signal })
      .then(function (response) { if (!response.ok) throw new Error('Roster unavailable'); return response.json(); })
      .then(function (data) {
        if (route !== id || metadataRequest !== controller || !data || !data.killteamId) return;
        metadata = { id: data.killteamId, name: data.killteam && data.killteam.killteamName || 'Kill-team',
          custom: !!(data.killteam && data.killteam.userId) };
      }).catch(function () {}).finally(function () {
        clearTimeout(timeout);
        if (metadataRequest === controller) { metadataRequest = null; scanAll(); }
      });
  }
  function indexFor(team) {
    if (!catalog || !catalog[team.id]) return null;
    if (indexes[team.id]) return indexes[team.id];
    var table = Object.create(null);
    function add(name, entry) {
      var key = normalize(name); if (!key) return;
      if (Object.prototype.hasOwnProperty.call(table, key) && (!table[key] || table[key].id !== entry.id)) table[key] = null;
      else table[key] = entry;
    }
    var records = catalog[team.id].ops || {};
    Object.keys(records).forEach(function (id) {
      var item = records[id]; if (!item || !item.name) return;
      var atlas = item.atlas || team.id; if (!catalog[atlas]) return;
      var entry = { id: id, item: item, atlas: atlas, sheet: catalog[atlas] }; add(item.name, entry);
      if (!team.id.includes('INQ')) {
        var short = item.name; prefixes.forEach(function (prefix) { short = short.replace(prefix, ''); });
        add(short.replace(' Pathfinder', ''), entry);
      }
    });
    indexes[team.id] = table; return table;
  }
  function modelFor(card, team) {
    var index = indexFor(team); if (!index) return null;
    var header = card.firstElementChild;
    var primary = header.querySelector('h5 span.truncate') || header.querySelector('h5');
    var secondary = header.querySelector('.text-xs.text-left') || header.querySelector('.text-muted.text-xs');
    var type = preferences.showOpTypeFirst ? primary : secondary;
    if (!type) type = primary || secondary;
    return type ? index[normalize(type.textContent.trim().replace(/^\d+\.\s*/, ''))] || null : null;
  }
  function ensureStyle() {
    if (document.getElementById('ktdash-app-roster-portraits')) return;
    var style = document.createElement('style'); style.id = 'ktdash-app-roster-portraits'; style.textContent = css;
    (document.head || document.documentElement).appendChild(style);
  }
  function addArtwork(card, header, team) {
    var loadingCatalog = !catalog && catalogPromise;
    var model = team && modelFor(card, team);
    var key = loadingCatalog ? 'catalog-loading' : model ? model.id : team ? team.id : metadataRequest ? 'team-loading' : 'unavailable';
    var old = header.querySelector('[data-ktdash-artwork]');
    if (old && old.dataset.ktdashArtwork === key) return;
    if (old) old.remove();
    var figure = document.createElement('figure'); figure.className = 'ktdash-app-artwork'; figure.dataset.ktdashArtwork = key;
    var caption = document.createElement('figcaption');
    if (loadingCatalog) {
      var waiting = document.createElement('span'); waiting.className = 'ktdash-app-artwork-unavailable';
      waiting.textContent = 'Loading photo…'; figure.appendChild(waiting); caption.textContent = 'Operative photo';
    } else if (model) {
      figure.title = 'Official miniature reference: ' + model.item.name;
      var tile = document.createElement('div'); tile.className = 'ktdash-app-model';
      tile.setAttribute('role', 'img'); tile.setAttribute('aria-label', model.item.name + ' official miniature reference');
      var scale = 78 / 256;
      tile.style.backgroundImage = 'url("/_ktdash_android/models/1.7/' + model.atlas + '.webp")';
      tile.style.backgroundRepeat = 'no-repeat';
      tile.style.backgroundSize = model.sheet.width * scale + 'px ' + model.sheet.height * scale + 'px';
      tile.style.backgroundPosition = -model.item.x * scale + 'px ' + -model.item.y * scale + 'px';
      caption.textContent = 'Model reference'; figure.appendChild(tile);
    } else if (team) {
      var heading = card.querySelector('h5'), name = heading ? heading.textContent.trim() : 'operative';
      figure.title = team.name + ' artwork — fallback for ' + name;
      var image = document.createElement('img'); image.width = 78; image.height = 78;
      image.loading = 'lazy'; image.decoding = 'async'; image.alt = team.name + ' kill-team artwork (fallback for ' + name + ')';
      var id = encodeURIComponent(team.id), urls = ['/img/killteams/' + id + '_thumb.webp', '/api/killteams/' + id + '/portrait?thumb=1'];
      if (team.custom) urls.reverse();
      var attempt = 0;
      image.addEventListener('error', function () {
        if (attempt < urls.length) image.src = urls[attempt++];
        else { var label = document.createElement('span'); label.className = 'ktdash-app-artwork-unavailable'; label.textContent = 'No artwork available'; image.replaceWith(label); }
      });
      image.src = urls[attempt++]; caption.textContent = 'Kill-team artwork'; figure.appendChild(image);
    } else {
      var label = document.createElement('span'); label.className = 'ktdash-app-artwork-unavailable';
      label.textContent = metadataRequest ? 'Loading photo…' : 'No photo available'; figure.appendChild(label); caption.textContent = 'Operative photo';
    }
    figure.appendChild(caption); header.insertBefore(figure, header.firstChild);
  }
  function decorate(card, team) {
    if (!card.isConnected) return;
    var header = card.firstElementChild; if (!header || !header.classList.contains('grid')) return;
    var image = header.querySelector('img[src*="/api/ops/"][src*="/portrait"]');
    if (image && !watchedImages.has(image)) {
      watchedImages.add(image);
      image.addEventListener('error', function () { image.dataset.ktdashFailedPortrait = image.getAttribute('src') || ''; schedule(card); });
      image.addEventListener('load', function () { delete image.dataset.ktdashFailedPortrait; schedule(card); });
      if (!image.alt) image.alt = 'Uploaded operative portrait';
    }
    var broken = image && (image.dataset.ktdashFailedPortrait === image.getAttribute('src') || (image.complete && image.naturalWidth === 0));
    if (image && !broken) {
      image.parentElement.classList.remove('ktdash-app-broken-portrait'); card.classList.remove('ktdash-app-portrait');
      var old = header.querySelector('[data-ktdash-artwork]'); if (old) old.remove();
    } else {
      if (image) image.parentElement.classList.add('ktdash-app-broken-portrait');
      card.classList.add('ktdash-app-portrait'); addArtwork(card, header, team);
    }
  }
  function observe() {
    if (document.documentElement) observer.observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['data-font'] });
  }
  function refresh() {
    if (window.__ktdashRosterNavigation) window.__ktdashRosterNavigation.check();
    var match = location.pathname.match(routePattern), id = match ? match[1] : '';
    if (id !== route) {
      if (metadataRequest) metadataRequest.abort();
      route = id; metadata = null; metadataAttempted = false; metadataRequest = null; dirty.clear(); fullScan = true; readPreferences();
    }
    if (!id || !document.body) return;
    // The settings effect marks hydration with data-font; slow images/ads need not finish.
    if (!document.body.hasAttribute('data-font') && document.readyState !== 'complete') return;
    loadCatalog(); enableUploads(); var team = teamDetails();
    if (fullScan) { document.querySelectorAll('.opcard').forEach(function (card) { dirty.add(card); }); fullScan = false; }
    if (!team && dirty.size) readMissingTeam(id);
    observer.disconnect();
    try { ensureStyle(); dirty.forEach(function (card) { decorate(card, team); }); dirty.clear(); }
    finally { observe(); }
  }
  var observer = new MutationObserver(function (records) {
    var match = location.pathname.match(routePattern), id = match ? match[1] : '';
    if (id !== route) { scanAll(); return; }
    if (!id) return;
    records.forEach(function (record) {
      if (record.type === 'attributes') { scanAll(); return; }
      var target = record.target.nodeType === 1 ? record.target : record.target.parentElement;
      var card = target && target.closest('.opcard'); if (card) schedule(card);
      record.addedNodes.forEach(function (node) {
        if (node.nodeType !== 1) return;
        if (node.matches('.opcard')) schedule(node);
        else node.querySelectorAll('.opcard').forEach(function (item) { schedule(item); });
      });
    });
  });
  window.__ktdashRosterPortraits = { refresh: scanAll, version: '1.7' };
  window.addEventListener('popstate', scanAll); window.addEventListener('pageshow', scanAll); window.addEventListener('load', scanAll);
  document.addEventListener('DOMContentLoaded', function () { observe(); scanAll(); });
  window.addEventListener('storage', function (event) { if (event.key === 'settings') { readPreferences(); scanAll(); } });
  observe(); scanAll();
})();
