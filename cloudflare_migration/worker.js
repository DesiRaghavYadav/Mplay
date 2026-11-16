/**
 * Cloudflare Worker API for OTT Application
 * Supports Cloudflare D1 Database, KV Storage, and R2 Object Storage
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Content-Type': 'application/json'
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    if (request.method === 'POST' || request.method === 'PUT') {
      if (env.DB && path.startsWith('/api/')) {
        const endpoint = path.replace('/api/', '').split('/')[0].split('?')[0];
        if (endpoint) {
          try {
            const body = await request.json().catch(() => ({}));
            const docId = String(body.id || body._id || body.userid || body.userId || Date.now());
            
            // Ensure table exists
            await env.DB.prepare(`CREATE TABLE IF NOT EXISTS ${endpoint} (id TEXT PRIMARY KEY, data TEXT);`).run().catch(() => {});
            
            // Insert or replace record
            await env.DB.prepare(`INSERT OR REPLACE INTO ${endpoint} (id, data) VALUES (?, ?);`)
              .bind(docId, JSON.stringify(body))
              .run();

            return new Response(JSON.stringify({ success: true, message: `Saved to Cloudflare D1 (${endpoint})`, id: docId }), { headers: corsHeaders });
          } catch (e) {
            return new Response(JSON.stringify({ success: false, error: e.message }), { status: 500, headers: corsHeaders });
          }
        }
      }
    }

    try {
      // 1. Stats and Browser Dashboard Endpoint
      if (path === '/' || path === '/api/stats' || path === '/api/count') {
        const stats = {};
        const tables = [
          'comics',
          'stories',
          'categories',
          'video_categories',
          'channels',
          'reels',
          'version',
          'userdata',
          'fcm_history',
          'ad_config',
          'notifications',
          'audio_batches',
          'batchvideo'
        ];

        if (env.DB) {
          for (const table of tables) {
            try {
              if (table === 'video_categories' || table === 'audio_batches' || table === 'batchvideo') {
                // Return unique aggregated category/batch count
                const { results } = await env.DB.prepare(`SELECT data FROM ${table}`).all();
                const names = new Set();
                results.forEach(r => {
                  try {
                    const doc = JSON.parse(r.data);
                    const identifier = doc.batchName || doc.batchId || doc.name || doc.title || doc._id || doc.id;
                    if (identifier) names.add(identifier);
                  } catch(e) {}
                });
                stats[table] = { total_documents: names.size, raw_chunks: results.length, url: `/api/${table}` };
              } else {
                const { results } = await env.DB.prepare(`SELECT COUNT(*) as count FROM ${table}`).all();
                stats[table] = { total_documents: results[0]?.count || 0, url: `/api/${table}` };
              }
            } catch (e) {
              stats[table] = { total_documents: 0, error: 'Table not seeded yet or missing', url: `/api/${table}` };
            }
          }
        }

        if (path === '/' && request.headers.get('accept')?.includes('text/html')) {
          const rowsHtml = Object.entries(stats).map(([table, info]) => `
            <tr>
              <td style="padding: 10px; border-bottom: 1px solid #333;"><b>${table}</b></td>
              <td style="padding: 10px; border-bottom: 1px solid #333; color: #4CAF50;"><b>${info.total_documents}</b> docs ${info.raw_chunks ? `(${info.raw_chunks} chunks)` : ''}</td>
              <td style="padding: 10px; border-bottom: 1px solid #333;"><a href="${info.url}" target="_blank" style="color: #64B5F6;">View JSON Data</a></td>
            </tr>
          `).join('');

          const html = `
            <!DOCTYPE html>
            <html>
            <head>
              <title>Cloudflare D1 OTT API Status</title>
              <style>
                body { font-family: sans-serif; background: #121212; color: #fff; padding: 30px; }
                h1 { color: #FF9800; }
                table { width: 100%; max-width: 700px; border-collapse: collapse; background: #1E1E1E; border-radius: 8px; overflow: hidden; }
                th { background: #2C2C2C; text-align: left; padding: 12px; }
              </style>
            </head>
            <body>
              <h1>🚀 Cloudflare D1 Database API</h1>
              <p>Your D1 Database & Worker integration is active!</p>
              <table>
                <thead><tr><th>Collection</th><th>Count</th><th>API Endpoint</th></tr></thead>
                <tbody>${rowsHtml}</tbody>
              </table>
            </body>
            </html>
          `;
          return new Response(html, { headers: { 'Content-Type': 'text/html' } });
        }

        return new Response(JSON.stringify({ status: 'active', database: 'Cloudflare D1', collections: stats }, null, 2), { headers: corsHeaders });
      }

      // Helper function to extract array from D1 table storing raw JSON documents in 'data' column
      const fetchD1Collection = async (tableName) => {
        if (!env.DB) return null;
        try {
          const { results } = await env.DB.prepare(`SELECT data FROM ${tableName}`).all();
          return results.map(r => {
            try { return JSON.parse(r.data); } catch (e) { return null; }
          }).filter(Boolean);
        } catch (e) {
          return null;
        }
      };

      let data = null;

      // 2. Specific API Endpoints
      if (env.DB && path.startsWith('/api/')) {
        const endpoint = path.replace('/api/', '').split('/')[0].split('?')[0];
        if (endpoint === 'video_categories') {
          const { results } = await env.DB.prepare('SELECT data FROM video_categories').all();
          const categoryMap = {};
          for (const row of results) {
            try {
              const doc = JSON.parse(row.data);
              const name = doc.name;
              let videos = [];
              if (typeof doc.videosJson === 'string') {
                try { videos = JSON.parse(doc.videosJson); } catch (e) {}
              } else if (Array.isArray(doc.videosJson)) {
                videos = doc.videosJson;
              }
              if (!categoryMap[name]) {
                categoryMap[name] = { ...doc, videos };
              } else {
                categoryMap[name].videos.push(...videos);
              }
            } catch (e) {}
          }
          data = Object.values(categoryMap);
        } else if (endpoint === 'audio_batches') {
          const { results } = await env.DB.prepare('SELECT data FROM audio_batches').all();
          const batchMap = {};
          for (const row of results) {
            try {
              const doc = JSON.parse(row.data);
              const batchName = doc.batchName || doc._id;
              let audios = [];
              if (typeof doc.audiosJson === 'string') {
                try { audios = JSON.parse(doc.audiosJson); } catch (e) {}
              } else if (Array.isArray(doc.audiosJson)) {
                audios = doc.audiosJson;
              }
              if (!batchMap[batchName]) {
                batchMap[batchName] = { ...doc, audios };
              } else {
                batchMap[batchName].audios.push(...audios);
              }
            } catch (e) {}
          }
          data = Object.values(batchMap);
        } else if (endpoint === 'batchvideo' || endpoint === 'batch_video') {
          const tableName = 'batchvideo';
          const { results } = await env.DB.prepare(`SELECT data FROM ${tableName}`).all().catch(() => ({ results: [] }));
          if (results && results.length > 0) {
            data = results.map(r => {
              try { return JSON.parse(r.data); } catch (e) { return null; }
            }).filter(Boolean);
          }
        } else if (endpoint) {
          data = await fetchD1Collection(endpoint);
        }
      }

      // 3. Fallback to KV if D1 is not populated
      if (data === null && env.MIGRATION_KV) {
        const key = path.replace('/api/', '');
        data = await env.MIGRATION_KV.get(key, 'json');
      }

      if (data !== null) {
        return new Response(JSON.stringify(data), { headers: corsHeaders });
      }

      return new Response(JSON.stringify({
        status: 'active',
        message: 'Endpoint ready. Ensure data is seeded in Cloudflare D1.',
        endpoint: path
      }), { headers: corsHeaders });

    } catch (e) {
      return new Response(JSON.stringify({ error: e.message }), { status: 500, headers: corsHeaders });
    }
  }
};
