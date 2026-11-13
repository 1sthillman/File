const CACHE_NAME = 'file-manav-sayim-v40-20261113';

// Cache-safe asset list (UTF-8 safe and relative to SW scope)
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon.svg'
];

// Install: Cache all core assets immediately
self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return Promise.allSettled(
        ASSETS.map((url) =>
          cache.add(url).catch((err) => {
            console.warn('[SW] Cache add failed for:', url, err);
          })
        )
      );
    })
  );
});

// Activate: Clean up old caches immediately
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            console.log('[SW] Eski onbellek siliniyor:', key);
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Cache First with Network Fallback (Offline First for ultimate reliability)
self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  // Handle same origin requests
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.match(event.request).then((cachedResponse) => {
        // Return cached version if found
        if (cachedResponse) {
          // Fetch updated version in background (Stale-While-Revalidate)
          fetch(event.request).then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              const resClone = networkResponse.clone();
              caches.open(CACHE_NAME).then((cache) => cache.put(event.request, resClone));
            }
          }).catch(() => {});
          return cachedResponse;
        }

        // If not in cache, try network
        return fetch(event.request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const resClone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, resClone));
          }
          return networkResponse;
        }).catch(async () => {
          // Offline fallback for navigation
          if (event.request.mode === 'navigate') {
            const indexCached = await caches.match('./index.html') || await caches.match('./');
            if (indexCached) return indexCached;
          }
          return new Response('Çevrimdışı Mod - Manav Sayım', {
            status: 503,
            headers: { 'Content-Type': 'text/plain; charset=utf-8' }
          });
        });
      })
    );
  }
});
