package in.onlinebank.backend.repository;

import in.onlinebank.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE `customer_id` SET `next_val` = (SELECT `next_val`)-1 WHERE `next_val` != 1000000", nativeQuery = true)
    public void sequenceRollBack();

    public UserEntity findByUserEmail(String userEmail);
}
