package in.onlinebank.backend.repository;

import in.onlinebank.backend.entity.AmountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmountRepository extends JpaRepository<AmountEntity, Long> {
}
