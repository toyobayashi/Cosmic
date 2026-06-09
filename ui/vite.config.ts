import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import fs from 'fs'

function copySync (src: string, dest: string) {
  if (fs.existsSync(src)) {
    const stats = fs.statSync(src)
    if (stats.isDirectory()) {
      fs.mkdirSync(dest, { recursive: true })
      const entries = fs.readdirSync(src)
      entries.forEach(entry => {
        copySync(path.join(src, entry), path.join(dest, entry))
      })
    } else {
      fs.copyFileSync(src, dest)
    }
  }
}

function copyPlugin () {
  return {
    name: 'copy-plugin',
    apply: 'build',
    writeBundle () {
      const destDir = path.resolve(__dirname, '../src/main/resources/static')
      const srcDir = path.resolve(__dirname, 'dist')

      fs.rmSync(destDir, { recursive: true, force: true })
      copySync(srcDir, destDir)
    },
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), copyPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 8787,
    proxy: {
      '/api': {
        target: 'http://localhost:8686',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
})
