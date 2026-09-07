export type CustomerType = 'NUEVO' | 'FRECUENTE' | 'CONFIANZA'

export type RentalStatus =
  | 'BORRADOR'
  | 'RESERVADO'
  | 'DOCUMENTO_EMITIDO'
  | 'EN_CURSO'
  | 'DEVUELTO'
  | 'FACTURACION_FINAL'
  | 'PENDIENTE_PAGO'
  | 'CERRADO'
  | 'CANCELADO'

export type DocumentType = 'CONTRATO' | 'REMISION'

export type GeneratedDocumentType =
  | 'CONTRATO'
  | 'REMISION'
  | 'DEVOLUCION'
  | 'FACTURA'
  | 'REPORTE'

export type PaymentType = 'EFECTIVO' | 'QR' | 'ABONO'
export type PaymentStatus = 'PENDIENTE' | 'CONFIRMADO' | 'RECHAZADO' | 'ANULADO'
export type MovementType = 'INGRESO' | 'GASTO'
export type DebtStatus = 'ABIERTA' | 'PARCIAL' | 'PAGADA' | 'VENCIDA'

export interface User {
  id?: number
  username: string
  fullName?: string
  email?: string
  active?: boolean
  roles: string[]
  permissions?: string[]
  token?: string
}

export interface LoginResponse {
  token: string
  username: string
  fullName: string
  roles: string[]
  permissions: string[]
}

export interface Customer {
  id: number
  document: string
  name: string
  phone?: string
  email?: string
  address?: string
  idCopyPath?: string
  dataConsent: boolean
  reputationScore?: number
  customerType?: CustomerType
}

export interface CustomerRequest {
  document: string
  name: string
  phone?: string
  email?: string
  address?: string
  dataConsent: boolean
  customerType?: CustomerType
}

export interface Product {
  id: number
  name: string
  description?: string
  dailyPrice: number
  stockTotal: number
  stockAvailable: number
  minStockThreshold?: number
  active: boolean
  lowStock: boolean
}

export interface ProductRequest {
  name: string
  description?: string
  dailyPrice: number
  stockTotal: number
  minStockThreshold?: number
  active?: boolean
}

export interface RentalItem {
  id?: number
  productId: number
  productName?: string
  quantity: number
  unitPrice?: number
  estimatedDays?: number
  actualDays?: number
}

export interface Rental {
  id: number
  customerId: number
  customerName?: string
  status: RentalStatus
  startDate: string
  endDate?: string
  actualReturnDate?: string
  deliveryAddress?: string
  deliveryReference?: string
  deliveryLat?: number
  deliveryLng?: number
  haulingEnabled?: boolean
  haulingCost?: number
  discountAmount?: number
  discountReason?: string
  documentType?: DocumentType
  skipContract?: boolean
  totalAmount?: number
  finalAmount?: number
  estimatedDays?: number
  actualDays?: number
  items: RentalItem[]
}

export interface CreateRentalRequest {
  customerId: number
  startDate: string
  estimatedDays: number
  deliveryAddress?: string
  deliveryReference?: string
  deliveryLat?: number
  deliveryLng?: number
  haulingEnabled?: boolean
  haulingCost?: number
  discountAmount?: number
  discountReason?: string
  documentType?: DocumentType
  skipContract?: boolean
  items: { productId: number; quantity: number }[]
}

export interface Payment {
  id: number
  rentalId: number
  paymentType: PaymentType
  amount: number
  status: PaymentStatus
  reference?: string
  paidAt?: string
}

export interface PaymentRequest {
  rentalId: number
  paymentType: PaymentType
  amount: number
  reference?: string
}

export interface Document {
  id: number
  rentalId: number
  documentType: GeneratedDocumentType
  serialNumber?: string
  filePath?: string
  generatedAt?: string
}

export interface Debt {
  id: number
  customerId: number
  customerName?: string
  rentalId?: number
  balance: number
  dueDate?: string
  overdueDays?: number
  status: DebtStatus
}

export interface FinancialMovement {
  id: number
  movementType: MovementType
  amount: number
  description?: string
  category?: string
  movementDate: string
}

export interface Role {
  id: number
  name: string
  description?: string
  permissions: string[]
}

export interface CreateUserRequest {
  username: string
  password?: string
  fullName?: string
  email?: string
  roles: string[]
}

export interface ChartPoint {
  name?: string
  label?: string
  value?: number
  count?: number
  amount?: number
  date?: string
  productName?: string
  customerName?: string
  [key: string]: string | number | undefined
}

export interface Dashboard {
  rentalsActive: number
  rentalsClosed: number
  lowStockProducts: number
  openDebts: number
  incomeTotal: number
  expenseTotal: number
  topProducts: ChartPoint[]
  topCustomersByCount: ChartPoint[]
  topCustomersByValue: ChartPoint[]
  incomeByDay: ChartPoint[]
}

export interface BackgroundCheckRow {
  id: number
  customer?: { id: number; name?: string } | null
  document: string
  result?: string
  provider?: string
  evidencePath?: string
  checkedAt?: string
}
