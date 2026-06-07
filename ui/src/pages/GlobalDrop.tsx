import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Input, Space, Modal, Form, InputNumber, message } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import GameIdInput from '../components/GameIdInput'
import api from '../api/client'

interface GlobalDropEntry {
  id: number; continent: number; itemid: number; chance: number;
  minimum_quantity: number; maximum_quantity: number; questid: number; comments: string | null;
}

export default function GlobalDrop() {
  const [data, setData] = useState<{ records: GlobalDropEntry[]; total: number; page: number; size: number }>(
    { records: [], total: 0, page: 1, size: 20 })
  const [loading, setLoading] = useState(false)
  const [searchContinent, setSearchContinent] = useState('')
  const [searchItem, setSearchItem] = useState<number | null>(null)
  const [addOpen, setAddOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState<GlobalDropEntry | null>(null)
  const [addForm] = Form.useForm()
  const [editForm] = Form.useForm()

  const fetchData = useCallback(async (page = 1, size = 20) => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = { page, size }
      if (searchContinent) params.continent = parseInt(searchContinent)
      if (searchItem) params.itemId = searchItem
      const res = await api.get('/drop/v1/global/list', { params })
      setData(res.data.data)
    } catch { message.error('Failed to load global drops') }
    finally { setLoading(false) }
  }, [searchContinent, searchItem])

  useEffect(() => { fetchData() }, [fetchData])

  const handleAdd = async (values: Record<string, number | string>) => {
    try {
      await api.post('/drop/v1/global', values)
      message.success('Drop added'); setAddOpen(false); addForm.resetFields(); fetchData()
    } catch { message.error('Failed to add drop') }
  }

  const handleEdit = async (values: Record<string, number | string>) => {
    if (!editing) return
    try {
      await api.put(`/drop/v1/global/${editing.id}`, values)
      message.success('Drop updated'); setEditOpen(false); fetchData()
    } catch { message.error('Failed to update drop') }
  }

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/drop/v1/global/${id}`)
      message.success('Drop deleted'); fetchData()
    } catch { message.error('Failed to delete drop') }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: 'Cont.', dataIndex: 'continent', width: 60 },
    { title: 'Item ID', dataIndex: 'itemid', width: 80 },
    { title: 'Item Name', dataIndex: 'itemName', ellipsis: true },
    { title: 'Chance', dataIndex: 'chance', width: 70 },
    { title: 'Min', dataIndex: 'minimum_quantity', width: 50 },
    { title: 'Max', dataIndex: 'maximum_quantity', width: 50 },
    { title: 'Quest', dataIndex: 'questid', width: 60 },
    { title: 'Comments', dataIndex: 'comments', render: (v: string | null) => v || '-', ellipsis: true },
    { title: 'Actions', key: 'actions', width: 150, render: (_: unknown, r: GlobalDropEntry) => (
      <Space>
        <Button size="small" onClick={() => { setEditing(r); editForm.setFieldsValue(r); setEditOpen(true) }}>Edit</Button>
        <Button size="small" danger onClick={() => handleDelete(r.id)}>Delete</Button>
      </Space>
    )},
  ]

  const formFields = <>
    <Form.Item name="continent" label="Continent" rules={[{ required: true }]}><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="itemId" label="Item" rules={[{ required: true }]}><GameIdInput searchType="Item" placeholder="Search item..." /></Form.Item>
    <Form.Item name="chance" label="Chance" rules={[{ required: true }]}><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="minQty" label="Min Quantity"><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="maxQty" label="Max Quantity"><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="questId" label="Quest ID"><InputNumber className="w-full" /></Form.Item>
  </>

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Global Drop</h2>
      <Space className="mb-4" wrap>
        <Input placeholder="Continent" value={searchContinent} onChange={(e) => setSearchContinent(e.target.value)} style={{ width: 120 }} />
        <GameIdInput searchType="Item" placeholder="Item name or ID" value={searchItem} onChange={setSearchItem} style={{ width: 240 }} />
        <Button type="primary" onClick={() => fetchData()}>Search</Button>
        <Button icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add</Button>
        <Button icon={<ReloadOutlined />} onClick={() => fetchData(data.page, data.size)}>Refresh</Button>
      </Space>
      <Table dataSource={data.records} columns={columns} rowKey="id" loading={loading}
        pagination={{ current: data.page, pageSize: data.size, total: data.total, showSizeChanger: true, onChange: (p, s) => fetchData(p, s) }} />
      <Modal title="Add Global Drop" open={addOpen} onCancel={() => setAddOpen(false)} onOk={() => addForm.submit()}>
        <Form form={addForm} layout="vertical" onFinish={handleAdd}>{formFields}</Form>
      </Modal>
      <Modal title="Edit Global Drop" open={editOpen} onCancel={() => setEditOpen(false)} onOk={() => editForm.submit()}>
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>{formFields}</Form>
      </Modal>
    </div>
  )
}
