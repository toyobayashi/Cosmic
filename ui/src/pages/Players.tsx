import { useState, useEffect, useCallback, useMemo } from 'react'
import { Table, Button, Space, Tag, message, Modal, Form, Select, Card, InputNumber, Input, List, Spin } from 'antd'
import { GiftOutlined, ReloadOutlined, GlobalOutlined, EnvironmentOutlined } from '@ant-design/icons'
import api from '../api/client'
import GameIdInput from '../components/GameIdInput'

interface OnlineChar {
  id: number; name: string; level: number; job: number; world: number; mapId: number; mapName: string;
}

interface MapInfo {
  id: number; name: string;
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

const giveTypes = [
  { label: 'NX Credit', value: 'nxCredit' },
  { label: 'NX Prepaid', value: 'nxPrepaid' },
  { label: 'Maple Point', value: 'maplePoint' },
  { label: 'Mesos', value: 'meso' },
  { label: 'EXP', value: 'exp' },
  { label: 'Item', value: 'item' },
  { label: 'Fame', value: 'fame' },
  { label: 'GM Level', value: 'gmLevel' },
  { label: 'EXP Rate (World)', value: 'expRate' },
  { label: 'Meso Rate (World)', value: 'mesoRate' },
  { label: 'Drop Rate (World)', value: 'dropRate' },
]

const rateTypes = ['expRate', 'mesoRate', 'dropRate']

export default function Players() {
  const [onlineChars, setOnlineChars] = useState<OnlineChar[]>([])
  const [loading, setLoading] = useState(false)
  const [giveOpen, setGiveOpen] = useState(false)
  const [giveGlobal, setGiveGlobal] = useState(false)
  const [selectedChar, setSelectedChar] = useState<OnlineChar | null>(null)
  const [giveForm] = Form.useForm()
  const [giveType, setGiveType] = useState('')
  const [searchText, setSearchText] = useState('')

  const [warpOpen, setWarpOpen] = useState(false)
  const [warpChar, setWarpChar] = useState<OnlineChar | null>(null)
  const [mapSearchText, setMapSearchText] = useState('')
  const [mapResults, setMapResults] = useState<MapInfo[]>([])
  const [mapSearching, setMapSearching] = useState(false)
  const [selectedMapId, setSelectedMapId] = useState<number | null>(null)
  const [warping, setWarping] = useState(false)

  const fetchOnline = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/character/v1/online')
      setOnlineChars(res.data.data || [])
    } catch { message.error('Failed to load online players') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchOnline() }, [fetchOnline])

  const filteredChars = useMemo(() => {
    const kw = searchText.trim().toLowerCase()
    if (!kw) return onlineChars
    return onlineChars.filter(c =>
      c.name.toLowerCase().includes(kw) ||
      String(c.id).includes(kw) ||
      getJobName(c.job).toLowerCase().includes(kw) ||
      (c.mapName || '').toLowerCase().includes(kw) ||
      String(c.mapId).includes(kw)
    )
  }, [onlineChars, searchText])

  const handleGive = async (values: Record<string, number>) => {
    try {
      const payload: Record<string, unknown> = { type: giveType }
      if (giveGlobal) { payload.global = true }
      else if (selectedChar) { payload.characterId = selectedChar.id }
      if (rateTypes.includes(giveType)) { payload.rate = values.rate }
      else if (giveType === 'item') { payload.itemId = values.itemId; payload.quantity = values.quantity || 1 }
      else { payload.quantity = values.quantity }
      await api.post('/give/v1/resource', { data: payload })
      message.success('Resource granted')
      setGiveOpen(false); giveForm.resetFields()
    } catch { message.error('Failed to give resource') }
  }

  const handleSearchMaps = useCallback(async (keyword: string) => {
    if (!keyword.trim()) { setMapResults([]); return }
    setMapSearching(true)
    try {
      const res = await api.get('/common/v1/mapSearch', { params: { keyword } })
      setMapResults(res.data.data || [])
    } catch { setMapResults([]) }
    finally { setMapSearching(false) }
  }, [])

  const handleWarp = async () => {
    if (!warpChar || !selectedMapId) return
    setWarping(true)
    try {
      await api.post('/character/v1/warp', { characterId: warpChar.id, mapId: selectedMapId })
      message.success(`Warped ${warpChar.name} to map ${selectedMapId}`)
      setWarpOpen(false)
      setWarpChar(null)
      setSelectedMapId(null)
      setMapSearchText('')
      setMapResults([])
    } catch { message.error('Failed to warp character') }
    finally { setWarping(false) }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Level', dataIndex: 'level', key: 'level', width: 70 },
    {
      title: 'Job',
      dataIndex: 'job',
      key: 'job',
      render: (v: number) => <Tag>{v} - {getJobName(v)}</Tag>,
    },
    { title: 'World', dataIndex: 'world', key: 'world', width: 70 },
    {
      title: 'Map',
      dataIndex: 'mapId',
      key: 'mapId',
      render: (mapId: number, r: OnlineChar) => r.mapName ? `${r.mapName} (${mapId})` : String(mapId),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 180,
      render: (_: unknown, r: OnlineChar) => (
        <Space>
          <Button size="small" icon={<GiftOutlined />}
            onClick={() => { setSelectedChar(r); setGiveGlobal(false); setGiveOpen(true); setGiveType(''); giveForm.resetFields() }}>
            Give
          </Button>
          <Button size="small" icon={<EnvironmentOutlined />}
            onClick={() => {
              setWarpChar(r); setWarpOpen(true); setSelectedMapId(null);
              setMapSearchText(''); setMapResults([]);
            }}>
            Set Map
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Players</h2>
      <Card title="Online Players" extra={
        <Space>
          <Button icon={<GlobalOutlined />} onClick={() => { setGiveGlobal(true); setSelectedChar(null); setGiveOpen(true); setGiveType(''); giveForm.resetFields() }}>Global Give</Button>
          <Button icon={<ReloadOutlined />} onClick={fetchOnline}>Refresh</Button>
        </Space>
      }>
        <Input.Search
          placeholder="Search by name, ID, job, map..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          allowClear
          style={{ width: 320, marginBottom: 16 }}
        />
        <Table
          dataSource={filteredChars}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 15, showSizeChanger: true, pageSizeOptions: ['10', '15', '30', '50'], showTotal: (total) => `Total: ${total}` }}
          size="small"
          locale={{ emptyText: 'No players online' }}
        />
      </Card>

      <Modal title={giveGlobal ? 'Global Give' : `Give to: ${selectedChar?.name || ''}`}
        open={giveOpen} onCancel={() => setGiveOpen(false)} onOk={() => giveForm.submit()}>
        <Form form={giveForm} layout="vertical" onFinish={handleGive}>
          <Form.Item label="Type" required>
            <Select options={giveTypes} value={giveType} onChange={setGiveType} placeholder="Select type" />
          </Form.Item>
          {giveType === 'item' && (
            <Form.Item name="itemId" label="Item" rules={[{ required: true }]}>
              <GameIdInput searchType="Item" placeholder="Search item by name or ID" />
            </Form.Item>
          )}
          {giveType && !rateTypes.includes(giveType) && (
            <Form.Item name="quantity" label="Quantity/Value" rules={[{ required: true }]}>
              <InputNumber className="w-full" />
            </Form.Item>
          )}
          {rateTypes.includes(giveType) && (
            <Form.Item name="rate" label="Rate (World-Level)" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={0.1} placeholder="e.g. 2.0 for double" stringMode={false} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal title={`Set Map for: ${warpChar?.name || ''}`}
        open={warpOpen}
        onCancel={() => { setWarpOpen(false); setWarpChar(null); setSelectedMapId(null); setMapSearchText(''); setMapResults([]) }}
        onOk={handleWarp}
        confirmLoading={warping}
        okButtonProps={{ disabled: !selectedMapId }}
      >
        <div className="mb-4">
          <Input.Search
            placeholder="Search map by name or ID"
            value={mapSearchText}
            onChange={(e) => { setMapSearchText(e.target.value); handleSearchMaps(e.target.value) }}
            onSearch={(v) => handleSearchMaps(v)}
            enterButton
            loading={mapSearching}
          />
        </div>
        {mapSearching ? (
          <div className="text-center py-8"><Spin /></div>
        ) : (
          <List
            dataSource={mapResults}
            style={{ maxHeight: 300, overflowY: 'auto' }}
            renderItem={(item) => (
              <List.Item
                key={item.id}
                onClick={() => setSelectedMapId(item.id)}
                style={{
                  cursor: 'pointer',
                  padding: '8px 12px',
                  backgroundColor: selectedMapId === item.id ? '#e6f4ff' : undefined,
                }}
              >
                <span><Tag>{item.id}</Tag> {item.name}</span>
              </List.Item>
            )}
            locale={{ emptyText: mapSearchText ? 'No maps found' : 'Enter a keyword to search' }}
          />
        )}
      </Modal>
    </div>
  )
}
