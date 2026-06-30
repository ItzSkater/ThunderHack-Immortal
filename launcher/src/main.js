// THI Launcher frontend logic. Talks to the Rust backend via Tauri (withGlobalTauri).
const TAURI = window.__TAURI__;
const invoke = TAURI?.core?.invoke;
const listen = TAURI?.event?.listen;
const appWindow = TAURI?.window?.getCurrentWindow?.();

const $ = (id) => document.getElementById(id);

// ---------- navigation ----------
function switchPage(page) {
  document.querySelectorAll('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.page === page));
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  $(`page-${page}`).classList.add('active');
  if (page === 'updates') loadUpdates();
}
document.querySelectorAll('.nav-item').forEach(i => i.addEventListener('click', () => switchPage(i.dataset.page)));
$('gotoSettings').addEventListener('click', () => switchPage('settings'));

// ---------- window controls ----------
$('winMin').addEventListener('click', () => appWindow?.minimize());
$('winClose').addEventListener('click', () => $('closeOverlay').classList.add('show'));
$('closeNo').addEventListener('click', () => $('closeOverlay').classList.remove('show'));
$('closeYes').addEventListener('click', () => appWindow ? appWindow.close() : window.close());

// ---------- nick mirror ----------
const nickInput = $('nickInput');
nickInput.addEventListener('input', () => {
  const v = nickInput.value.trim() || '?';
  $('displayName').textContent = v;
  $('avatarLetter').textContent = v[0].toUpperCase();
  scheduleSave();
});

// ---------- toggles ----------
document.querySelectorAll('.toggle').forEach(t =>
  t.addEventListener('click', () => { t.classList.toggle('on'); scheduleSave(); })
);

// ---------- config <-> UI ----------
function uiToConfig() {
  return {
    nick: nickInput.value.trim() || 'Player',
    ram_mb: parseInt($('ramSelect').value, 10) || 4096,
    java_path: $('javaPath').value.trim(),
    jvm_args: $('jvmArgs').value,
    game_dir: $('gameDir').value.trim(),
    mc_version: '1.21',
    fabric_loader: $('fabricLoader').value.trim(),
    resolution: $('resSelect').value,
    close_on_launch: $('tgClose').classList.contains('on'),
    auto_update: $('tgAuto').classList.contains('on'),
    fullscreen: $('tgFull').classList.contains('on'),
  };
}

function configToUi(c) {
  nickInput.value = c.nick ?? 'Player';
  $('displayName').textContent = c.nick ?? 'Player';
  $('avatarLetter').textContent = (c.nick ?? 'P')[0].toUpperCase();
  if (c.ram_mb) $('ramSelect').value = String(c.ram_mb);
  $('javaPath').value = c.java_path ?? '';
  $('jvmArgs').value = c.jvm_args ?? '';
  $('gameDir').value = c.game_dir ?? '';
  $('fabricLoader').value = c.fabric_loader ?? '';
  if (c.resolution) $('resSelect').value = c.resolution;
  $('tgClose').classList.toggle('on', !!c.close_on_launch);
  $('tgAuto').classList.toggle('on', !!c.auto_update);
  $('tgFull').classList.toggle('on', !!c.fullscreen);
}

let saveTimer = null;
function scheduleSave() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => invoke?.('save_config', { cfg: uiToConfig() }).catch(() => {}), 400);
}
['ramSelect', 'javaSelect', 'javaPath', 'jvmArgs', 'gameDir', 'fabricLoader', 'resSelect']
  .forEach(id => $(id).addEventListener('change', scheduleSave));

// ---------- launch ----------
function setStatus(msg, kind) {
  const el = $('statusLine');
  el.textContent = msg || '';
  el.className = 'status-line' + (kind ? ' ' + kind : '');
  $('barStatus').textContent = msg || 'Готов к запуску';
}

$('launchBtn').addEventListener('click', async () => {
  if (!invoke) { setStatus('Запуск доступен только в приложении', 'error'); return; }
  const btn = $('launchBtn');
  btn.disabled = true;
  setStatus('Запуск...', null);
  try {
    await invoke('save_config', { cfg: uiToConfig() });
    await invoke('launch_game', { cfg: uiToConfig() });
    if (uiToConfig().close_on_launch && appWindow) setTimeout(() => appWindow.close(), 1500);
  } catch (e) {
    setStatus(String(e), 'error');
  } finally {
    btn.disabled = false;
  }
});

if (listen) {
  listen('launch://status', (ev) => {
    const p = ev.payload || {};
    setStatus(p.message || '', p.stage === 'error' ? 'error' : p.stage === 'done' ? 'ok' : null);
  });
}

// ---------- updates ----------
let updatesLoaded = false;
async function loadUpdates() {
  if (updatesLoaded || !invoke) return;
  updatesLoaded = true;
  const list = $('updatesList');
  try {
    const releases = await invoke('check_updates');
    if (!releases.length) { list.innerHTML = '<div class="update-desc">Нет релизов.</div>'; return; }
    if (releases[0]) { $('bannerVer').textContent = releases[0].tag; $('barVer').textContent = releases[0].tag; }
    list.innerHTML = releases.map((r, i) => `
      <div class="update-card">
        <div class="update-header">
          <div class="update-ver">${esc(r.tag)}</div>
          <span class="update-status ${i === 0 ? 'current' : 'old'}">${i === 0 ? 'последняя' : (r.prerelease ? 'pre-release' : 'архив')}</span>
        </div>
        <div class="update-desc">${esc(r.body || r.name || '').slice(0, 280)}</div>
      </div>`).join('');
  } catch (e) {
    updatesLoaded = false;
    list.innerHTML = `<div class="update-desc">Не удалось загрузить: ${esc(String(e))}</div>`;
  }
}
function esc(s) { return s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])); }

// ---------- module categories (static, mirrors the mod) ----------
const CATEGORIES = [
  ['Combat', 'Боевые модули', '38'], ['Movement', 'Передвижение', '28'],
  ['Render', 'Визуальные', '35'], ['Player', 'Игрок', '22'],
  ['Misc', 'Разное', '30'], ['Exploit', 'Эксплойты', '18'],
  ['Crash', 'Краш-модули', '21'], ['HUD', 'Интерфейс', '15'],
  ['World', 'Мир', '12'], ['Client', 'Клиент', '10'],
];
$('modulesGrid').innerHTML = CATEGORIES.map(([n, c, k]) => `
  <div class="module-card"><div><div class="module-name">${n}</div><div class="module-cat">${c}</div></div><div class="module-count">${k}</div></div>`).join('');

// ---------- init ----------
(async () => {
  if (invoke) {
    try { configToUi(await invoke('get_config')); } catch {}
  }
})();
