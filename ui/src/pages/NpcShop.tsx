import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Modal, Form, InputNumber, message, Row, Col } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import GameIdInput from '../components/GameIdInput'
import api from '../api/client'

interface ShopItem {
  shopitemid: number; shopid: number; itemid: number; price: number; pitch: number; position: number;
}

export default function NpcShop() {
  const [shops, setShops] = useState<{ shopid: number; npcid: number }[]>([])
  const [items, setItems] = useState<ShopItem[]>([])
  const [selectedShop, setSelectedShop] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [addOpen, setAddOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<ShopItem | null>(null)
  const [addForm] = Form.useForm()
  const [editForm] = Form.useForm()

  const fetchShops = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/shop/v1/list')
      setShops(res.data.data || [])
    } catch { message.error('Failed to load shops') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchShops() }, [fetchShops])

  const fetchItems = async (shopId: number) => {
    setSelectedShop(shopId)
    setLoading(true)
    try {
      const res = await api.get('/shop/v1/items', { params: { shopId } })
      setItems(res.data.data || [])
    } catch { message.error('Failed to load items') }
    finally { setLoading(false) }
  }

  const handleAdd = async (values: { itemId: number; price: number; pitch: number; position: number }) => {
    try {
      await api.post('/shop/v1/item', { ...values, shopId: selectedShop })
      message.success('Item added')
      setAddOpen(false); addForm.resetFields()
      if (selectedShop) fetchItems(selectedShop)
    } catch { message.error('Failed to add item') }
  }

  const handleEdit = async (values: { itemId: number; price: number; pitch: number; position: number }) => {
    if (!editingItem) return
    try {
      await api.put(`/shop/v1/item/${editingItem.shopitemid}`, values)
      message.success('Item updated')
      setEditOpen(false)
      if (selectedShop) fetchItems(selectedShop)
    } catch { message.error('Failed to update item') }
  }

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/shop/v1/item/${id}`)
      message.success('Item deleted')
      if (selectedShop) fetchItems(selectedShop)
    } catch { message.error('Failed to delete item') }
  }

  const shopColumns = [
    { title: 'Shop ID', dataIndex: 'shopid', width: 80 },
    { title: 'NPC ID', dataIndex: 'npcid', width: 100 },
    { title: 'NPC Name', dataIndex: 'npcName', ellipsis: true },
    { title: 'Actions', key: 'actions', render: (_: unknown, r: { shopid: number }) => (
      <Button size="small" onClick={() => fetchItems(r.shopid)}>View Items</Button>
    )},
  ]

  const itemColumns = [
    { title: 'ID', dataIndex: 'shopitemid', width: 60 },
    { title: 'Item ID', dataIndex: 'itemid', width: 80 },
    { title: 'Item Name', dataIndex: 'itemName', ellipsis: true },
    { title: 'Price', dataIndex: 'price', width: 80 },
    { title: 'Pitch', dataIndex: 'pitch', width: 60 },
    { title: 'Pos', dataIndex: 'position', width: 60 },
    { title: 'Actions', key: 'actions', width: 150, render: (_: unknown, r: ShopItem) => (
      <Space>
        <Button size="small" onClick={() => { setEditingItem(r); editForm.setFieldsValue(r); setEditOpen(true) }}>Edit</Button>
        <Button size="small" danger onClick={() => handleDelete(r.shopitemid)}>Delete</Button>
      </Space>
    )},
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">NPC Shop Management</h2>
      <Row gutter={24}>
        <Col span={10}>
          <Table dataSource={shops} columns={shopColumns} rowKey="shopid" loading={loading} pagination={{ pageSize: 15 }} size="small" />
        </Col>
        <Col span={14}>
          {selectedShop && (
            <>
              <Space className="mb-3">
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add Item</Button>
                <Button icon={<ReloadOutlined />} onClick={() => fetchItems(selectedShop)}>Refresh</Button>
              </Space>
              <Table dataSource={items} columns={itemColumns} rowKey="shopitemid" loading={loading} pagination={false} size="small" />
            </>
          )}
        </Col>
      </Row>
      <Modal title="Add Shop Item" open={addOpen} onCancel={() => setAddOpen(false)} onOk={() => addForm.submit()}>
        <Form form={addForm} layout="vertical" onFinish={handleAdd}>
          <Form.Item name="itemId" label="Item" rules={[{ required: true }]}><GameIdInput searchType="Item" placeholder="Search item..." /></Form.Item>
          <Form.Item name="price" label="Price" rules={[{ required: true }]}><InputNumber className="w-full" /></Form.Item>
          <Form.Item name="pitch" label="Pitch"><InputNumber className="w-full" /></Form.Item>
          <Form.Item name="position" label="Position"><InputNumber className="w-full" /></Form.Item>
        </Form>
      </Modal>
      <Modal title="Edit Shop Item" open={editOpen} onCancel={() => setEditOpen(false)} onOk={() => editForm.submit()}>
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item name="itemId" label="Item"><GameIdInput searchType="Item" placeholder="Search item..." /></Form.Item>
          <Form.Item name="price" label="Price"><InputNumber className="w-full" /></Form.Item>
          <Form.Item name="pitch" label="Pitch"><InputNumber className="w-full" /></Form.Item>
          <Form.Item name="position" label="Position"><InputNumber className="w-full" /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
