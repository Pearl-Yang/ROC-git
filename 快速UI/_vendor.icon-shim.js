/*!
 * _vendor.icon-shim.js
 * --------------------------------------------------------------------
 * 把页面里所有 <iconify-icon icon="lucide:xxx"> 替换成本地 SVG sprite
 * 零外网，完全离线。
 * --------------------------------------------------------------------
 */
(function () {
  'use strict';

  // 老名 -> 新名（iconify-api 返回的 aliases）
  var ALIAS = {
    'alert-circle': 'circle-alert',
    'alert-triangle': 'triangle-alert',
    'building-2': 'building-complex',
    'check-circle': 'circle-check-big',
    'check-circle-2': 'circle-check',
    'check-square': 'square-check-big',
    'download-cloud': 'cloud-download',
    'edit-3': 'pen-line',
    'git-commit': 'git-commit-horizontal',
    'help-circle': 'circle-question-mark',
    'history': 'rotate-ccw-clock',
    'home': 'house',
    'more-horizontal': 'ellipsis',
    'more-vertical': 'ellipsis-vertical',
    'plus-circle': 'circle-plus',
    'trash-2': 'trash',
    'upload-cloud': 'cloud-upload'
  };

  function resolve(name) { return ALIAS[name] || name; }

  function nodeToAttrs(node) {
    // node 形如 ["path", { d: "..." }] 或 ["circle", { cx: 0, ... }]
    var tag = node[0];
    var a = node[1] || {};
    var out = [];
    for (var k in a) {
      var v = a[k];
      if (v === true) out.push(k);
      else if (v !== false && v != null) out.push(k + '="' + String(v).replace(/"/g, '&quot;') + '"');
    }
    return '<' + tag + ' ' + out.join(' ') + ' />';
  }

  function buildSymbol(name, nodes) {
    var body = (nodes || []).map(nodeToAttrs).join('');
    // lucide 24x24 viewBox
    return '<symbol id="ico-' + name + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + body + '</symbol>';
  }

  function replaceAll(icons) {
    var sprite = '<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" style="position:absolute;width:0;height:0;overflow:hidden" aria-hidden="true">';
    var keys = Object.keys(icons);
    for (var i = 0; i < keys.length; i++) {
      sprite += buildSymbol(keys[i], icons[keys[i]]);
    }
    sprite += '</svg>';
    var div = document.createElement('div');
    div.innerHTML = sprite;
    document.body.appendChild(div.firstChild);

    // 替换所有 <iconify-icon>
    var nodes = document.querySelectorAll('iconify-icon');
    for (var j = 0; j < nodes.length; j++) {
      var el = nodes[j];
      var iconAttr = el.getAttribute('icon') || '';
      var m = iconAttr.match(/^lucide:([^:]+)$/);
      if (!m) continue;
      var name = resolve(m[1]);
      var w = el.getAttribute('width') || '20';
      var h = el.getAttribute('height') || w;
      var cls = el.getAttribute('class') || '';
      // 保留所有属性：aria-label 等
      var otherAttrs = '';
      for (var k = 0; k < el.attributes.length; k++) {
        var a = el.attributes[k];
        if (a.name === 'icon' || a.name === 'width' || a.name === 'height' || a.name === 'class') continue;
        otherAttrs += ' ' + a.name + '="' + a.value + '"';
      }
      var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + w + '" height="' + h + '" class="' + cls + '"' + otherAttrs + '><use href="#ico-' + name + '"></use></svg>';
      var wrapper = document.createElement('span');
      wrapper.innerHTML = svg;
      el.parentNode.replaceChild(wrapper.firstChild, el);
    }
  }

  function init() {
    fetch('./_vendor.icons.json')
      .then(function (r) { return r.json(); })
      .then(replaceAll)
      .catch(function (e) {
        console.error('[icon-shim] 加载本地图标失败：', e);
      });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
