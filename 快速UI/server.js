const http = require('http');
const fs = require('fs');
const path = require('path');
const ROOT = String.raw`d:\ProjectSpace\2026年上学年\软件设计实践\快速UI`;
const server = http.createServer((req, res) => {
  let p = req.url.split('?')[0];
  if (p === '/' || p === '') p = '/roc-git-prototype.html';
  const file = path.join(ROOT, p);
  fs.readFile(file, (err, data) => {
    if (err) { res.statusCode = 404; res.end('Not Found: ' + file); return; }
    const ext = path.extname(file);
    const ct = ext === '.html' ? 'text/html; charset=utf-8' :
               ext === '.js' ? 'application/javascript' :
               ext === '.css' ? 'text/css' : 'application/octet-stream';
    res.setHeader('Content-Type', ct);
    res.end(data);
  });
});
server.listen(8765, () => console.log('Server started on 8765'));
