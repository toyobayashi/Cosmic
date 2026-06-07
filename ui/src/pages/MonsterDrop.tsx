import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Input, Space, Modal, Form, InputNumber, message } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import GameIdInput from '../components/GameIdInput'
import api from '../api/client'

interface DropEntry {
  id: number; dropperid: number; itemid: number; chance: number;
  minimum_quantity: number; maximum_quantity: number; questid: number;
}

export default function MonsterDrop() {
  const [data, setData] = useState<{ records: DropEntry[]; total: number; page: number; size: number }>(
    { records: [], total: 0, page: 1, size: 20 })
  const [loading, setLoading] = useState(false)
  const [searchMonster, setSearchMonster] = useState<number | null>(null)
  const [searchItem, setSearchItem] = useState<number | null>(null)
  const [addOpen, setAddOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState<DropEntry | null>(null)
  const [addForm] = Form.useForm()
  const [editForm] = Form.useForm()

  const fetchData = useCallback(async (page = 1, size = 20) => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = { page, size }
      if (searchMonster) params.dropperId = searchMonster
      if (searchItem) params.itemId = searchItem
      const res = await api.get('/drop/v1/list', { params })
      setData(res.data.data)
    } catch { message.error('Failed to load drops') }
    finally { setLoading(false) }
  }, [searchMonster, searchItem])

  useEffect(() => { fetchData() }, [fetchData])

  const handleAdd = async (values: Record<string, number>) => {
    try {
      await api.post('/drop/v1', values)
      message.success('Drop added'); setAddOpen(false); addForm.resetFields(); fetchData()
    } catch { message.error('Failed to add drop') }
  }

  const handleEdit = async (values: Record<string, number>) => {
    if (!editing) return
    try {
      await api.put(`/drop/v1/${editing.id}`, values)
      message.success('Drop updated'); setEditOpen(false); fetchData()
    } catch { message.error('Failed to update drop') }
  }

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/drop/v1/${id}`)
      message.success('Drop deleted'); fetchData()
    } catch { message.error('Failed to delete drop') }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: 'Monster ID', dataIndex: 'dropperid', width: 90 },
    { title: 'Monster Name', dataIndex: 'mobName', ellipsis: true },
    { title: 'Item ID', dataIndex: 'itemid', width: 80 },
    { title: 'Item Name', dataIndex: 'itemName', ellipsis: true },
    { title: 'Chance', dataIndex: 'chance', width: 70 },
    { title: 'Min', dataIndex: 'minimum_quantity', width: 50 },
    { title: 'Max', dataIndex: 'maximum_quantity', width: 50 },
    { title: 'Quest', dataIndex: 'questid', width: 60 },
    { title: 'Actions', key: 'actions', width: 150, render: (_: unknown, r: DropEntry) => (
      <Space>
        <Button size="small" onClick={() => { setEditing(r); editForm.setFieldsValue(r); setEditOpen(true) }}>Edit</Button>
        <Button size="small" danger onClick={() => handleDelete(r.id)}>Delete</Button>
      </Space>
    )},
  ]

  const formFields = <>
    <Form.Item name="dropperId" label="Monster" rules={[{ required: true }]}><GameIdInput searchType="Mob" placeholder="Search monster..." /></Form.Item>
    <Form.Item name="itemId" label="Item" rules={[{ required: true }]}><GameIdInput searchType="Item" placeholder="Search item..." /></Form.Item>
    <Form.Item name="chance" label="Chance" rules={[{ required: true }]}><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="minQty" label="Min Quantity"><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="maxQty" label="Max Quantity"><InputNumber className="w-full" /></Form.Item>
    <Form.Item name="questId" label="Quest ID"><InputNumber className="w-full" /></Form.Item>
  </>

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Monster Drop</h2>
      <Space className="mb-4" wrap>
        <GameIdInput searchType="Mob" placeholder="Monster name or ID" value={searchMonster} onChange={setSearchMonster} style={{ width: 240 }} />
        <GameIdInput searchType="Item" placeholder="Item name or ID" value={searchItem} onChange={setSearchItem} style={{ width: 240 }} />
        <Button type="primary" onClick={() => fetchData()}>Search</Button>
        <Button icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add</Button>
        <Button icon={<ReloadOutlined />} onClick={() => fetchData(data.page, data.size)}>Refresh</Button>
      </Space>
      <Table dataSource={data.records} columns={columns} rowKey="id" loading={loading}
        pagination={{ current: data.page, pageSize: data.size, total: data.total, showSizeChanger: true, onChange: (p, s) => fetchData(p, s) }} />
      <Modal title="Add Drop" open={addOpen} onCancel={() => setAddOpen(false)} onOk={() => addForm.submit()}>
        <Form form={addForm} layout="vertical" onFinish={handleAdd}>{formFields}</Form>
      </Modal>
      <Modal title="Edit Drop" open={editOpen} onCancel={() => setEditOpen(false)} onOk={() => editForm.submit()}>
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>{formFields}</Form>
      </Modal>
    </div>
  )
}
