import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Radio, Input, Tag, Card, Modal, Form, Select, message } from 'antd'
import { SearchOutlined, ReloadOutlined, EditOutlined, SaveOutlined } from '@ant-design/icons'
import api from '../api/client'

interface ConfigEntry {
  id: number
  configType: string
  configSubType: string
  configClazz: string
  configCode: string
  configValue: string
  configDesc: string
}

const clazzLabels: Record<string, string> = {
  'java.lang.Integer': 'Int',
  'java.lang.Long': 'Long',
  'java.lang.Float': 'Float',
  'java.lang.Double': 'Double',
  'java.lang.Boolean': 'Boolean',
  'java.lang.String': 'String',
}

function getClzTag(clz: string) {
  let color = 'blue'
  switch (clz) {
    case 'java.lang.Integer': case 'java.lang.Long': color = 'blue'; break
    case 'java.lang.Float': case 'java.lang.Double': color = 'cyan'; break
    case 'java.lang.Boolean': color = 'red'; break
    default: color = 'green'
  }
  return <Tag color={color}>{clazzLabels[clz] || clz}</Tag>
}

const worldNames = ['Scania', 'Bera', 'Broa', 'Windia', 'Khaini', 'Bellocan', 'Mardia', 'Kradia', 'Yellonde', 'Demethos',
  'Galicia', 'Kastia', 'Judis', 'Arcenia', 'Plana', 'El Nido', 'Kalluna', 'Stius', 'Croa', 'Zenith', 'Medere']

const serverCategories = ['Database', 'Network', 'Rates', 'Features', 'GM Security',
  'Events & PQs', 'Cash Shop', 'Scroll & Skill', 'Quest', 'Character', 'Equipment',
  'Guild', 'Family', 'Wedding', 'Pet', 'Maker', 'Miscellaneous']

