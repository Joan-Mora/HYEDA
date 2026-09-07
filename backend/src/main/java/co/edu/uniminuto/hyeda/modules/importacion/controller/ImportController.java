package co.edu.uniminuto.hyeda.modules.importacion.controller;

import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;
import co.edu.uniminuto.hyeda.modules.importacion.service.ImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/importacion")
@PreAuthorize("hasAnyAuthority('IMPORTACION','ADMIN')")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/productos-docx")
    public Map<String, Object> products(@RequestParam("file") MultipartFile file) {
        return importService.importProductsFromDocx(file);
    }

    @PostMapping("/inventario-excel")
    public Map<String, Object> inventory(@RequestParam("file") MultipartFile file) {
        return importService.importInventoryFromExcel(file);
    }

    @PostMapping("/clientes-excel")
    public Map<String, Object> customers(@RequestParam("file") MultipartFile file) {
        return importService.importCustomersFromExcel(file);
    }

    @PostMapping("/movimientos-excel")
    public Map<String, Object> movements(@RequestParam("file") MultipartFile file,
                                         @RequestParam MovementType type) {
        return importService.importMovementsFromExcel(file, type);
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(@RequestParam(value = "products", required = false) MultipartFile products,
                                    @RequestParam(value = "inventory", required = false) MultipartFile inventory,
                                    @RequestParam(value = "customers", required = false) MultipartFile customers,
                                    @RequestParam(value = "ingresos", required = false) MultipartFile ingresos,
                                    @RequestParam(value = "gastos", required = false) MultipartFile gastos) {
        return importService.importAllSeed(products, inventory, customers, ingresos, gastos);
    }
}
