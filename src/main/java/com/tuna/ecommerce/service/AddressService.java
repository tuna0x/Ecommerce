package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.address.ReqCreateAddressDTO;
import com.tuna.ecommerce.domain.request.address.ReqUpdateAddressDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.address.ResAddressDTO;
import com.tuna.ecommerce.repository.AddressRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final UserService userService;

    public User findByUser() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : null;
        return this.userService.findByUsername(email);
    }

    public Address createAddress(ReqCreateAddressDTO req){

        User user= this.findByUser();
        List<Address> addresses = this.addressRepository.findByUserId(user.getId());
        
        Address address = new Address();
        address.setReceiverName(req.getReceiverName());
        address.setPhone(req.getPhone());
        address.setProvince(req.getProvince());
        address.setDistrict(req.getDistrict());
        address.setWard(req.getWard());
        address.setDetail(req.getDetail());
        address.setUser(user);

        // Logic: First address must be default. Otherwise follow request.
        if (addresses.isEmpty()) {
            address.setIsDefault(true);
        } else {
            address.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : false);
        }

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            for (Address add : addresses) {
                add.setIsDefault(false);
            }
            this.addressRepository.saveAll(addresses);
        }

        return this.addressRepository.save(address);
    }

    public Address getAddressById(Long id){
        return this.addressRepository.findById(id).orElse(null);
    }

    public Address UpdateAddress(ReqUpdateAddressDTO req) {
        User user = this.findByUser();
        Address address = this.getAddressById(req.getId());
        if (address != null && address.getUser().getId().equals(user.getId())) {
            address.setReceiverName(req.getReceiverName());
            address.setPhone(req.getPhone());
            address.setProvince(req.getProvince());
            address.setDistrict(req.getDistrict());
            address.setWard(req.getWard());
            address.setDetail(req.getDetail());

            if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
                List<Address> addresses = this.addressRepository.findByUserId(user.getId());
                for (Address add : addresses) {
                    add.setIsDefault(false);
                }
                this.addressRepository.saveAll(addresses);
                address.setIsDefault(true);
            } else if (!Boolean.TRUE.equals(req.getIsDefault()) && Boolean.TRUE.equals(address.getIsDefault())) {
                // If unsetting default, we might want to ensure at least one remains default, 
                // but for now we follow the request.
                address.setIsDefault(false);
            }

            address = this.addressRepository.save(address);
        }
        return address;
    }

    public ResultPaginationDTO getAllAddress(Specification<Address> spec, Pageable page) {
        User user = this.findByUser();
        Specification<Address> userSpec = (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"),
                user.getId());

        Specification<Address> combinedSpec = (spec == null) ? userSpec : spec.and(userSpec);

        Page<Address> address = this.addressRepository.findAll(combinedSpec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(address.getNumber() + 1);
        meta.setPageSize(address.getSize());
        meta.setPages(address.getTotalPages());
        meta.setTotal(address.getTotalElements());

        List<ResAddressDTO> list = address.getContent().stream().map(item -> this.convertToAddressDTO(item))
                .collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public void deleteById(Long id) {
        User user = this.findByUser();
        Address address = this.getAddressById(id);
        if (address != null && address.getUser().getId().equals(user.getId())) {
            this.addressRepository.deleteById(id);
        }
    }

    public void setDefaultAddress(long addressId) {
        User user = this.findByUser();
        Address targetAddress = this.getAddressById(addressId);

        if (targetAddress != null && targetAddress.getUser().getId().equals(user.getId())) {
            List<Address> addresses = this.addressRepository.findByUserId(user.getId());

            for (Address add : addresses) {
                add.setIsDefault(add.getId().equals(addressId));
            }

            addressRepository.saveAll(addresses);
        }
    }

    public ResAddressDTO convertToAddressDTO(Address address){
        ResAddressDTO res= new ResAddressDTO();
        ResAddressDTO.UserInner userInner= new ResAddressDTO.UserInner();

        res.setId(address.getId());
        res.setReceiverName(address.getReceiverName());
        res.setPhone(address.getPhone());
        res.setProvince(address.getProvince());
        res.setDistrict(address.getDistrict());
        res.setWard(address.getWard());
        res.setDetail(address.getDetail());
        res.setIsDefault(Boolean.TRUE.equals(address.getIsDefault()));

        User user = this.findByUser();
        userInner.setId(user.getId());
        userInner.setEmail(user.getEmail());
        if (user.getUserProfile() != null) {
            userInner.setName(user.getUserProfile().getName());
        }
        res.setUserInner(userInner);
        return res;
    }
}
