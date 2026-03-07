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
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.repository.AddressRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public User findByUser(){
        String email =this.securityUtil.getCurrentUserLogin().isPresent() ? this.securityUtil.getCurrentUserLogin().get():null;
        return this.userService.findByUsername(email);
    }

    public Address createAddress(ReqCreateAddressDTO req){

        User user= this.findByUser();

        Address address= new Address();
        address.setReceiverName(req.getReceiverName());
        address.setPhone(req.getPhone());
        address.setProvince(req.getProvince());
        address.setDistrict(req.getDistrict());
        address.setWard(req.getWard());
        address.setDetail(req.getDetail());
        address.setDefault(false);
        address.setUser(user);
        return this.addressRepository.save(address);
    }

    public Address getAddressById(Long id){
        return this.addressRepository.findById(id).orElse(null);
    }

    public Address UpdateAddress(ReqUpdateAddressDTO req){
        Address address= this.getAddressById(req.getId());
        if (address!=null) {
        address.setReceiverName(req.getReceiverName());
        address.setPhone(req.getPhone());
        address.setProvince(req.getProvince());
        address.setDistrict(req.getDistrict());
        address.setWard(req.getWard());
        address.setDetail(req.getDetail());
        address=this.addressRepository.save(address);
        }
        return address;
    }

        public ResultPaginationDTO getAllAddress(Specification<Address> spec, Pageable page) {
        Page<Address> address = this.addressRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(address.getNumber() + 1);
        meta.setPageSize(address.getSize());
        meta.setPages(address.getTotalPages());
        meta.setTotal(address.getTotalElements());

        List<ResAddressDTO> list=address.getContent().stream().map(item->this.convertToAddressDTO(item)).collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public void deleteById(Long id){
        this.addressRepository.deleteById(id);
    }

    public void setDefaultAddress(long addressId){
        User user= this.findByUser();

        List<Address> addresses= this.addressRepository.findByUserId(user.getId());

        for(Address add: addresses){
            add.setDefault(false);
        }

     Address address= this.addressRepository.findById(addressId).orElse(null);
    address.setDefault(true);
    addressRepository.saveAll(addresses);

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
        res.setDefault(address.isDefault());

        User user= this.findByUser();
        userInner.setId(user.getId());
        userInner.setEmail(user.getEmail());
        userInner.setName(user.getName());
        res.setUserInner(userInner);
    return res;
    }
}
