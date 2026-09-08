package co.edu.uniminuto.hyeda.shared.company;

import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySettingsService {

    private final CompanySettingsRepository repository;

    public CompanySettingsService(CompanySettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CompanySettings get() {
        return repository.findById(CompanySettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("No hay parametros de empresa configurados"));
    }

    @Transactional
    public CompanySettings update(CompanySettingsRequest request) {
        CompanySettings settings = get();
        settings.setLegalName(request.legalName());
        settings.setTradeName(request.tradeName());
        settings.setNit(request.nit());
        settings.setOwnerName(request.ownerName());
        settings.setTaxRegime(request.taxRegime());
        settings.setPhone(request.phone());
        settings.setEmail(request.email());
        settings.setAddress(request.address());
        settings.setCity(request.city());
        settings.setWhatsappAdmin(request.whatsappAdmin());
        if (request.lateGraceDays() != null) {
            settings.setLateGraceDays(request.lateGraceDays());
        }
        return repository.save(settings);
    }
}
