package co.edu.uniminuto.hyeda.rental.service;

import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.shared.company.CompanySettings;
import co.edu.uniminuto.hyeda.shared.company.CompanySettingsService;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private CompanySettingsService companySettingsService;
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        companySettingsService = mock(CompanySettingsService.class);
        CompanySettings settings = new CompanySettings();
        settings.setLateGraceDays(0);
        when(companySettingsService.get()).thenReturn(settings);
        pricingService = new PricingService(companySettingsService);
    }

    @Test
    void billableDaysNeverBelowOne() {
        LocalDate day = LocalDate.of(2026, 9, 7);
        assertThat(pricingService.billableDays(day, day)).isEqualTo(1);
        assertThat(pricingService.billableDays(day, day.plusDays(3))).isEqualTo(3);
    }

    @Test
    void discountRequiresReason() {
        assertThatThrownBy(() -> pricingService.validateDiscount(
                new BigDecimal("100000"), new BigDecimal("10000"), " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void earlyReturnChargesOnlyUsedDays() {
        Rental rental = rentalWithItem(new BigDecimal("10000"), 1, 5);
        PricingService.Quote quote = pricingService.quoteFinal(
                rental, rental.getStartDate().plusDays(2), BigDecimal.ZERO);

        assertThat(quote.total()).isEqualByComparingTo("20000");
        assertThat(quote.lateFees()).isEqualByComparingTo("0");
        assertThat(rental.getActualDays()).isEqualTo(2);
    }

    @Test
    void lateReturnAddsDailyFee() {
        Rental rental = rentalWithItem(new BigDecimal("10000"), 1, 2);
        rental.getItems().getFirst().setLateFeeSnapshot(new BigDecimal("15000"));

        PricingService.Quote quote = pricingService.quoteFinal(
                rental, rental.getStartDate().plusDays(4), BigDecimal.ZERO);

        assertThat(quote.itemsSubtotal()).isEqualByComparingTo("20000");
        assertThat(quote.lateFees()).isEqualByComparingTo("30000");
        assertThat(quote.total()).isEqualByComparingTo("50000");
    }

    private Rental rentalWithItem(BigDecimal unitPrice, int quantity, int days) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        RentalItem item = RentalItem.builder()
                .productId(1L)
                .productName("Andamio")
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lateFeeSnapshot(unitPrice)
                .estimatedDays(days)
                .build();
        Rental rental = Rental.builder()
                .startDate(start)
                .endDate(start.plusDays(days))
                .estimatedDays(days)
                .items(new ArrayList<>(List.of(item)))
                .build();
        pricingService.quoteEstimate(rental);
        return rental;
    }
}
