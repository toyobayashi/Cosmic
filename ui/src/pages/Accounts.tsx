import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Input, Space, Modal, Form, Tag, message, Popconfirm, Select, Row, Col, DatePicker, Tooltip } from 'antd'
import { PlusOutlined, SearchOutlined, ReloadOutlined, StopOutlined, CheckOutlined } from '@ant-design/icons'
import api from '../api/client'
import dayjs from 'dayjs'

interface AccountInfo {
  id: number
  name: string
  email: string | null
  gender: number
  loggedin: number
  lastlogin: string | null
  createdat: string
  birthday: string | null
  banned: number
  banreason: string | null
  characterslots: number
  webadmin: number
  nick: string | null
  mute: number
  nxCredit: number | null
  maplePoint: number | null
  nxPrepaid: number | null
  rewardpoints: number
  votepoints: number
  language: number
}

interface PageData {
  records: AccountInfo[]
  total: number
  page: number
  size: number
  pages: number
}

export default function Accounts() {
  const [data, setData] = useState<PageData>({ records: [], total: 0, page: 1, size: 10, pages: 0 })
  const [loading, setLoading] = useState(false)
  const [searchName, setSearchName] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [banOpen, setBanOpen] = useState(false)
  const [selectedAccount, setSelectedAccount] = useState<AccountInfo | null>(null)
  const [createForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [banForm] = Form.useForm()

  const fetchData = useCallback(async (page = 1, size = 10) => {
    setLoading(true)
    try {
      const res = await api.get('/account/v1', { params: { page, size, name: searchName || undefined } })
      setData(res.data.data)
    } catch {
      message.error('Failed to load accounts')
    } finally {
      setLoading(false)
    }
  }, [searchName])

  useEffect(() => { fetchData() }, [fetchData])

  const handleCreate = async (values: Record<string, string>) => {
    try {
      const res = await api.post('/account/v1', { data: values })
      if (res.data.code === 200) {
        message.success('Account created')
        setCreateOpen(false)
        createForm.resetFields()
        fetchData()
      } else {
        message.error(res.data.message)
      }
    } catch {
      message.error('Failed to create account')
    }
  }

  const handleEdit = async (values: Record<string, unknown>) => {
    if (!selectedAccount) return
    const data = { ...values }
    if (data.birthday && dayjs.isDayjs(data.birthday)) {
      data.birthday = (data.birthday as dayjs.Dayjs).format('YYYY-MM-DD')
    }
    try {
      const res = await api.put(`/account/v1/${selectedAccount.id}`, { data })
      if (res.data.code === 200) {
        message.success('Account updated')
        setEditOpen(false)
        fetchData()
      } else {
        message.error(res.data.message)
      }
    } catch {
      message.error('Failed to update account')
    }
  }

  const handleBan = async (values: { reason: string }) => {
    if (!selectedAccount) return
    try {
      await api.put(`/account/v1/${selectedAccount.id}/ban`, { data: values })
      message.success('Account banned')
      setBanOpen(false)
      fetchData()
    } catch {
      message.error('Failed to ban account')
    }
  }

  const handleUnban = async (id: number) => {
    try {
      await api.put(`/account/v1/${id}/unban`)
      message.success('Account unbanned')
      fetchData()
    } catch {
      message.error('Failed to unban account')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await api.delete(`/account/v1/${id}`)
      message.success('Account deleted')
      fetchData()
    } catch {
      message.error('Failed to delete account')
    }
  }

  const handleResetLogin = async (id: number) => {
    try {
      await api.put(`/account/v1/${id}/reset/logged`)
      message.success('Login state reset')
      fetchData()
    } catch {
      message.error('Failed to reset login state')
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (v: string | null) => v || '-' },
    {
      title: 'Banned',
      dataIndex: 'banned',
      key: 'banned',
      width: 80,
      render: (v: number, record: AccountInfo) => v ? (
        <Tooltip title={record.banreason || 'No reason provided'}>
          <Tag color="red">Yes</Tag>
        </Tooltip>
      ) : <Tag color="green">No</Tag>,
    },
    {
      title: 'Online',
      dataIndex: 'loggedin',
      key: 'loggedin',
      render: (v: number) => v ? <Tag color="blue">Yes</Tag> : <Tag>No</Tag>,
    },
    {
      title: 'GM',
      dataIndex: 'webadmin',
      key: 'webadmin',
      render: (v: number) => v ? <Tag color="gold">Admin</Tag> : null,
    },
    { title: 'Last Login', dataIndex: 'lastlogin', key: 'lastlogin', render: (v: string | null) => v || '-' },
    { title: 'Created', dataIndex: 'createdat', key: 'createdat', render: (v: string) => v?.substring(0, 19) || '-' },
    { title: 'Ban Reason', dataIndex: 'banreason', key: 'banreason', render: (v: string | null) => v || '-' },
    {
      title: 'Actions',
      key: 'actions',
      width: 320,
      render: (_: unknown, record: AccountInfo) => (
        <Space size="small">
          <Button size="small" onClick={() => {
            const vals = { ...record, birthday: record.birthday ? dayjs(record.birthday) : undefined }
            setSelectedAccount(record); editForm.setFieldsValue(vals); setEditOpen(true)
          }}>
            Edit
          </Button>
          {record.banned ? (
            <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleUnban(record.id)}>
              Unban
            </Button>
          ) : (
            <Button size="small" danger icon={<StopOutlined />} onClick={() => { setSelectedAccount(record); setBanOpen(true) }}>
              Ban
            </Button>
          )}
          <Button size="small" onClick={() => handleResetLogin(record.id)}>Reset Login</Button>
          <Popconfirm title="Delete this account?" onConfirm={() => handleDelete(record.id)} okType="danger">
            <Button size="small" danger>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Accounts</h2>

      <Row gutter={16} className="mb-4">
        <Col flex="auto">
          <Input.Search
            placeholder="Search by name"
            allowClear
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            onSearch={() => fetchData()}
            prefix={<SearchOutlined />}
            style={{ maxWidth: 320 }}
          />
        </Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => fetchData(data.page, data.size)}>Refresh</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Create Account</Button>
          </Space>
        </Col>
      </Row>

      <Table
        dataSource={data.records}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: data.page,
          pageSize: data.size,
          total: data.total,
          showSizeChanger: true,
          onChange: (page, size) => fetchData(page, size),
        }}
      />

      <Modal title="Create Account" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => createForm.submit()}>
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true, min: 6 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="email" label="Email">
            <Input />
          </Form.Item>
          <Form.Item name="birthday" label="Birthday">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="gender" label="Gender">
            <Select allowClear placeholder="Select gender">
              <Select.Option value={0}>Male</Select.Option>
              <Select.Option value={1}>Female</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Edit Account" open={editOpen} onCancel={() => setEditOpen(false)} onOk={() => editForm.submit()} width={600}>
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="name" label="Username"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="email" label="Email"><Input /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="pin" label="PIN"><Input maxLength={10} /></Form.Item></Col>
            <Col span={12}><Form.Item name="pic" label="PIC"><Input maxLength={26} /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="nick" label="Nickname"><Input maxLength={20} /></Form.Item></Col>
            <Col span={12}><Form.Item name="mute" label="Mute"><Select allowClear><Select.Option value={0}>No</Select.Option><Select.Option value={1}>Yes</Select.Option></Select></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="banned" label="Banned"><Select allowClear><Select.Option value={0}>No</Select.Option><Select.Option value={1}>Yes</Select.Option></Select></Form.Item></Col>
            <Col span={12}><Form.Item name="webadmin" label="Web Admin"><Select allowClear><Select.Option value={0}>No</Select.Option><Select.Option value={1}>Yes</Select.Option></Select></Form.Item></Col>
          </Row>
          <Form.Item name="banreason" label="Ban Reason"><Input.TextArea rows={2} /></Form.Item>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="nxCredit" label="NX Credit"><Input type="number" /></Form.Item></Col>
            <Col span={8}><Form.Item name="maplePoint" label="Maple Points"><Input type="number" /></Form.Item></Col>
            <Col span={8}><Form.Item name="nxPrepaid" label="NX Prepaid"><Input type="number" /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="rewardpoints" label="Reward Pts"><Input type="number" /></Form.Item></Col>
            <Col span={8}><Form.Item name="votepoints" label="Vote Pts"><Input type="number" /></Form.Item></Col>
            <Col span={8}><Form.Item name="characterslots" label="Char Slots"><Input type="number" /></Form.Item></Col>
          </Row>
          <Form.Item name="birthday" label="Birthday"><DatePicker style={{ width: '100%' }} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Ban Account" open={banOpen} onCancel={() => setBanOpen(false)} onOk={() => banForm.submit()}>
        <Form form={banForm} layout="vertical" onFinish={handleBan}>
          <Form.Item name="reason" label="Reason" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
