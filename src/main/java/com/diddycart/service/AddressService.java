package com.diddycart.service;

import com.diddycart.dto.address.AddressDTO;
import com.diddycart.models.Address;
import com.diddycart.models.User;
import com.diddycart.repository.AddressRepository;
import com.diddycart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    // Fetch all addresses for a user
    public List<Address> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return addressRepository.findByUser(user);
    }

    // Fetch address by ID
    public Address getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this address");
        }

        return address;
    }

    // Create new address
    @Transactional
    public Address createAddress(Long userId, AddressDTO addressDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setLabel(addressDTO.getType());
        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPincode(addressDTO.getPincode());

        return addressRepository.save(address);
    }

    // Update existing address
    @Transactional
    public Address updateAddress(Long addressId, Long userId, AddressDTO addressDTO) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this address");
        }

        // Update fields
        address.setLabel(addressDTO.getType());
        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPincode(addressDTO.getPincode());

        return addressRepository.save(address);
    }

    // Delete address
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this address");
        }

        addressRepository.delete(address);
    }
}
