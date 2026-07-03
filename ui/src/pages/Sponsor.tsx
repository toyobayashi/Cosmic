import { useCallback, useEffect, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd'
import { CheckOutlined, CloseOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import api from '../api/client'

interface SponsorOrder {
  orderId: string
  account: string
  amountCents: number
  expectedNx: number
  awardedNx: number | null
  status: string
  note: string | null
  createdAt: string | null
  updatedAt: string | null
  updatedBy: number | null
}

export default function Sponsor() {
  const [orders, setOrders] = useState<SponsorOrder[]>([])
  const [loading, setLoading] = useState(false)
  const [accountFilter, setAccountFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<string | undefined>()
  const [appliedAccountFilter, setAppliedAccountFilter] = useState('')
  const [appliedStatusFilter, setAppliedStatusFilter] = useState<string | undefined>()
  const [awardOpen, setAwardOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<SponsorOrder | null>(null)
  const [awardForm] = Form.useForm<{ awardedNx: number }>()

  const fetchOrders = useCallback(async (account: string, status?: string) => {
    setLoading(true)
    try {
      const params: { account?: string; status?: string } = {}
      if (account.trim()) params.account = account.trim()
      if (status) params.status = status

      const res = await api.get('/sponsor/v1/orders', { params })
      if (res.data.code === 200) {
        setOrders(res.data.data || [])
      } else {
        message.error(res.data.message)
      }
    } catch {
      message.error('Failed to load sponsor orders')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchOrders('', undefined) }, [fetchOrders])

  const handleSearch = () => {
    setAppliedAccountFilter(accountFilter)
    setAppliedStatusFilter(statusFilter)
    fetchOrders(accountFilter, statusFilter)
  }

  const handleRefresh = () => {
    fetchOrders(appliedAccountFilter, appliedStatusFilter)
  }

  const openAward = (order: SponsorOrder) => {
    setSelectedOrder(order)
    awardForm.setFieldsValue({ awardedNx: order.expectedNx })
    setAwardOpen(true)
  }

  const handleAward = async (values: { awardedNx: number }) => {
    if (!selectedOrder) return

    try {
      const res = await api.post(`/sponsor/v1/orders/${selectedOrder.orderId}/award`, {
        data: { awardedNx: values.awardedNx },
      })
      if (res.data.code === 200) {
        message.success('Sponsor order awarded')
        setAwardOpen(false)
        setSelectedOrder(null)
        handleRefresh()
      } else {
        message.error(res.data.message)
      }
    } catch {
      message.error('Failed to award sponsor order')
    }
  }

  const handleClose = async (order: SponsorOrder) => {
    try {
      const res = await api.post(`/sponsor/v1/orders/${order.orderId}/close`)
      if (res.data.code === 200) {
        message.success('Sponsor order closed')
        handleRefresh()
      } else {
        message.error(res.data.message)
      }
    } catch {
      message.error('Failed to close sponsor order')
    }
  }

  const columns = [
    { title: 'Order ID', dataIndex: 'orderId', key: 'orderId', width: 210 },
    { title: 'Account', dataIndex: 'account', key: 'account', width: 150 },
    {
      title: 'Amount',
      dataIndex: 'amountCents',
      key: 'amountCents',
      render: (value: number) => `${(value / 100).toFixed(2)} CNY`,
    },
    { title: 'Expected NX', dataIndex: 'expectedNx', key: 'expectedNx' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => {
        const color = value === 'PENDING' ? 'gold' : value === 'CLOSED' ? 'red' : 'green'
        return <Tag color={color}>{value}</Tag>
      },
    },
    { title: 'Awarded NX', dataIndex: 'awardedNx', key: 'awardedNx', render: (v: number | null) => v ?? '-' },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', render: (v: string | null) => v?.substring(0, 19) || '-' },
    { title: 'Updated', dataIndex: 'updatedAt', key: 'updatedAt', render: (v: string | null) => v?.substring(0, 19) || '-' },
    {
      title: 'Action',
      key: 'action',
      width: 190,
      render: (_: unknown, record: SponsorOrder) => (
        record.status === 'PENDING' ? (
          <Space size="small">
            <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => openAward(record)}>
              Award
            </Button>
            <Popconfirm
              title="Close this unpaid order?"
              onConfirm={() => handleClose(record)}
              okText="Close"
              okType="danger"
            >
              <Button size="small" danger icon={<CloseOutlined />}>
                Close
              </Button>
            </Popconfirm>
          </Space>
        ) : '-'
      ),
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold m-0">Sponsor</h2>
        <Space>
          <Input
            placeholder="Account"
            value={accountFilter}
            onChange={(event) => setAccountFilter(event.target.value)}
            allowClear
          />
          <Select
            placeholder="All statuses"
            allowClear
            className="w-36"
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'PENDING', label: 'Pending' },
              { value: 'AWARDED', label: 'Awarded' },
              { value: 'CLOSED', label: 'Closed' },
            ]}
          />
          <Button icon={<SearchOutlined />} onClick={handleSearch}>Search</Button>
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>Refresh</Button>
        </Space>
      </div>

      <Table
        dataSource={orders}
        columns={columns}
        rowKey="orderId"
        loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: true }}
      />

      <Modal
        title={selectedOrder ? `Award sponsor order #${selectedOrder.orderId}` : 'Award sponsor order'}
        open={awardOpen}
        onCancel={() => setAwardOpen(false)}
        onOk={() => awardForm.submit()}
        okText="Award"
      >
        {selectedOrder && (
          <div className="mb-4">
            <div>Account: {selectedOrder.account}</div>
            <div>Amount: {(selectedOrder.amountCents / 100).toFixed(2)} CNY</div>
            <div>Default NX: {selectedOrder.expectedNx}</div>
          </div>
        )}
        <Form form={awardForm} layout="vertical" onFinish={handleAward}>
          <Form.Item
            name="awardedNx"
            label="Awarded NX"
            rules={[{ required: true, message: 'Please enter awarded NX' }]}
          >
            <InputNumber min={1} max={1000000} precision={0} className="w-full" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
