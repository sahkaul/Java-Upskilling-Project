package com.example.accounts.reository;

import com.example.accounts.entity.AccessControlList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessControlListRepository extends JpaRepository<AccessControlList, Long> {

    List<AccessControlList> findByAccountAccountId(Long accountId);

    List<AccessControlList> findByUserIdAndAccountAccountId(Long userId, Long accountId);

    Optional<AccessControlList> findByAccountAccountIdAndUserId(Long accountId, Long userId);

    List<AccessControlList> findByCustomerCustomerId(Long customerId);

    boolean existsByAccountAccountIdAndUserId(Long accountId, Long userId);
}

