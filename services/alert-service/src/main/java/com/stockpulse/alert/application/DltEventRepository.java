package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.DltEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DltEventRepository extends JpaRepository<DltEvent, Long> {}
