import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Tag, message, Modal, Form, Select, Card, InputNumber } from 'antd'
import { GiftOutlined, ReloadOutlined, GlobalOutlined } from '@ant-design/icons'
import api from '../api/client'

interface OnlineChar {
  id: number; name: string; level: number; job: number; world: number; mapId: number;
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
]

export default function Players() {
  const [onlineChars, setOnlineChars] = useState<OnlineChar[]>([])
  const [loading, setLoading] = useState(false)
  const [giveOpen, setGiveOpen] = useState(false)
  const [giveGlobal, setGiveGlobal] = useState(false)
  const [selectedChar, setSelectedChar] = useState<OnlineChar | null>(null)
  const [giveForm] = Form.useForm()
  const [giveType, setGiveType] = useState('')

  const fetchOnline = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/character/v1/online')
      setOnlineChars(res.data.data || [])
    } catch { message.error('Failed to load online players') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchOnline() }, [fetchOnline])

  const handleGive = async (values: Record<string, number>) => {
    try {
      const payload: Record<string, unknown> = { type: giveType }
      if (giveGlobal) { payload.global = true }
      else if (selectedChar) { payload.characterId = selectedChar.id }
      if (giveType === 'item') { payload.itemId = values.itemId; payload.quantity = values.quantity || 1 }
      else { payload.quantity = values.quantity }
      await api.post('/give/v1/resource', { data: payload })
      message.success('Resource granted')
      setGiveOpen(false); giveForm.resetFields()
    } catch { message.error('Failed to give resource') }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 }, { title: 'Name', dataIndex: 'name' },
    { title: 'Level', dataIndex: 'level' }, { title: 'Job', dataIndex: 'job', render: (v: number) => <Tag>{v}</Tag> },
    { title: 'World', dataIndex: 'world' }, { title: 'Map', dataIndex: 'mapId' },
    { title: 'Actions', key: 'actions', render: (_: unknown, r: OnlineChar) => (
      <Button size="small" icon={<GiftOutlined />}
        onClick={() => { setSelectedChar(r); setGiveGlobal(false); setGiveOpen(true); setGiveType(''); giveForm.resetFields() }}>
        Give
      </Button>
    )},
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
        <Table dataSource={onlineChars} columns={columns} rowKey="id" loading={loading} pagination={false} size="small"
          locale={{ emptyText: 'No players online' }} />
      </Card>
      <Modal title={giveGlobal ? 'Global Give' : `Give to: ${selectedChar?.name || ''}`}
        open={giveOpen} onCancel={() => setGiveOpen(false)} onOk={() => giveForm.submit()}>
        <Form form={giveForm} layout="vertical" onFinish={handleGive}>
          <Form.Item label="Type" required>
            <Select options={giveTypes} value={giveType} onChange={setGiveType} placeholder="Select type" />
          </Form.Item>
          {giveType === 'item' && (
            <Form.Item name="itemId" label="Item ID" rules={[{ required: true }]}>
              <InputNumber className="w-full" />
            </Form.Item>
          )}
          {giveType && (
            <Form.Item name="quantity" label="Quantity/Value" rules={[{ required: true }]}>
              <InputNumber className="w-full" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}
