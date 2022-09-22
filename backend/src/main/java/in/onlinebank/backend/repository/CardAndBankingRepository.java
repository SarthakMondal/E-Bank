package in.onlinebank.backend.repository;

import in.onlinebank.backend.entity.CardAndBankingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAndBankingRepository extends JpaRepository<CardAndBankingEntity, Long> {
    public CardAndBankingEntity findByCardNo(String cardNo);
    public CardAndBankingEntity findByNetBankingId(String bankingId);

}
