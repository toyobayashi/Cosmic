import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Input, Space, Tag, message, Row, Col, Card } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import api from '../api/client'

interface CharacterInfo {
  id: number
  name: string
  level: number
  job: number
  world: number
  gm: number
  fame: number
  meso: number
  guildid: number
  createdate: string
  lastLogoutTime: string | null
}

interface OnlineChar {
  id: number
  name: string
  level: number
  job: number
  world: number
  mapId: number
}

const jobNames: Record<number, string> = {
  0: 'Beginner', 100: 'Warrior', 110: 'Fighter', 111: 'Crusader', 112: 'Hero',
  120: 'Page', 121: 'White Knight', 122: 'Paladin', 130: 'Spearman', 131: 'Dragon Knight', 132: 'Dark Knight',
  200: 'Magician', 210: 'F/P Wizard', 211: 'F/P Mage', 212: 'F/P Archmage',
  220: 'I/L Wizard', 221: 'I/L Mage', 222: 'I/L Archmage', 230: 'Cleric', 231: 'Priest', 232: 'Bishop',
  300: 'Bowman', 310: 'Hunter', 311: 'Ranger', 312: 'Bowmaster',
  320: 'Crossbowman', 321: 'Sniper', 322: 'Marksman',
  400: 'Thief', 410: 'Assassin', 411: 'Hermit', 412: 'Night Lord',
  420: 'Bandit', 421: 'Chief Bandit', 422: 'Shadower',
  500: 'Pirate', 510: 'Brawler', 511: 'Marauder', 512: 'Buccaneer',
  520: 'Gunslinger', 521: 'Outlaw', 522: 'Corsair',
  900: 'GM', 910: 'Super GM',
  1000: 'Noblesse', 1100: 'Dawn Warrior', 1110: 'Dawn Warrior II', 1111: 'Dawn Warrior III', 1112: 'Dawn Warrior IV',
  1200: 'Blaze Wizard', 1210: 'Blaze Wizard II', 1211: 'Blaze Wizard III', 1212: 'Blaze Wizard IV',
  1300: 'Wind Archer', 1310: 'Wind Archer II', 1311: 'Wind Archer III', 1312: 'Wind Archer IV',
  1400: 'Night Walker', 1410: 'Night Walker II', 1411: 'Night Walker III', 1412: 'Night Walker IV',
  1500: 'Thunder Breaker', 1510: 'Thunder Breaker II', 1511: 'Thunder Breaker III', 1512: 'Thunder Breaker IV',
  2000: 'Legend', 2100: 'Aran I', 2110: 'Aran II', 2111: 'Aran III', 2112: 'Aran IV',
}

function getJobName(jobId: number): string {
  return jobNames[jobId] || `Job ${jobId}`
}

export default function Characters() {
  const [onlineChars, setOnlineChars] = useState<OnlineChar[]>([])
  const [searchAccountId, setSearchAccountId] = useState('')
  const [characters, setCharacters] = useState<CharacterInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [onlineLoading, setOnlineLoading] = useState(false)

  const fetchOnline = useCallback(async () => {
    setOnlineLoading(true)
    try {
      const res = await api.get('/character/v1/online')
      setOnlineChars(res.data.data || [])
    } catch {
      message.error('Failed to load online characters')
    } finally {
      setOnlineLoading(false)
    }
  }, [])

  const fetchByAccount = useCallback(async () => {
    if (!searchAccountId) { setCharacters([]); return }
    setLoading(true)
    try {
      const res = await api.get('/character/v1/list', { params: { accountId: parseInt(searchAccountId) } })
      setCharacters(res.data.data || [])
    } catch {
      message.error('Failed to load characters')
    } finally {
      setLoading(false)
    }
  }, [searchAccountId])

  useEffect(() => { fetchOnline() }, [fetchOnline])

  const onlineColumns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Level', dataIndex: 'level', key: 'level' },
    {
      title: 'Job',
      dataIndex: 'job',
      key: 'job',
      render: (v: number) => <Tag>{getJobName(v)}</Tag>,
    },
    { title: 'World', dataIndex: 'world', key: 'world' },
    { title: 'Map', dataIndex: 'mapId', key: 'mapId' },
  ]

  const charColumns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Level', dataIndex: 'level', key: 'level' },
    {
      title: 'Job',
      dataIndex: 'job',
      key: 'job',
      render: (v: number) => <Tag>{getJobName(v)}</Tag>,
    },
    {
      title: 'GM',
      dataIndex: 'gm',
      key: 'gm',
      render: (v: number) => v ? <Tag color="gold">GM</Tag> : null,
    },
    { title: 'Fame', dataIndex: 'fame', key: 'fame' },
    { title: 'Meso', dataIndex: 'meso', key: 'meso' },
    { title: 'World', dataIndex: 'world', key: 'world' },
    { title: 'Guild', dataIndex: 'guildid', key: 'guildid', render: (v: number) => v || '-' },
    { title: 'Created', dataIndex: 'createdate', key: 'createdate', render: (v: string) => v?.substring(0, 19) || '-' },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Characters</h2>

      <Card title="Online Characters" className="mb-6" extra={
        <Button icon={<ReloadOutlined />} onClick={fetchOnline} size="small">Refresh</Button>
      }>
        <Table
          dataSource={onlineChars}
          columns={onlineColumns}
          rowKey="id"
          loading={onlineLoading}
          pagination={false}
          size="small"
          locale={{ emptyText: 'No characters online' }}
        />
      </Card>

      <Card title="Search by Account ID">
        <Row gutter={16} className="mb-4">
          <Col>
            <Input.Search
              placeholder="Enter account ID"
              value={searchAccountId}
              onChange={(e) => setSearchAccountId(e.target.value)}
              onSearch={fetchByAccount}
              enterButton
              prefix={<SearchOutlined />}
              style={{ width: 240 }}
            />
          </Col>
        </Row>
        <Table
          dataSource={characters}
          columns={charColumns}
          rowKey="id"
          loading={loading}
          pagination={false}
          locale={{ emptyText: searchAccountId ? 'No characters found' : 'Enter an account ID to search' }}
        />
      </Card>
    </div>
  )
}
