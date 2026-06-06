import { useState, useMemo, useCallback } from 'react'
import {
  Plus, Wallet, TrendingUp, TrendingDown, X, Check,
  Trash2, ArrowUpCircle, ArrowDownCircle,
  Pencil, ChevronLeft, ChevronRight, BarChart3, List, RotateCcw,
} from 'lucide-react'
import * as bridge from '../bridge'
import type { FinanceTx, FinanceOverrideKey, FinanceOverrides } from '../bridge'

interface Props {
  transactions: FinanceTx[]
  onRefresh: () => void
}

type Tab = 'overview' | 'income' | 'expenses' | 'history'

// ── Formatters ────────────────────────────────────────────────────────────

function fmtUzs(n: number) {
  return n.toLocaleString('uz-UZ', { maximumFractionDigits: 0 }) + " so'm"
}
function fmtUsd(n: number) {
  return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmt(n: number, cur: string) {
  return cur === 'UZS' ? fmtUzs(n) : fmtUsd(n)
}

// ── Category suggestions ──────────────────────────────────────────────────

const INCOME_CATS  = ['Salary', 'Freelance', 'Business', 'Investment', 'Bonus', 'Gift', 'Other']
const EXPENSE_CATS = ['Food', 'Transport', 'Rent', 'Utilities', 'Health', 'Shopping', 'Entertainment', 'Education', 'Savings', 'Other']

// ── Stat card (with optional manual override editing) ─────────────────────

function StatCard({ label, value, numericValue, overrideKey, overrides, color, sub, dim, onSave, onClear }: {
  label: string
  value: string
  numericValue: number
  overrideKey: FinanceOverrideKey | null
  overrides: FinanceOverrides
  color: string
  sub?: string
  dim?: boolean
  onSave: (key: FinanceOverrideKey, amount: number) => void
  onClear: (key: FinanceOverrideKey) => void
}) {
  const [editing, setEditing]   = useState(false)
  const [inputVal, setInputVal] = useState('')

  const isOverridden = !!overrideKey && overrideKey in overrides

  const startEdit = () => {
    setInputVal(String(numericValue))
    setEditing(true)
  }

  const save = () => {
    const n = parseFloat(inputVal)
    if (isNaN(n) || !overrideKey) return
    onSave(overrideKey, n)
    setEditing(false)
  }

  const cancel = () => setEditing(false)

  return (
    <div className={`card flex-1 min-w-0 p-4 relative group ${dim ? 'opacity-50' : ''}`}>
      {isOverridden && (
        <div className="absolute top-2 right-2 w-1.5 h-1.5 rounded-full bg-accent/70" title="Manually set" />
      )}
      <p className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-2">{label}</p>
      {editing ? (
        <div className="flex items-center gap-1">
          <input
            type="number"
            className="input text-sm flex-1 py-0.5 px-2 min-w-0"
            value={inputVal}
            onChange={e => setInputVal(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') save(); if (e.key === 'Escape') cancel() }}
            autoFocus
          />
          <button onClick={save} className="p-1 rounded hover:bg-white/[0.08] text-emerald-400 transition-colors" title="Save">
            <Check size={13} />
          </button>
          <button onClick={cancel} className="p-1 rounded hover:bg-white/[0.08] text-fg-subtle transition-colors" title="Cancel">
            <X size={13} />
          </button>
        </div>
      ) : (
        <div className="flex items-start gap-1 min-w-0">
          <p className="text-lg font-bold truncate flex-1" style={{ color }}>{value}</p>
          {overrideKey && (
            <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0 mt-0.5">
              {isOverridden && (
                <button onClick={() => onClear(overrideKey)}
                  className="p-1 rounded hover:bg-red-500/10 text-fg-subtle hover:text-red-400 transition-all"
                  title="Reset to calculated">
                  <RotateCcw size={11} />
                </button>
              )}
              <button onClick={startEdit}
                className="p-1 rounded hover:bg-white/[0.08] text-fg-subtle hover:text-accent transition-all"
                title="Set manually">
                <Pencil size={11} />
              </button>
            </div>
          )}
        </div>
      )}
      {sub && !editing && <p className="text-xs text-fg-subtle mt-0.5 truncate">{sub}</p>}
    </div>
  )
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────

function TxnDialog({ initial, onSave, onClose }: {
  initial?: FinanceTx
  onSave: (data: Omit<FinanceTx, 'id' | 'createdAt'> & { id?: number }) => void
  onClose: () => void
}) {
  const today = new Date().toISOString().split('T')[0]
  const editing = !!initial

  const [type,     setType]     = useState<'INCOME' | 'EXPENSE'>(initial?.type ?? 'EXPENSE')
  const [amount,   setAmount]   = useState(initial?.amount?.toString() ?? '')
  const [currency, setCurrency] = useState<'UZS' | 'USD'>((initial?.currency as 'UZS' | 'USD') ?? 'UZS')
  const [category, setCategory] = useState(initial?.category ?? '')
  const [desc,     setDesc]     = useState(initial?.description ?? '')
  const [txnDate,  setDate]     = useState(initial?.txnDate ?? today)
  const [showSug,  setShowSug]  = useState(false)

  const cats = type === 'INCOME' ? INCOME_CATS : EXPENSE_CATS
  const filtered = cats.filter(c => c.toLowerCase().includes(category.toLowerCase()))
  const isValid  = amount.trim() !== '' && Number(amount) > 0 && txnDate !== ''

  const handleSave = () => {
    if (!isValid) return
    const data = {
      type,
      amount: Number(amount),
      currency,
      category:    category.trim()  || null,
      description: desc.trim()      || null,
      txnDate,
    }
    onSave(editing ? { ...data, id: initial!.id } : data)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.72)' }}
      onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className="w-full max-w-sm bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] p-6 space-y-4">

        {/* Header */}
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-fg">{editing ? 'Edit Transaction' : 'Add Transaction'}</h2>
          <button onClick={onClose} className="p-1 rounded text-fg-subtle hover:text-fg transition-colors">
            <X size={16} />
          </button>
        </div>

        {/* Type toggle */}
        <div className="flex gap-2">
          {(['INCOME', 'EXPENSE'] as const).map(t => (
            <button key={t} onClick={() => { setType(t); setCategory('') }}
              className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-semibold transition-all border ${
                type === t
                  ? t === 'INCOME'
                    ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                    : 'bg-red-500/20 text-red-400 border-red-500/40'
                  : 'bg-white/[0.04] text-fg-muted border-transparent hover:bg-white/[0.07]'
              }`}>
              {t === 'INCOME' ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
              {t === 'INCOME' ? 'Income' : 'Expense'}
            </button>
          ))}
        </div>

        {/* Amount + Currency */}
        <div className="flex gap-2">
          <input type="number" min="0" step="any" className="input flex-1" placeholder="Amount *"
            value={amount} onChange={e => setAmount(e.target.value)} autoFocus />
          <select className="input w-24" value={currency} onChange={e => setCurrency(e.target.value as 'UZS' | 'USD')}>
            <option value="UZS">UZS</option>
            <option value="USD">USD</option>
          </select>
        </div>

        {/* Category */}
        <div className="relative">
          <input className="input w-full" placeholder="Category"
            value={category}
            onChange={e => setCategory(e.target.value)}
            onFocus={() => setShowSug(true)}
            onBlur={() => setTimeout(() => setShowSug(false), 150)} />
          {showSug && filtered.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 z-20 bg-surface border border-white/[0.09] rounded-lg shadow-xl overflow-hidden">
              {filtered.map(s => (
                <button key={s} onMouseDown={() => setCategory(s)}
                  className="w-full text-left px-3 py-2 text-xs text-fg-muted hover:bg-white/[0.06] hover:text-fg transition-colors">
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Description */}
        <input className="input w-full" placeholder="Description (optional)"
          value={desc} onChange={e => setDesc(e.target.value)} />

        {/* Date */}
        <div>
          <label className="form-label">Date</label>
          <input type="date" className="input w-full" value={txnDate} onChange={e => setDate(e.target.value)} />
        </div>

        {/* Actions */}
        <div className="flex gap-2 justify-end pt-1">
          <button className="btn-ghost text-sm px-4" onClick={onClose}>Cancel</button>
          <button className="btn-primary text-sm flex items-center gap-1.5 px-4"
            disabled={!isValid} onClick={handleSave}>
            <Check size={14} /> {editing ? 'Update' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Mini bar chart (CSS only) ─────────────────────────────────────────────

interface MonthBar { label: string; income: number; expense: number }

function MiniChart({ bars, currency }: { bars: MonthBar[]; currency: 'UZS' | 'USD' }) {
  const maxVal = Math.max(...bars.map(b => Math.max(b.income, b.expense)), 1)
  return (
    <div className="card p-4">
      <p className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">Monthly Trend</p>
      <div className="flex items-end gap-1.5 h-24">
        {bars.map((b, i) => (
          <div key={i} className="flex-1 flex flex-col items-center gap-0.5 min-w-0">
            <div className="w-full flex gap-0.5 items-end h-20">
              <div className="flex-1 rounded-sm bg-emerald-500/60 transition-all"
                style={{ height: `${Math.max(2, (b.income / maxVal) * 100)}%` }}
                title={`Income: ${fmt(b.income, currency)}`} />
              <div className="flex-1 rounded-sm bg-red-500/60 transition-all"
                style={{ height: `${Math.max(2, (b.expense / maxVal) * 100)}%` }}
                title={`Expense: ${fmt(b.expense, currency)}`} />
            </div>
            <p className="text-[9px] text-fg-subtle truncate w-full text-center">{b.label}</p>
          </div>
        ))}
      </div>
      <div className="flex gap-4 mt-2">
        <span className="flex items-center gap-1 text-[10px] text-fg-subtle">
          <span className="w-2 h-2 rounded-sm bg-emerald-500/60 inline-block" /> Income
        </span>
        <span className="flex items-center gap-1 text-[10px] text-fg-subtle">
          <span className="w-2 h-2 rounded-sm bg-red-500/60 inline-block" /> Expense
        </span>
      </div>
    </div>
  )
}

// ── Category breakdown ────────────────────────────────────────────────────

function CategoryBreakdown({ txns, currency }: { txns: FinanceTx[]; currency: 'UZS' | 'USD' }) {
  const byCat = useMemo(() => {
    const map = new Map<string, number>()
    for (const t of txns) {
      if (t.currency !== currency) continue
      const cat = t.category || 'Uncategorised'
      map.set(cat, (map.get(cat) ?? 0) + t.amount)
    }
    return Array.from(map.entries()).sort(([, a], [, b]) => b - a)
  }, [txns, currency])

  const total = byCat.reduce((s, [, v]) => s + v, 0)
  if (byCat.length === 0) return null

  return (
    <div className="card p-4 space-y-2">
      <p className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-1">By Category ({currency})</p>
      {byCat.map(([cat, val]) => (
        <div key={cat} className="space-y-0.5">
          <div className="flex justify-between text-xs">
            <span className="text-fg-muted">{cat}</span>
            <span className="text-fg font-medium">{fmt(val, currency)}</span>
          </div>
          <div className="h-1 rounded-full bg-white/[0.06]">
            <div className="h-full rounded-full bg-accent/60 transition-all"
              style={{ width: `${(val / total) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

// ── Transaction row ───────────────────────────────────────────────────────

function TxnRow({ txn, onEdit, onDelete }: {
  txn: FinanceTx
  onEdit: (t: FinanceTx) => void
  onDelete: (id: number) => void
}) {
  const isIncome = txn.type === 'INCOME'
  return (
    <div className="card flex items-center gap-3 px-4 py-3 hover:border-white/[0.12] transition-all">
      <div className={`shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
        isIncome ? 'bg-emerald-500/10' : 'bg-red-500/10'
      }`}>
        {isIncome
          ? <ArrowUpCircle size={15} className="text-emerald-400" />
          : <ArrowDownCircle size={15} className="text-red-400" />}
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-fg truncate">
          {txn.description || txn.category || (isIncome ? 'Income' : 'Expense')}
        </p>
        <div className="flex items-center gap-2 mt-0.5">
          {txn.category && (
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/[0.06] text-fg-subtle">{txn.category}</span>
          )}
          {txn.description && txn.category && (
            <span className="text-[10px] text-fg-subtle truncate">{txn.description}</span>
          )}
          <span className="text-[10px] text-fg-subtle/60">{txn.currency}</span>
        </div>
      </div>

      <p className={`text-sm font-bold shrink-0 ${isIncome ? 'text-emerald-400' : 'text-red-400'}`}>
        {isIncome ? '+' : '-'}{fmt(txn.amount, txn.currency)}
      </p>

      <div className="flex items-center gap-1 shrink-0">
        <button onClick={() => onEdit(txn)}
          className="p-1.5 rounded hover:bg-white/[0.08] text-fg-subtle hover:text-accent transition-all"
          title="Edit">
          <Pencil size={12} />
        </button>
        <button onClick={() => onDelete(txn.id)}
          className="p-1.5 rounded hover:bg-red-500/10 text-fg-subtle hover:text-red-400 transition-all"
          title="Delete">
          <Trash2 size={12} />
        </button>
      </div>
    </div>
  )
}

// ── Grouped transaction list ──────────────────────────────────────────────

function TxnList({ txns, onEdit, onDelete, emptyText }: {
  txns: FinanceTx[]
  onEdit: (t: FinanceTx) => void
  onDelete: (id: number) => void
  emptyText: string
}) {
  const grouped = useMemo(() => {
    const map = new Map<string, FinanceTx[]>()
    for (const t of [...txns].sort((a, b) => b.txnDate.localeCompare(a.txnDate))) {
      const arr = map.get(t.txnDate) ?? []
      arr.push(t)
      map.set(t.txnDate, arr)
    }
    return Array.from(map.entries())
  }, [txns])

  if (grouped.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <Wallet size={36} className="text-fg-subtle/20 mb-3" />
        <p className="text-fg-muted text-sm font-medium">{emptyText}</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {grouped.map(([date, rows]) => {
        const d = new Date(date + 'T12:00:00')
        const label = d.toLocaleDateString('en', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
        const dayIncome  = rows.filter(r => r.type === 'INCOME').reduce((s, r) => s + r.amount, 0)
        const dayExpense = rows.filter(r => r.type === 'EXPENSE').reduce((s, r) => s + r.amount, 0)
        const currency   = rows[0]?.currency ?? 'UZS'
        return (
          <div key={date}>
            <div className="flex items-center gap-3 mb-2">
              <p className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase shrink-0">{label}</p>
              <div className="flex-1 h-px bg-white/[0.05]" />
              <div className="flex gap-3 text-[10px] font-semibold shrink-0">
                {dayIncome  > 0 && <span className="text-emerald-400">+{fmt(dayIncome,  currency)}</span>}
                {dayExpense > 0 && <span className="text-red-400">-{fmt(dayExpense, currency)}</span>}
              </div>
            </div>
            <div className="space-y-1">
              {rows.map(t => <TxnRow key={t.id} txn={t} onEdit={onEdit} onDelete={onDelete} />)}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ── Main view ─────────────────────────────────────────────────────────────

export default function Finance({ transactions, onRefresh }: Props) {
  const [tab,       setTab]       = useState<Tab>('overview')
  const [showDialog, setShowDialog] = useState(false)
  const [editing,   setEditing]   = useState<FinanceTx | undefined>()
  const [currency,  setCurrency]  = useState<'UZS' | 'USD'>('UZS')
  const [overrides, setOverrides] = useState<FinanceOverrides>(() => bridge.getFinanceOverrides())

  const now = new Date()
  const [year,  setYear]  = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const [statsKey, setStatsKey] = useState(0)
  const stats = useMemo(() => bridge.getFinanceStats(), [transactions, overrides, statsKey])

  const handleSaveOverride = useCallback((key: FinanceOverrideKey, amount: number) => {
    bridge.setFinanceOverride(key, amount)
    setOverrides(bridge.getFinanceOverrides())
    setStatsKey(k => k + 1)
    onRefresh()
  }, [onRefresh])

  const handleClearOverride = useCallback((key: FinanceOverrideKey) => {
    bridge.clearFinanceOverride(key)
    setOverrides(bridge.getFinanceOverrides())
    setStatsKey(k => k + 1)
    onRefresh()
  }, [onRefresh])

  // Current month transactions
  const monthTxns = useMemo(() =>
    transactions.filter(t => {
      const d = new Date(t.txnDate + 'T00:00:00')
      return d.getFullYear() === year && d.getMonth() + 1 === month
    }), [transactions, year, month])

  const incomeTxns  = useMemo(() => transactions.filter(t => t.type === 'INCOME'),  [transactions])
  const expenseTxns = useMemo(() => transactions.filter(t => t.type === 'EXPENSE'), [transactions])

  // Build last 6 months for chart
  const chartBars = useMemo((): MonthBar[] => {
    const bars: MonthBar[] = []
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const y = d.getFullYear(), m = d.getMonth() + 1
      const slice = transactions.filter(t => {
        const td = new Date(t.txnDate + 'T00:00:00')
        return td.getFullYear() === y && td.getMonth() + 1 === m && t.currency === currency
      })
      bars.push({
        label: d.toLocaleString('en', { month: 'short' }),
        income:  slice.filter(t => t.type === 'INCOME').reduce((s, t) => s + t.amount, 0),
        expense: slice.filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount, 0),
      })
    }
    return bars
  }, [transactions, currency])

  const prevMonth = () => { if (month === 1) { setMonth(12); setYear(y => y - 1) } else setMonth(m => m - 1) }
  const nextMonth = () => { if (month === 12) { setMonth(1); setYear(y => y + 1) } else setMonth(m => m + 1) }
  const monthLabel = new Date(year, month - 1, 1).toLocaleString('en', { month: 'long', year: 'numeric' })

  const openAdd  = () => { setEditing(undefined); setShowDialog(true) }
  const openEdit = (t: FinanceTx) => { setEditing(t); setShowDialog(true) }

  const handleSave = (data: Omit<FinanceTx, 'id' | 'createdAt'> & { id?: number }) => {
    const result = data.id
      ? bridge.updateTransaction(data as FinanceTx)
      : bridge.addTransaction(data)
    if (result === null) return   // bridge call failed — keep dialog open
    onRefresh()
    setShowDialog(false)
    setEditing(undefined)
  }

  const handleDelete = (id: number) => {
    if (!confirm('Delete this transaction?')) return
    bridge.deleteTransaction(id)
    onRefresh()
  }

  const TABS = [
    { id: 'overview'  as Tab, label: 'Overview',  Icon: BarChart3 },
    { id: 'income'    as Tab, label: 'Income',     Icon: TrendingUp },
    { id: 'expenses'  as Tab, label: 'Expenses',   Icon: TrendingDown },
    { id: 'history'   as Tab, label: 'All History',Icon: List },
  ]

  return (
    <div className="h-full flex flex-col overflow-hidden">

      {/* Header */}
      <div className="px-6 pt-6 pb-4 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <Wallet size={20} className="text-accent" />
          <h1 className="text-xl font-bold text-fg">Finance</h1>
        </div>
        <div className="flex items-center gap-2">
          {/* Currency toggle */}
          <div className="flex gap-1 bg-white/[0.04] rounded-lg p-1 border border-white/[0.06]">
            {(['UZS', 'USD'] as const).map(c => (
              <button key={c} onClick={() => setCurrency(c)}
                className={`px-3 py-1 rounded text-xs font-semibold transition-all ${
                  currency === c ? 'bg-accent/20 text-accent' : 'text-fg-subtle hover:text-fg'
                }`}>{c}</button>
            ))}
          </div>
          <button onClick={openAdd} className="btn-primary flex items-center gap-2 text-sm">
            <Plus size={15} /> Add
          </button>
        </div>
      </div>

      {/* Stats row */}
      <div className="px-6 pb-4 grid grid-cols-4 gap-3 shrink-0">
        {/* Total Balance */}
        {(() => {
          const balVal = (currency === 'UZS' ? stats?.all.balanceUzs : stats?.all.balanceUsd) ?? 0
          const balKey: FinanceOverrideKey = currency === 'UZS' ? 'all.balance_uzs' : 'all.balance_usd'
          return (
            <StatCard label="Total Balance"
              value={currency === 'UZS' ? fmtUzs(balVal) : fmtUsd(balVal)}
              numericValue={balVal}
              overrideKey={balKey}
              overrides={overrides}
              sub={currency === 'UZS'
                ? `+${fmtUzs(stats?.all.incomeUzs ?? 0)} / -${fmtUzs(stats?.all.expenseUzs ?? 0)}`
                : `+${fmtUsd(stats?.all.incomeUsd ?? 0)} / -${fmtUsd(stats?.all.expenseUsd ?? 0)}`}
              color={balVal >= 0 ? '#10b981' : '#ef4444'}
              onSave={handleSaveOverride}
              onClear={handleClearOverride} />
          )
        })()}
        {/* This Month Income */}
        {(() => {
          const incVal = (currency === 'UZS' ? stats?.month.incomeUzs : stats?.month.incomeUsd) ?? 0
          const incKey: FinanceOverrideKey = currency === 'UZS' ? 'month.income_uzs' : 'month.income_usd'
          return (
            <StatCard label="This Month Income"
              value={currency === 'UZS' ? fmtUzs(incVal) : fmtUsd(incVal)}
              numericValue={incVal}
              overrideKey={incKey}
              overrides={overrides}
              color="#10b981"
              onSave={handleSaveOverride}
              onClear={handleClearOverride} />
          )
        })()}
        {/* This Month Expenses */}
        {(() => {
          const expVal = (currency === 'UZS' ? stats?.month.expenseUzs : stats?.month.expenseUsd) ?? 0
          const expKey: FinanceOverrideKey = currency === 'UZS' ? 'month.expense_uzs' : 'month.expense_usd'
          return (
            <StatCard label="This Month Expenses"
              value={currency === 'UZS' ? fmtUzs(expVal) : fmtUsd(expVal)}
              numericValue={expVal}
              overrideKey={expKey}
              overrides={overrides}
              color="#ef4444"
              onSave={handleSaveOverride}
              onClear={handleClearOverride} />
          )
        })()}
        {/* This Month Net — derived, no override */}
        {(() => {
          const netVal = currency === 'UZS'
            ? (stats?.month.incomeUzs ?? 0) - (stats?.month.expenseUzs ?? 0)
            : (stats?.month.incomeUsd ?? 0) - (stats?.month.expenseUsd ?? 0)
          return (
            <StatCard label="This Month Net"
              value={currency === 'UZS' ? fmtUzs(netVal) : fmtUsd(netVal)}
              numericValue={netVal}
              overrideKey={null}
              overrides={overrides}
              color={netVal >= 0 ? '#10b981' : '#ef4444'}
              onSave={handleSaveOverride}
              onClear={handleClearOverride} />
          )
        })()}
      </div>

      {/* Tabs */}
      <div className="px-6 pb-3 flex gap-1 shrink-0 border-b border-white/[0.05]">
        {TABS.map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold transition-all ${
              tab === id
                ? 'bg-accent/15 text-accent border border-accent/30'
                : 'text-fg-muted hover:bg-white/[0.05] hover:text-fg'
            }`}>
            <Icon size={13} /> {label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="flex-1 overflow-y-auto px-6 py-5">

        {/* ── Overview ───────────────────────────────────────────────────── */}
        {tab === 'overview' && (
          <div className="space-y-5">
            <MiniChart bars={chartBars} currency={currency} />
            <CategoryBreakdown
              txns={transactions.filter(t => t.type === 'INCOME')}
              currency={currency} />
            <CategoryBreakdown
              txns={transactions.filter(t => t.type === 'EXPENSE')}
              currency={currency} />
            {transactions.length > 0 && (
              <div>
                <p className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">Recent Transactions</p>
                <div className="space-y-1">
                  {[...transactions].slice(0, 8).map(t => (
                    <TxnRow key={t.id} txn={t} onEdit={openEdit} onDelete={handleDelete} />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── Income history ─────────────────────────────────────────────── */}
        {tab === 'income' && (
          <div className="space-y-5">
            <div className="flex items-center gap-3">
              <div className="card px-4 py-2 flex items-center gap-2">
                <TrendingUp size={14} className="text-emerald-400" />
                <span className="text-xs text-fg-subtle">Total Income</span>
                <span className="text-sm font-bold text-emerald-400 ml-2">
                  {currency === 'UZS' ? fmtUzs(stats?.all.incomeUzs ?? 0) : fmtUsd(stats?.all.incomeUsd ?? 0)}
                </span>
              </div>
              <span className="text-xs text-fg-subtle">{incomeTxns.length} transaction{incomeTxns.length !== 1 ? 's' : ''}</span>
            </div>
            <CategoryBreakdown txns={incomeTxns} currency={currency} />
            <TxnList txns={incomeTxns} onEdit={openEdit} onDelete={handleDelete}
              emptyText="No income recorded yet" />
          </div>
        )}

        {/* ── Expense history ────────────────────────────────────────────── */}
        {tab === 'expenses' && (
          <div className="space-y-5">
            <div className="flex items-center gap-3">
              <div className="card px-4 py-2 flex items-center gap-2">
                <TrendingDown size={14} className="text-red-400" />
                <span className="text-xs text-fg-subtle">Total Expenses</span>
                <span className="text-sm font-bold text-red-400 ml-2">
                  {currency === 'UZS' ? fmtUzs(stats?.all.expenseUzs ?? 0) : fmtUsd(stats?.all.expenseUsd ?? 0)}
                </span>
              </div>
              <span className="text-xs text-fg-subtle">{expenseTxns.length} transaction{expenseTxns.length !== 1 ? 's' : ''}</span>
            </div>
            <CategoryBreakdown txns={expenseTxns} currency={currency} />
            <TxnList txns={expenseTxns} onEdit={openEdit} onDelete={handleDelete}
              emptyText="No expenses recorded yet" />
          </div>
        )}

        {/* ── All History (month navigator) ──────────────────────────────── */}
        {tab === 'history' && (
          <div className="space-y-4">
            {/* Month nav */}
            <div className="flex items-center gap-3">
              <button onClick={prevMonth}
                className="p-1.5 rounded hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
                <ChevronLeft size={16} />
              </button>
              <span className="text-sm font-semibold text-fg min-w-[150px] text-center">{monthLabel}</span>
              <button onClick={nextMonth}
                className="p-1.5 rounded hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
                <ChevronRight size={16} />
              </button>
              <span className="text-xs text-fg-subtle ml-2">{monthTxns.length} transaction{monthTxns.length !== 1 ? 's' : ''}</span>
              <div className="ml-auto flex gap-3 text-xs font-semibold">
                <span className="text-emerald-400">
                  +{fmt(monthTxns.filter(t => t.type === 'INCOME' && t.currency === currency).reduce((s, t) => s + t.amount, 0), currency)}
                </span>
                <span className="text-red-400">
                  -{fmt(monthTxns.filter(t => t.type === 'EXPENSE' && t.currency === currency).reduce((s, t) => s + t.amount, 0), currency)}
                </span>
              </div>
            </div>
            <TxnList txns={monthTxns} onEdit={openEdit} onDelete={handleDelete}
              emptyText={`No transactions in ${monthLabel}`} />
          </div>
        )}
      </div>

      {/* Add / Edit dialog */}
      {showDialog && (
        <TxnDialog
          initial={editing}
          onSave={handleSave}
          onClose={() => { setShowDialog(false); setEditing(undefined) }}
        />
      )}
    </div>
  )
}
