package in.onlinebank.backend.repository;

import in.onlinebank.backend.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    public AccountEntity findByAccountNo(String accountNo);
}