export default function Config() {
  const [selectedType, setSelectedType] = useState('')
  const [selectedSubType, setSelectedSubType] = useState('')
  const [searchText, setSearchText] = useState('')
  const [filterKeyword, setFilterKeyword] = useState('')
  const [data, setData] = useState<ConfigEntry[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [editOpen, setEditOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<ConfigEntry | null>(null)
  const [editForm] = Form.useForm()

  const fetchData = useCallback(async (p?: number, ps?: number) => {
    setLoading(true)
    const cp = p ?? page
    const cs = ps ?? pageSize
    try {
      const res = await api.get('/config/v1', {
        params: {
          type: selectedType || undefined,
          subType: selectedSubType || undefined,
          filter: filterKeyword || undefined,
          page: cp,
          size: cs,
        },
      })
      setData(res.data.data?.records || [])
      setTotal(res.data.data?.totalRow || 0)
    } catch {
      message.error('Failed to load config')
    } finally {
      setLoading(false)
    }
  }, [selectedType, selectedSubType, filterKeyword])

  useEffect(() => { fetchData(1, pageSize) }, [fetchData])

  const handleTypeChange = (value: string) => {
    setSelectedType(value)
    setSelectedSubType('')
    setPage(1)
  }

  const handleSubTypeChange = (value: string) => {
    setSelectedSubType(value)
    setPage(1)
  }

  const handleSearch = () => {
    setFilterKeyword(searchText)
    setPage(1)
  }

  const handleEdit = async (values: { configValue: string }) => {
    if (!editingConfig) return
    try {
      await api.put('/config/v1', {
        data: {
          configType: editingConfig.configType,
          configSubType: editingConfig.configSubType,
          configCode: editingConfig.configCode,
          configValue: values.configValue,
        },
      })
      message.success('Config updated (in memory). Click Save to persist to disk.')
      setEditOpen(false)
      fetchData()
    } catch {
      message.error('Failed to update config')
    }
  }

  const handleSaveToDisk = async () => {
    try {
      await api.post('/config/v1/save')
      message.success('Config saved to disk')
    } catch {
      message.error('Failed to save config to disk')
    }
  }

  const subTypeLabel = (st: string) => {
    const n = parseInt(st)
    return !isNaN(n) && n >= 0 && n < worldNames.length ? `${worldNames[n]} (${st})` : st
  }

  const subTypeOptions = () => {
    if (selectedType === 'world') {
      return Array.from({ length: 21 }, (_, i) => ({ value: String(i), label: `${worldNames[i]} (${i})` }))
    }
    return serverCategories.map(st => ({ value: st, label: st }))
  }

  const columns = [
    {
      title: 'Type', dataIndex: 'configType', width: 80,
      render: (v: string) => <Tag color="orangered">{v}</Tag>,
    },
    {
      title: 'Category/World', dataIndex: 'configSubType', width: 160,
      render: (v: string, record: ConfigEntry) => (
        <Tag color="purple">{record.configType === 'world' ? subTypeLabel(v) : v}</Tag>
      ),
    },
    {
      title: 'Class', dataIndex: 'configClazz', width: 90, align: 'center' as const,
      render: (v: string) => getClzTag(v),
    },
    { title: 'Code', dataIndex: 'configCode', width: 280 },
    {
      title: 'Value', dataIndex: 'configValue', width: 120,
      render: (v: string) => {
        if (v === 'true') return <Tag color="green">true</Tag>
        if (v === 'false') return <Tag color="red">false</Tag>
        return <code>{v}</code>
      },
    },
    /* {
      title: 'Actions', key: 'actions', width: 80, align: 'center' as const,
      render: (_: unknown, record: ConfigEntry) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => {
          setEditingConfig(record)
          editForm.setFieldsValue({ configValue: record.configValue })
          setEditOpen(true)
        }}>
          Edit
        </Button>
      ),
    }, */
  ]

  const isBoolean = editingConfig?.configClazz === 'java.lang.Boolean'

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">Config Management</h2>

      <Card className="mb-4">
        <Space direction="vertical" className="w-full" style={{ width: '100%' }}>
          <Space wrap>
            <span>Type:</span>
            <Radio.Group value={selectedType} onChange={(e) => handleTypeChange(e.target.value)}>
              <Radio.Button value="">All</Radio.Button>
              <Radio.Button value="server">server</Radio.Button>
              <Radio.Button value="world">world</Radio.Button>
            </Radio.Group>
          </Space>
          <Space wrap>
            <span>{selectedType === 'world' ? 'World:' : 'Category:'}</span>
            <Radio.Group value={selectedSubType} onChange={(e) => handleSubTypeChange(e.target.value)}>
              <Radio.Button value="">All</Radio.Button>
              {subTypeOptions().map(opt => (
                <Radio.Button key={opt.value} value={opt.value}>{opt.label}</Radio.Button>
              ))}
            </Radio.Group>
          </Space>
          <Space>
            <Input.Search
              placeholder="Filter by code or value"
              allowClear
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              onSearch={handleSearch}
              onClear={() => { setSearchText(''); setFilterKeyword(''); setPage(1) }}
              prefix={<SearchOutlined />}
              style={{ width: 320 }}
            />
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>Refresh</Button>
            {/* <Button icon={<SaveOutlined />} type="primary" onClick={handleSaveToDisk}>Save to Disk</Button> */}
          </Space>
        </Space>
      </Card>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `Total ${t} items`,
          onChange: (p, ps) => {
            setPage(p)
            setPageSize(ps)
            fetchData(p, ps)
          },
        }}
        size="small"
      />

      <Modal
        title="Edit Config"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => editForm.submit()}
        okText="Save"
      >
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item label="Code">
            <Input value={editingConfig?.configCode} disabled />
          </Form.Item>
          <Form.Item label="Type">
            <Input value={editingConfig?.configType} disabled />
          </Form.Item>
          <Form.Item label="Category">
            <Input value={editingConfig ? subTypeLabel(editingConfig.configSubType) : ''} disabled />
          </Form.Item>
          <Form.Item label="Class">
            <Input value={editingConfig?.configClazz} disabled />
          </Form.Item>
          <Form.Item
            name="configValue"
            label="Value"
            rules={[{ required: true, message: 'Please enter a value' }]}
            extra="Changes take effect in memory immediately. Use 'Save to Disk' to persist across restarts."
          >
            {isBoolean ? (
              <Select>
                <Select.Option value="true">true</Select.Option>
                <Select.Option value="false">false</Select.Option>
              </Select>
            ) : (
              <Input />
            )}
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
