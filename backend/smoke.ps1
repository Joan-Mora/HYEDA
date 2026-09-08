$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8081/api'

function Show($label, $value) { Write-Host ("  {0}: {1}" -f $label, $value) }

# Sufijo unico para poder correr el smoke varias veces sobre la misma base.
$run = (Get-Date).ToString('HHmmss')

# Muestra el cuerpo del error del backend en vez del generico de PowerShell.
trap [System.Net.WebException] {
    $response = $_.Exception.Response
    if ($response) {
        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        Write-Host ("  ERROR: " + $reader.ReadToEnd()) -ForegroundColor Red
    }
    break
}

Write-Host '== login =='
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' `
    -Body (@{ username = 'admin'; password = 'AdminHyeda2026!' } | ConvertTo-Json)
$h = @{ Authorization = "Bearer $($login.accessToken)" }
Show 'usuario' $login.user.username

Write-Host '== producto =='
$product = Invoke-RestMethod -Method Post -Uri "$base/inventario" -Headers $h -ContentType 'application/json' `
    -Body (@{ sku = "AND-$run"; name = 'Andamio tubular 1.50m'; category = 'Andamios'; unit = 'UNIDAD';
              dailyPrice = 8000; lateFeePerDay = 10000; replacementValue = 350000;
              stockTotal = 20; minStockThreshold = 3; active = $true } | ConvertTo-Json)
Show 'producto' "$($product.id) $($product.name) stock=$($product.stockAvailable)"

Write-Host '== cliente =='
$customer = Invoke-RestMethod -Method Post -Uri "$base/clientes" -Headers $h -ContentType 'application/json' `
    -Body (@{ document = "11$run"; documentType = 'CC'; name = 'Pedro Ramirez Gomez';
              phone = '0000000000'; email = 'pedro@example.com'; customerType = 'NUEVO';
              dataConsent = $true } | ConvertTo-Json)
Show 'cliente' "$($customer.id) $($customer.name) tipo=$($customer.customerType)"

Write-Host '== elegibilidad =='
$elig = Invoke-RestMethod -Uri "$base/alquileres/elegibilidad/$($customer.id)" -Headers $h
Show 'politica' $elig.suggestedPolicy
Show 'avisos' ($elig.warnings -join ' / ')

Write-Host '== alquiler =='
$rental = Invoke-RestMethod -Method Post -Uri "$base/alquileres" -Headers $h -ContentType 'application/json' `
    -Body (@{ customerId = $customer.id; documentPolicy = 'CONTRATO';
              startDate = '2026-09-01'; endDate = '2026-09-11';
              deliveryAddress = 'Calle 20 # 14-30, Granada, Meta';
              haulingEnabled = $true; haulingCost = 30000;
              discountAmount = 10000; discountReason = 'Cliente referido';
              dataConsentAccepted = $true;
              items = @(@{ productId = $product.id; quantity = 4 }) } | ConvertTo-Json -Depth 5)
Show 'alquiler' "$($rental.code) estado=$($rental.status) total=$($rental.estimatedTotal)"

Write-Host '== contrato PDF =='
$doc = Invoke-RestMethod -Method Post -Uri "$base/documentos/alquiler/$($rental.id)/emitir" -Headers $h
Show 'documento' "$($doc.serialNumber) $($doc.documentTypeLabel)"
Invoke-WebRequest -Uri "$base/documentos/$($doc.id)/descargar" -Headers $h -OutFile "$PSScriptRoot\..\smoke-contrato.pdf" | Out-Null
Show 'pdf' ((Get-Item "$PSScriptRoot\..\smoke-contrato.pdf").Length.ToString() + ' bytes')

Write-Host '== pago parcial en efectivo =='
$pay = Invoke-RestMethod -Method Post -Uri "$base/pagos/alquiler/$($rental.id)/efectivo" -Headers $h `
    -ContentType 'application/json' -Body (@{ amount = 200000; amountTendered = 200000 } | ConvertTo-Json)
Show 'pago' "$($pay.paymentType) $($pay.amount) vuelto=$($pay.changeGiven)"

Write-Host '== entrega =='
$rental = Invoke-RestMethod -Method Post -Uri "$base/alquileres/$($rental.id)/entregar" -Headers $h
Show 'estado' $rental.status

Write-Host '== devolucion anticipada (8 dias) =='
$rental = Invoke-RestMethod -Method Post -Uri "$base/alquileres/$($rental.id)/devolucion" -Headers $h `
    -ContentType 'application/json' `
    -Body (@{ returnDate = '2026-09-09'; notes = 'Todo en orden';
              incidents = @() } | ConvertTo-Json -Depth 5)
Show 'estado' $rental.status
Show 'final' $rental.finalAmount

Write-Host '== acta de devolucion =='
$devDoc = Invoke-RestMethod -Method Post -Uri "$base/documentos/alquiler/$($rental.id)/devolucion" -Headers $h
Show 'documento' $devDoc.serialNumber

Write-Host '== cuenta =='
$account = Invoke-RestMethod -Uri "$base/pagos/alquiler/$($rental.id)" -Headers $h
Show 'debe' $account.balance
Show 'excedente' $account.surplus

Write-Host '== liquidar =='
$account = Invoke-RestMethod -Method Post -Uri "$base/pagos/alquiler/$($rental.id)/liquidar" -Headers $h `
    -ContentType 'application/json' -Body (@{ surplusHandling = 'SALDO_A_FAVOR' } | ConvertTo-Json)
Show 'estado' $account.status
Show 'saldo' $account.balance

Write-Host '== analiticas =='
$dash = Invoke-RestMethod -Uri "$base/analiticas/tablero?desde=2026-09-01&hasta=2026-09-30" -Headers $h
Show 'abiertos' $dash.kpis.rentalsOpened
Show 'facturado' $dash.kpis.billed
Show 'top' ($dash.topProducts | ForEach-Object { $_.name })

Write-Host '== reporte PDF/Excel =='
Invoke-WebRequest -Uri "$base/analiticas/reporte.pdf?desde=2026-09-01&hasta=2026-09-30" -Headers $h -OutFile "$PSScriptRoot\..\smoke-reporte.pdf" | Out-Null
Invoke-WebRequest -Uri "$base/analiticas/reporte.xlsx?desde=2026-09-01&hasta=2026-09-30" -Headers $h -OutFile "$PSScriptRoot\..\smoke-reporte.xlsx" | Out-Null
Show 'pdf' ((Get-Item "$PSScriptRoot\..\smoke-reporte.pdf").Length.ToString() + ' bytes')
Show 'xlsx' ((Get-Item "$PSScriptRoot\..\smoke-reporte.xlsx").Length.ToString() + ' bytes')

Write-Host '== finanzas =='
$fin = Invoke-RestMethod -Uri "$base/finanzas/resumen?desde=2026-09-01&hasta=2026-09-30" -Headers $h
Show 'ingresos' $fin.income
Show 'gastos' $fin.expense

Write-Host '== notificaciones =='
$notif = Invoke-RestMethod -Uri "$base/notificaciones" -Headers $h
Show 'total' $notif.totalElements
$notif.content | ForEach-Object { Show $_.templateCode $_.status }

Write-Host 'OK'
