(function () {
  'use strict';
  if (window.__ktdashPictureViewer) return;
  var viewer = null, previousFocus = null;
  function close() {
    if (!viewer) return false;
    viewer.remove(); viewer = null;
    if (previousFocus && previousFocus.isConnected) previousFocus.focus();
    previousFocus = null; return true;
  }
  function open(source, title, model) {
    close(); previousFocus = document.activeElement;
    var overlay = document.createElement('div');
    overlay.setAttribute('role', 'dialog'); overlay.setAttribute('aria-modal', 'true'); overlay.setAttribute('aria-label', title);
    overlay.style.cssText = 'position:fixed;top:0;right:0;bottom:0;left:0;z-index:2147483647;background:#000e;color:#fff;overflow:auto;padding:80px 16px 24px;overscroll-behavior:contain';
    var controls = document.createElement('div');
    controls.style.cssText = 'position:fixed;top:12px;left:12px;right:12px;display:flex;gap:8px;justify-content:center;z-index:1';
    var stage = document.createElement('div');
    stage.style.cssText = 'display:grid;justify-items:center;gap:12px;min-width:100%;width:max-content';
    var label = document.createElement('div'); label.textContent = title;
    label.style.cssText = 'color:#fff!important;font:700 18px/1.4 sans-serif;text-align:center;max-width:90vw';
    var media = document.createElement(model ? 'div' : 'img');
    media.style.cssText = 'flex:none;background-repeat:no-repeat;object-fit:contain;max-width:none';
    if (model) { media.style.backgroundImage = 'url("' + source + '")'; media.setAttribute('role', 'img'); media.setAttribute('aria-label', title); }
    else {
      media.src = source; media.alt = title;
      // Open the cached thumbnail immediately, then upgrade uploaded portraits in place.
      try {
        var full = new URL(source, location.href);
        if (full.origin === location.origin && /\/api\/ops\/[^/]+\/portrait$/.test(full.pathname) && full.searchParams.has('thumb')) {
          full.searchParams.delete('thumb');
          var original = new Image();
          original.onload = function () { if (viewer === overlay) media.src = full.href; };
          original.src = full.href;
        }
      } catch (_) {}
    }
    var zoom = 1, base = Math.min(Math.max(window.innerWidth - 40, 240), 512);
    function resize() {
      var size = Math.round(base * zoom); media.style.width = size + 'px'; media.style.height = size + 'px';
      if (model) {
        var factor = size / 256;
        media.style.backgroundSize = model.sheet.width * factor + 'px ' + model.sheet.height * factor + 'px';
        media.style.backgroundPosition = -model.item.x * factor + 'px ' + -model.item.y * factor + 'px';
      }
    }
    function button(text, name, action) {
      var b = document.createElement('button'); b.type = 'button'; b.textContent = text; b.setAttribute('aria-label', name);
      b.style.cssText = 'background:#cf5718;color:#000;border:2px solid #000;border-radius:8px;padding:10px 16px;font:700 16px sans-serif;min-height:44px';
      b.addEventListener('click', action); controls.appendChild(b); return b;
    }
    button('−', 'Zoom out', function () { zoom = Math.max(1, zoom - 0.5); resize(); });
    button('+', 'Zoom in', function () { zoom = Math.min(3, zoom + 0.5); resize(); });
    var closer = button('Close', 'Close picture', close);
    overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
    overlay.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') { event.preventDefault(); close(); }
      if (event.key === 'Tab') {
        var buttons = controls.querySelectorAll('button');
        if (event.shiftKey && document.activeElement === buttons[0]) { event.preventDefault(); buttons[buttons.length - 1].focus(); }
        else if (!event.shiftKey && document.activeElement === buttons[buttons.length - 1]) { event.preventDefault(); buttons[0].focus(); }
      }
    });
    stage.appendChild(label); stage.appendChild(media); overlay.appendChild(controls); overlay.appendChild(stage); resize();
    document.body.appendChild(overlay); viewer = overlay; closer.focus();
  }
  function bind(figure, action, label) {
    figure.setAttribute('role', 'button'); figure.tabIndex = 0; figure.setAttribute('aria-label', label);
    figure.style.cursor = 'zoom-in'; figure.style.touchAction = 'manipulation';
    figure.addEventListener('click', function (event) { event.preventDefault(); event.stopPropagation(); action(); });
    figure.addEventListener('keydown', function (event) {
      if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); event.stopPropagation(); action(); }
    });
  }
  document.addEventListener('click', function (event) {
    var image = event.target instanceof Element ? event.target.closest('.opcard img[src*="/api/ops/"][src*="/portrait"]') : null;
    if (!image || !image.complete || !image.naturalWidth) return;
    event.preventDefault(); event.stopImmediatePropagation(); open(image.currentSrc || image.src, image.alt || 'Operative portrait');
  }, true);
  window.addEventListener('popstate', close);
  window.__ktdashPictureViewer = { open: open, close: close, bind: bind };
})();
