package co.edu.uniminuto.hyeda.modules.clientes.mapper;

import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerDto;
import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerRequest;
import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerDto toDto(Customer entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idCopyPath", ignore = true)
    @Mapping(target = "reputationScore", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idCopyPath", ignore = true)
    @Mapping(target = "reputationScore", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget Customer entity, CustomerRequest request);
}
