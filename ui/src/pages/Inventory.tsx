import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Tag, message, Modal, Form, Select, Card, Input, InputNumber, Row, Col, Tabs } from 'antd'
import { ReloadOutlined, DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons'
import api from '../api/client'

interface InventoryItem {
  id: number
  characterId: number
  itemId: number
  itemName: string | null
  inventoryType: number
  position: number
  quantity: number
  owner: string
  petId: number
  expiration: number
  giftFrom: string
  equipment?: Record<string, number>
}

interface InventoryType {
  inventoryType: number
  name: string
}

interface CharacterInfo {
  id: number
  name: string
  level: number
  job: number
  world: number
  gm: number
  online: number
}

const typeLabels: Record<number, string> = { 1: 'Equip', 2: 'Use', 3: 'Setup', 4: 'Etc', 5: 'Cash' }

export default function Inventory() {
  const [types, setTypes] = useState<InventoryType[]>([])
  const [characters, setCharacters] = useState<CharacterInfo[]>([])
  const [selectedChar, setSelectedChar] = useState<CharacterInfo | null>(null)
  const [charSearch, setCharSearch] = useState('')
  const [activeType, setActiveType] = useState('1')
  const [data, setData] = useState<InventoryItem[]>([])
  const [loading, setLoading] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<InventoryItem | null>(null)
  const [editForm] = Form.useForm()

  const fetchTypes = useCallback(async () => {
    try {
      const res = await api.get('/inventory/v1/types')
      setTypes(res.data.data || [])
    } catch { message.error('Failed to load inventory types') }
  }, [])

  const searchCharacters = useCallback(async (keyword?: string) => {
    try {
      const res = await api.get('/inventory/v1/characters', { params: { keyword } })
      setCharacters(res.data.data || [])
    } catch { message.error('Failed to search characters') }
  }, [])

  const fetchData = useCallback(async () => {
    if (!selectedChar) { setData([]); return }
    setLoading(true)
    try {
      const params: Record<string, unknown> = {
        characterId: selectedChar.id,
        inventoryType: parseInt(activeType),
        page: 1,
        size: 500,
      }
      const res = await api.get('/inventory/v1', { params })
      setData(res.data.data?.records || [])
    } catch { message.error('Failed to load inventory') }
    finally { setLoading(false) }
  }, [selectedChar, activeType])

  useEffect(() => { fetchTypes() }, [fetchTypes])
  useEffect(() => { searchCharacters() }, [searchCharacters])
  useEffect(() => { fetchData() }, [fetchData])

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/inventory/v1/${id}`)
      message.success('Item deleted')
      fetchData()
    } catch { message.error('Failed to delete item') }
  }

  const handleEdit = async (values: Record<string, number>) => {
    if (!editingItem) return
    try {
      const payload: Record<string, number> = {}
      if (values.quantity !== undefined && values.quantity !== editingItem.quantity) payload.quantity = values.quantity
      if (values.position !== undefined && values.position !== editingItem.position) payload.position = values.position
      if (editingItem.equipment) {
        const eqFields = ['upgradeslots', 'level', 'str', 'dex', 'item_int', 'luk', 'hp', 'mp', 'watk', 'matk', 'wdef', 'mdef', 'acc', 'avoid', 'hands', 'speed', 'jump']
        eqFields.forEach(f => {
          if (values[f] !== undefined && values[f] !== editingItem.equipment?.[f]) payload[f] = values[f]
        })
      }
      if (Object.keys(payload).length === 0) { message.info('No changes'); setEditOpen(false); return }
      await api.put(`/inventory/v1/${editingItem.id}`, { data: payload })
      message.success('Item updated')
      setEditOpen(false)
      fetchData()
    } catch { message.error('Failed to update item') }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: 'Item ID', dataIndex: 'itemId', width: 90 },
    { title: 'Item Name', dataIndex: 'itemName', width: 180, render: (v: string | null) => v || '-' },
    { title: 'Position', dataIndex: 'position', width: 70 },
    { title: 'Qty', dataIndex: 'quantity', width: 60 },
    { title: 'Pet ID', dataIndex: 'petId', width: 60, render: (v: number) => v > 0 ? v : '-' },
    { title: 'Expiration', dataIndex: 'expiration', width: 100, render: (v: number) => v > 0 ? new Date(v).toLocaleDateString() : '-' },
    {
      title: 'Actions', key: 'actions', width: 120,
      render: (_: unknown, r: InventoryItem) => (
        <Space size="small">
          <Button size="small" icon={<EditOutlined />} onClick={() => {
            setEditingItem(r)
            const initVals: Record<string, number> = { quantity: r.quantity, position: r.position }
            if (r.equipment) Object.entries(r.equipment).forEach(([k, v]) => { initVals[k] = v })
            editForm.setFieldsValue(initVals)
            setEditOpen(true)
          }}>Edit</Button>
          <Button size="small" danger icon={<DeleteOutlined />}
            onClick={() => handleDelete(r.id)}>Delete</Button>
        </Space>
      ),
    },
  ]

  const eqColumns = ['upgradeslots', 'level', 'str', 'dex', 'item_int', 'luk', 'hp', 'mp', 'watk', 'matk', 'wdef', 'mdef', 'acc', 'avoid', 'hands', 'speed', 'jump']

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Inventory Management</h2>

      <Card className="mb-4">
        <Row gutter={16} align="middle">
          <Col flex="auto">
            <Space>
              <Select
                showSearch
                placeholder="Search character by name or ID"
                style={{ width: 320 }}
                value={selectedChar?.id}
                onSearch={(val) => { setCharSearch(val); searchCharacters(val) }}
                onChange={(val) => {
                  const c = characters.find(ch => ch.id === val)
                  setSelectedChar(c || null)
                }}
                filterOption={false}
                notFoundContent={null}
                options={characters.map(c => ({
                  value: c.id,
                  label: `${c.name} (ID: ${c.id}, Lv.${c.level}, ${c.online ? 'Online' : 'Offline'})`,
                }))}
                allowClear
                onClear={() => { setSelectedChar(null); setData([]) }}
              />
              {selectedChar && (
                <Tag color={selectedChar.online ? 'green' : 'default'}>
                  {selectedChar.online ? 'Online' : 'Offline'}
                </Tag>
              )}
            </Space>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={fetchData} disabled={!selectedChar}>Refresh</Button>
          </Col>
        </Row>
      </Card>

      {selectedChar && (
        <Card>
          <Tabs
            activeKey={activeType}
            onChange={(key) => setActiveType(key)}
            items={types.map(t => ({
              key: String(t.inventoryType),
              label: t.name,
            }))}
          />
          <Table
            dataSource={data}
            columns={columns}
            rowKey="id"
            loading={loading}
            pagination={false}
            size="small"
            locale={{ emptyText: 'No items found' }}
            expandable={{
              expandedRowRender: (record: InventoryItem) => record.equipment ? (
                <div className="p-2 bg-gray-50">
                  {eqColumns.map(key => (
                    <Tag key={key} style={{ marginBottom: 4 }}>
                      {key}: {record.equipment?.[key] ?? '-'}
                    </Tag>
                  ))}
                </div>
              ) : null,
              rowExpandable: (record: InventoryItem) => !!record.equipment,
            }}
          />
        </Card>
      )}

      <Modal title="Edit Item" open={editOpen} onCancel={() => setEditOpen(false)} onOk={() => editForm.submit()} width={600}>
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="quantity" label="Quantity"><InputNumber className="w-full" /></Form.Item></Col>
            <Col span={12}><Form.Item name="position" label="Position"><InputNumber className="w-full" /></Form.Item></Col>
          </Row>
          {editingItem?.equipment && (
            <>
              <h4 className="mb-2">Equipment Stats</h4>
              <Row gutter={16}>
                {eqColumns.map(key => (
                  <Col span={8} key={key}>
                    <Form.Item name={key} label={key}>
                      <InputNumber className="w-full" />
                    </Form.Item>
                  </Col>
                ))}
              </Row>
            </>
          )}
        </Form>
      </Modal>
    </div>
  )
}
