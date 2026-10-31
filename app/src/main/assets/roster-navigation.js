(function () {
  'use strict';
  if (window.__ktdashRosterNavigation) { window.__ktdashRosterNavigation.check(); return; }
  var pending = null;
  var timeout = null;
  var notice = null;

  function clear() {
    if (timeout) clearTimeout(timeout);
    timeout = null; pending = null;
    if (notice) notice.remove();
    notice = null;
  }
  function check() {
    if (pending && location.pathname !== pending.from) clear();
  }
  function rosterLink(element) {
    var anchor = element.closest('a[href]');
    if (!anchor) return null;
    var url = new URL(anchor.href, location.href);
    return url.origin === location.origin && /^\/rosters\/[A-Za-z0-9_-]+\/?$/.test(url.pathname) &&
      !url.search && !url.hash && (!anchor.target || anchor.target === '_self') ? anchor : null;
  }
  document.addEventListener('click', function (event) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    var target = event.target instanceof Element ? event.target : null;
    if (!target) return;
    var anchor = rosterLink(target);
    if (!anchor || new URL(anchor.href).pathname === location.pathname) return;
    if (pending && pending.url === anchor.href) {
      event.preventDefault(); event.stopImmediatePropagation(); return;
    }
    clear();
    pending = { url: anchor.href, from: location.pathname };
    notice = document.createElement('div');
    notice.setAttribute('role', 'status');
    notice.id = 'ktdash-opening-roster';
    notice.textContent = 'Opening roster…';
    notice.style.cssText = 'position:fixed;top:8px;left:50%;transform:translateX(-50%);z-index:2147483647;padding:10px 18px;border:2px solid #000;border-radius:8px;background:#cf5718;color:#000;font:700 16px/1.4 sans-serif;pointer-events:none;box-shadow:0 2px 8px #0008';
    document.body.appendChild(notice);
    // Let the site's normal fast client navigation run first. If it stalls,
    // complete the exact same requested GET navigation without another tap.
    timeout = setTimeout(function () {
      if (!pending) return;
      if (location.pathname !== pending.from) { clear(); return; }
      var url = pending.url; clear(); location.assign(url);
    }, 6000);
  }, true);
  document.addEventListener('click', function (event) {
    if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    var target = event.target instanceof Element ? event.target : null;
    if (!target || target.closest('a,button,input,select,textarea,label,[role="button"],[role="menuitem"],[contenteditable="true"]')) return;
    var card = target.closest('.bg-card.group');
    if (!card) return;
    var links = Array.from(card.querySelectorAll('a[href]')).filter(function (a) { return rosterLink(a); });
    if (!links.length || links.some(function (a) { return a.href !== links[0].href; })) return;
    links[0].click();
  });
  window.addEventListener('popstate', clear);
  window.addEventListener('pageshow', clear);
  window.__ktdashRosterNavigation = { check: check };
})();
