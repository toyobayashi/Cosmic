import { useState, useCallback } from 'react'
import { Select, Spin } from 'antd'
import api from '../api/client'

interface Props {
  searchType?: 'Mob' | 'Item'
  placeholder?: string
  value?: number | null
  onChange?: (value: number | null) => void
  style?: React.CSSProperties
}

let searchTimer: ReturnType<typeof setTimeout> | null = null

export default function GameIdInput({ searchType, placeholder, value, onChange, style }: Props) {
  const [options, setOptions] = useState<{ value: number; label: string }[]>([])
  const [searching, setSearching] = useState(false)
  const [searchText, setSearchText] = useState('')

  const handleSearch = useCallback((text: string) => {
    setSearchText(text)
    if (!text || text.length < 2) { setOptions([]); return }
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(async () => {
      setSearching(true)
      try {
        const params: Record<string, string> = { keyword: text }
        if (searchType === 'Mob') params.types = 'mob'
        else if (searchType === 'Item') params.types = 'item'
        const res = await api.get('/common/v1/informationSearch', { params })
        const items = (res.data.data || []).slice(0, 50).map((r: { id: number; name: string; type: string }) => ({
          value: r.id,
          label: `${r.id} - ${r.name}`,
        }))
        setOptions(items)
      } catch { setOptions([]) }
      finally { setSearching(false) }
    }, 400)
  }, [searchType])

  return (
    <Select
      showSearch
      allowClear
      value={value}
      onChange={(v) => onChange?.(v ?? null)}
      onSearch={handleSearch}
      onClear={() => { setOptions([]); setSearchText('') }}
      placeholder={placeholder || 'Type name or ID'}
      style={style}
      filterOption={false}
      notFoundContent={searching ? <Spin size="small" /> : (searchText.length < 2 ? 'Type at least 2 chars' : 'No results')}
      options={options}
      popupMatchSelectWidth={false}
    />
  )
}
