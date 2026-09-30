// Copies jsPDF + html2canvas (from node_modules) into a lib folder so PDF/photo features work OFFLINE.
const fs = require('fs'), path = require('path');
const dest = path.resolve(process.argv[2] || path.join(__dirname, 'lib'));
fs.mkdirSync(dest, {recursive: true});
const files = [['jspdf/dist/jspdf.umd.min.js', 'jspdf.umd.min.js'], ['html2canvas/dist/html2canvas.min.js', 'html2canvas.min.js']];
for (const [src, name] of files) {
  const p = path.join(__dirname, 'node_modules', src);
  if (!fs.existsSync(p)) { console.error('MISSING ' + p); process.exit(1); }
  fs.copyFileSync(p, path.join(dest, name));
  console.log('copied', name, '->', dest);
}
