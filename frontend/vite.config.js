import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // 自定义中间件：直接提供 uploads 目录的静态文件，绕过代理
  configureServer(server) {
    server.middlewares.use('/uploads', (req, res) => {
      // 去掉 query string
      const urlPath = (req.url || '').split('?')[0]
      // Connect 挂载在 /uploads 时会自动剥离前缀，req.url 变成 /xxx.jpg
      // 去掉前导 /，避免 path.resolve 在 Windows 上将其视为驱动器根路径
      let filename = urlPath.replace(/^\/uploads\/?/, '').replace(/^\/+/, '')
      if (!filename) { res.statusCode = 404; res.end(); return }
      // 防路径穿越
      if (filename.includes('..')) { res.statusCode = 403; res.end(); return }
      const filePath = path.resolve('../backend/uploads', filename)
      // 确保解析后的路径仍在 uploads 目录内
      const uploadsRoot = path.resolve('../backend/uploads')
      if (!filePath.startsWith(uploadsRoot)) { res.statusCode = 403; res.end(); return }
      if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        const ext = path.extname(filePath).toLowerCase()
        const mimeTypes = {
          '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png',
          '.gif': 'image/gif', '.webp': 'image/webp', '.svg': 'image/svg+xml',
        }
        res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream')
        res.setHeader('Cache-Control', 'public, max-age=86400')
        fs.createReadStream(filePath).pipe(res)
      } else {
        console.log(`[vite] 图片不存在: ${filePath}`)
        res.statusCode = 404
        res.end()
      }
    })
  },
})
