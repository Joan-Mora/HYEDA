export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type UserProfile = {
  id: number
  username: string
  fullName: string
  email?: string
  phone?: string
  mustChangePassword: boolean
  roles: string[]
  permissions: string[]
}

export type SessionResponse = {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  user: UserProfile
}

export type ProductView = {
  id: number
  sku?: string
  name: string
  description?: string
  category?: string
  unit: string
  dailyPrice: number
  lateFeePerDay: number
  replacementValue: number
  stockTotal: number
  stockAvailable: number
  stockInUse: number
  minStockThreshold: number
  lowStock: boolean
  outOfStock: boolean
  active: boolean
}

export type CustomerView = {
  id: number
  document: string
  documentType: string
  name: string
  phone?: string
  email?: string
  hasIdCopy: boolean
  dataConsent: boolean
  reputationScore: number
  customerType: string
  rentalCount: number
  canSkipContract: boolean
  notes?: string
  active: boolean
  addresses: AddressView[]
}

export type AddressView = {
  id?: number
  label?: string
  address: string
  reference?: string
  lat?: number
  lng?: number
  mapsUrl?: string
  isDefault: boolean
}

export type RentalRow = {
  id: number
  code: string
  status: string
  documentPolicy: string
  customerId: number
  customerName: string
  customerDocument: string
  startDate: string
  endDate: string
  actualReturnDate?: string
  amount: number
  pendingAmount: number
  itemCount: number
  overdue: boolean
  overdueDays: number
}

export type RentalView = RentalRow & {
  customerPhone?: string
  estimatedDays: number
  actualDays?: number
  deliveryAddress: string
  deliveryReference?: string
  deliveryMapsUrl?: string
  haulingEnabled: boolean
  haulingCost: number
  discountAmount: number
  discountReason?: string
  itemsSubtotal: number
  estimatedTotal: number
  lateFeeTotal: number
  incidentChargeTotal: number
  finalAmount?: number
  currentAmount: number
  paidAmount: number
  refundAmount: number
  signed: boolean
  notes?: string
  items: RentalItemView[]
}

export type RentalItemView = {
  id: number
  productId: number
  productName: string
  quantity: number
  quantityReturned: number
  unitPrice: number
  lineEstimated: number
}

export type QuoteView = {
  days: number
  itemsSubtotal: number
  hauling: number
  discount: number
  total: number
  lines: { productId: number; productName: string; quantity: number; unitPrice: number; availableStock: number; lineTotal: number }[]
}

export type RentalEligibility = {
  customerId: number
  customerName: string
  customerType: string
  reputationScore: number
  rentalCount: number
  canSkipContract: boolean
  hasIdCopy: boolean
  hasDataConsent: boolean
  openDebt: number
  availableCredit: number
  suggestedPolicy: string
  warnings: string[]
}

export type Dashboard = {
  from: string
  to: string
  kpis: {
    rentalsOpened: number
    successfulRentals: number
    activeRentals: number
    overdueRentals: number
    problemReturns: number
    income: number
    expense: number
    profit: number
    billed: number
    openDebt: number
    averageTicket: number
    unitsOut: number
    distinctCustomers: number
  }
  topProducts: { productId: number; name: string; units: number; amount: number }[]
  topCustomersByCount: { customerId: number; name: string; document: string; rentals: number; amount: number }[]
  topCustomersByValue: { customerId: number; name: string; document: string; rentals: number; amount: number }[]
  stockouts: { productId: number; name: string; available: number; total: number; outOfStock: boolean }[]
  incidents: { condition: string; count: number; charged: number }[]
  revenueSeries: { date: string; billed: number; income: number; expense: number }[]
  rentalsByStatus: { status: string; count: number }[]
}

export type MovementView = {
  id: number
  movementType: 'INGRESO' | 'GASTO'
  amount: number
  description?: string
  category?: string
  movementDate: string
  rentalCode?: string
  automatic: boolean
}

export type DebtRow = {
  id: number
  customerId: number
  customerName: string
  customerPhone?: string
  rentalId?: number
  rentalCode?: string
  originalAmount: number
  balance: number
  dueDate?: string
  overdueDays: number
  agingBucket: string
  status: string
}

export type DocumentRow = {
  id: number
  documentType: string
  documentTypeLabel: string
  serialNumber: string
  title: string
  rentalId?: number
  rentalCode?: string
  customerName?: string
  signed: boolean
  generatedAt: string
}

export type ImportJobView = {
  id: number
  jobType: string
  fileName: string
  format: string
  status: string
  rowsTotal: number
  rowsOk: number
  rowsError: number
  errorLog?: string
  hasErrorReport: boolean
  startedAt: string
}

export type PreviewResponse = {
  jobType: string
  fileName: string
  format: string
  headers: string[]
  suggestedMapping: Record<string, string>
  requiredFields: string[]
  rows: { rowNumber: number; values: Record<string, string>; errors: string[] }[]
  totalRows: number
  validRows: number
  errorRows: number
}

export type CustomerProfile = {
  customer: CustomerView
  activeRentals: { id: number; code: string; status: string; startDate: string; endDate: string; amount: number; overdue: boolean }[]
  finishedRentals: { id: number; code: string; status: string; amount: number }[]
  itemsInPossession: { productName: string; quantity: number; rentalCode: string; dueDate: string; overdue: boolean }[]
  openDebts: { id: number; rentalCode?: string; balance: number; overdueDays: number; status: string }[]
  totalDebt: number
  availableCredit: number
  lifetimeValue: number
  reputationHistory: { eventType: string; delta: number; scoreAfter: number; reason?: string; createdAt: string }[]
  backgroundChecks: { id: number; status: string; result?: string; hasEvidence: boolean; checkedAt: string }[]
}
