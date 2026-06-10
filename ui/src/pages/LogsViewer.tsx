import { useState, useEffect, useCallback, useRef } from 'react'
import { Card, Button, Select, Space, Spin, Typography, message } from 'antd'
import { ReloadOutlined, FileTextOutlined } from '@ant-design/icons'
import api from '../api/client'

const { Text } = Typography

interface LogFile {
  name: string
  size: number
  lastModified: number
}

interface LogContent {
  file: string
  size: number
  lastModified: number
  lines: string[]
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  return d.toLocaleString()
}

export default function LogsViewer() {
  const [files, setFiles] = useState<LogFile[]>([])
  const [selectedFile, setSelectedFile] = useState<string>('cosmic-log.log')
  const [content, setContent] = useState<LogContent | null>(null)
  const [loading, setLoading] = useState(false)
  const [tailLines, setTailLines] = useState(500)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const contentRef = useRef<HTMLPreElement>(null)

  const fetchFiles = useCallback(async () => {
    try {
      const res = await api.get('/log/v1/files')
      setFiles(res.data.data || [])
    } catch {
      message.error('Failed to load log files')
    }
  }, [])

  const fetchContent = useCallback(async () => {
    if (!selectedFile) return
    setLoading(true)
    try {
      const res = await api.get('/log/v1/content', {
        params: { file: selectedFile, tail: tailLines },
      })
      setContent(res.data.data)
    } catch {
      message.error('Failed to read log')
    } finally {
      setLoading(false)
    }
  }, [selectedFile, tailLines])

  useEffect(() => { fetchFiles() }, [fetchFiles])
  useEffect(() => { fetchContent() }, [fetchContent])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = setInterval(() => {
      fetchFiles()
      fetchContent()
    }, 5000)
    return () => clearInterval(timer)
  }, [autoRefresh, fetchFiles, fetchContent])

  useEffect(() => {
    const el = contentRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [content?.lines])

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Server Logs</h2>

      <Card className="mb-4">
        <Space wrap>
          <span className="text-gray-500">File:</span>
          <Select
            showSearch
            value={selectedFile}
            onChange={(v) => setSelectedFile(v)}
            style={{ width: 220 }}
            options={files.map((f) => ({
              value: f.name,
              label: (
                <span className="flex justify-between gap-3">
                  <FileTextOutlined />
                  <span>{f.name}</span>
                  <span className="text-gray-400 text-xs">{formatSize(f.size)}</span>
                </span>
              ),
            }))}
          />
          <span className="text-gray-500">Lines:</span>
          <Select
            value={tailLines}
            onChange={(v) => setTailLines(v)}
            style={{ width: 100 }}
            options={[
              { value: 100, label: '100' },
              { value: 200, label: '200' },
              { value: 500, label: '500' },
              { value: 1000, label: '1000' },
              { value: 2000, label: '2000' },
            ]}
          />
          <Button icon={<ReloadOutlined />} onClick={() => { fetchFiles(); fetchContent() }}>
            Refresh
          </Button>
          <Button
            type={autoRefresh ? 'primary' : 'default'}
            onClick={() => setAutoRefresh(!autoRefresh)}
          >
            {autoRefresh ? 'Auto: ON' : 'Auto: OFF'}
          </Button>
        </Space>
        {content && (
          <div className="mt-2">
            <Text type="secondary">
              {content.file} &middot; {formatSize(content.size)} &middot; {formatTime(content.lastModified)}
            </Text>
          </div>
        )}
      </Card>

      <Card>
        <Spin spinning={loading}>
          <pre
            ref={contentRef}
            className="text-xs leading-5 m-0 p-3 rounded overflow-auto"
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              maxHeight: 'calc(100vh - 340px)',
              minHeight: 400,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
            }}
          >
            {content?.lines?.length ? content.lines.join('\n') : 'No log data'}
          </pre>
        </Spin>
      </Card>
    </div>
  )
}
