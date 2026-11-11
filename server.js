const http = require('http');
const fs = require('fs');
const path = require('path');

const ROOT = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
};

function startServer(port) {
  const server = http.createServer((req, res) => {
    let reqPath = decodeURI(req.url.split('?')[0]);
    if (reqPath === '/' || reqPath === '' || reqPath === '/index.html' || reqPath === '/Manav_Sayim_Zebra_v16.html' || reqPath === '/Manav_Sayim_Zebra_v17.html') {
      reqPath = '/Manav_Sayim_Zebra_v17.html';
    }
    
    const filePath = path.join(ROOT, reqPath);
    
    fs.stat(filePath, (err, stats) => {
      if (err || !stats.isFile()) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('404 Not Found: ' + reqPath);
        return;
      }
      
      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';
      
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0',
        'Access-Control-Allow-Origin': '*'
      });
      
      fs.createReadStream(filePath).pipe(res);
    });
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`Port ${port} dolu, port ${port + 1} deneniyor...`);
      startServer(port + 1);
    } else {
      console.error('Sunucu hatasi:', err);
    }
  });

  server.listen(port, '0.0.0.0', () => {
    console.log(`MANAV SAYIM ZEBRA SUNUCUSU AKTIF (v17): http://localhost:${port}`);
  });
}

startServer(3000);
