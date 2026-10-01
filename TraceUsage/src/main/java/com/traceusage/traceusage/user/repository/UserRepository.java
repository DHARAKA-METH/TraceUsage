
package com.traceusage.traceusage.user.repository;

import com.traceusage.traceusage.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}