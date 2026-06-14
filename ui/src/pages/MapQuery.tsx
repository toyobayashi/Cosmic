import { useState, useCallback, useRef } from 'react'
import { Table, Input, Space, Row, Col, Tag, Descriptions, Modal, Button, Image } from 'antd'
import { SearchOutlined, PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons'
import api from '../api/client'

interface MapInfo {
  id: number
  name: string
}

interface MapMonster {
  id: number
  name: string
  level: number
  maxHP: number
  maxMP: number
  exp: number
  ice: number
  lightning: number
  fire: number
  poison: number
  holy: number
  dark: number
  physical: number
}

interface MapDetail {
  id: number
  name: string
  streetName: string
  bgm: string
  monsters: MapMonster[]
}

export default function MapQuery() {
  const [results, setResults] = useState<MapInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedMap, setSelectedMap] = useState<MapDetail | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [bgmPlaying, setBgmPlaying] = useState(false)
  const audioRef = useRef<HTMLAudioElement | null>(null)

  const handleSearch = useCallback(async (kw?: string) => {
    const q = kw ?? keyword
    if (!q.trim()) return
    setLoading(true)
    try {
      const res = await api.get('/common/v1/mapSearch', { params: { keyword: q } })
      setResults(res.data.data || [])
    } catch {
    } finally {
      setLoading(false)
    }
  }, [keyword])

  const handleViewDetail = async (mapId: number) => {
    setDetailOpen(true)
    setDetailLoading(true)
    setSelectedMap(null)
    stopBgm()
    try {
      const res = await api.get(`/map/v1/${mapId}`)
      setSelectedMap(res.data.data)
    } catch {
    } finally {
      setDetailLoading(false)
    }
  }

  const handleCloseDetail = () => {
    setDetailOpen(false)
    stopBgm()
  }

  const stopBgm = () => {
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current = null
    }
    setBgmPlaying(false)
  }

  const toggleBgm = (mapId: number) => {
    if (bgmPlaying) {
      stopBgm()
    } else {
      const audio = new Audio(`/api/map/v1/bgm/${mapId}`)
      audioRef.current = audio
      audio.play().then(() => setBgmPlaying(true)).catch(() => setBgmPlaying(false))
      audio.addEventListener('ended', () => setBgmPlaying(false))
    }
  }

  const columns = [
    { title: 'Map ID', dataIndex: 'id', key: 'id', width: 120 },
    { title: 'Map Name', dataIndex: 'name', key: 'name' },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_: unknown, record: MapInfo) => (
        <Button size="small" type="primary" onClick={() => handleViewDetail(record.id)}>
          Detail
        </Button>
      ),
    },
  ]

  const elemLabel = (v: number) => {
    switch (v) {
      case 1: return <Tag color="red">Immune</Tag>
      case 2: return <Tag color="orange">Strong</Tag>
      case 3: return <Tag color="green">Weak</Tag>
      default: return <span className="text-gray-400">-</span>
    }
  }

  const monsterColumns = [
    {
      title: 'Image',
      key: 'image',
      width: 80,
      render: (_: unknown, record: MapMonster) => (
        <Image
          src={`/api/map/v1/mob-image/${record.id}`}
          style={{ maxHeight: 48, width: 'auto' }}
          fallback="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 48'><rect fill='%23ddd' width='80' height='48'/></svg>"
          preview={false}
        />
      ),
    },
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Name', dataIndex: 'name', key: 'name', width: 120 },
    { title: 'Lv', dataIndex: 'level', key: 'level', width: 55 },
    { title: 'HP', dataIndex: 'maxHP', key: 'maxHP', width: 80, render: (v: number) => v.toLocaleString() },
    { title: 'MP', dataIndex: 'maxMP', key: 'maxMP', width: 60 },
    { title: 'EXP', dataIndex: 'exp', key: 'exp', width: 70, render: (v: number) => v.toLocaleString() },
    { title: 'I', dataIndex: 'ice', key: 'ice', width: 80, render: elemLabel },
    { title: 'L', dataIndex: 'lightning', key: 'lightning', width: 80, render: elemLabel },
    { title: 'F', dataIndex: 'fire', key: 'fire', width: 80, render: elemLabel },
    { title: 'P', dataIndex: 'poison', key: 'poison', width: 80, render: elemLabel },
    { title: 'S', dataIndex: 'holy', key: 'holy', width: 80, render: elemLabel },
    { title: 'D', dataIndex: 'dark', key: 'dark', width: 80, render: elemLabel },
    { title: 'H', dataIndex: 'physical', key: 'physical', width: 80, render: elemLabel },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Map Query</h2>

      <Row gutter={16} className="mb-4">
        <Col flex="auto">
          <Input.Search
            placeholder="Search by map name or ID"
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={() => handleSearch()}
            prefix={<SearchOutlined />}
            style={{ maxWidth: 480 }}
            enterButton
          />
        </Col>
      </Row>

      <Table
        dataSource={results}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />

      <Modal
        title={selectedMap ? `Map Detail: ${selectedMap.name} (${selectedMap.id})` : 'Loading...'}
        open={detailOpen}
        onCancel={handleCloseDetail}
        footer={null}
        width={1100}
      >
        <div style={{ maxHeight: '60vh', overflowY: 'auto' }}>
        {detailLoading && <p>Loading...</p>}
        {selectedMap && (
          <>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Map ID">{selectedMap.id}</Descriptions.Item>
              <Descriptions.Item label="BGM">
                {selectedMap.bgm ? (
                  <Space>
                    <Tag color="blue">{selectedMap.bgm}</Tag>
                    <Button
                      size="small"
                      type="primary"
                      icon={bgmPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                      onClick={() => toggleBgm(selectedMap.id)}
                    >
                      {bgmPlaying ? 'Stop' : 'Play'}
                    </Button>
                  </Space>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Map Name">{selectedMap.name || '-'}</Descriptions.Item>
              <Descriptions.Item label="Street Name">{selectedMap.streetName || '-'}</Descriptions.Item>
            </Descriptions>

            <h3 className="text-lg font-semibold mt-6 mb-3">
              Monsters ({selectedMap.monsters.length})
            </h3>
            <Table
              dataSource={selectedMap.monsters}
              columns={monsterColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </>
        )}
        </div>
      </Modal>
    </div>
  )
}
