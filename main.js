const {app, BrowserWindow, ipcMain, shell, Menu} = require('electron');
const path = require('path');
const fs = require('fs');

if (!app.requestSingleInstanceLock()) { app.quit(); }

let win = null;
const pending = new Map();

function esc(s){return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function isExt(u){return /^(https?|mailto|tel):/i.test(u);}

function createWindow(){
  win = new BrowserWindow({
    width: 1400, height: 900, minWidth: 900, minHeight: 650,
    show: false, backgroundColor: '#0b1426', title: 'PROTOCOL SAKHA',
    icon: path.join(__dirname, 'pdr_icon.ico'),
    webPreferences: {
      contextIsolation: true, nodeIntegration: false, sandbox: true,
      preload: path.join(__dirname, 'preload.js'), spellcheck: false
    }
  });
  win.once('ready-to-show', () => win.show());
  setTimeout(() => { if (win && !win.isDestroyed() && !win.isVisible()) win.show(); }, 5000);
  win.loadFile(path.join(__dirname, 'index.html'));
  win.webContents.setWindowOpenHandler(({url}) => { if (isExt(url)) shell.openExternal(url); return {action: 'deny'}; });
  win.webContents.on('will-navigate', (e, url) => {
    if (!url.startsWith('file://')) { e.preventDefault(); if (isExt(url)) shell.openExternal(url); }
  });
  win.webContents.on('render-process-gone', () => { try { win.reload(); } catch (e) {} });
  win.webContents.on('did-fail-load', (e, code, desc, url, isMain) => {
    if (isMain && code !== -3) setTimeout(() => { try { win.loadFile(path.join(__dirname, 'index.html')); } catch (x) {} }, 800);
  });
  win.on('closed', () => { win = null; });
}

// ---- auto-save of all app data: Documents\\PROTOCOL-SAKHA (survives uninstall / can be copied to another PC) ----
function dataDir(){ const d = path.join(app.getPath('documents'), 'PROTOCOL-SAKHA'); try { fs.mkdirSync(d, {recursive: true}); } catch (e) {} return d; }
function writeData(json){
  try {
    const d = dataDir();
    fs.writeFileSync(path.join(d, 'PROTOCOL-SAKHA-DATA.json'), json, 'utf8');
    const day = new Date().toISOString().slice(0, 10);
    const daily = path.join(d, 'backup-' + day + '.json');
    if (!fs.existsSync(daily)) {
      fs.writeFileSync(daily, json, 'utf8');
      const olds = fs.readdirSync(d).filter(f => /^backup-\d{4}-\d{2}-\d{2}\.json$/.test(f)).sort();
      while (olds.length > 30) { try { fs.unlinkSync(path.join(d, olds.shift())); } catch (e) {} }
    }
    return true;
  } catch (e) { return false; }
}
ipcMain.on('pdr-save', (e, json) => { writeData(json); });
ipcMain.on('pdr-save-sync', (e, json) => { e.returnValue = writeData(json); });
ipcMain.on('pdr-open-dir', () => { shell.openPath(dataDir()); });

// window.prompt() is not supported by Electron -> small modal window with synchronous reply
ipcMain.on('pdr-prompt', (event, msg, def) => {
  const parent = BrowserWindow.fromWebContents(event.sender);
  let done = false, pw = null;
  const finish = (v) => {
    if (done) return; done = true;
    if (pw) pending.delete(pw.webContents.id);
    event.returnValue = v;
    try { if (pw && !pw.isDestroyed()) pw.close(); } catch (e) {}
  };
  pw = new BrowserWindow({
    parent, modal: !!parent, width: 480, height: 240, resizable: false, minimizable: false, maximizable: false,
    show: false, autoHideMenuBar: true, backgroundColor: '#0b1426', title: 'PROTOCOL SAKHA',
    webPreferences: {contextIsolation: true, sandbox: true, nodeIntegration: false, preload: path.join(__dirname, 'prompt-preload.js')}
  });
  pw.removeMenu();
  const id = pw.webContents.id;
  pending.set(id, finish);
  pw.on('closed', () => finish(null));
  const html = '<!doctype html><meta charset="utf-8"><style>body{margin:0;padding:18px;background:#0b1426;color:#e5edf9;font:15px "Nirmala UI","Segoe UI",sans-serif}' +
    '#m{margin-bottom:12px;line-height:1.5;white-space:pre-wrap}input{width:100%;box-sizing:border-box;padding:10px;font-size:15px;border-radius:6px;border:1px solid #3b82f6;background:#fff;color:#111}' +
    '.b{margin-top:16px;text-align:right}button{padding:8px 22px;margin-left:8px;font-size:14px;border:0;border-radius:6px;cursor:pointer}#ok{background:#2563eb;color:#fff}#no{background:#475569;color:#fff}</style>' +
    '<div id="m">' + esc(msg) + '</div><input id="v" value="' + esc(def) + '" autofocus>' +
    '<div class="b"><button id="no">રદ કરો</button><button id="ok">ઓકે</button></div>' +
    '<script>var v=document.getElementById("v");v.focus();v.select();' +
    'document.getElementById("ok").onclick=function(){pdrPrompt.done(v.value)};' +
    'document.getElementById("no").onclick=function(){pdrPrompt.done(null)};' +
    'document.addEventListener("keydown",function(e){if(e.key==="Enter")pdrPrompt.done(v.value);if(e.key==="Escape")pdrPrompt.done(null)});<\/script>';
  pw.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(html));
  pw.once('ready-to-show', () => pw.show());
});
ipcMain.on('pdr-prompt-done', (e, v) => { const f = pending.get(e.sender.id); if (f) f(v == null ? null : String(v)); });

app.on('second-instance', () => { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
