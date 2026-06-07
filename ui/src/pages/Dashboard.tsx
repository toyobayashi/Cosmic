import { useState, useEffect, useCallback } from 'react'
import { Card, Statistic, Row, Col, Table, Tag, Spin, message, Button, Space, Input, Divider } from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  TeamOutlined,
  UserOutlined,
  GlobalOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import api from '../api/client'

interface WorldInfo {
  id: number
  name: string
  eventMessage: string
  flag: number
  channels: number
  channelList: { worldId: number; channelId: number; onlineCount: number }[]
}

export default function Dashboard() {
  const [online, setOnline] = useState<boolean | null>(null)
  const [playerCount, setPlayerCount] = useState(0)
  const [accountCount, setAccountCount] = useState(0)
  const [version, setVersion] = useState('')
  const [worlds, setWorlds] = useState<WorldInfo[]>([])
  const [loading, setLoading] = useState(true)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<{ type: string; id: number; name: string }[]>([])
  const [searching, setSearching] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [onlineRes, playerRes, accountRes, versionRes, worldRes] = await Promise.all([
        api.get('/server/v1/online'),
        api.get('/server/v1/playerCount'),
        api.get('/server/v1/accountCount'),
        api.get('/server/v1/version'),
        api.get('/server/v1/world/list'),
      ])
      setOnline(onlineRes.data.data)
      setPlayerCount(playerRes.data.data)
      setAccountCount(accountRes.data.data)
      setVersion(versionRes.data.data)
      setWorlds(worldRes.data.data || [])
    } catch {
      message.error('Failed to load dashboard data')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
    const interval = setInterval(loadData, 15000)
    return () => clearInterval(interval)
  }, [loadData])

  const handleReload = async (type: string) => {
    try {
      await api.post(`/server/v1/reload/${type}`)
      message.success(`${type} reloaded`)
    } catch { message.error(`Failed to reload ${type}`) }
  }

  const handleServerAction = async (action: string) => {
    try {
      await api.post(`/server/v1/${action}`)
      message.success(`${action} executed`)
      if (action === 'shutdown') return
      setTimeout(loadData, 2000)
    } catch { message.error(`Failed to ${action}`) }
  }

  const handleSearch = async () => {
    if (!searchKeyword.trim()) return
    setSearching(true)
    try {
      const res = await api.get('/common/v1/informationSearch', { params: { keyword: searchKeyword } })
      setSearchResults(res.data.data || [])
    } catch { message.error('Search failed') }
    finally { setSearching(false) }
  }

  if (loading && online === null) return <Spin size="large" className="flex justify-center mt-32" />

  const channelColumns = [
    { title: 'World', dataIndex: 'worldId', width: 60 },
    { title: 'Channel', dataIndex: 'channelId', width: 80 },
    { title: 'Players', dataIndex: 'onlineCount', render: (v: number) => <Tag color={v > 0 ? 'green' : 'default'}>{v}</Tag> },
  ]

  const worldColumns = [
    { title: 'ID', dataIndex: 'id', width: 50 },
    { title: 'Name', dataIndex: 'name' },
    { title: 'Status', dataIndex: 'flag', render: (flag: number) => flag === 1 ? <Tag color="orange">Event</Tag> : flag === 2 ? <Tag color="green">New</Tag> : <Tag>Normal</Tag> },
    { title: 'Message', dataIndex: 'eventMessage' },
    { title: 'Channels', dataIndex: 'channels' },
  ]

  const searchColumns = [
    { title: 'Type', dataIndex: 'type', width: 80, render: (v: string) => <Tag>{v}</Tag> },
    { title: 'ID', dataIndex: 'id', width: 100 },
    { title: 'Name', dataIndex: 'name' },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Dashboard</h2>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="Server Status" value={online ? 'Online' : 'Offline'}
              valueStyle={{ color: online ? '#52c41a' : '#ff4d4f' }}
              prefix={online ? <CheckCircleOutlined /> : <CloseCircleOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Online Players" value={playerCount} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Total Accounts" value={accountCount} prefix={<UserOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Version" value={version} prefix={<GlobalOutlined />} /></Card>
        </Col>
      </Row>

      <Card title="Server Control" className="mb-6">
        <Space wrap>
          <Button type="primary" onClick={() => handleServerAction('startServer')} disabled={!!online}>Start</Button>
          <Button danger onClick={() => handleServerAction('stopServer')}>Stop</Button>
          <Button onClick={() => handleServerAction('restartServer')}>Restart</Button>
          <Divider type="vertical" />
          <Button icon={<ReloadOutlined />} onClick={() => handleReload('events')}>Reload Events</Button>
          <Button icon={<ReloadOutlined />} onClick={() => handleReload('maps')}>Reload Maps</Button>
          <Button icon={<ReloadOutlined />} onClick={() => handleReload('portals')}>Reload Portals</Button>
          <Button icon={<ReloadOutlined />} onClick={() => handleReload('drops')}>Reload Drops</Button>
          <Button icon={<ReloadOutlined />} onClick={() => handleReload('shops')}>Reload Shops</Button>
        </Space>
      </Card>

      <Card title="Worlds" className="mb-6">
        <Table dataSource={worlds} columns={worldColumns} rowKey="id" pagination={false}
          expandable={{ expandedRowRender: (r) => <Table dataSource={r.channelList} columns={channelColumns} rowKey={(x) => `${x.worldId}-${x.channelId}`} pagination={false} size="small" /> }} />
      </Card>

      <Card title="Search Game Data">
        <Space className="mb-4">
          <Input.Search placeholder="Search items by keyword..." value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} onSearch={handleSearch} enterButton prefix={<SearchOutlined />} style={{ width: 400 }} />
        </Space>
        <Table dataSource={searchResults} columns={searchColumns} rowKey={(r) => `${r.type}-${r.id}`} loading={searching} pagination={false} size="small" locale={{ emptyText: 'Enter a keyword to search' }} />
      </Card>
    </div>
  )
}
