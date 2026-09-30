const {contextBridge, ipcRenderer} = require('electron');
contextBridge.exposeInMainWorld('pdrPrompt', {
  done: (v) => ipcRenderer.send('pdr-prompt-done', v)
});
