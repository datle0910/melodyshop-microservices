package com.melodyshop.user.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.user.dto.AddressDTO;
import com.melodyshop.user.entity.Address;
import com.melodyshop.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private static final int MAX_ADDRESSES = 10;

    public List<AddressDTO> getAddresses(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AddressDTO createAddress(String userId, AddressDTO dto) {
        if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new BadRequestException("Tối đa " + MAX_ADDRESSES + " địa chỉ");
        }

        Address address = Address.builder()
                .userId(userId)
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .province(dto.getProvince())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .addressDetail(dto.getAddressDetail())
                .isDefault(false)
                .build();

        // If first address, set as default
        if (addressRepository.countByUserId(userId) == 0) {
            address.setIsDefault(true);
        }

        return toDTO(addressRepository.save(address));
    }

    @Transactional
    public AddressDTO updateAddress(String userId, String addressId, AddressDTO dto) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        address.setFullName(dto.getFullName());
        address.setPhone(dto.getPhone());
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setWard(dto.getWard());
        address.setAddressDetail(dto.getAddressDetail());

        return toDTO(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String userId, String addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (address.getIsDefault()) {
            throw new BadRequestException("Không thể xóa địa chỉ mặc định. Hãy đặt địa chỉ khác làm mặc định trước.");
        }

        addressRepository.delete(address);
    }

    @Transactional
    public AddressDTO setDefault(String userId, String addressId) {
        Address newDefault = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Unset current default
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(old -> {
                    old.setIsDefault(false);
                    addressRepository.save(old);
                });

        newDefault.setIsDefault(true);
        return toDTO(addressRepository.save(newDefault));
    }

    private AddressDTO toDTO(Address a) {
        return AddressDTO.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .province(a.getProvince())
                .district(a.getDistrict())
                .ward(a.getWard())
                .addressDetail(a.getAddressDetail())
                .isDefault(a.getIsDefault())
                .build();
    }
}
