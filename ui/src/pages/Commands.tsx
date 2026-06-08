import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Radio, Input, Tag, Card, message } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import api from '../api/client'

interface CommandInfo {
  id: number
  syntax: string
  clazz: string
  description: string
  defaultLevel: number
  level: number
  enabled: boolean
}

const levelColors: Record<number, string> = {
  0: 'default', 1: 'blue', 2: 'green', 3: 'orange', 4: 'purple', 5: 'magenta', 6: 'red',
}

export default function Commands() {
  const [data, setData] = useState<CommandInfo[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [selectedLevel, setSelectedLevel] = useState(-1)
  const [syntax, setSyntax] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/command/v1', {
        params: {
          level: selectedLevel === -1 ? undefined : selectedLevel,
          syntax: syntax || undefined,
          page,
          size: pageSize,
        },
      })
      setData(res.data.data?.records || [])
      setTotal(res.data.data?.totalRow || 0)
    } catch {
      message.error('Failed to load commands')
    } finally {
      setLoading(false)
    }
  }, [selectedLevel, syntax, page, pageSize])

  useEffect(() => { fetchData() }, [fetchData])

  const columns = [
    {
      title: 'Syntax', dataIndex: 'syntax', width: 160,
      render: (v: string) => <code>{v}</code>,
    },
    { title: 'Description', dataIndex: 'description', width: 300 },
    {
      title: 'GM Level', dataIndex: 'level', width: 90, align: 'center' as const,
      render: (v: number) => <Tag color={levelColors[v] || 'default'}>Level {v}</Tag>,
    },
    {
      title: 'Enabled', dataIndex: 'enabled', width: 80, align: 'center' as const,
      render: (v: boolean) => v ? <Tag color="green">Yes</Tag> : <Tag color="red">No</Tag>,
    },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">GM Command Info</h2>

      <Card className="mb-4">
        <Space direction="vertical" className="w-full">
          <Space>
            <span>GM Level:</span>
            <Radio.Group value={selectedLevel} onChange={(e) => { setSelectedLevel(e.target.value); setPage(1) }}>
              <Radio.Button value={-1}>All</Radio.Button>
              {[0, 1, 2, 3, 4, 5, 6].map(lv => (
                <Radio.Button key={lv} value={lv}>Lv.{lv}</Radio.Button>
              ))}
            </Radio.Group>
          </Space>
          <Space>
            <Input.Search
              placeholder="Search by command name"
              allowClear
              value={syntax}
              onChange={(e) => setSyntax(e.target.value)}
              onSearch={() => { setPage(1); fetchData() }}
              prefix={<SearchOutlined />}
              style={{ width: 280 }}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchData}>Refresh</Button>
          </Space>
        </Space>
      </Card>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `Total ${t} commands`,
          onChange: (p, ps) => { setPage(p); setPageSize(ps) },
        }}
        size="small"
      />
    </div>
  )
}
